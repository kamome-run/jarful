package dev.jarful.desktop

import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.runDesktopComposeUiTest
import dev.jarful.data.Store
import dev.jarful.model.Language
import dev.jarful.ui.JarfulApp
import dev.jarful.ui.ds.DesignSystem
import kotlinx.coroutines.test.TestScope
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/** Renders the routine dialog at phone portrait and landscape sizes and saves screenshots for review. */
@OptIn(ExperimentalTestApi::class)
class DialogLayoutTest {
    private val out = File("build/screenshots").apply { mkdirs() }

    private fun store(): Store = Store(null, TestScope(), "Inbox").also { it.load(); it.updateSettings { s -> s.copy(onboardingDone = true, language = Language.JA) } }

    private fun shoot(name: String, w: Int, h: Int) = runDesktopComposeUiTest(width = w, height = h) {
        val store = store()
        setContent { JarfulApp(store, darkTheme = false, designSystemOverride = DesignSystem.MATERIAL) }
        onNodeWithText("ルーチン").performClick(); waitForIdle()
        onNodeWithText("ルーチンを追加").performClick(); waitForIdle()
        onNodeWithText("曜日").assertIsDisplayed()
        onNodeWithText("有効").performScrollTo().assertIsDisplayed() // reachable by scrolling on short screens
        val img = onAllNodes(isRoot()).onLast().captureToImage() // the dialog layer
        val png = img.asSkiaBitmap().let { bmp -> org.jetbrains.skia.Image.makeFromBitmap(bmp).encodeToData(EncodedImageFormat.PNG)!!.bytes }
        File(out, "$name.png").writeBytes(png)
    }

    @Test
    fun routineDialogPhonePortrait() = shoot("routine-dialog-412x915", 412, 915)

    @Test
    fun routineDialogPhoneLandscape() = shoot("routine-dialog-915x412", 915, 412)
}
