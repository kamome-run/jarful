package dev.jarful.desktop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import dev.jarful.data.Store
import dev.jarful.model.INBOX_ID
import dev.jarful.model.Language
import dev.jarful.model.Routine
import dev.jarful.ui.JarfulApp
import dev.jarful.ui.ds.DesignSystem
import kotlinx.coroutines.test.TestScope
import kotlin.test.Test
import kotlin.test.assertEquals

/** Drag-and-drop reordering with the ≡ handle (FR-1.6, FR-6.10): down, up, one row at a time, inbox pinned. */
@OptIn(ExperimentalTestApi::class)
class DragReorderUiTest {
    private fun store(): Store = Store(null, TestScope(), "Inbox").also {
        it.load(); it.updateSettings { s -> s.copy(onboardingDone = true, language = Language.EN) }
    }

    /** Drags the handle [dy] pixels in small steps like a finger would, then releases. */
    private fun androidx.compose.ui.test.SemanticsNodeInteraction.dragBy(dy: Float) = performTouchInput {
        down(center)
        val steps = 20
        repeat(steps) { moveBy(Offset(0f, dy / steps), delayMillis = 16) }
        up()
    }

    @Test
    fun routinesDragDownThenUpMovesExactlyOneRow() = runComposeUiTest {
        val store = store()
        for (n in listOf("A", "B", "C", "D")) store.upsertRoutine(Routine(id = n, title = n, category = "Morning"))
        setContent { JarfulApp(store, darkTheme = false, designSystemOverride = DesignSystem.MATERIAL) }
        onNodeWithText("Routines").performClick(); waitForIdle()
        fun order() = store.data.value.routines.sortedBy { it.order }.map { it.title }
        onNodeWithTag("drag:A").dragBy(110f); waitForIdle()
        assertEquals(listOf("B", "A", "C", "D"), order())
        onNodeWithTag("drag:D").dragBy(-110f); waitForIdle()
        assertEquals(listOf("B", "A", "D", "C"), order())
        onNodeWithTag("drag:A").dragBy(-110f); waitForIdle()
        assertEquals(listOf("A", "B", "D", "C"), order())
    }

    @Test
    fun columnsDragKeepsInboxOnTop() = runComposeUiTest {
        val store = store()
        for (n in listOf("A", "B", "C")) store.addTask(null, n)
        setContent { JarfulApp(store, darkTheme = false, designSystemOverride = DesignSystem.MATERIAL) }
        // wide layout: the root column is already visible next to the Today list
        waitForIdle()
        fun order() = dev.jarful.domain.TaskTree.childrenOf(store.data.value.tasks, null).map { it.title }
        val a = store.data.value.tasks.first { it.title == "A" }.id
        val c = store.data.value.tasks.first { it.title == "C" }.id
        onNodeWithTag("drag:$a", useUnmergedTree = true).dragBy(120f); waitForIdle()
        assertEquals(listOf("Inbox", "B", "A", "C"), order())
        onNodeWithTag("drag:$c", useUnmergedTree = true).dragBy(-120f); waitForIdle()
        assertEquals(listOf("Inbox", "B", "C", "A"), order())
        // dragging far above the inbox: the row stops right below it
        onNodeWithTag("drag:$a", useUnmergedTree = true).dragBy(-400f); waitForIdle()
        assertEquals(listOf("Inbox", "A", "B", "C"), order())
        assertEquals(INBOX_ID, dev.jarful.domain.TaskTree.childrenOf(store.data.value.tasks, null).first().id)
    }
}
