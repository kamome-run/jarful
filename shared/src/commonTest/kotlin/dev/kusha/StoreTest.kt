package dev.kusha

import dev.kusha.data.Store
import dev.kusha.domain.Dates
import dev.kusha.model.INBOX_ID
import dev.kusha.model.Routine
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
