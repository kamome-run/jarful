package dev.jarful.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dev.jarful.data.Store
import dev.jarful.model.INBOX_ID
import dev.jarful.model.Language
import dev.jarful.ui.JarfulApp
import dev.jarful.ui.ds.DesignSystem
import kotlinx.coroutines.test.TestScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TaskBatchDeleteUiTest {
    private fun store() = Store(null, TestScope(), "Inbox").also {
        it.load()
        it.updateSettings { settings -> settings.copy(onboardingDone = true, language = Language.EN) }
    }

    @Test
    fun selectCancelDeleteAndUndoOnDesktop() = runComposeUiTest {
        val store = store()
        val a = store.addTask(null, "Alpha")!!
        val child = store.addTask(a.id, "Nested")!!
        val b = store.addTask(null, "Beta")!!
        val keep = store.addTask(null, "Keep")!!
        store.ticketize(child.id)
        val before = store.data.value
        setContent { JarfulApp(store, darkTheme = false, designSystemOverride = DesignSystem.FLUENT) }
        onNodeWithTag("taskSelectMode").performClick()
        onNodeWithTag("deleteSelectedTasks").assertIsNotEnabled()
        onNodeWithTag("selectTask:${a.id}", useUnmergedTree = true).performClick()
        onNodeWithText("Beta").performClick()
        onNodeWithTag("deleteSelectedTasks").performClick()
        onNodeWithText("Cancel").performClick()
        assertEquals(before, store.data.value)
        onNodeWithTag("deleteSelectedTasks").performClick()
        onNodeWithText("Delete").performClick()
        waitForIdle()
        assertEquals(setOf(INBOX_ID, keep.id), store.data.value.tasks.map { it.id }.toSet())
        assertTrue(store.data.value.tickets.isEmpty())
        onNodeWithTag("undoTaskAction").performClick()
        waitForIdle()
        assertEquals(before, store.data.value)
        assertFalse(store.data.value.tasks.first { it.id == b.id }.done)
    }

    @Test
    fun compactSelectionNavigatesToChildrenWithoutCompletingTasks() = runComposeUiTest {
        val store = store()
        val parent = store.addTask(null, "Parent")!!
        val child = store.addTask(parent.id, "Child")!!
        val sibling = store.addTask(null, "Keep")!!
        val before = store.data.value
        setContent {
            Box(Modifier.width(400.dp)) {
                JarfulApp(store, darkTheme = false, designSystemOverride = DesignSystem.MATERIAL)
            }
        }
        onNodeWithText("Columns").performClick()
        onNodeWithTag("taskSelectMode").performClick()
        onNodeWithTag("selectTask:${parent.id}", useUnmergedTree = true).performClick()
        onNodeWithTag("openTask:${parent.id}").performClick()
        onNodeWithTag("taskSelectAll").performClick()
        onNodeWithText("2 selected").assertExists()
        assertEquals(before, store.data.value)
        onNodeWithTag("taskSelectNone").performClick()
        onNodeWithTag("deleteSelectedTasks").assertIsNotEnabled()
        onNodeWithTag("taskSelectAll").performClick()
        onNodeWithTag("deleteSelectedTasks").performClick()
        onNodeWithText("Delete").performClick()
        waitForIdle()
        assertEquals(setOf(INBOX_ID, parent.id, sibling.id), store.data.value.tasks.map { it.id }.toSet())
        assertFalse(store.data.value.tasks.any { it.id == child.id })
        onNodeWithTag("undoTaskAction").performClick()
        waitForIdle()
        assertEquals(before, store.data.value)
    }

    @Test
    fun selectAllProtectsInboxAndHiddenTasksAndClosingClearsSelection() = runComposeUiTest {
        val store = store()
        store.addTask(null, "Alpha")
        store.addTask(null, "Beta")
        val hidden = store.addTask(null, "Hidden completed task")!!
        store.toggleTaskDone(hidden.id)
        store.updateSettings { it.copy(showCompleted = false) }
        val before = store.data.value
        setContent { JarfulApp(store, darkTheme = false, designSystemOverride = DesignSystem.MATERIAL) }
        onNodeWithTag("taskSelectMode").performClick()
        onNodeWithTag("taskSelectAll").performClick()
        onNodeWithText("2 selected").assertExists()
        onNodeWithTag("taskSelectMode").performClick()
        onNodeWithTag("taskSelectMode").performClick()
        onNodeWithTag("deleteSelectedTasks").assertIsNotEnabled()
        assertEquals(before, store.data.value)
    }
}
