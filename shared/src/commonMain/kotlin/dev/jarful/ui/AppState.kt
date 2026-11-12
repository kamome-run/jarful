package dev.jarful.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.jarful.data.Store
import dev.jarful.domain.Dates
import dev.jarful.domain.Ids
import dev.jarful.domain.TaskTree
import dev.jarful.model.INBOX_ID
import dev.jarful.model.Routine
import dev.jarful.model.Task
import dev.jarful.model.Ticket
import dev.jarful.platform.SoundPlayer
import dev.jarful.platform.nowMillis
import dev.jarful.platform.vibrateShort
import dev.jarful.print.PrintResult
import dev.jarful.print.PrinterClient
import dev.jarful.print.TicketFormatter
import dev.jarful.sound.CrumpleSound
import dev.jarful.ui.i18n.Strings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DayOfWeek

enum class Tab { TODAY, COLUMNS, ROUTINES, STATS, SETTINGS }

/** Transient UI state: selection, focus, dialogs, feedback. Persistent state lives in [Store]. */
class AppState(val store: Store, val scope: CoroutineScope, var strings: Strings) {

    var tab by mutableStateOf(Tab.TODAY)

    // ----- Columns (FR-1) -----
    /** path[i] = selected task id in column i. Column i lists children of path[i-1] (root for i = 0). */
    val path = mutableStateListOf<String>()
    var focusedColumn by mutableIntStateOf(0)
    /** parentId of the column that currently shows the "new task" field (null = root; NONE = hidden). */
    var addingIn by mutableStateOf<String?>(NONE)
    var renamingId by mutableStateOf<String?>(null)
    var detailTaskId by mutableStateOf<String?>(null)
    var breakDownTaskId by mutableStateOf<String?>(null)
    var moveTaskId by mutableStateOf<String?>(null)
    var confirmDeleteId by mutableStateOf<String?>(null)
    var compactColumnIndex by mutableIntStateOf(0)

    // ----- Dialogs -----
    var refocusOpen by mutableStateOf(false)
    var editingRoutine by mutableStateOf<Routine?>(null)
    var importOpen by mutableStateOf(false)
    var showShortcuts by mutableStateOf(false)
    var showOnboarding by mutableStateOf(false)

    // ----- Feedback (FR-4) -----
    var combo by mutableIntStateOf(0)
    private var lastDoneAt = 0L
    var lastCompletedTicketId by mutableStateOf<String?>(null)
    var jarDropSignal by mutableIntStateOf(0)
    var toast by mutableStateOf<String?>(null)
    var printing by mutableStateOf(false)

    private val sound = SoundPlayer()
    private val crumplePcm by lazy { CrumpleSound.crumple() }
    private val chimePcm by lazy { CrumpleSound.chime() }
    private val printer = PrinterClient()

    val data get() = store.data.value

    fun parentIdForColumn(i: Int): String? = if (i == 0) null else path.getOrNull(i - 1)
    fun selectedIdInColumn(i: Int): String? = path.getOrNull(i)
    val selectedTaskId: String? get() = path.getOrNull(focusedColumn)
    val columnCount: Int get() = path.size + 1

    fun select(column: Int, id: String?) {
        while (path.size > column) path.removeAt(path.size - 1)
        if (id != null) path.add(id)
        focusedColumn = column
        compactColumnIndex = column
    }

    fun moveSelection(delta: Int) {
        val col = focusedColumn
        val kids = visibleChildren(parentIdForColumn(col))
        if (kids.isEmpty()) return
        val cur = kids.indexOfFirst { it.id == selectedIdInColumn(col) }
        val next = (if (cur < 0) (if (delta > 0) 0 else kids.size - 1) else (cur + delta)).coerceIn(0, kids.size - 1)
        select(col, kids[next].id)
    }

    fun focusRight() {
        val id = selectedIdInColumn(focusedColumn) ?: return
        val kids = visibleChildren(id)
        val col = focusedColumn + 1
        if (kids.isNotEmpty()) select(col, kids.first().id) else { focusedColumn = col; compactColumnIndex = col }
    }

    fun focusLeft() {
        if (focusedColumn == 0) return
        val col = focusedColumn - 1
        val keep = path.getOrNull(col)
        select(col, keep)
    }

    fun visibleChildren(parentId: String?): List<Task> {
        val kids = TaskTree.childrenOf(data.tasks, parentId)
        val show = data.settings.showCompleted
        val open = kids.filter { !it.done }
        return if (show) open + kids.filter { it.done } else open
    }

    fun columnTitle(i: Int): String = parentIdForColumn(i)?.let { TaskTree.byId(data.tasks, it)?.title } ?: strings.root

    // ----- Task actions -----

    fun startAdd(parentId: String?) { renamingId = null; addingIn = parentId }
    fun startAddChildOfSelection() {
        val id = selectedTaskId ?: return
        val col = focusedColumn + 1
        select(focusedColumn, id)
        focusedColumn = col; compactColumnIndex = col
        startAdd(id)
    }
    fun cancelEdit() { addingIn = NONE; renamingId = null }

    /** Commits the new-task field. Returns the created task. [andChild] opens a child field (Tab). */
    fun commitAdd(parentId: String?, title: String, andChild: Boolean): Task? {
        val t = store.addTask(parentId, title) ?: return null
        val col = if (parentId == null) 0 else path.indexOf(parentId) + 1
        select(col, t.id)
        if (andChild) { focusedColumn = col + 1; compactColumnIndex = col + 1; addingIn = t.id } else addingIn = parentId
        return t
    }

    fun commitRename(id: String, title: String) { store.renameTask(id, title); renamingId = null }

    fun toggleDone(id: String) { store.toggleTaskDone(id) }

    fun deleteTask(id: String) {
        val col = path.indexOf(id)
        store.deleteTask(id)
        if (col >= 0) select(col, null)
        confirmDeleteId = null
    }

    fun ticketizeSelected() { selectedTaskId?.let { ticketize(it) } }
    fun ticketize(id: String) { if (id != INBOX_ID) { store.ticketize(id); showToast(strings.toToday + " ✓") } }
    fun ticketizeColumn(parentId: String?) { store.ticketizeColumn(parentId); showToast(strings.columnToToday + " ✓") }

    fun undo() { if (store.undo()) showToast(strings.undo) }

    // ----- Tickets (FR-3, FR-4) -----

    fun todayIso(): String = Dates.iso(Dates.today())
    fun todayTickets(): List<Ticket> = data.tickets.filter { it.date == todayIso() }.sortedWith(compareBy({ it.isDone }, { it.order }))
    fun oldOpenTickets(): List<Ticket> = data.tickets.filter { it.date < todayIso() && !it.isDone }.sortedBy { it.date }
    fun loopsToday(): Int = data.tickets.count { it.date == todayIso() && it.isDone }

    /** Called after the crumple animation finished (FR-4.1). Applies completion + feedback. */
    fun completeTicket(id: String) {
        store.completeTicket(id)
        lastCompletedTicketId = id
        jarDropSignal++
    }

    /** Immediate feedback at the moment the user taps complete (FR-4.6). */
    fun feedback() {
        val now = nowMillis()
        combo = if (now - lastDoneAt <= 10 * 60 * 1000L) combo + 1 else 1
        lastDoneAt = now
        val s = data.settings
        if (s.soundEnabled) sound.play(crumplePcm, CrumpleSound.SAMPLE_RATE)
        if (s.hapticsEnabled) vibrateShort(40)
    }

    fun timeUp() { if (data.settings.soundEnabled) sound.play(chimePcm, CrumpleSound.SAMPLE_RATE); if (data.settings.hapticsEnabled) vibrateShort(200) }

    fun undoComplete(id: String) { store.uncompleteTicket(id); lastCompletedTicketId = null; combo = (combo - 1).coerceAtLeast(0) }

    fun refocus(text: String) {
        val created = store.refocus(text.lines())
        refocusOpen = false
        if (created.isNotEmpty()) { tab = Tab.TODAY; store.startTicket(created.first().id) }
    }

    fun ticketsToPrint(kind: PrintTarget): List<Ticket> = when (kind) {
        PrintTarget.Today -> todayTickets().filter { !it.isDone }
        PrintTarget.Routines -> todayTickets().filter { !it.isDone && it.routineId != null }
        is PrintTarget.Column -> visibleChildren(kind.parentId).filter { !it.done && it.id != INBOX_ID }.map { virtualTicket(it) }
        is PrintTarget.Task -> listOfNotNull(TaskTree.byId(data.tasks, kind.id)?.let { virtualTicket(it) })
        is PrintTarget.Tickets -> kind.tickets
    }

    private fun virtualTicket(t: Task): Ticket = Ticket(
        id = "virtual-" + t.id, date = todayIso(), taskId = t.id,
        category = TaskTree.pathTo(data.tasks, t.parentId).lastOrNull()?.title ?: "", title = t.title,
        estimateMin = t.estimateMin, timeboxMin = t.timeboxMin,
    )

    fun print(kind: PrintTarget) {
        val tickets = ticketsToPrint(kind)
        if (tickets.isEmpty()) return
        val settings = data.settings.printer
        printing = true
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                val bytes = TicketFormatter.encodeAll(tickets, settings) { dateLabel(it.date) }
                printer.send(settings, bytes)
            }
            printing = false
            when (result) {
                PrintResult.Ok -> { store.markPrinted(tickets.map { it.id }); showToast(strings.printOk) }
                is PrintResult.Error -> showToast(strings.printFailed(result.message))
            }
        }
    }

    fun testPrint() {
        val settings = data.settings.printer
        printing = true
        scope.launch {
            val result = withContext(Dispatchers.Default) { printer.send(settings, TicketFormatter.encodeTestPage(settings)) }
            printing = false
            showToast(when (result) { PrintResult.Ok -> strings.printOk; is PrintResult.Error -> strings.printFailed(result.message) })
        }
    }

    fun dateLabel(iso: String): String = "$iso (${strings.dayShort(Dates.dayOfWeek(iso))})"

    fun showToast(msg: String) { toast = msg }

    // ----- Routines -----
    fun newRoutine(): Routine = Routine(id = Ids.next("r"), title = "", category = "", weekdays = DayOfWeek.entries.toSet())

    fun addSampleRoutines() {
        strings.sampleRoutines.forEachIndexed { i, (title, cat, est) ->
            store.upsertRoutine(Routine(id = Ids.next("r"), title = title, category = cat, estimateMin = est, order = i))
        }
        store.regenerateToday()
    }

    fun finishOnboarding(addSamples: Boolean) {
        if (addSamples) addSampleRoutines()
        store.updateSettings { it.copy(onboardingDone = true) }
        showOnboarding = false
    }

    companion object { const val NONE = "__none__" }
}

sealed class PrintTarget {
    data object Today : PrintTarget()
    data object Routines : PrintTarget()
    data class Column(val parentId: String?) : PrintTarget()
    data class Task(val id: String) : PrintTarget()
    data class Tickets(val tickets: List<Ticket>) : PrintTarget()
}
