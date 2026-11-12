package dev.jarful.platform

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.Charset
import java.util.Locale
import java.util.UUID

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

private val ctx: Context get() = AndroidPlatform.appContext

actual class FileStore actual constructor(private val fileName: String) {
    private fun file() = File(ctx.filesDir, fileName)
    actual fun read(): String? = file().takeIf { it.exists() }?.readText()
    actual fun writeAtomic(text: String) {
        val f = file(); val tmp = File(f.parentFile, "$fileName.tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
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

private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

private fun adapter(): BluetoothAdapter? =
    (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

private fun hasConnectPermission(): Boolean =
    Build.VERSION.SDK_INT < 31 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

actual fun bluetoothSupported(): Boolean = ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH) && adapter() != null

actual suspend fun ensureBluetoothPermission(): Boolean {
    if (hasConnectPermission()) return true
    val requester = AndroidPlatform.permissionRequester ?: return false
    val deferred = CompletableDeferred<Boolean>()
    withContext(Dispatchers.Main) {
        requester(arrayOf(Manifest.permission.BLUETOOTH_CONNECT)) { deferred.complete(it) }
    }
    return deferred.await()
}

@Suppress("MissingPermission")
actual suspend fun listBluetoothDevices(): List<PrinterEndpoint> {
    if (!bluetoothSupported() || !ensureBluetoothPermission()) return emptyList()
    val a = adapter() ?: return emptyList()
    return runCatching { a.bondedDevices.map { PrinterEndpoint(it.address, it.name ?: it.address) } }.getOrDefault(emptyList())
        .sortedBy { it.name }
}

@Suppress("MissingPermission")
actual suspend fun sendBluetooth(address: String, bytes: ByteArray, timeoutMs: Int, chunkSize: Int): Unit = withContext(Dispatchers.IO) {
    val a = adapter() ?: throw IllegalStateException("BLUETOOTH_UNAVAILABLE")
    if (!a.isEnabled) throw IllegalStateException("BLUETOOTH_OFF")
    val device = a.getRemoteDevice(address)
    runCatching { a.cancelDiscovery() }
    val socket = try {
        device.createRfcommSocketToServiceRecord(SPP_UUID).also { it.connect() }
    } catch (e: Exception) {
        // Fallback for devices that do not advertise SPP correctly: channel 1 via reflection.
        val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
        (m.invoke(device, 1) as android.bluetooth.BluetoothSocket).also { it.connect() }
    }
    socket.use { s ->
        val out = s.outputStream
        var off = 0
        while (off < bytes.size) {
            val n = minOf(chunkSize, bytes.size - off)
            out.write(bytes, off, n); out.flush()
            off += n
            Thread.sleep(20)
        }
        Thread.sleep(300) // let the printer drain before closing
    }
}

actual fun serialSupported(): Boolean = false
actual suspend fun listSerialPorts(): List<PrinterEndpoint> = emptyList()
actual suspend fun sendSerial(portId: String, bytes: ByteArray, timeoutMs: Int, chunkSize: Int) {
    throw UnsupportedOperationException("SERIAL_UNSUPPORTED")
}

actual fun encodeText(text: String, charsetName: String): ByteArray = text.toByteArray(Charset.forName(charsetName))

actual fun renderTextBitmap(lines: List<TextLine>, widthPx: Int, paddingPx: Int): MonoBitmap {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    data class Placed(val text: String, val line: TextLine, val y: Float)
    val placed = ArrayList<Placed>()
    var y = paddingPx.toFloat()
    val inner = (widthPx - paddingPx * 2).toFloat()
    for (l in lines) {
        if (l.sizePx <= 0f) { placed.add(Placed("", l, y)); y += 8f; continue }
        paint.textSize = l.sizePx
        paint.typeface = if (l.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        val fm = paint.fontMetrics
        val lineH = (fm.descent - fm.ascent) * 1.1f
        var rest = l.text.ifEmpty { " " }
        while (rest.isNotEmpty()) {
            val n = paint.breakText(rest, true, inner, null).coerceAtLeast(1)
            placed.add(Placed(rest.substring(0, n), l, y - fm.ascent))
            rest = rest.substring(n)
            y += lineH
        }
        y += 4f
    }
    val height = (y + paddingPx).toInt().coerceAtLeast(8)
    val bmp = Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp); c.drawColor(Color.WHITE)
    for (p in placed) {
        if (p.line.sizePx <= 0f) {
            paint.strokeWidth = 2f
            c.drawLine(paddingPx.toFloat(), p.y + 3f, (widthPx - paddingPx).toFloat(), p.y + 3f, paint); continue
        }
        paint.textSize = p.line.sizePx
        paint.typeface = if (p.line.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        val w = paint.measureText(p.text)
        val x = if (p.line.center) (widthPx - w) / 2f else paddingPx.toFloat()
        c.drawText(p.text, x, p.y, paint)
    }
    val px = IntArray(widthPx * height); bmp.getPixels(px, 0, widthPx, 0, 0, widthPx, height)
    val wb = (widthPx + 7) / 8
    val rows = Array(height) { yy ->
        val row = ByteArray(wb)
        for (x in 0 until widthPx) {
            val p = px[yy * widthPx + x]
            val lum = (Color.red(p) * 299 + Color.green(p) * 587 + Color.blue(p) * 114) / 1000
            if (lum < 128) row[x / 8] = (row[x / 8].toInt() or (0x80 shr (x % 8))).toByte()
        }
        row
    }
    bmp.recycle()
    return MonoBitmap(widthPx, height, rows)
}

actual class SoundPlayer actual constructor() {
    actual fun play(pcm: ShortArray, sampleRate: Int) {
        runCatching {
            val bytes = pcm.size * 2
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(sampleRate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(bytes)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(pcm, 0, pcm.size)
            track.setNotificationMarkerPosition(pcm.size)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack?) { runCatching { track.release() } }
                override fun onPeriodicNotification(t: AudioTrack?) {}
            })
            track.play()
        }
    }
}

actual fun vibrateShort(durationMs: Long) {
    runCatching {
        val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31) (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        else @Suppress("DEPRECATION") (ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
        if (v == null || !v.hasVibrator()) return
        v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}

actual fun nowMillis(): Long = System.currentTimeMillis()
actual fun systemLanguageTag(): String = Locale.getDefault().toLanguageTag()
actual fun hasHardwareKeyboard(): Boolean = ctx.resources.configuration.keyboard == Configuration.KEYBOARD_QWERTY
actual fun copyToClipboard(text: String) {
    (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.setPrimaryClip(ClipData.newPlainText("Jarful", text))
}
