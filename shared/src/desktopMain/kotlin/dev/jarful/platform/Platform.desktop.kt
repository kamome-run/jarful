package dev.jarful.platform

import com.fazecast.jSerialComm.SerialPort
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.image.BufferedImage
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.Charset
import java.util.Locale
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.concurrent.thread

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

fun dataDirectory(): File {
    val os = System.getProperty("os.name").lowercase()
    val dir = when {
        os.contains("win") -> File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "Jarful")
        os.contains("mac") -> File(System.getProperty("user.home"), "Library/Application Support/Jarful")
        else -> File(System.getenv("XDG_DATA_HOME") ?: (System.getProperty("user.home") + "/.local/share"), "jarful")
    }
    dir.mkdirs()
    return dir
}

actual class FileStore actual constructor(private val fileName: String) {
    private fun file() = File(dataDirectory(), fileName)
    actual fun read(): String? = file().takeIf { it.exists() }?.readText()
    actual fun writeAtomic(text: String) {
        val f = file(); val tmp = File(f.parentFile, "$fileName.tmp")
        tmp.writeText(text)
        try {
            java.nio.file.Files.move(tmp.toPath(), f.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Exception) {
            java.nio.file.Files.move(tmp.toPath(), f.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        }
    }
    actual fun path(): String = file().absolutePath
}

actual suspend fun sendRawTcp(host: String, port: Int, bytes: ByteArray, timeoutMs: Int): Unit = withContext(Dispatchers.IO) {
    Socket().use { s ->
        s.connect(InetSocketAddress(host, port), timeoutMs)
        s.soTimeout = timeoutMs
        val out = s.getOutputStream()
        out.write(bytes); out.flush()
    }
}

actual fun bluetoothSupported(): Boolean = false
actual suspend fun ensureBluetoothPermission(): Boolean = true
actual suspend fun listBluetoothDevices(): List<PrinterEndpoint> = emptyList()
actual suspend fun sendBluetooth(address: String, bytes: ByteArray, timeoutMs: Int, chunkSize: Int) {
    throw UnsupportedOperationException("BLUETOOTH_UNSUPPORTED_USE_SERIAL")
}

actual fun serialSupported(): Boolean = true

actual suspend fun listSerialPorts(): List<PrinterEndpoint> = withContext(Dispatchers.IO) {
    runCatching {
        SerialPort.getCommPorts().map { PrinterEndpoint(it.systemPortName, "${it.systemPortName} — ${it.descriptivePortName}") }
    }.getOrDefault(emptyList())
}

actual suspend fun sendSerial(portId: String, bytes: ByteArray, timeoutMs: Int, chunkSize: Int): Unit = withContext(Dispatchers.IO) {
    val port = SerialPort.getCommPort(portId)
    port.setComPortParameters(115200, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY)
    port.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, timeoutMs, timeoutMs)
    if (!port.openPort(timeoutMs)) throw IllegalStateException("PORT_OPEN_FAILED: $portId")
    try {
        var off = 0
        while (off < bytes.size) {
            val n = minOf(chunkSize, bytes.size - off)
            val written = port.writeBytes(bytes, n, off)
            if (written < 0) throw IllegalStateException("WRITE_FAILED")
            off += n
            Thread.sleep(20)
        }
        Thread.sleep(300)
    } finally {
        port.closePort()
    }
}

actual fun encodeText(text: String, charsetName: String): ByteArray = text.toByteArray(Charset.forName(charsetName))

actual fun renderTextBitmap(lines: List<TextLine>, widthPx: Int, paddingPx: Int): MonoBitmap {
    System.setProperty("java.awt.headless", "true")
    data class Placed(val text: String, val line: TextLine, val y: Int)
    val probe = BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
    val pg = probe.createGraphics()
    val placed = ArrayList<Placed>()
    var y = paddingPx
    val inner = widthPx - paddingPx * 2
    val allText = lines.joinToString("") { it.text }
    val family = printerFontFamily(allText)
    fun fontFor(l: TextLine) = Font(family, if (l.bold) Font.BOLD else Font.PLAIN, l.sizePx.toInt())
    for (l in lines) {
        if (l.sizePx <= 0f) { placed.add(Placed("", l, y)); y += 8; continue }
        val fm = pg.getFontMetrics(fontFor(l))
        val lineH = (fm.height * 1.1f).toInt()
        var rest = l.text.ifEmpty { " " }
        while (rest.isNotEmpty()) {
            var n = rest.length
            while (n > 1 && fm.stringWidth(rest.substring(0, n)) > inner) n--
            placed.add(Placed(rest.substring(0, n), l, y + fm.ascent))
            rest = rest.substring(n)
            y += lineH
        }
        y += 4
    }
    pg.dispose()
    val height = (y + paddingPx).coerceAtLeast(8)
    val img = BufferedImage(widthPx, height, BufferedImage.TYPE_INT_RGB)
    val g = img.createGraphics()
    g.color = Color.WHITE; g.fillRect(0, 0, widthPx, height)
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    g.color = Color.BLACK
    for (p in placed) {
        if (p.line.sizePx <= 0f) { g.fillRect(paddingPx, p.y + 2, widthPx - paddingPx * 2, 2); continue }
        g.font = fontFor(p.line)
        val w = g.fontMetrics.stringWidth(p.text)
        val x = if (p.line.center) (widthPx - w) / 2 else paddingPx
        g.drawString(p.text, x, p.y)
    }
    g.dispose()
    val wb = (widthPx + 7) / 8
    val rows = Array(height) { yy ->
        val row = ByteArray(wb)
        for (x in 0 until widthPx) {
            val rgb = img.getRGB(x, yy)
            val lum = (((rgb shr 16) and 0xFF) * 299 + ((rgb shr 8) and 0xFF) * 587 + (rgb and 0xFF) * 114) / 1000
            if (lum < 128) row[x / 8] = (row[x / 8].toInt() or (0x80 shr (x % 8))).toByte()
        }
        row
    }
    return MonoBitmap(widthPx, height, rows)
}

private val preferredPrinterFonts = listOf(
    "Yu Gothic UI", "Yu Gothic", "Meiryo UI", "Meiryo", "MS Gothic", "MS UI Gothic", "BIZ UDGothic",
    "Noto Sans JP", "Noto Sans CJK JP", "Source Han Sans JP", "IPAGothic", "IPAexGothic", "TakaoGothic", "VL Gothic",
    "Hiragino Sans", "Hiragino Kaku Gothic ProN",
)

@Volatile private var cachedFamily: String? = null

/** Picks a font family that can display [text]; prefers Japanese-capable system fonts (FR-9.4). */
fun printerFontFamily(text: String): String {
    cachedFamily?.let { fam -> if (Font(fam, Font.PLAIN, 12).canDisplayUpTo(text) == -1) return fam }
    val ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
    val available = ge.availableFontFamilyNames.toSet()
    val candidates = preferredPrinterFonts.filter { it in available } + Font.SANS_SERIF + Font.DIALOG
    val chosen = candidates.firstOrNull { Font(it, Font.PLAIN, 12).canDisplayUpTo(text) == -1 }
        ?: ge.allFonts.firstOrNull { it.canDisplayUpTo(text) == -1 }?.family
        ?: Font.SANS_SERIF
    cachedFamily = chosen
    return chosen
}

actual class SoundPlayer actual constructor() {
    actual fun play(pcm: ShortArray, sampleRate: Int) {
        thread(isDaemon = true, name = "jarful-sound") {
            runCatching {
                val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
                val bytes = ByteArray(pcm.size * 2)
                for (i in pcm.indices) { val v = pcm[i].toInt(); bytes[i * 2] = (v and 0xFF).toByte(); bytes[i * 2 + 1] = ((v shr 8) and 0xFF).toByte() }
                val line = AudioSystem.getSourceDataLine(format)
                line.open(format); line.start(); line.write(bytes, 0, bytes.size); line.drain(); line.close()
            }
        }
    }
}

actual fun vibrateShort(durationMs: Long) { /* no haptics on desktop */ }
actual fun nowMillis(): Long = System.currentTimeMillis()
actual fun systemLanguageTag(): String = Locale.getDefault().toLanguageTag()
actual fun hasHardwareKeyboard(): Boolean = true
actual fun copyToClipboard(text: String) {
    runCatching { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null) }
}
