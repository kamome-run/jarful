package dev.jarful

import dev.jarful.data.Store
import dev.jarful.model.AppData
import dev.jarful.model.Routine
import dev.jarful.model.Task
import dev.jarful.model.Ticket
import dev.jarful.sync.SyncMerge
import dev.jarful.sync.SyncPayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncMergeTest {
    private fun task(id: String, title: String, at: Long, parent: String? = null) =
        Task(id = id, parentId = parent, title = title, createdAt = 1, updatedAt = at)

    @Test
    fun newerEditWins() {
        val a = SyncPayload(tasks = listOf(task("t1", "old", 10)))
        val b = SyncPayload(tasks = listOf(task("t1", "new", 20)))
        assertEquals("new", SyncMerge.merge(a, b).tasks.single().title)
        assertEquals("new", SyncMerge.merge(b, a).tasks.single().title) // symmetric
    }

    @Test
    fun deleteAfterEditDeletes_editAfterDeleteRevives() { // AC-11
        val edited = SyncPayload(tasks = listOf(task("t1", "edited", 10)))
        val deletedLater = SyncPayload(tombstones = mapOf("t1" to 20L))
        val m1 = SyncMerge.merge(edited, deletedLater)
        assertTrue(m1.tasks.isEmpty()); assertEquals(20L, m1.tombstones["t1"])

        val deletedEarly = SyncPayload(tombstones = mapOf("t1" to 5L))
        val m2 = SyncMerge.merge(edited, deletedEarly)
        assertEquals("edited", m2.tasks.single().title)
        assertTrue("t1" !in m2.tombstones) // losing tombstone is pruned
    }

    @Test
    fun unionOfEntitiesAndGeneratedDates() {
        val a = SyncPayload(tasks = listOf(task("a", "A", 1)), tickets = listOf(Ticket(id = "k1", date = "2026-09-22", category = "", title = "x", updatedAt = 1)), routineGeneratedDates = setOf("2026-09-22"))
        val b = SyncPayload(tasks = listOf(task("b", "B", 1)), routines = listOf(Routine(id = "r1", title = "R", updatedAt = 1)), routineGeneratedDates = setOf("2026-09-23"))
        val m = SyncMerge.merge(a, b)
        assertEquals(setOf("a", "b"), m.tasks.map { it.id }.toSet())
        assertEquals(1, m.tickets.size); assertEquals(1, m.routines.size)
        assertEquals(setOf("2026-09-22", "2026-09-23"), m.routineGeneratedDates)
    }

    @Test
    fun orphanedChildIsReparentedToRoot() {
        val a = SyncPayload(tasks = listOf(task("p", "Parent", 1), task("c", "Child", 30, parent = "p")))
        val b = SyncPayload(tombstones = mapOf("p" to 20L))
        val m = SyncMerge.merge(a, b)
        assertEquals(null, m.tasks.single { it.id == "c" }.parentId)
    }

    @Test
    fun storeStampsChangedEntitiesAndTombstones() { // FR-14.5
        val before = AppData(tasks = listOf(task("a", "A", 5), task("b", "B", 5)), tickets = listOf(Ticket(id = "k", date = "d", category = "", title = "T", updatedAt = 5)))
        val after = before.copy(tasks = listOf(task("a", "A2", 5)), tickets = before.tickets) // b removed, a changed, ticket untouched
        val stamped = Store.stamp(before, after, now = 100)
        assertEquals(100L, stamped.tasks.single().updatedAt)
        assertEquals(100L, stamped.tombstones["b"])
        assertEquals(5L, stamped.tickets.single().updatedAt)
    }
}
