package dev.jarful.desktop

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.use
import dev.jarful.data.Store
import dev.jarful.domain.Ids
import dev.jarful.model.PaperWidth
import dev.jarful.model.Routine
import dev.jarful.model.Ticket
import dev.jarful.platform.MonoBitmap
import dev.jarful.print.TicketFormatter
import dev.jarful.ui.JarfulApp
import dev.jarful.ui.ds.DesignSystem
import dev.jarful.ui.i18n.JA
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

    private fun seededStore(lang: dev.jarful.model.Language = dev.jarful.model.Language.JA): Store {
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
        store.updateSettings { it.copy(onboardingDone = true, language = lang) }
        store.regenerateToday()
        store.ticketizeColumn(kitchen.id)
        val first = store.data.value.tickets.first { it.title == "皿を洗う" }
        store.completeTicket(first.id)
        store.completeTicket(store.data.value.tickets.first { it.title == "台を拭く" }.id)
        store.startTicket(store.data.value.tickets.first { it.title == "床を掃く" }.id)
        return store
    }

    private fun shoot(name: String, w: Int, h: Int, dark: Boolean = false, system: DesignSystem? = null, lang: dev.jarful.model.Language = dev.jarful.model.Language.JA) {
        val store = seededStore(lang)
        ImageComposeScene(width = w, height = h, coroutineContext = Dispatchers.Unconfined).use { scene ->
            scene.setContent { JarfulApp(store, darkTheme = dark, designSystemOverride = system) }
            scene.render(1_000_000_000L)
            val img = scene.render(2_000_000_000L)
            val png = img.encodeToData(EncodedImageFormat.PNG)!!.bytes
            File(out, "$name.png").writeBytes(png)
            assertTrue(png.size > 10_000, "screenshot too small: ${png.size}")
        }
    }

    // Windows: Fluent Design (desktop default)
    @Test
    fun windowsFluentLight() = shoot("windows-fluent-1280x800", 1280, 800, system = DesignSystem.FLUENT)

    @Test
    fun windowsFluentDark() = shoot("windows-fluent-1280x800-dark", 1280, 800, dark = true, system = DesignSystem.FLUENT)

    @Test
    fun windowsFluentNarrow() = shoot("windows-fluent-600x800", 600, 800, system = DesignSystem.FLUENT)

    // Android: Material 3
    @Test
    fun androidMaterialTablet() = shoot("android-material-1280x800", 1280, 800, system = DesignSystem.MATERIAL)

    @Test
    fun androidMaterialPhone() = shoot("android-material-412x915", 412, 915, system = DesignSystem.MATERIAL)

    @Test
    fun androidMaterialPhoneDark() = shoot("android-material-412x915-dark", 412, 915, dark = true, system = DesignSystem.MATERIAL)

    @Test
    fun arabicRtlLayout() = shoot("android-material-412x915-ar", 412, 915, system = DesignSystem.MATERIAL, lang = dev.jarful.model.Language.AR)

    @Test
    fun taiwaneseMandarinFluent() = shoot("windows-fluent-1280x800-zh-TW", 1280, 800, system = DesignSystem.FLUENT, lang = dev.jarful.model.Language.ZH_TW)

    @Test
    fun multilingualTicketsRasterize() {
        val samples = listOf(
            "ar" to Ticket(id = "1", date = "2026-09-23", category = "المطبخ", title = "غسل الأطباق (ضعها في غسالة الصحون فقط)", estimateMin = 5),
            "ru" to Ticket(id = "2", date = "2026-09-23", category = "Кухня", title = "Помыть посуду", estimateMin = 5),
            "vi" to Ticket(id = "3", date = "2026-09-23", category = "Bếp", title = "Rửa chén (chỉ cần xếp vào máy)", estimateMin = 5),
            "zh-TW" to Ticket(id = "4", date = "2026-09-23", category = "廚房", title = "洗碗（放進洗碗機就好）", estimateMin = 5),
            "pl" to Ticket(id = "5", date = "2026-09-23", category = "Kuchnia", title = "Zmyć naczynia (włożyć do zmywarki)", estimateMin = 5),
        )
        for ((lang, t) in samples) {
            val bmp = TicketFormatter.renderTicket(t, PaperWidth.MM58, "2026-09-23")
            val black = (0 until bmp.height).sumOf { y -> (0 until bmp.width).count { x -> (bmp.rows[y][x / 8].toInt() and (0x80 shr (x % 8))) != 0 } }
            assertTrue(black > 500, "$lang ticket has almost no ink: $black")
            ImageIO.write(toImage(bmp), "png", File(out, "ticket-raster-$lang.png"))
        }
    }

    @Test
    fun ticketRasterLooksRight() {
        val t = Ticket(id = "k", date = "2026-09-22", category = "キッチン", title = "皿を洗う（食洗機に入れるだけ）", estimateMin = 5, timeboxMin = 10)
        val bmp = TicketFormatter.renderTicket(t, PaperWidth.MM58, "2026-09-22 (火)")
        assertTrue(bmp.width == 384 && bmp.height in 60..600, "unexpected size ${bmp.width}x${bmp.height}")
        val black = (0 until bmp.height).sumOf { y -> (0 until bmp.width).count { x -> (bmp.rows[y][x / 8].toInt() and (0x80 shr (x % 8))) != 0 } }
        assertTrue(black > 500, "ticket bitmap has almost no ink: $black")
        ImageIO.write(toImage(bmp), "png", File(out, "ticket-raster-384.png"))
        val bytes = TicketFormatter.encodeAll(listOf(t, t.copy(id = "k2", title = "ゴミを出す")), dev.jarful.model.PrinterSettings()) { "2026-09-22 (火)" }
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
