package dev.jarful.data

import dev.jarful.domain.Dates
import dev.jarful.domain.Ids
import dev.jarful.domain.RoutineScheduler
import dev.jarful.domain.TaskTree
import dev.jarful.model.AppData
import dev.jarful.model.INBOX_ID
import dev.jarful.model.Routine
import dev.jarful.model.SCHEMA_VERSION
import dev.jarful.model.Settings
import dev.jarful.model.Task
import dev.jarful.model.Ticket
import dev.jarful.model.TicketState
import dev.jarful.platform.FileStore
import dev.jarful.platform.ioDispatcher
import dev.jarful.platform.nowMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

const val DATA_FILE = "jarful-data.json"

val json = Json { ignoreUnknownKeys = true; prettyPrint = false; encodeDefaults = true }

/** Single source of truth. All mutations go through [mutate] which persists atomically (FR-12.1). */
class Store(
    private val file: FileStore?,
    private val scope: CoroutineScope,
    private val inboxTitle: String,
) {
    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> = _data

    /** Undo stack of previous snapshots (FR-2.5, FR-4.5). */
    private val undoStack = ArrayDeque<AppData>()
    private var saveJob: Job? = null

    fun load() {
        val text = try { file?.read() } catch (_: Throwable) { null }
        val parsed = text?.let { runCatching { json.decodeFromString(AppData.serializer(), it) }.getOrNull() }
        val base = migrate(parsed ?: AppData())
        _data.value = base.copy(tasks = TaskTree.ensureInbox(base.tasks, inboxTitle, nowMillis()))
        prepareRoutines()
    }

    private fun migrate(d: AppData): AppData = when {
        d.schemaVersion < SCHEMA_VERSION -> d.copy(schemaVersion = SCHEMA_VERSION)
        else -> d
    }

    fun exportJson(): String = Json { prettyPrint = true; encodeDefaults = true }.encodeToString(AppData.serializer(), _data.value)

    fun importJson(text: String): Boolean {
        val parsed = runCatching { json.decodeFromString(AppData.serializer(), text) }.getOrNull() ?: return false
        mutate(undoable = true) { migrate(parsed).copy(tasks = TaskTree.ensureInbox(parsed.tasks, inboxTitle, nowMillis())) }
        return true
    }

    fun mutate(undoable: Boolean = false, f: (AppData) -> AppData) {
        val before = _data.value
        val after = f(before)
        if (after === before) return
        if (undoable) { undoStack.addLast(before); while (undoStack.size > 50) undoStack.removeFirst() }
        _data.value = after
        scheduleSave()
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()

    fun undo(): Boolean {
        val prev = undoStack.removeLastOrNull() ?: return false
        _data.value = prev
        scheduleSave()
        return true
    }

    private fun scheduleSave() {
        val f = file ?: return
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(150)
            val snapshot = _data.value
            withContext(ioDispatcher) {
                runCatching { f.writeAtomic(json.encodeToString(AppData.serializer(), snapshot)) }
            }
        }
    }

    fun flush() {
        val f = file ?: return
        saveJob?.cancel()
        runCatching { f.writeAtomic(json.encodeToString(AppData.serializer(), _data.value)) }
    }

    // ----- Routines / preparation (FR-6.3) -----

    fun prepareRoutines() {
        mutate { RoutineScheduler.generateAll(it, Dates.now(), nowMillis()) }
    }

    // ----- Tasks -----

    fun addTask(parentId: String?, title: String, afterId: String? = null): Task? {
        if (title.isBlank()) return null
        var created: Task? = null
        mutate(undoable = true) { d ->
            val (tasks, t) = TaskTree.add(d.tasks, parentId, title, nowMillis(), afterId)
            created = t
            d.copy(tasks = tasks)
        }
        return created
    }

    fun addTasksFromText(parentId: String?, text: String) =
        mutate(undoable = true) { it.copy(tasks = TaskTree.addManyFromText(it.tasks, parentId, text, nowMillis())) }

    fun renameTask(id: String, title: String) {
        if (title.isBlank()) return
        mutate(undoable = true) { it.copy(tasks = TaskTree.update(it.tasks, id, nowMillis()) { t -> t.copy(title = title.trim()) }) }
    }

    fun setTaskEstimate(id: String, estimateMin: Int?, timeboxMin: Int?) =
        mutate(undoable = true) { it.copy(tasks = TaskTree.update(it.tasks, id, nowMillis()) { t -> t.copy(estimateMin = estimateMin, timeboxMin = timeboxMin) }) }

    fun toggleTaskDone(id: String) {
        mutate(undoable = true) { d ->
            val t = TaskTree.byId(d.tasks, id) ?: return@mutate d
            val done = !t.done
            val now = nowMillis()
            var tickets = d.tickets
            if (done) {
                // completing the task completes today's pending ticket for it too
                tickets = tickets.map { k ->
                    if (k.taskId == id && k.date == Dates.iso(Dates.today()) && !k.isDone) k.copy(state = TicketState.DONE, doneAt = now) else k
                }
            }
            d.copy(tasks = TaskTree.setDone(d.tasks, id, done, now), tickets = tickets)
        }
    }

    fun deleteTask(id: String) = mutate(undoable = true) { d ->
        val (tasks, removed) = TaskTree.delete(d.tasks, id)
        val removedIds = removed.map { it.id }.toSet()
        d.copy(tasks = tasks, tickets = d.tickets.filter { it.taskId !in removedIds || it.isDone })
    }

    fun moveTask(id: String, delta: Int) = mutate(undoable = true) { it.copy(tasks = TaskTree.moveWithinSiblings(it.tasks, id, delta)) }

    fun reparentTask(id: String, newParentId: String?) =
        mutate(undoable = true) { it.copy(tasks = TaskTree.reparent(it.tasks, id, newParentId, nowMillis())) }

    // ----- Tickets (FR-3) -----

    private fun categoryFor(tasks: List<Task>, t: Task): String =
        TaskTree.pathTo(tasks, t.parentId).lastOrNull()?.title ?: ""

    /** Creates today's ticket for the task unless one already exists (FR-3.1). Returns the ticket. */
    fun ticketize(taskId: String, date: String = Dates.iso(Dates.today())): Ticket? {
        var result: Ticket? = null
        mutate(undoable = true) { d ->
            val t = TaskTree.byId(d.tasks, taskId) ?: return@mutate d
            val existing = d.tickets.firstOrNull { it.taskId == taskId && it.date == date }
            if (existing != null) { result = existing; return@mutate d }
            val order = (d.tickets.filter { it.date == date }.maxOfOrNull { it.order } ?: -1) + 1
            val k = Ticket(
                id = Ids.next("k"), date = date, taskId = t.id, category = categoryFor(d.tasks, t), title = t.title,
                estimateMin = t.estimateMin, timeboxMin = t.timeboxMin, order = order,
            )
            result = k
            d.copy(tickets = d.tickets + k)
        }
        return result
    }

    /** Ticketizes all open direct children of [parentId] (FR-1.4). */
    fun ticketizeColumn(parentId: String?, date: String = Dates.iso(Dates.today())) {
        val d = data.value
        TaskTree.childrenOf(d.tasks, parentId).filter { !it.done && it.id != INBOX_ID }.forEach { ticketize(it.id, date) }
    }

    /** Adds ad-hoc tickets from refocus lines at the front of today (FR-7). Returns the created tickets. */
    fun refocus(lines: List<String>, date: String = Dates.iso(Dates.today())): List<Ticket> {
        val clean = lines.map { it.trim() }.filter { it.isNotBlank() }
        if (clean.isEmpty()) return emptyList()
        val created = ArrayList<Ticket>()
        mutate(undoable = true) { d ->
            var tasks = d.tasks
            val now = nowMillis()
            val minOrder = (d.tickets.filter { it.date == date && !it.isDone }.minOfOrNull { it.order } ?: 0)
            val newTickets = clean.mapIndexed { i, line ->
                val (t2, task) = TaskTree.add(tasks, INBOX_ID, line, now)
                tasks = t2
                Ticket(
                    id = Ids.next("k"), date = date, taskId = task.id, category = inboxTitle, title = task.title,
                    order = minOrder - clean.size + i,
                )
            }
            created.addAll(newTickets)
            d.copy(tasks = tasks, tickets = d.tickets + newTickets)
        }
        return created
    }

    fun startTicket(id: String) = mutate { d ->
        val now = nowMillis()
        d.copy(tickets = d.tickets.map {
            when {
                it.id == id && !it.isDone -> it.copy(state = TicketState.RUNNING, startedAt = it.startedAt ?: now)
                it.state == TicketState.RUNNING && it.id != id -> it.copy(state = TicketState.PENDING)
                else -> it
            }
        })
    }

    fun stopTicket(id: String) = mutate { d ->
        d.copy(tickets = d.tickets.map { if (it.id == id && it.state == TicketState.RUNNING) it.copy(state = TicketState.PENDING) else it })
    }

    /** Completes a ticket (FR-3.5). Also completes the source task unless routine/quota. */
    fun completeTicket(id: String) = mutate(undoable = true) { d ->
        val k = d.tickets.firstOrNull { it.id == id } ?: return@mutate d
        if (k.isDone) return@mutate d
        val now = nowMillis()
        val tickets = d.tickets.map { if (it.id == id) it.copy(state = TicketState.DONE, doneAt = now, quotaCount = it.quotaTarget ?: it.quotaCount) else it }
        val tasks = if (k.taskId != null && k.routineId == null) TaskTree.setDone(d.tasks, k.taskId, true, now) else d.tasks
        d.copy(tickets = tickets, tasks = tasks)
    }

    fun uncompleteTicket(id: String) = mutate(undoable = true) { d ->
        val k = d.tickets.firstOrNull { it.id == id } ?: return@mutate d
        val tickets = d.tickets.map { if (it.id == id) it.copy(state = TicketState.PENDING, doneAt = null, quotaCount = 0) else it }
        val tasks = if (k.taskId != null && k.routineId == null) TaskTree.setDone(d.tasks, k.taskId, false, nowMillis()) else d.tasks
        d.copy(tickets = tickets, tasks = tasks)
    }

    fun setQuotaCount(id: String, count: Int) = mutate { d ->
        d.copy(tickets = d.tickets.map { k ->
            if (k.id != id) k else {
                val target = k.quotaTarget ?: return@map k
                val c = count.coerceIn(0, target)
                if (c >= target) k.copy(quotaCount = c, state = TicketState.DONE, doneAt = nowMillis())
                else k.copy(quotaCount = c, state = if (k.isDone) TicketState.PENDING else k.state, doneAt = null)
            }
        })
    }

    fun removeTicket(id: String) = mutate(undoable = true) { d -> d.copy(tickets = d.tickets.filter { it.id != id }) }

    fun moveTicket(id: String, delta: Int) = mutate(undoable = true) { d ->
        val k = d.tickets.firstOrNull { it.id == id } ?: return@mutate d
        val day = d.tickets.filter { it.date == k.date && !it.isDone }.sortedBy { it.order }.toMutableList()
        val i = day.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j < 0 || j >= day.size) return@mutate d
        val tmp = day[i]; day[i] = day[j]; day[j] = tmp
        val reordered = day.mapIndexed { idx, t -> t.id to t.copy(order = idx) }.toMap()
        d.copy(tickets = d.tickets.map { reordered[it.id] ?: it })
    }

    fun extendTimebox(id: String, minutes: Int) = mutate { d ->
        d.copy(tickets = d.tickets.map { if (it.id == id) it.copy(timeboxMin = (it.timeboxMin ?: 0) + minutes, startedAt = nowMillis()) else it })
    }

    fun markPrinted(ids: Collection<String>) = mutate { d ->
        val now = nowMillis(); val set = ids.toSet()
        d.copy(tickets = d.tickets.map { if (it.id in set) it.copy(printedAt = now) else it })
    }

    /** Carries yesterday's (or older) open tickets over to today (FR-3.4). */
    fun carryOver(ids: Collection<String>) = mutate(undoable = true) { d ->
        val today = Dates.iso(Dates.today()); val set = ids.toSet()
        val base = (d.tickets.filter { it.date == today }.maxOfOrNull { it.order } ?: -1) + 1
        var i = 0
        d.copy(tickets = d.tickets.map { if (it.id in set && !it.isDone) it.copy(date = today, order = base + i++, state = TicketState.PENDING) else it })
    }

    fun discardOld(ids: Collection<String>) = mutate(undoable = true) { d -> val set = ids.toSet(); d.copy(tickets = d.tickets.filter { it.id !in set }) }

    // ----- Routines (FR-6) -----

    fun upsertRoutine(r: Routine) = mutate(undoable = true) { d ->
        val exists = d.routines.any { it.id == r.id }
        val routines = if (exists) d.routines.map { if (it.id == r.id) r else it }
        else d.routines + r.copy(order = (d.routines.maxOfOrNull { it.order } ?: -1) + 1)
        d.copy(routines = routines)
    }

    fun deleteRoutine(id: String) = mutate(undoable = true) { d -> d.copy(routines = d.routines.filter { it.id != id }) }

    fun moveRoutine(id: String, delta: Int) = mutate(undoable = true) { d ->
        val list = d.routines.sortedBy { it.order }.toMutableList()
        val i = list.indexOfFirst { it.id == id }; val j = i + delta
        if (i < 0 || j < 0 || j >= list.size) return@mutate d
        val tmp = list[i]; list[i] = list[j]; list[j] = tmp
        d.copy(routines = list.mapIndexed { k, r -> r.copy(order = k) })
    }

    /** Regenerates today's routine tickets after the routine list changed (keeps completed ones). */
    fun regenerateToday() = mutate { d ->
        val today = Dates.iso(Dates.today())
        val kept = d.tickets.filter { !(it.date == today && it.routineId != null && !it.isDone) }
        val base = d.copy(tickets = kept, routineGeneratedDates = d.routineGeneratedDates - today)
        RoutineScheduler.generateFor(base, Dates.today(), nowMillis())
    }

    // ----- Settings -----

    fun updateSettings(f: (Settings) -> Settings) = mutate { it.copy(settings = f(it.settings)) }
}
