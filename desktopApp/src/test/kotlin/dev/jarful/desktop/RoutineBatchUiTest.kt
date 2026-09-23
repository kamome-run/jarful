package dev.jarful.desktop

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import dev.jarful.data.Store
import dev.jarful.model.Language
import dev.jarful.model.Routine
import dev.jarful.ui.JarfulApp
import dev.jarful.ui.ds.DesignSystem
import kotlinx.coroutines.test.TestScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** v1.7 routine features: category headings, bulk toggle, duplicate with a count, print-for-a-date dialog. */
@OptIn(ExperimentalTestApi::class)
class RoutineBatchUiTest {
    private fun store(): Store = Store(null, TestScope(), "Inbox").also {
        it.load(); it.updateSettings { s -> s.copy(onboardingDone = true, language = Language.EN) }
        it.upsertRoutine(Routine(id = "r1", title = "Coffee", category = "Morning"))
        it.upsertRoutine(Routine(id = "r2", title = "Stretch", category = "Morning"))
        it.upsertRoutine(Routine(id = "r3", title = "Mail", category = "Work"))
    }

    @Test
    fun categoryHeadingsAndBulkDisable() = runComposeUiTest {
        val store = store()
        setContent { JarfulApp(store, darkTheme = false, designSystemOverride = DesignSystem.MATERIAL) }
        onNodeWithText("Routines").performClick(); waitForIdle()
        onNodeWithText("Morning").assertIsDisplayed()
        onNodeWithText("Work").assertIsDisplayed()
        onNodeWithTag("routineSelectMode").performClick(); waitForIdle()
        onNodeWithText("Select all").performClick(); waitForIdle()
        onNodeWithTag("disableSelected").performClick(); waitForIdle()
        assertTrue(store.data.value.routines.none { it.enabled })
        onNodeWithTag("enableSelected").performClick(); waitForIdle()
        assertTrue(store.data.value.routines.all { it.enabled })
    }

    @Test
    fun duplicateRoutineWithCount() = runComposeUiTest {
        val store = store()
        setContent { JarfulApp(store, darkTheme = false, designSystemOverride = DesignSystem.MATERIAL) }
        onNodeWithText("Routines").performClick(); waitForIdle()
        onAllNodesWithContentDescription("Duplicate").onFirst().performClick(); waitForIdle()
        onNodeWithTag("duplicateCount").performTextClearance()
        onNodeWithTag("duplicateCount").performTextInput("3"); waitForIdle()
        onNodeWithTag("duplicateOk").performClick(); waitForIdle()
        assertEquals(4, store.data.value.routines.count { it.title == "Coffee" })
    }

    @Test
    fun printForDateDialogListsTomorrow() = runComposeUiTest {
        val store = store()
        setContent { JarfulApp(store, darkTheme = false, designSystemOverride = DesignSystem.MATERIAL) }
        onNodeWithText("Routines").performClick(); waitForIdle()
        onNodeWithTag("routinePrintDate").performClick(); waitForIdle()
        onNodeWithText("Date to print").assertIsDisplayed()
        assertTrue(onAllNodes(hasText("Tomorrow", substring = true)).fetchSemanticsNodes().isNotEmpty())
        onNodeWithTag("printDate:1").performScrollTo().performClick(); waitForIdle()
        // the routines were added after load(), so the only routine tickets are the ones generated for the chosen day
        val generated = store.data.value.tickets.filter { it.routineId != null }
        assertEquals(3, generated.size)
        assertEquals(1, generated.map { it.date }.toSet().size)
    }
}
