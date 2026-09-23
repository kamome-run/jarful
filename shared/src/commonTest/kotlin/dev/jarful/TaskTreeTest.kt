package dev.jarful

import dev.jarful.domain.TaskTree
import dev.jarful.model.INBOX_ID
import dev.jarful.model.Task
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

class TaskTreeDepthAndCopyTest {
    private val now = 1_000L

    private fun chain(n: Int): Pair<List<Task>, List<Task>> {
        var tasks = emptyList<Task>(); val created = ArrayList<Task>()
        var parent: String? = null
        repeat(n) { i -> val (t, x) = TaskTree.add(tasks, parent, "L$i", now); tasks = t; created.add(x); parent = x.id }
        return tasks to created
    }

    @Test
    fun depthStopsAtGreatGrandchild() { // FR-1.1
        val (tasks, c) = chain(4) // root, child, grandchild, great-grandchild
        assertEquals(0, TaskTree.depth(tasks, c[0].id)); assertEquals(3, TaskTree.depth(tasks, c[3].id))
        assertTrue(TaskTree.canAddChild(tasks, c[2].id))
        assertFalse(TaskTree.canAddChild(tasks, c[3].id))
        assertTrue(TaskTree.canAddChild(tasks, null))
    }

    @Test
    fun reparentRefusesWhenSubtreeWouldExceedDepth() {
        val (tasks, c) = chain(4)
        val (t2, other) = TaskTree.add(tasks, null, "Other", now)
        // moving the root of a 4-deep chain under "Other" would make depth 4 -> refused
        assertEquals(t2, TaskTree.reparent(t2, c[0].id, other.id, now))
        // moving the leaf under "Other" is fine
        val moved = TaskTree.reparent(t2, c[3].id, other.id, now)
        assertEquals(other.id, TaskTree.byId(moved, c[3].id)!!.parentId)
    }

    @Test
    fun copyDuplicatesSubtreeNTimesAfterOriginal() { // FR-2.8
        var tasks = emptyList<Task>()
        val (t1, a) = TaskTree.add(tasks, null, "A", now); tasks = t1
        val (t2, _) = TaskTree.add(tasks, null, "Z", now); tasks = t2
        val (t3, b) = TaskTree.add(tasks, a.id, "B", now); tasks = t3
        val (t4, _) = TaskTree.add(tasks, b.id, "C", now); tasks = t4
        tasks = TaskTree.update(tasks, a.id, now) { it.copy(estimateMin = 5) }
        val out = TaskTree.copy(tasks, a.id, 2, now)
        assertEquals(listOf("A", "A", "A", "Z"), TaskTree.childrenOf(out, null).map { it.title })
        val copies = TaskTree.childrenOf(out, null).filter { it.title == "A" && it.id != a.id }
        assertEquals(2, copies.size)
        copies.forEach { cp ->
            assertEquals(5, cp.estimateMin)
            val kids = TaskTree.childrenOf(out, cp.id); assertEquals(listOf("B"), kids.map { it.title })
            assertEquals(listOf("C"), TaskTree.childrenOf(out, kids[0].id).map { it.title })
        }
        assertEquals(out.size, out.map { it.id }.toSet().size) // all ids unique
    }

    @Test
    fun moveToReordersSiblings() { // FR-1.6 drag and drop
        var tasks = emptyList<Task>()
        for (n in listOf("A", "B", "C", "D")) { val (t, _) = TaskTree.add(tasks, null, n, now); tasks = t }
        val a = TaskTree.childrenOf(tasks, null)[0]
        val out = TaskTree.moveTo(tasks, a.id, 2)
        assertEquals(listOf("B", "C", "A", "D"), TaskTree.childrenOf(out, null).map { it.title })
        val back = TaskTree.moveTo(out, a.id, 0)
        assertEquals(listOf("A", "B", "C", "D"), TaskTree.childrenOf(back, null).map { it.title })
    }
}
