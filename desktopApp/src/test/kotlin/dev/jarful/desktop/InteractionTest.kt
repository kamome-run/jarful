package dev.jarful.desktop

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import dev.jarful.data.Store
import dev.jarful.model.Language
import dev.jarful.ui.JarfulApp
import dev.jarful.ui.ds.DesignSystem
import kotlinx.coroutines.test.TestScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression tests for the "settings toggles do nothing" bug: screens must observe the store through
 * snapshot state, otherwise strong skipping leaves them stale after a click.
 */
@OptIn(ExperimentalTestApi::class)
class InteractionTest {
    private fun store(): Store = Store(null, TestScope(), "Inbox").also {
        it.load(); it.updateSettings { s -> s.copy(onboardingDone = true, language = Language.EN) }
    }

    @Test
    fun settingsSwitchTogglesAndRerenders() = runComposeUiTest {
        val store = store()
        setContent { JarfulApp(store, darkTheme = false, designSystemOverride = DesignSystem.MATERIAL) }
        onNodeWithText("Settings").performClick()
        onNodeWithText("Sound effects").assertIsDisplayed()
        val before = store.data.value.settings.soundEnabled
        onAllNodes(isToggleable()).onFirst().performClick() // first switch = sound effects
        waitForIdle()
        assertEquals(!before, store.data.value.settings.soundEnabled)
        onAllNodes(isToggleable()).onFirst().performClick()
        waitForIdle()
        assertEquals(before, store.data.value.settings.soundEnabled)
    }

    @Test
    fun bluetoothMacFieldIsShownForBluetoothTransports() = runComposeUiTest {
        val store = store()
        store.updateSettings { it.copy(printer = it.printer.copy(transport = dev.jarful.model.PrinterTransport.BLUETOOTH_LE)) }
        setContent { JarfulApp(store, darkTheme = false, designSystemOverride = DesignSystem.MATERIAL) }
        onNodeWithText("Settings").performClick(); waitForIdle()
        onNodeWithText("Bluetooth MAC").performScrollTo().assertIsDisplayed()
        store.updateSettings { it.copy(printer = it.printer.copy(transport = dev.jarful.model.PrinterTransport.TCP)) }
        waitForIdle()
        assertTrue(onAllNodes(hasText("Bluetooth MAC")).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun addingATaskShowsItInTheColumn() = runComposeUiTest {
        val store = store()
        setContent { JarfulApp(store, darkTheme = false, designSystemOverride = DesignSystem.MATERIAL) }
        // wide layout: the columns pane is visible on the Today screen already
        store.addTask(null, "Water the plants")
        waitForIdle()
        onNodeWithText("Water the plants").assertIsDisplayed()
        store.renameTask(store.data.value.tasks.first { it.title == "Water the plants" }.id, "Water the cactus")
        waitForIdle()
        onNodeWithText("Water the cactus").assertIsDisplayed()
        assertTrue(onAllNodes(hasText("Water the plants")).fetchSemanticsNodes().isEmpty())
    }
}
