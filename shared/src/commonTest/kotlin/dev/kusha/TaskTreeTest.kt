package dev.kusha

import dev.kusha.domain.TaskTree
import dev.kusha.model.INBOX_ID
import dev.kusha.model.Task
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaskTreeTest {
    private val now = 1_000L

    @Test
    fun addAndChildren() {
        var tasks = emptyList<Task>()
        val (t1, a) = TaskTree.add(tasks, null, "Clean the house", now); tasks = t1
        val (t2, b) = TaskTree.add(tasks, a.id, "Kitchen", now); tasks = t2
        val (t3, _) = TaskTree.add(tasks, b.id, "Wash dishes", now); tasks = t3
        assertEquals(listOf("Clean the house"), TaskTree.childrenOf(tasks, null).map { it.title })
        assertEquals(listOf("Kitchen"), TaskTree.childrenOf(tasks, a.id).map { it.title })
        assertEquals(1 to 1, TaskTree.childCounts(tasks, a.id)) // AC-1
        assertEquals(listOf("Clean the house", "Kitchen"), TaskTree.pathTo(tasks, b.id).map { it.title })
    }

    @Test
    fun insertAfterKeepsOrder() {
        var tasks = emptyList<Task>()
        val (t1, a) = TaskTree.add(tasks, null, "A", now); tasks = t1
        val (t2, _) = TaskTree.add(tasks, null, "C", now); tasks = t2
        val (t3, _) = TaskTree.add(tasks, null, "B", now, afterId = a.id); tasks = t3
        assertEquals(listOf("A", "B", "C"), TaskTree.childrenOf(tasks, null).map { it.title })
    }

    @Test
    fun deleteRemovesSubtreeAndRestoreBringsItBack() {
        var tasks = emptyList<Task>()
        val (t1, a) = TaskTree.add(tasks, null, "A", now); tasks = t1
        val (t2, b) = TaskTree.add(tasks, a.id, "B", now); tasks = t2
        val (t3, _) = TaskTree.add(tasks, b.id, "C", now); tasks = t3
        val (after, removed) = TaskTree.delete(tasks, a.id)
        assertTrue(after.isEmpty()); assertEquals(3, removed.size)
        assertEquals(3, TaskTree.restore(after, removed).size)
    }

    @Test
    fun inboxCannotBeDeletedOrMoved() {
        val tasks = TaskTree.ensureInbox(emptyList(), "Inbox", now)
        assertEquals(tasks, TaskTree.delete(tasks, INBOX_ID).first)
        assertEquals(tasks, TaskTree.reparent(tasks, INBOX_ID, "x", now))
    }

    @Test
    fun setDoneCascadesDown() {
        var tasks = emptyList<Task>()
        val (t1, a) = TaskTree.add(tasks, null, "A", now); tasks = t1
        val (t2, _) = TaskTree.add(tasks, a.id, "B", now); tasks = t2
        tasks = TaskTree.setDone(tasks, a.id, true, now)
        assertTrue(tasks.all { it.done })
        tasks = TaskTree.setDone(tasks, a.id, false, now)
        assertFalse(tasks.first { it.id == a.id }.done)
        assertTrue(tasks.first { it.parentId == a.id }.done) // children keep their state on undo
    }

    @Test
    fun reparentRefusesCycles() {
        var tasks = emptyList<Task>()
        val (t1, a) = TaskTree.add(tasks, null, "A", now); tasks = t1
        val (t2, b) = TaskTree.add(tasks, a.id, "B", now); tasks = t2
        assertEquals(tasks, TaskTree.reparent(tasks, a.id, b.id, now))
        val moved = TaskTree.reparent(tasks, b.id, null, now)
        assertEquals(null, moved.first { it.id == b.id }.parentId)
    }

    @Test
    fun moveWithinSiblings() {
        var tasks = emptyList<Task>()
        val (t1, a) = TaskTree.add(tasks, null, "A", now); tasks = t1
        val (t2, _) = TaskTree.add(tasks, null, "B", now); tasks = t2
        tasks = TaskTree.moveWithinSiblings(tasks, a.id, +1)
        assertEquals(listOf("B", "A"), TaskTree.childrenOf(tasks, null).map { it.title })
    }

    @Test
    fun addManyFromTextSplitsLines() {
        val tasks = TaskTree.addManyFromText(emptyList(), null, "- Wash dishes\n\n* Wipe surfaces\n  Clean floor ", now)
        assertEquals(listOf("Wash dishes", "Wipe surfaces", "Clean floor"), TaskTree.childrenOf(tasks, null).map { it.title })
    }
}
