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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import dev.jarful.data.Store
import java.time.LocalDate
import dev.jarful.model.Language
import dev.jarful.ui.JarfulApp
import dev.jarful.ui.ds.DesignSystem
import kotlinx.coroutines.test.TestScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TicketBatchDeleteUiTest {
    private fun store() = Store(null, TestScope(), "Inbox").also {
        it.load()
        it.updateSettings { settings -> settings.copy(onboardingDone = true, language = Language.EN) }
    }

    @Test
    fun desktopSelectCancelDeleteAndUndoPreservesSourceTasks() = runComposeUiTest {
        val store = store()
        val a = store.addTask(null, "Alpha")!!
        val b = store.addTask(null, "Beta")!!
        val selected = store.ticketize(a.id)!!
        val keep = store.ticketize(b.id)!!
        val before = store.data.value
        setContent { JarfulApp(store, darkTheme = false, designSystemOverride = DesignSystem.FLUENT) }
        onNodeWithTag("ticketSelectMode").performClick()
        onNodeWithTag("deleteSelectedTickets").assertIsNotEnabled()
        onNodeWithTag("selectTicket:${selected.id}", useUnmergedTree = true).performScrollTo().performClick()
        assertEquals(before, store.data.value)
        onNodeWithTag("deleteSelectedTickets").performClick()
        onNodeWithText("Cancel").performClick()
        assertEquals(before, store.data.value)
        onNodeWithTag("deleteSelectedTickets").performClick()
        onNodeWithText("Delete").performClick()
        waitForIdle()
        assertEquals(listOf(keep.id), store.data.value.tickets.map { it.id })
        assertEquals(before.tasks, store.data.value.tasks)
        onNodeWithTag("undoTicketAction").performClick()
        waitForIdle()
        assertEquals(before, store.data.value)
    }

    @Test
    fun compactSelectAllIncludesCompletedButLeavesOtherDates() = runComposeUiTest {
        val store = store()
        val a = store.addTask(null, "Alpha")!!
        val b = store.addTask(null, "Beta")!!
        store.ticketize(a.id)
        val done = store.ticketize(b.id)!!
        store.completeTicket(done.id)
        val old = store.ticketize(a.id, LocalDate.now().minusDays(1).toString())!!
        val future = store.ticketize(a.id, LocalDate.now().plusDays(1).toString())!!
        val before = store.data.value
        setContent {
            Box(Modifier.width(400.dp)) {
                JarfulApp(store, darkTheme = false, designSystemOverride = DesignSystem.MATERIAL)
            }
        }
        onNodeWithTag("ticketSelectMode").performClick()
        onNodeWithTag("ticketSelectAll").performClick()
        onNodeWithText("2 selected").assertExists()
        onNodeWithTag("ticketSelectNone").performClick()
        onNodeWithTag("deleteSelectedTickets").assertIsNotEnabled()
        onNodeWithTag("ticketSelectAll").performClick()
        onNodeWithTag("deleteSelectedTickets").performClick()
        onNodeWithText("Delete").performClick()
        waitForIdle()
        assertEquals(setOf(old.id, future.id), store.data.value.tickets.map { it.id }.toSet())
        assertEquals(before.tasks, store.data.value.tasks)
        onNodeWithTag("undoTicketAction").performClick()
        waitForIdle()
        assertEquals(before, store.data.value)
        onNodeWithTag("ticketSelectMode").performClick()
        onNodeWithTag("ticketSelectAll").performClick()
        onNodeWithTag("ticketSelectMode").performClick()
        onNodeWithTag("ticketSelectMode").performClick()
        onNodeWithTag("deleteSelectedTickets").assertIsNotEnabled()
        assertTrue(store.data.value.tasks.first { it.id == b.id }.done)
    }
}
