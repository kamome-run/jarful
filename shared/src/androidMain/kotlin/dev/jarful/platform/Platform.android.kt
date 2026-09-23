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
import java.util.concurrent.TimeUnit

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

private fun granted(p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

actual suspend fun ensureBluetoothPermission(scan: Boolean): Boolean {
    val needed = ArrayList<String>()
    if (Build.VERSION.SDK_INT >= 31) {
        if (!granted(Manifest.permission.BLUETOOTH_CONNECT)) needed += Manifest.permission.BLUETOOTH_CONNECT
        if (scan && !granted(Manifest.permission.BLUETOOTH_SCAN)) needed += Manifest.permission.BLUETOOTH_SCAN
    } else if (scan && !granted(Manifest.permission.ACCESS_FINE_LOCATION)) {
        needed += Manifest.permission.ACCESS_FINE_LOCATION // BLE scanning needs location before Android 12
    }
    if (needed.isEmpty()) return true
    val requester = AndroidPlatform.permissionRequester ?: return false
    val deferred = CompletableDeferred<Boolean>()
    withContext(Dispatchers.Main) { requester(needed.toTypedArray()) { deferred.complete(it) } }
    return deferred.await()
}

actual fun bleSupported(): Boolean = ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) && adapter() != null

/** Well-known writable characteristics of BLE thermal printers, tried in order before a generic search. */
private val BLE_PRINTER_CHARACTERISTICS = listOf(
    UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb"), // generic FF00 service (most 58mm pocket printers)
    UUID.fromString("0000ae01-0000-1000-8000-00805f9b34fb"), // "cat" printers (AE30 service)
    UUID.fromString("00002af1-0000-1000-8000-00805f9b34fb"), // BLE printer profile (18F0 service)
    UUID.fromString("49535343-8841-43f4-a8d4-ecbe34729bb3"), // ISSC / Microchip transparent UART
    UUID.fromString("bef8d6c9-9c21-4c9e-b632-bd58c1009f9f"), // Phomemo / Peripage family
    UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"), // Nordic UART TX
)

@Suppress("MissingPermission")
actual suspend fun listBleDevices(): List<PrinterEndpoint> {
    if (!bleSupported() || !ensureBluetoothPermission(scan = true)) return emptyList()
    val a = adapter() ?: return emptyList()
    if (!a.isEnabled) return emptyList()
    val found = LinkedHashMap<String, PrinterEndpoint>()
    runCatching {
        a.bondedDevices.filter { it.type == android.bluetooth.BluetoothDevice.DEVICE_TYPE_LE || it.type == android.bluetooth.BluetoothDevice.DEVICE_TYPE_DUAL }
            .forEach { found[it.address] = PrinterEndpoint(it.address, it.name ?: it.address) }
    }
    val scanner = a.bluetoothLeScanner ?: return found.values.toList()
    val done = CompletableDeferred<Unit>()
    val cb = object : android.bluetooth.le.ScanCallback() {
        override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult) {
            val d = result.device ?: return
            val name = result.scanRecord?.deviceName ?: d.name ?: return // unnamed devices are rarely printers
            found[d.address] = PrinterEndpoint(d.address, name)
        }
        override fun onScanFailed(errorCode: Int) { done.complete(Unit) }
    }
    val settings = android.bluetooth.le.ScanSettings.Builder().setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY).build()
    withContext(Dispatchers.Main) { runCatching { scanner.startScan(null, settings, cb) }.onFailure { done.complete(Unit) } }
    kotlinx.coroutines.withTimeoutOrNull(4_000) { done.await() }
    withContext(Dispatchers.Main) { runCatching { scanner.stopScan(cb) } }
    return found.values.sortedBy { it.name }
}

private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

@Suppress("MissingPermission")
actual suspend fun sendBlePlan(address: String, plan: List<BleWrite>, timeoutMs: Int, chunkSize: Int): List<String> = withContext(Dispatchers.IO) {
    val received = java.util.Collections.synchronizedList(ArrayList<String>())
    fun record(c: android.bluetooth.BluetoothGattCharacteristic, v: ByteArray?) { received += c.uuid.toString().substring(4, 8) + ":" + (v ?: ByteArray(0)).joinToString("") { "%02x".format(it) } }
    val a = adapter() ?: throw IllegalStateException("BLUETOOTH_UNAVAILABLE")
    if (!a.isEnabled) throw IllegalStateException("BLUETOOTH_OFF")
    val device = a.getRemoteDevice(address)
    val connected = java.util.concurrent.CountDownLatch(1)
    val discovered = java.util.concurrent.CountDownLatch(1)
    val mtuLatch = java.util.concurrent.CountDownLatch(1)
    var writeLatch = java.util.concurrent.CountDownLatch(1)
    var descLatch = java.util.concurrent.CountDownLatch(1)
    var notifyLatch = java.util.concurrent.CountDownLatch(1)
    var notifyFilter: UUID? = null
    var mtu = 23
    var failure: String? = null
    val cb = object : android.bluetooth.BluetoothGattCallback() {
        override fun onConnectionStateChange(g: android.bluetooth.BluetoothGatt, status: Int, newState: Int) {
            if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) connected.countDown()
            else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) { failure = failure ?: "DISCONNECTED($status)"; connected.countDown(); discovered.countDown(); mtuLatch.countDown(); writeLatch.countDown(); descLatch.countDown(); notifyLatch.countDown() }
        }
        override fun onServicesDiscovered(g: android.bluetooth.BluetoothGatt, status: Int) { discovered.countDown() }
        override fun onMtuChanged(g: android.bluetooth.BluetoothGatt, m: Int, status: Int) { if (status == android.bluetooth.BluetoothGatt.GATT_SUCCESS) mtu = m; mtuLatch.countDown() }
        override fun onCharacteristicWrite(g: android.bluetooth.BluetoothGatt, c: android.bluetooth.BluetoothGattCharacteristic, status: Int) {
            if (status != android.bluetooth.BluetoothGatt.GATT_SUCCESS) failure = "WRITE_FAILED($status)"
            writeLatch.countDown()
        }
        override fun onDescriptorWrite(g: android.bluetooth.BluetoothGatt, d: android.bluetooth.BluetoothGattDescriptor, status: Int) { descLatch.countDown() }
        @Deprecated("pre-33 callback")
        override fun onCharacteristicChanged(g: android.bluetooth.BluetoothGatt, c: android.bluetooth.BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT < 33) record(c, @Suppress("DEPRECATION") c.value)
            if (notifyFilter == null || c.uuid == notifyFilter) notifyLatch.countDown()
        }
        override fun onCharacteristicChanged(g: android.bluetooth.BluetoothGatt, c: android.bluetooth.BluetoothGattCharacteristic, value: ByteArray) {
            record(c, value)
            if (notifyFilter == null || c.uuid == notifyFilter) notifyLatch.countDown()
        }
    }
    val gatt = device.connectGatt(ctx, false, cb, android.bluetooth.BluetoothDevice.TRANSPORT_LE)
        ?: throw IllegalStateException("GATT_CONNECT_FAILED")
    try {
        if (!connected.await(timeoutMs.toLong(), TimeUnit.MILLISECONDS) || failure != null) throw IllegalStateException(failure ?: "CONNECT_TIMEOUT")
        if (!gatt.requestMtu(512)) mtuLatch.countDown()
        mtuLatch.await(3, TimeUnit.SECONDS)
        gatt.discoverServices()
        if (!discovered.await(timeoutMs.toLong(), TimeUnit.MILLISECONDS) || failure != null) throw IllegalStateException(failure ?: "DISCOVERY_TIMEOUT")
        val all = gatt.services.flatMap { it.characteristics }
        fun writable(c: android.bluetooth.BluetoothGattCharacteristic) =
            c.properties and (android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE or android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
        fun resolve(uuid: String?): android.bluetooth.BluetoothGattCharacteristic {
            if (uuid != null) return all.firstOrNull { it.uuid.toString().equals(uuid, ignoreCase = true) } ?: throw IllegalStateException("CHARACTERISTIC_NOT_FOUND:$uuid")
            return BLE_PRINTER_CHARACTERISTICS.firstNotNullOfOrNull { u -> all.firstOrNull { it.uuid == u && writable(it) } }
                ?: all.firstOrNull { writable(it) && it.service.uuid.toString().let { u -> !u.startsWith("00001800") && !u.startsWith("00001801") } }
                ?: throw IllegalStateException("NO_WRITABLE_CHARACTERISTIC")
        }
        val enabledNotify = HashSet<UUID>()
        fun enableNotify(uuid: String) {
            val c = all.firstOrNull { it.uuid.toString().equals(uuid, ignoreCase = true) } ?: return
            if (!enabledNotify.add(c.uuid)) return
            gatt.setCharacteristicNotification(c, true)
            val d = c.getDescriptor(CCCD_UUID) ?: return
            descLatch = java.util.concurrent.CountDownLatch(1)
            val indicate = c.properties and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
            val value = if (indicate) android.bluetooth.BluetoothGattDescriptor.ENABLE_INDICATION_VALUE else android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            if (Build.VERSION.SDK_INT >= 33) gatt.writeDescriptor(d, value) else @Suppress("DEPRECATION") run { d.value = value; gatt.writeDescriptor(d) }
            descLatch.await(2, TimeUnit.SECONDS)
        }
        for (step in plan) {
            step.awaitNotify?.let { enableNotify(it) }
            val target = resolve(step.characteristic)
            val noResponse = target.properties and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0
            target.writeType = if (noResponse) android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE else android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            val chunk = minOf(chunkSize, (mtu - 3).coerceAtLeast(20))
            if (step.awaitNotify != null) { notifyFilter = UUID.fromString(step.awaitNotify); notifyLatch = java.util.concurrent.CountDownLatch(1) }
            var off = 0
            while (off < step.bytes.size) {
                val n = minOf(chunk, step.bytes.size - off)
                val part = step.bytes.copyOfRange(off, off + n)
                writeLatch = java.util.concurrent.CountDownLatch(1)
                val ok = if (Build.VERSION.SDK_INT >= 33) {
                    gatt.writeCharacteristic(target, part, target.writeType) == android.bluetooth.BluetoothStatusCodes.SUCCESS
                } else {
                    @Suppress("DEPRECATION") run { target.value = part; gatt.writeCharacteristic(target) }
                }
                if (!ok) throw IllegalStateException("WRITE_REJECTED")
                if (!writeLatch.await(timeoutMs.toLong(), TimeUnit.MILLISECONDS)) throw IllegalStateException("WRITE_TIMEOUT")
                failure?.let { throw IllegalStateException(it) }
                off += n
                if (noResponse) Thread.sleep(12) // pace writes for printers without flow control
            }
            if (step.awaitNotify != null) notifyLatch.await(3, TimeUnit.SECONDS) // tolerant: continue even without a reply
            if (step.delayMs > 0) Thread.sleep(step.delayMs)
        }
        Thread.sleep(400)
        received.toList()
    } finally {
        runCatching { gatt.disconnect() }; runCatching { gatt.close() }
    }
}

@Suppress("MissingPermission")
actual suspend fun listBluetoothDevices(): List<PrinterEndpoint> {
    if (!bluetoothSupported() || !ensureBluetoothPermission(scan = false)) return emptyList()
    val a = adapter() ?: return emptyList()
    return runCatching { a.bondedDevices.map { PrinterEndpoint(it.address, it.name ?: it.address) } }.getOrDefault(emptyList())
        .sortedBy { it.name }
}

@Suppress("MissingPermission")
private fun classicStrategies(device: android.bluetooth.BluetoothDevice): List<Pair<String, () -> android.bluetooth.BluetoothSocket>> = listOf(
    "SPP secure" to { device.createRfcommSocketToServiceRecord(SPP_UUID) },
    "SPP insecure" to { device.createInsecureRfcommSocketToServiceRecord(SPP_UUID) },
    "RFCOMM channel 1" to { device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType).invoke(device, 1) as android.bluetooth.BluetoothSocket },
    "RFCOMM channel 1 insecure" to { device.javaClass.getMethod("createInsecureRfcommSocket", Int::class.javaPrimitiveType).invoke(device, 1) as android.bluetooth.BluetoothSocket },
)

/** Connects with each strategy in turn; returns the socket or throws with every error collected. */
@Suppress("MissingPermission")
private fun connectClassic(a: BluetoothAdapter, device: android.bluetooth.BluetoothDevice, log: MutableList<String>? = null): android.bluetooth.BluetoothSocket {
    val errors = ArrayList<String>()
    for ((name, make) in classicStrategies(device)) {
        runCatching { a.cancelDiscovery() }
        Thread.sleep(150)
        val socket = try { make() } catch (e: Throwable) { errors += "$name: create failed (${e.message ?: e::class.simpleName})"; log?.add(errors.last()); continue }
        try {
            socket.connect()
            log?.add("$name: connected")
            return socket
        } catch (e: Throwable) {
            errors += "$name: ${e.message ?: e::class.simpleName}"
            log?.add(errors.last())
            runCatching { socket.close() }
        }
    }
    throw IllegalStateException(errors.joinToString("; "))
}

@Suppress("MissingPermission")
actual suspend fun sendBluetooth(address: String, bytes: ByteArray, timeoutMs: Int, chunkSize: Int): Unit = withContext(Dispatchers.IO) {
    val a = adapter() ?: throw IllegalStateException("BLUETOOTH_UNAVAILABLE")
    if (!a.isEnabled) throw IllegalStateException("BLUETOOTH_OFF")
    val device = a.getRemoteDevice(address)
    if (device.bondState != android.bluetooth.BluetoothDevice.BOND_BONDED) throw IllegalStateException("NOT_PAIRED")
    val socket = connectClassic(a, device)
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

@Suppress("MissingPermission")
actual suspend fun diagnoseBluetooth(address: String): String = withContext(Dispatchers.IO) {
    val sb = StringBuilder()
    sb.appendLine("Jarful Bluetooth diagnostics")
    sb.appendLine("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) ${Build.MANUFACTURER} ${Build.MODEL}")
    val a = adapter()
    if (a == null) { sb.appendLine("adapter: none"); return@withContext sb.toString() }
    sb.appendLine("adapter enabled: ${a.isEnabled}; classic=${bluetoothSupported()} le=${bleSupported()}")
    sb.appendLine("permissions: connect=${Build.VERSION.SDK_INT < 31 || granted(Manifest.permission.BLUETOOTH_CONNECT)} scan=${Build.VERSION.SDK_INT < 31 || granted(Manifest.permission.BLUETOOTH_SCAN)} location=${granted(Manifest.permission.ACCESS_FINE_LOCATION)}")
    if (!a.isEnabled) return@withContext sb.toString()
    val device = a.getRemoteDevice(address)
    val type = when (device.type) {
        android.bluetooth.BluetoothDevice.DEVICE_TYPE_CLASSIC -> "CLASSIC (BR/EDR)"
        android.bluetooth.BluetoothDevice.DEVICE_TYPE_LE -> "LE only"
        android.bluetooth.BluetoothDevice.DEVICE_TYPE_DUAL -> "DUAL (BR/EDR + LE)"
        else -> "UNKNOWN"
    }
    val bond = when (device.bondState) { android.bluetooth.BluetoothDevice.BOND_BONDED -> "BONDED"; android.bluetooth.BluetoothDevice.BOND_BONDING -> "BONDING"; else -> "NOT BONDED" }
    sb.appendLine("device: ${device.name ?: "?"} [$address] type=$type bond=$bond")
    runCatching { device.fetchUuidsWithSdp() }
    Thread.sleep(2500)
    val uuids = device.uuids?.map { it.uuid.toString() } ?: emptyList()
    sb.appendLine("SDP uuids (${uuids.size}): ${if (uuids.isEmpty()) "none" else uuids.joinToString(", ")}")
    sb.appendLine("has SPP (00001101): ${uuids.any { it.startsWith("00001101") }}")

    if (device.bondState == android.bluetooth.BluetoothDevice.BOND_BONDED) {
        val log = ArrayList<String>()
        try { connectClassic(a, device, log).close(); sb.appendLine("classic: OK") } catch (e: Throwable) { sb.appendLine("classic: FAILED") }
        log.forEach { sb.appendLine("  $it") }
    } else sb.appendLine("classic: skipped (not paired)")

    if (bleSupported()) {
        val connected = java.util.concurrent.CountDownLatch(1)
        val discovered = java.util.concurrent.CountDownLatch(1)
        var status = -1
        val cb = object : android.bluetooth.BluetoothGattCallback() {
            override fun onConnectionStateChange(g: android.bluetooth.BluetoothGatt, st: Int, newState: Int) {
                status = st
                if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) connected.countDown() else { connected.countDown(); discovered.countDown() }
            }
            override fun onServicesDiscovered(g: android.bluetooth.BluetoothGatt, st: Int) { discovered.countDown() }
        }
        val gatt = device.connectGatt(ctx, false, cb, android.bluetooth.BluetoothDevice.TRANSPORT_LE)
        if (gatt == null) sb.appendLine("le: connectGatt returned null") else try {
            if (!connected.await(10, TimeUnit.SECONDS)) sb.appendLine("le: connect timeout")
            else if (status != android.bluetooth.BluetoothGatt.GATT_SUCCESS) sb.appendLine("le: connect failed status=$status")
            else {
                gatt.discoverServices()
                if (!discovered.await(10, TimeUnit.SECONDS)) sb.appendLine("le: discovery timeout")
                else {
                    sb.appendLine("le: connected, ${gatt.services.size} services")
                    for (svc in gatt.services) {
                        sb.appendLine("  service ${svc.uuid}")
                        for (c in svc.characteristics) {
                            val p = c.properties
                            val props = buildList {
                                if (p and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ != 0) add("R")
                                if (p and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE != 0) add("W")
                                if (p and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) add("WNR")
                                if (p and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) add("N")
                                if (p and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) add("I")
                            }.joinToString("")
                            val known = if (BLE_PRINTER_CHARACTERISTICS.contains(c.uuid)) " <- known printer characteristic" else ""
                            sb.appendLine("    char ${c.uuid} [$props]$known")
                        }
                    }
                }
            }
        } finally { runCatching { gatt.disconnect() }; runCatching { gatt.close() } }
    }
    sb.toString()
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
            var n = paint.breakText(rest, true, inner, null).coerceAtLeast(1)
            if (n < rest.length) { val cut = rest.lastIndexOf(' ', n - 1); if (cut > 0 && n - cut <= 24) n = cut + 1 }
            placed.add(Placed(rest.substring(0, n).trimEnd(), l, y - fm.ascent))
            rest = rest.substring(n).trimStart()
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
        // Android applies script fallback fonts, Arabic shaping and bidi inside drawText; we only choose alignment.
        val x = when {
            p.line.center -> (widthPx - w) / 2f
            p.line.rtl -> (widthPx - paddingPx) - w
            else -> paddingPx.toFloat()
        }
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
