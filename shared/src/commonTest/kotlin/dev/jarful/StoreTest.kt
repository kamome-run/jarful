package dev.jarful

import dev.jarful.data.Store
import dev.jarful.domain.Dates
import dev.jarful.model.INBOX_ID
import dev.jarful.model.Routine
import kotlinx.coroutines.test.TestScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StoreTest {
    private fun store(): Store = Store(null, TestScope(), "Inbox").also { it.load() }

    @Test
    fun loadsWithInboxOnly() { // AC-9
        val s = store()
        assertEquals(listOf(INBOX_ID), s.data.value.tasks.map { it.id })
    }

    @Test
    fun ticketizeColumnNoDuplicates() { // AC-2
        val s = store()
        val p = s.addTask(null, "Kitchen")!!
        s.addTask(p.id, "Dishes"); s.addTask(p.id, "Floor")
        s.ticketizeColumn(p.id); s.ticketizeColumn(p.id)
        val today = Dates.iso(Dates.today())
        assertEquals(2, s.data.value.tickets.count { it.date == today })
        assertEquals("Kitchen", s.data.value.tickets.first().category)
    }

    @Test
    fun completingTicketCompletesTaskAndUndoWorks() {
        val s = store()
        val t = s.addTask(null, "Write")!!
        val k = s.ticketize(t.id)!!
        s.completeTicket(k.id)
        assertTrue(s.data.value.tasks.first { it.id == t.id }.done)
        s.uncompleteTicket(k.id)
        assertFalse(s.data.value.tasks.first { it.id == t.id }.done)
        assertTrue(s.canUndo()); s.undo()
        assertTrue(s.data.value.tasks.first { it.id == t.id }.done)
    }

    @Test
    fun quotaReachesTargetCompletes() { // AC-5
        val s = store()
        s.upsertRoutine(Routine(id = "r", title = "Emails", quotaTarget = 3))
        s.regenerateToday()
        val k = s.data.value.tickets.first { it.routineId == "r" }
        repeat(3) { i -> s.setQuotaCount(k.id, i + 1) }
        assertTrue(s.data.value.tickets.first { it.id == k.id }.isDone)
    }

    @Test
    fun disablingARoutineRemovesItsOpenTicketForToday() { // FR-6 / user report
        val s = store()
        s.upsertRoutine(Routine(id = "r1", title = "Coffee")); s.upsertRoutine(Routine(id = "r2", title = "Mail"))
        s.regenerateToday()
        assertEquals(2, s.data.value.tickets.count { it.routineId != null })
        s.upsertRoutine(s.data.value.routines.first { it.id == "r2" }.copy(enabled = false)); s.regenerateToday()
        assertEquals(listOf("r1"), s.data.value.tickets.mapNotNull { it.routineId })
    }

    @Test
    fun refocusCreatesInboxTasksAtFront() { // FR-7
        val s = store()
        val t = s.addTask(null, "Later")!!; s.ticketize(t.id)
        val created = s.refocus(listOf("Desk", "", "Mail"))
        assertEquals(2, created.size)
        assertTrue(created.all { s.data.value.tasks.first { x -> x.id == it.taskId }.parentId == INBOX_ID })
        val order = s.data.value.tickets.sortedBy { it.order }.map { it.title }
        assertEquals(listOf("Desk", "Mail", "Later"), order)
    }

    @Test
    fun deleteTaskDropsOpenTickets() {
        val s = store()
        val t = s.addTask(null, "X")!!; s.ticketize(t.id)
        s.deleteTask(t.id)
        assertTrue(s.data.value.tickets.isEmpty())
        assertNotNull(s.data.value.tasks.firstOrNull { it.id == INBOX_ID })
    }

    @Test
    fun exportImportRoundTrip() {
        val s = store(); s.addTask(null, "Keep me")
        val json = s.exportJson()
        val s2 = store(); assertTrue(s2.importJson(json))
        assertTrue(s2.data.value.tasks.any { it.title == "Keep me" })
        assertFalse(s2.importJson("not json"))
    }
}

class StoreRoutineBatchTest {
    private fun store(): Store = Store(null, TestScope(), "Inbox").also { it.load() }

    @Test
    fun bulkToggleRegeneratesToday() { // FR-6.8
        val s = store()
        s.upsertRoutine(Routine(id = "r1", title = "A")); s.upsertRoutine(Routine(id = "r2", title = "B")); s.upsertRoutine(Routine(id = "r3", title = "C"))
        s.regenerateToday()
        s.setRoutinesEnabled(listOf("r1", "r3"), false)
        assertEquals(listOf(false, true, false), s.data.value.routines.sortedBy { it.order }.map { it.enabled })
        assertEquals(listOf("r2"), s.data.value.tickets.mapNotNull { it.routineId })
        s.setRoutinesEnabled(listOf("r1"), true)
        assertEquals(setOf("r1", "r2"), s.data.value.tickets.mapNotNull { it.routineId }.toSet())
    }

    @Test
    fun copyRoutineInsertsCopiesRightAfterOriginal() { // FR-6.9
        val s = store()
        s.upsertRoutine(Routine(id = "r1", title = "A", category = "Morning", estimateMin = 3)); s.upsertRoutine(Routine(id = "r2", title = "B"))
        s.copyRoutine("r1", 3)
        val titles = s.data.value.routines.sortedBy { it.order }.map { it.title }
        assertEquals(listOf("A", "A", "A", "A", "B"), titles)
        assertEquals(5, s.data.value.routines.map { it.id }.toSet().size)
        assertTrue(s.data.value.routines.filter { it.title == "A" }.all { it.category == "Morning" && it.estimateMin == 3 })
        s.undo()
        assertEquals(listOf("A", "B"), s.data.value.routines.sortedBy { it.order }.map { it.title })
    }

    @Test
    fun moveRoutineToReorders() { // FR-6.10
        val s = store()
        for (n in listOf("A", "B", "C")) s.upsertRoutine(Routine(id = n, title = n))
        s.moveRoutineTo("C", 0)
        assertEquals(listOf("C", "A", "B"), s.data.value.routines.sortedBy { it.order }.map { it.title })
    }

    @Test
    fun regenerateForTomorrowHonoursWeekdaysAndToggles() { // FR-6.7
        val s = store()
        val tomorrow = Dates.plusDays(Dates.today(), 1)
        s.upsertRoutine(Routine(id = "r1", title = "Every day"))
        s.upsertRoutine(Routine(id = "r2", title = "Not tomorrow", weekdays = kotlinx.datetime.DayOfWeek.entries.toSet() - tomorrow.dayOfWeek))
        s.upsertRoutine(Routine(id = "r3", title = "Off", enabled = false))
        s.regenerateFor(tomorrow)
        val iso = Dates.iso(tomorrow)
        assertEquals(listOf("r1"), s.data.value.tickets.filter { it.date == iso }.mapNotNull { it.routineId })
        // calling again does not duplicate
        s.regenerateFor(tomorrow)
        assertEquals(1, s.data.value.tickets.count { it.date == iso })
    }

    @Test
    fun addTaskRefusedBelowGreatGrandchild() { // FR-1.1
        val s = store()
        val a = s.addTask(null, "A")!!; val b = s.addTask(a.id, "B")!!; val c = s.addTask(b.id, "C")!!; val d = s.addTask(c.id, "D")!!
        assertEquals(null, s.addTask(d.id, "E"))
        assertEquals(4, s.data.value.tasks.size - 1) // A..D, minus inbox
        s.copyTask(a.id, 1)
        assertEquals(2, s.data.value.tasks.count { it.title == "A" })
        assertEquals(2, s.data.value.tasks.count { it.title == "D" })
    }
}
