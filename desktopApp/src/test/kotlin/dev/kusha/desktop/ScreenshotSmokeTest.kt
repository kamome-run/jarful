package dev.kusha.desktop

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.use
import dev.kusha.data.Store
import dev.kusha.domain.Ids
import dev.kusha.model.PaperWidth
import dev.kusha.model.Routine
import dev.kusha.model.Ticket
import dev.kusha.platform.MonoBitmap
import dev.kusha.print.TicketFormatter
import dev.kusha.ui.KushaApp
import dev.kusha.ui.i18n.JA
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestScope
import org.jetbrains.skia.EncodedImageFormat
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Headless smoke test: renders the real app offscreen with Skia (no display needed) at desktop and
 * phone sizes, and rasterizes a ticket the way the printer path does. Outputs land in build/screenshots.
 */
@OptIn(ExperimentalComposeUiApi::class)
class ScreenshotSmokeTest {
    private val out = File("build/screenshots").apply { mkdirs() }

    private fun seededStore(): Store {
        val store = Store(null, TestScope(), JA.inbox)
        store.load()
        val house = store.addTask(null, "家の掃除")!!
        val kitchen = store.addTask(house.id, "キッチン")!!
        store.addTask(kitchen.id, "皿を洗う"); store.addTask(kitchen.id, "台を拭く"); store.addTask(kitchen.id, "床を掃く"); store.addTask(kitchen.id, "ゴミを出す")
        val bath = store.addTask(house.id, "浴室")!!
        store.addTask(bath.id, "シャワーを洗う"); store.addTask(bath.id, "トイレを洗う")
        store.addTask(null, "夕食を作る"); store.addTask(null, "旅行の計画")
        store.upsertRoutine(Routine(id = Ids.next("r"), title = "コーヒーを淹れる", category = "朝", estimateMin = 3, order = 0))
        store.upsertRoutine(Routine(id = Ids.next("r"), title = "メールを処理", category = "仕事", quotaTarget = 10, order = 1))
        store.updateSettings { it.copy(onboardingDone = true, language = dev.kusha.model.Language.JA) }
        store.regenerateToday()
        store.ticketizeColumn(kitchen.id)
        val first = store.data.value.tickets.first { it.title == "皿を洗う" }
        store.completeTicket(first.id)
        store.completeTicket(store.data.value.tickets.first { it.title == "台を拭く" }.id)
        store.startTicket(store.data.value.tickets.first { it.title == "床を掃く" }.id)
        return store
    }

    private fun shoot(name: String, w: Int, h: Int) {
        val store = seededStore()
        ImageComposeScene(width = w, height = h, coroutineContext = Dispatchers.Unconfined).use { scene ->
            scene.setContent { KushaApp(store) }
            scene.render(1_000_000_000L)
            val img = scene.render(2_000_000_000L)
            val png = img.encodeToData(EncodedImageFormat.PNG)!!.bytes
            File(out, "$name.png").writeBytes(png)
            assertTrue(png.size > 10_000, "screenshot too small: ${png.size}")
        }
    }

    @Test
    fun desktopWideLayout() = shoot("desktop-1280x800", 1280, 800)

    @Test
    fun phoneCompactLayout() = shoot("phone-412x915", 412, 915)

    @Test
    fun ticketRasterLooksRight() {
        val t = Ticket(id = "k", date = "2026-09-22", category = "キッチン", title = "皿を洗う（食洗機に入れるだけ）", estimateMin = 5, timeboxMin = 10)
        val bmp = TicketFormatter.renderTicket(t, PaperWidth.MM58, "2026-09-22 (火)")
        assertTrue(bmp.width == 384 && bmp.height in 60..600, "unexpected size ${bmp.width}x${bmp.height}")
        val black = (0 until bmp.height).sumOf { y -> (0 until bmp.width).count { x -> (bmp.rows[y][x / 8].toInt() and (0x80 shr (x % 8))) != 0 } }
        assertTrue(black > 500, "ticket bitmap has almost no ink: $black")
        ImageIO.write(toImage(bmp), "png", File(out, "ticket-raster-384.png"))
        val bytes = TicketFormatter.encodeAll(listOf(t, t.copy(id = "k2", title = "ゴミを出す")), dev.kusha.model.PrinterSettings()) { "2026-09-22 (火)" }
        assertTrue(bytes.size > bmp.widthBytes * bmp.height, "raster bytes should contain the image")
    }

    private fun toImage(b: MonoBitmap): BufferedImage {
        val img = BufferedImage(b.width, b.height, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until b.height) for (x in 0 until b.width) {
            val on = (b.rows[y][x / 8].toInt() and (0x80 shr (x % 8))) != 0
            img.setRGB(x, y, if (on) 0x000000 else 0xFFFFFF)
        }
        return img
    }
}
