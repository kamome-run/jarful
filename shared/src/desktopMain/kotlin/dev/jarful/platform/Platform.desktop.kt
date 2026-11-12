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
import java.util.concurrent.TimeUnit
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
actual suspend fun ensureBluetoothPermission(scan: Boolean): Boolean = true

// ----- Bluetooth LE on Windows through a bundled PowerShell/WinRT helper (FR-9.1 d) -----

private val isWindows: Boolean get() = System.getProperty("os.name").lowercase().contains("win")

@Volatile private var bleUnavailableReason: String = ""

private val bleScript: File? by lazy {
    if (!isWindows) { bleUnavailableReason = "not Windows (" + System.getProperty("os.name") + ")"; return@lazy null }
    try {
        val bytes = object {}.javaClass.getResourceAsStream("/jarful-ble.ps1")?.readBytes()
        if (bytes == null) { bleUnavailableReason = "helper script missing from the build"; return@lazy null }
        val f = File(dataDirectory(), "jarful-ble.ps1")
        if (!f.exists() || !f.readBytes().contentEquals(bytes)) f.writeBytes(bytes)
        f
    } catch (e: Throwable) { bleUnavailableReason = "cannot extract helper: " + (e.message ?: e::class.simpleName); null }
}

actual fun bluetoothCapabilityNote(): String =
    if (bleSupported()) "Bluetooth LE: available (PowerShell/WinRT helper at " + bleScript?.absolutePath + ")"
    else "Bluetooth LE: unavailable — " + bleUnavailableReason.ifBlank { "unknown" } + ". Use a COM port or TCP."


private fun powershell(): String {
    val root = System.getenv("SystemRoot") ?: "C:\\Windows"
    val exe = File(root, "System32\\WindowsPowerShell\\v1.0\\powershell.exe")
    return if (exe.exists()) exe.absolutePath else "powershell.exe"
}

/** Runs the helper and returns stdout; throws with the first stderr line on failure. */
private fun runBle(vararg args: String, timeoutMs: Long = 30_000): String {
    val script = bleScript ?: throw UnsupportedOperationException("BLE_UNSUPPORTED_ON_THIS_OS")
    val pb = ProcessBuilder(listOf(powershell(), "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-File", script.absolutePath) + args)
    val p = pb.start()
    val out = StringBuilder(); val err = StringBuilder()
    val nativeCharset = runCatching { Charset.forName(System.getProperty("native.encoding") ?: System.getProperty("sun.jnu.encoding") ?: "MS932") }.getOrDefault(Charsets.UTF_8)
    val tOut = thread(isDaemon = true) { p.inputStream.bufferedReader(Charsets.UTF_8).useLines { it.forEach { l -> out.appendLine(l) } } }
    val tErr = thread(isDaemon = true) { p.errorStream.bufferedReader(nativeCharset).useLines { it.forEach { l -> err.appendLine(l) } } }
    if (!p.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) { p.destroyForcibly(); throw IllegalStateException("BLE_HELPER_TIMEOUT") }
    tOut.join(1000); tErr.join(1000)
    runCatching { File(dataDirectory(), "jarful-ble.log").appendText("[" + java.time.LocalDateTime.now() + "] " + args.joinToString(" ") + "\n" + out + err + "\n") }
    if (p.exitValue() != 0) {
        val fromScript = out.lines().firstOrNull { it.startsWith("ERROR:") }
        throw IllegalStateException((fromScript ?: err.lines().firstOrNull { it.isNotBlank() } ?: "BLE_HELPER_FAILED(${p.exitValue()})").take(400))
    }
    return out.toString()
}

actual fun bleSupported(): Boolean = bleScript != null

actual suspend fun listBleDevices(): List<PrinterEndpoint> = withContext(Dispatchers.IO) {
    runCatching {
        runBle("list").lines().filter { it.contains('|') }.map { val (a, n) = it.split('|', limit = 2); PrinterEndpoint(a.trim(), n.trim()) }
            .distinctBy { it.id }.sortedBy { it.name }
    }.getOrDefault(emptyList())
}

actual suspend fun sendBlePlan(address: String, plan: List<BleWrite>, timeoutMs: Int, chunkSize: Int): List<String> = withContext(Dispatchers.IO) {
    val tmp = File.createTempFile("jarful-ble", ".bin")
    try {
        for (step in plan) {
            tmp.writeBytes(step.bytes)
            // Windows negotiates the MTU itself; 20-byte chunks are safe for every printer.
            runBle("write", address, step.characteristic ?: "auto", tmp.absolutePath, minOf(chunkSize, 20).toString(), timeoutMs = timeoutMs.toLong() + 60_000)
            if (step.delayMs > 0) Thread.sleep(step.delayMs)
        }
    } finally { tmp.delete() }
    emptyList()
}

actual suspend fun listBluetoothDevices(): List<PrinterEndpoint> = emptyList()
actual suspend fun sendBluetooth(address: String, bytes: ByteArray, timeoutMs: Int, chunkSize: Int) {
    throw UnsupportedOperationException("BLUETOOTH_UNSUPPORTED_USE_SERIAL")
}

actual fun serialSupported(): Boolean = true
actual suspend fun diagnoseBluetooth(address: String): String = withContext(Dispatchers.IO) {
    if (!bleSupported()) return@withContext "Bluetooth diagnostics: BLE helper unavailable on this OS. Use a COM port (README 6.5) or TCP."
    val sb = StringBuilder("Jarful Bluetooth diagnostics (Windows, WinRT helper)\n")
    sb.appendLine("ports: " + runCatching { SerialPort.getCommPorts().joinToString { it.systemPortName + " " + it.descriptivePortName } }.getOrDefault("?"))
    sb.append(runCatching { runBle("services", address) }.getOrElse { "le: error " + (it.message ?: "?") + "\n" })
    sb.toString()
}

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

/** Prefer breaking a line at the last space before [fit] (space-separated scripts); CJK breaks anywhere. */
internal fun wordBoundary(text: String, fit: Int): Int {
    if (fit >= text.length) return fit
    val cut = text.lastIndexOf(' ', fit - 1)
    return if (cut > 0 && fit - cut <= 24) cut + 1 else fit
}

actual fun renderTextBitmap(lines: List<TextLine>, widthPx: Int, paddingPx: Int): MonoBitmap {
    System.setProperty("java.awt.headless", "true")
    data class Placed(val text: String, val line: TextLine, val y: Int)
    val probe = BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
    val pg = probe.createGraphics()
    val placed = ArrayList<Placed>()
    var y = paddingPx
    val inner = widthPx - paddingPx * 2
    // Pick a font per line so a Japanese category and an Arabic title each get a font that covers their script.
    fun fontFor(l: TextLine) = Font(printerFontFamily(l.text), if (l.bold) Font.BOLD else Font.PLAIN, l.sizePx.toInt())
    for (l in lines) {
        if (l.sizePx <= 0f) { placed.add(Placed("", l, y)); y += 8; continue }
        val fm = pg.getFontMetrics(fontFor(l))
        val lineH = (fm.height * 1.1f).toInt()
        var rest = l.text.ifEmpty { " " }
        while (rest.isNotEmpty()) {
            var n = rest.length
            while (n > 1 && fm.stringWidth(rest.substring(0, n)) > inner) n--
            n = wordBoundary(rest, n)
            placed.add(Placed(rest.substring(0, n).trimEnd(), l, y + fm.ascent))
            rest = rest.substring(n).trimStart()
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
        // Java2D drawString performs bidi reordering and Arabic shaping for complex scripts.
        val x = when {
            p.line.center -> (widthPx - w) / 2
            p.line.rtl -> (widthPx - paddingPx) - w
            else -> paddingPx
        }
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

/** Preferred UI/print fonts per script family; the first one installed that covers the text wins (FR-9.4). */
private val preferredPrinterFonts = listOf(
    // Latin, Cyrillic, Vietnamese, Arabic (Windows system fonts first)
    "Segoe UI", "Segoe UI Variable Text", "Arial", "Tahoma", "Noto Sans", "Noto Sans Arabic", "DejaVu Sans", "Liberation Sans",
    // Japanese
    "Yu Gothic UI", "Yu Gothic", "Meiryo UI", "Meiryo", "MS Gothic", "BIZ UDGothic", "Noto Sans JP", "Noto Sans CJK JP", "IPAGothic", "VL Gothic", "Hiragino Sans",
    // Traditional Chinese (Taiwan)
    "Microsoft JhengHei UI", "Microsoft JhengHei", "PMingLiU", "Noto Sans TC", "Noto Sans CJK TC", "PingFang TC",
    // Simplified Chinese
    "Microsoft YaHei UI", "Microsoft YaHei", "Noto Sans SC", "Noto Sans CJK SC",
)

private val familyCache = java.util.concurrent.ConcurrentHashMap<String, String>()

/** Picks a font family that can display every character of [text]; cached per script signature. */
fun printerFontFamily(text: String): String {
    val key = text.map { Character.UnicodeScript.of(it.code).name }.distinct().sorted().joinToString(",")
    familyCache[key]?.let { return it }
    val ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
    val available = ge.availableFontFamilyNames.toSet()
    val candidates = preferredPrinterFonts.filter { it in available } + Font.SANS_SERIF + Font.DIALOG
    val chosen = candidates.firstOrNull { Font(it, Font.PLAIN, 12).canDisplayUpTo(text) == -1 }
        ?: ge.allFonts.firstOrNull { it.canDisplayUpTo(text) == -1 }?.family
        ?: Font.SANS_SERIF
    familyCache[key] = chosen
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
