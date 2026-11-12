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

/** Requests the runtime permission needed for Bluetooth (Android 12+). Returns true when granted. */
expect suspend fun ensureBluetoothPermission(): Boolean

/** Lists serial ports (Windows COM ports / Linux tty). Empty where unsupported. */
expect suspend fun listSerialPorts(): List<PrinterEndpoint>

/** Sends bytes to a serial port in chunks. Throws on failure. */
expect suspend fun sendSerial(portId: String, bytes: ByteArray, timeoutMs: Int, chunkSize: Int)

/** Whether serial ports are available on this platform. */
expect fun serialSupported(): Boolean

/** Encodes text for the printer in the given charset name ("UTF-8" / "Shift_JIS"). */
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

/** One line of text to rasterize. */
data class TextLine(val text: String, val sizePx: Float, val bold: Boolean = false, val center: Boolean = false)

/** Renders lines (and horizontal rules for empty text with [TextLine.sizePx] <= 0) to a 1-bit bitmap [widthPx] wide. */
expect fun renderTextBitmap(lines: List<TextLine>, widthPx: Int, paddingPx: Int): MonoBitmap

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
