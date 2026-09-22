package dev.jarful.domain

import dev.jarful.model.INBOX_ID
import dev.jarful.model.Task

/** Pure functions over the flat task list that implement the hierarchy (FR-1, FR-2). */
object TaskTree {

    fun childrenOf(tasks: List<Task>, parentId: String?): List<Task> =
        tasks.filter { it.parentId == parentId }.sortedWith(compareBy({ it.order }, { it.createdAt }))

    fun byId(tasks: List<Task>, id: String): Task? = tasks.firstOrNull { it.id == id }

    /** Path from root to [id], inclusive. Empty if not found. */
    fun pathTo(tasks: List<Task>, id: String?): List<Task> {
        if (id == null) return emptyList()
        val index = tasks.associateBy { it.id }
        val out = ArrayList<Task>()
        var cur = index[id]
        var guard = 0
        while (cur != null && guard++ < 1000) {
            out.add(cur)
            cur = cur.parentId?.let { index[it] }
        }
        return out.reversed()
    }

    /** All descendants of [id] (not including itself). */
    fun descendants(tasks: List<Task>, id: String): List<Task> {
        val byParent = tasks.groupBy { it.parentId }
        val out = ArrayList<Task>()
        val stack = ArrayDeque<String>()
        stack.add(id)
        while (stack.isNotEmpty()) {
            val p = stack.removeLast()
            byParent[p]?.forEach { c -> out.add(c); stack.add(c.id) }
        }
        return out
    }

    /** Pair(openChildren, totalChildren) counted over direct children (FR-1.3). */
    fun childCounts(tasks: List<Task>, id: String): Pair<Int, Int> {
        val kids = tasks.filter { it.parentId == id }
        return kids.count { !it.done } to kids.size
    }

    fun isAncestor(tasks: List<Task>, ancestorId: String, id: String): Boolean =
        pathTo(tasks, id).dropLast(1).any { it.id == ancestorId }

    fun nextOrder(tasks: List<Task>, parentId: String?): Int =
        (tasks.filter { it.parentId == parentId }.maxOfOrNull { it.order } ?: -1) + 1

    fun add(tasks: List<Task>, parentId: String?, title: String, now: Long, afterId: String? = null): Pair<List<Task>, Task> {
        val siblings = childrenOf(tasks, parentId)
        val insertAt = afterId?.let { a -> siblings.indexOfFirst { it.id == a } + 1 }?.takeIf { it > 0 } ?: siblings.size
        val task = Task(
            id = Ids.next("t"), parentId = parentId, title = title.trim(), order = insertAt,
            createdAt = now, updatedAt = now,
        )
        val reordered = siblings.toMutableList().apply { add(insertAt, task) }
            .mapIndexed { i, t -> t.copy(order = i) }
        val others = tasks.filter { it.parentId != parentId }
        return (others + reordered) to task
    }

    fun update(tasks: List<Task>, id: String, now: Long, f: (Task) -> Task): List<Task> =
        tasks.map { if (it.id == id) f(it).copy(updatedAt = now) else it }

    /** Marks [id] done/undone. Marking done also marks all descendants done (FR-3.5). */
    fun setDone(tasks: List<Task>, id: String, done: Boolean, now: Long): List<Task> {
        val ids = (descendants(tasks, id).map { it.id } + id).toSet()
        return tasks.map {
            if (it.id in ids && (done || it.id == id)) it.copy(done = done, doneAt = if (done) now else null, updatedAt = now) else it
        }
    }

    /** Removes [id] and all descendants. Returns the new list and the removed tasks (for undo). */
    fun delete(tasks: List<Task>, id: String): Pair<List<Task>, List<Task>> {
        if (id == INBOX_ID) return tasks to emptyList()
        val removed = descendants(tasks, id) + tasks.filter { it.id == id }
        val removedIds = removed.map { it.id }.toSet()
        return tasks.filter { it.id !in removedIds } to removed
    }

    fun restore(tasks: List<Task>, removed: List<Task>): List<Task> {
        val existing = tasks.map { it.id }.toSet()
        return tasks + removed.filter { it.id !in existing }
    }

    /** Moves the task up (-1) or down (+1) among its siblings (FR-2.5). */
    fun moveWithinSiblings(tasks: List<Task>, id: String, delta: Int): List<Task> {
        val task = byId(tasks, id) ?: return tasks
        val siblings = childrenOf(tasks, task.parentId).toMutableList()
        val i = siblings.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j < 0 || j >= siblings.size) return tasks
        val tmp = siblings[i]; siblings[i] = siblings[j]; siblings[j] = tmp
        val reordered = siblings.mapIndexed { k, t -> t.copy(order = k) }
        return tasks.filter { it.parentId != task.parentId } + reordered
    }

    /** Re-parents [id] under [newParentId]. Refuses cycles. */
    fun reparent(tasks: List<Task>, id: String, newParentId: String?, now: Long): List<Task> {
        if (id == INBOX_ID) return tasks
        if (newParentId == id) return tasks
        if (newParentId != null && isAncestor(tasks, id, newParentId)) return tasks
        val order = nextOrder(tasks, newParentId)
        return tasks.map { if (it.id == id) it.copy(parentId = newParentId, order = order, updatedAt = now) else it }
    }

    /** Splits multi-line text into child tasks (FR-2.6). */
    fun addManyFromText(tasks: List<Task>, parentId: String?, text: String, now: Long): List<Task> {
        var cur = tasks
        text.lines().map { it.trim().trimStart('-', '*', '•', ' ') }.filter { it.isNotBlank() }.forEach {
            cur = add(cur, parentId, it, now).first
        }
        return cur
    }

    fun ensureInbox(tasks: List<Task>, inboxTitle: String, now: Long): List<Task> =
        if (tasks.any { it.id == INBOX_ID }) tasks
        else listOf(Task(id = INBOX_ID, parentId = null, title = inboxTitle, order = -1, createdAt = now, updatedAt = now)) + tasks
}
