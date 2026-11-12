package dev.jarful.desktop

import dev.jarful.data.Store
import dev.jarful.model.INBOX_ID
import dev.jarful.ui.AppState
import dev.jarful.ui.i18n.EN
import kotlinx.coroutines.test.TestScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TaskSelectionStateTest {
    @Test
    fun deletionTrimsAnOpenDescendantPathAndRestoresInOneUndo() {
        val scope = TestScope()
        val store = Store(null, scope, "Inbox").also { it.load() }
        val parent = store.addTask(null, "Parent")!!
        val child = store.addTask(parent.id, "Child")!!
        val before = store.data.value
        val state = AppState(store, scope, EN)
        state.select(0, parent.id)
        state.select(1, child.id)
        state.startTaskSelect()
        state.selectTasks(setOf(parent.id, child.id, INBOX_ID, "missing"))
        assertEquals(setOf(parent.id, child.id), state.selectedTaskIds)
        state.requestDeleteSelectedTasks()
        state.clearTaskSelection() // confirmation operates on the captured selection
        state.deleteSelectedTasks()
        assertTrue(state.path.isEmpty())
        assertEquals(0, state.focusedColumn)
        assertEquals(0, state.compactColumnIndex)
        assertTrue(state.selectedTaskIds.isEmpty())
        assertNull(state.pendingTaskDeleteIds)
        state.undo()
        assertEquals(before, state.data)
    }

    @Test
    fun externalDeletionDropsStaleSelections() {
        val scope = TestScope()
        val store = Store(null, scope, "Inbox").also { it.load() }
        val a = store.addTask(null, "A")!!
        val b = store.addTask(null, "B")!!
        val state = AppState(store, scope, EN)
        state.startTaskSelect()
        state.selectTasks(setOf(a.id, b.id))
        store.deleteTask(a.id)
        assertEquals(setOf(b.id), state.selectedTaskIds)
        state.exitTaskSelect()
        assertTrue(state.selectedTaskIds.isEmpty())
    }
}
