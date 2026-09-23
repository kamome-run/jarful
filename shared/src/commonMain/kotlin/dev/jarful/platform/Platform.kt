package dev.jarful.platform

import kotlinx.coroutines.CoroutineDispatcher

/** Dispatcher for blocking I/O (file, socket). */
expect val ioDispatcher: CoroutineDispatcher

/** Simple atomic text-file storage in the app's private data directory (FR-12.1). */
expect class FileStore(fileName: String) {
    fun read(): String?
    fun writeAtomic(text: String)
    fun path(): String
}

/** Sends raw bytes to a TCP endpoint (port 9100 printers). Throws on failure. */
expect suspend fun sendRawTcp(host: String, port: Int, bytes: ByteArray, timeoutMs: Int)

/** A paired Bluetooth Classic device or a serial port, as shown in the picker. */
data class PrinterEndpoint(val id: String, val name: String)

/** Lists paired Bluetooth Classic devices (Android). Empty where unsupported. */
expect suspend fun listBluetoothDevices(): List<PrinterEndpoint>

/** Sends bytes over Bluetooth SPP/RFCOMM to [address] in [chunkSize] pieces (FR-9.8). Throws on failure. */
expect suspend fun sendBluetooth(address: String, bytes: ByteArray, timeoutMs: Int, chunkSize: Int)

/** Whether Bluetooth Classic is available on this platform. */
expect fun bluetoothSupported(): Boolean

/** Whether Bluetooth Low Energy (GATT) is available on this platform. */
expect fun bleSupported(): Boolean

/** Short human-readable note on the Bluetooth capabilities of this build (shown in Settings → About). */
expect fun bluetoothCapabilityNote(): String

/**
 * Requests the runtime permissions needed for Bluetooth: connect (and scan/location when
 * [scan] is true, needed to discover BLE printers). Returns true when granted.
 */
expect suspend fun ensureBluetoothPermission(scan: Boolean = false): Boolean

/** Scans for nearby BLE devices for a few seconds and lists them together with bonded LE devices. */
expect suspend fun listBleDevices(): List<PrinterEndpoint>

/**
 * One step of a BLE print job: write [bytes] (chunked to the MTU) to [characteristic] (UUID string, or
 * null = auto-detect a well-known printer characteristic), then optionally wait up to ~3 s for a
 * notification on [awaitNotify] and sleep [delayMs].
 */
class BleWrite(val characteristic: String?, val bytes: ByteArray, val awaitNotify: String? = null, val delayMs: Long = 0)

/**
 * Runs a BLE print job over GATT: connects, negotiates the MTU, then executes [plan] in order (FR-9.1 d).
 * Protocols that need a control channel plus a data channel (MXW01) use several writes.
 * Returns every notification received during the job as "<char short id>:<hex>" for diagnostics.
 */
expect suspend fun sendBlePlan(address: String, plan: List<BleWrite>, timeoutMs: Int, chunkSize: Int): List<String>

/** Sends bytes to a BLE printer's auto-detected write characteristic. */
suspend fun sendBle(address: String, bytes: ByteArray, timeoutMs: Int, chunkSize: Int) {
    sendBlePlan(address, listOf(BleWrite(null, bytes)), timeoutMs, chunkSize)
}

/** Lists serial ports (Windows COM ports / Linux tty). Empty where unsupported. */
expect suspend fun listSerialPorts(): List<PrinterEndpoint>

/** Sends bytes to a serial port in chunks. Throws on failure. */
expect suspend fun sendSerial(portId: String, bytes: ByteArray, timeoutMs: Int, chunkSize: Int)

/** Whether serial ports are available on this platform. */
expect fun serialSupported(): Boolean

/**
 * Human-readable Bluetooth connection diagnostics for [address]: adapter state, device type and bond
 * state, SPP/SDP UUIDs, every Classic connect strategy with its error, and the GATT services /
 * characteristics found over LE. Used by the "Diagnose" button so users can paste the result into an issue.
 */
expect suspend fun diagnoseBluetooth(address: String): String

/** Encodes text for the printer in the given Java charset name (e.g. "UTF-8", "Shift_JIS", "windows-1256"). */
expect fun encodeText(text: String, charsetName: String): ByteArray

/** A 1-bit monochrome image: rows of packed bytes, MSB first, 1 = black. */
class MonoBitmap(val width: Int, val height: Int, val rows: Array<ByteArray>) {
    val widthBytes: Int get() = (width + 7) / 8
    fun packed(): ByteArray {
        val out = ByteArray(widthBytes * height)
        rows.forEachIndexed { y, r -> r.copyInto(out, y * widthBytes) }
        return out
    }
}

/** One line of text to rasterize. [rtl] right-aligns and lays the run out right-to-left (Arabic). */
data class TextLine(val text: String, val sizePx: Float, val bold: Boolean = false, val center: Boolean = false, val rtl: Boolean = false)

/**
 * Renders lines (and horizontal rules for empty text with [TextLine.sizePx] <= 0) to a 1-bit bitmap
 * [widthPx] wide. Implementations must pick a font that covers each line's script (CJK, Arabic,
 * Cyrillic, Vietnamese...), apply bidi/shaping, and honour [TextLine.rtl] (FR-9.4).
 */
expect fun renderTextBitmap(lines: List<TextLine>, widthPx: Int, paddingPx: Int): MonoBitmap

/** True when [text] contains right-to-left script characters (Arabic, Hebrew). */
fun isRtlText(text: String): Boolean = text.any { c -> c.code in 0x0590..0x08FF || c.code in 0xFB1D..0xFDFF || c.code in 0xFE70..0xFEFF }

/** Plays a short PCM 16-bit mono clip. */
expect class SoundPlayer() {
    fun play(pcm: ShortArray, sampleRate: Int)
}

/** Haptic feedback; no-op when the device has no vibrator (NFR-CB). */
expect fun vibrateShort(durationMs: Long)

/** Current epoch millis. */
expect fun nowMillis(): Long

/** BCP-47 language tag of the system locale, e.g. "ja-JP". */
expect fun systemLanguageTag(): String

/** Whether a hardware keyboard is likely attached (affects shortcut hints only). */
expect fun hasHardwareKeyboard(): Boolean

/** Copies text to the system clipboard. */
expect fun copyToClipboard(text: String)

// ----- LAN sync transport (FR-14) -----

data class HttpRequest(val method: String, val path: String, val body: String)
data class HttpResponse(val status: Int, val body: String, val contentType: String = "application/json; charset=utf-8")
data class HttpResult(val status: Int, val body: String)

/** Minimal HTTP/1.1 server bound to all interfaces. [handler] runs on a worker thread. */
expect class RawHttpServer(port: Int, handler: (HttpRequest) -> HttpResponse) {
    fun start()
    fun stop()
}

/** POSTs a JSON body and returns status + body. Throws on connection failure/timeout. */
expect suspend fun httpPostJson(url: String, body: String, timeoutMs: Int): HttpResult

/** Non-loopback IPv4 addresses of this device, for showing on the host's settings screen. */
expect fun localIpAddresses(): List<String>

// ----- Design system (NFR-8) -----

/** System UI font for the Fluent design system (Segoe UI Variable / Segoe UI on Windows), null if unavailable. */
expect fun fluentFontFamily(): androidx.compose.ui.text.font.FontFamily?
