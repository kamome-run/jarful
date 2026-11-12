package dev.jarful.desktop

import dev.jarful.data.Store
import java.time.LocalDate
import dev.jarful.ui.AppState
import dev.jarful.ui.i18n.EN
import kotlinx.coroutines.test.TestScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TicketSelectionStateTest {
    @Test
    fun restrictsSelectionToTodayAndUsesTheConfirmationSnapshot() {
        val scope = TestScope()
        val store = Store(null, scope, "Inbox").also { it.load() }
        val task = store.addTask(null, "Task")!!
        val today = store.ticketize(task.id)!!
        val old = store.ticketize(task.id, LocalDate.now().minusDays(1).toString())!!
        val future = store.ticketize(task.id, LocalDate.now().plusDays(1).toString())!!
        val before = store.data.value
        val state = AppState(store, scope, EN)
        state.startTicketSelect()
        state.toggleTicketSelection(old.id)
        state.toggleTicketSelection(future.id)
        state.toggleTicketSelection("missing")
        assertTrue(state.selectedTicketIds.isEmpty())
        state.selectAllTodayTickets()
        assertEquals(setOf(today.id), state.selectedTicketIds)
        state.requestDeleteSelectedTickets()
        state.clearTicketSelection()
        state.deleteSelectedTickets()
        assertEquals(setOf(old.id, future.id), state.data.tickets.map { it.id }.toSet())
        assertFalse(state.ticketSelectMode)
        assertNull(state.pendingTicketDeleteIds)
        state.undo()
        assertEquals(before, state.data)
    }

    @Test
    fun removesStaleIdsAndKeepsTaskAndTicketSelectionSeparate() {
        val scope = TestScope()
        val store = Store(null, scope, "Inbox").also { it.load() }
        val task = store.addTask(null, "Task")!!
        val ticket = store.ticketize(task.id)!!
        val state = AppState(store, scope, EN)
        state.startTaskSelect()
        state.toggleTaskSelection(task.id)
        state.startTicketSelect()
        assertFalse(state.taskSelectMode)
        assertTrue(state.selectedTaskIds.isEmpty())
        state.selectAllTodayTickets()
        store.removeTicket(ticket.id)
        assertTrue(state.selectedTicketIds.isEmpty())
        state.startTaskSelect()
        assertFalse(state.ticketSelectMode)
        assertNull(state.pendingTicketDeleteIds)
    }
}
