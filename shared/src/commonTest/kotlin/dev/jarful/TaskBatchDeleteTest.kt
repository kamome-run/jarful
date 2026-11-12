package dev.jarful

import dev.jarful.data.Store
import dev.jarful.model.INBOX_ID
import dev.jarful.model.TicketState
import kotlinx.coroutines.test.TestScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaskBatchDeleteTest {
    private fun store() = Store(null, TestScope(), "Inbox").also { it.load() }

    @Test
    fun overlappingSubtreesDeleteOnceAndUndoRestoresEverything() {
        val s = store()
        val parent = s.addTask(null, "Parent")!!
        val child = s.addTask(parent.id, "Child")!!
        val grandchild = s.addTask(child.id, "Grandchild")!!
        val other = s.addTask(INBOX_ID, "Other")!!
        val keep = s.addTask(null, "Keep")!!
        val open = s.ticketize(grandchild.id)!!
        val done = s.ticketize(child.id)!!
        s.mutate { d -> d.copy(tickets = d.tickets.map { if (it.id == done.id) it.copy(state = TicketState.DONE) else it }) }
        val before = s.data.value
        val ids = setOf(parent.id, child.id, grandchild.id, other.id)
        var changes = 0
        s.onChange = { changes++ }
        s.deleteTasks(ids + INBOX_ID + "missing")
        val after = s.data.value
        assertEquals(1, changes)
        assertEquals(setOf(INBOX_ID, keep.id), after.tasks.map { it.id }.toSet())
        assertEquals(listOf(done.id), after.tickets.map { it.id })
        assertTrue((ids + open.id).all { it in after.tombstones })
        assertFalse(done.id in after.tombstones)
        assertTrue(s.undo())
        assertEquals(before, s.data.value)
    }

    @Test
    fun invalidOrInboxSelectionDoesNotConsumeUndo() {
        val s = store()
        s.addTask(null, "Previous action")
        val before = s.data.value
        s.deleteTasks(emptySet())
        s.deleteTasks(setOf(INBOX_ID, "missing"))
        assertEquals(before, s.data.value)
        s.undo()
        assertEquals(listOf(INBOX_ID), s.data.value.tasks.map { it.id })
    }
}
