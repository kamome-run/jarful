package dev.jarful.sync

import dev.jarful.model.AppData
import dev.jarful.model.Routine
import dev.jarful.model.Task
import dev.jarful.model.Ticket
import kotlinx.serialization.Serializable

const val SYNC_PROTOCOL_VERSION = 1

/** The synced subset of [AppData] (FR-14.3). Settings are device-local and excluded. */
@Serializable
data class SyncPayload(
    val tasks: List<Task> = emptyList(),
    val tickets: List<Ticket> = emptyList(),
    val routines: List<Routine> = emptyList(),
    val tombstones: Map<String, Long> = emptyMap(),
    val routineGeneratedDates: Set<String> = emptySet(),
) {
    companion object {
        fun of(d: AppData) = SyncPayload(d.tasks, d.tickets, d.routines, d.tombstones, d.routineGeneratedDates)
    }
}

@Serializable
data class SyncRequest(val protocolVersion: Int = SYNC_PROTOCOL_VERSION, val pin: String, val deviceId: String, val data: SyncPayload)

@Serializable
data class SyncResponse(val protocolVersion: Int = SYNC_PROTOCOL_VERSION, val data: SyncPayload)

/** Pure last-writer-wins merge with tombstones (FR-14.4). Symmetric: merge(a, b) == merge(b, a). */
object SyncMerge {

    private fun <T> mergeEntities(a: List<T>, b: List<T>, id: (T) -> String, updatedAt: (T) -> Long, tombstones: Map<String, Long>): List<T> {
        val out = LinkedHashMap<String, T>()
        for (e in a + b) {
            val k = id(e)
            val cur = out[k]
            if (cur == null || updatedAt(e) > updatedAt(cur)) out[k] = e
        }
        return out.values.filter { e -> (tombstones[id(e)] ?: Long.MIN_VALUE) <= updatedAt(e) }
    }

    private fun mergeTombstones(a: Map<String, Long>, b: Map<String, Long>): Map<String, Long> {
        val out = HashMap(a)
        for ((k, v) in b) out[k] = maxOf(out[k] ?: Long.MIN_VALUE, v)
        return out
    }

    fun merge(a: SyncPayload, b: SyncPayload): SyncPayload {
        val tomb = mergeTombstones(a.tombstones, b.tombstones)
        val tasks = mergeEntities(a.tasks, b.tasks, { it.id }, { it.updatedAt }, tomb)
        val tickets = mergeEntities(a.tickets, b.tickets, { it.id }, { it.updatedAt }, tomb)
        val routines = mergeEntities(a.routines, b.routines, { it.id }, { it.updatedAt }, tomb)
        // drop tombstones that lost against a newer edit, so the entity does not get re-deleted later
        val live = (tasks.map { it.id } + tickets.map { it.id } + routines.map { it.id }).toSet()
        val prunedTomb = tomb.filterKeys { it !in live }
        // orphaned tasks (parent deleted) are re-parented to root rather than lost
        val taskIds = tasks.map { it.id }.toSet()
        val fixedTasks = tasks.map { if (it.parentId != null && it.parentId !in taskIds) it.copy(parentId = null) else it }
        return SyncPayload(fixedTasks, tickets, routines, prunedTomb, a.routineGeneratedDates + b.routineGeneratedDates)
    }

    fun apply(d: AppData, p: SyncPayload): AppData =
        d.copy(tasks = p.tasks, tickets = p.tickets, routines = p.routines, tombstones = p.tombstones, routineGeneratedDates = p.routineGeneratedDates)
}
