package dev.jarful

import dev.jarful.data.Store
import dev.jarful.domain.Dates
import dev.jarful.model.Routine
import kotlinx.coroutines.test.TestScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TicketBatchDeleteTest {
    private fun store() = Store(null, TestScope(), "Inbox").also { it.load() }

    @Test
    fun deletesSelectedTicketsOnlyAndRestoresInOneUndo() {
        val s = store()
        val task = s.addTask(null, "Task")!!
        val doneTask = s.addTask(null, "Done task")!!
        val today = Dates.today()
        val open = s.ticketize(task.id)!!
        val done = s.ticketize(doneTask.id)!!
        s.completeTicket(done.id)
        val yesterday = s.ticketize(task.id, Dates.iso(Dates.minusDays(today, 1)))!!
        val tomorrow = s.ticketize(task.id, Dates.iso(Dates.plusDays(today, 1)))!!
        s.upsertRoutine(Routine(id = "routine", title = "Routine", quotaTarget = 3))
        s.regenerateToday()
        val routine = s.data.value.tickets.single { it.routineId == "routine" }
        val before = s.data.value
        val ids = setOf(open.id, done.id, routine.id)
        var changes = 0
        s.onChange = { changes++ }
        s.removeTickets(ids + "missing")
        assertEquals(1, changes)
        assertEquals(setOf(yesterday.id, tomorrow.id), s.data.value.tickets.map { it.id }.toSet())
        assertEquals(before.tasks, s.data.value.tasks)
        assertEquals(before.routines, s.data.value.routines)
        assertEquals(before.routineGeneratedDates, s.data.value.routineGeneratedDates)
        assertTrue(ids.all { it in s.data.value.tombstones })
        s.prepareRoutines()
        assertTrue(s.data.value.tickets.none { it.id in ids || it.routineId == "routine" })
        assertTrue(s.undo())
        assertEquals(before, s.data.value)
    }

    @Test
    fun emptyAndMissingIdsDoNotConsumeUndo() {
        val s = store()
        val task = s.addTask(null, "Task")!!
        s.ticketize(task.id)
        val before = s.data.value
        s.removeTickets(emptySet())
        s.removeTickets(setOf("missing"))
        assertEquals(before, s.data.value)
        s.undo()
        assertTrue(s.data.value.tickets.isEmpty())
        assertEquals(before.tasks, s.data.value.tasks)
    }
}
