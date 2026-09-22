package dev.jarful.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Shared JVM implementation used by both Android and desktop (FR-14). */
actual class RawHttpServer actual constructor(private val port: Int, private val handler: (HttpRequest) -> HttpResponse) {
    private var socket: ServerSocket? = null
    private val pool = Executors.newFixedThreadPool(4)
    @Volatile private var stopped = false

    actual fun start() {
        val ss = ServerSocket()
        ss.reuseAddress = true
        ss.bind(InetSocketAddress(port))
        socket = ss
        Thread({
            while (!stopped) {
                val client = try { ss.accept() } catch (_: SocketException) { break } catch (_: Throwable) { continue }
                pool.execute { runCatching { serve(client) } }
            }
        }, "jarful-sync-server").apply { isDaemon = true }.start()
    }

    actual fun stop() {
        stopped = true
        runCatching { socket?.close() }
        pool.shutdown()
        runCatching { pool.awaitTermination(1, TimeUnit.SECONDS) }
    }

    private fun serve(client: Socket) {
        client.soTimeout = 10_000
        client.use { c ->
            val input = c.getInputStream()
            val reader = BufferedReader(InputStreamReader(input, Charsets.ISO_8859_1))
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            val method = parts[0].uppercase()
            val path = parts[1].substringBefore('?')
            var contentLength = 0
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
                val idx = line.indexOf(':')
                if (idx > 0 && line.substring(0, idx).trim().equals("Content-Length", ignoreCase = true)) {
                    contentLength = line.substring(idx + 1).trim().toIntOrNull() ?: 0
                }
            }
            val bodyBytes = ByteArray(contentLength)
            var read = 0
            while (read < contentLength) {
                val ch = reader.read()
                if (ch < 0) break
                bodyBytes[read++] = ch.toByte()
            }
            val body = String(bodyBytes, 0, read, Charsets.UTF_8)
            val resp = try { handler(HttpRequest(method, path, body)) } catch (e: Throwable) { HttpResponse(500, """{"error":"INTERNAL"}""") }
            writeResponse(c.getOutputStream(), resp)
        }
    }

    private fun writeResponse(out: OutputStream, r: HttpResponse) {
        val bytes = r.body.toByteArray(Charsets.UTF_8)
        val reason = when (r.status) { 200 -> "OK"; 400 -> "Bad Request"; 403 -> "Forbidden"; 404 -> "Not Found"; 426 -> "Upgrade Required"; else -> "Error" }
        val head = "HTTP/1.1 ${r.status} $reason\r\nContent-Type: ${r.contentType}\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
        out.write(head.toByteArray(Charsets.ISO_8859_1)); out.write(bytes); out.flush()
    }
}

actual suspend fun httpPostJson(url: String, body: String, timeoutMs: Int): HttpResult = withContext(Dispatchers.IO) {
    val conn = URL(url).openConnection() as HttpURLConnection
    try {
        conn.requestMethod = "POST"
        conn.connectTimeout = timeoutMs
        conn.readTimeout = timeoutMs
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        val bytes = body.toByteArray(Charsets.UTF_8)
        conn.setFixedLengthStreamingMode(bytes.size)
        conn.outputStream.use { it.write(bytes) }
        val status = conn.responseCode
        val stream = if (status in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
        HttpResult(status, text)
    } finally {
        conn.disconnect()
    }
}

actual fun localIpAddresses(): List<String> = runCatching {
    NetworkInterface.getNetworkInterfaces().toList()
        .filter { it.isUp && !it.isLoopback && !it.isVirtual }
        .flatMap { ni -> ni.inetAddresses.toList().filter { it is java.net.Inet4Address && !it.isLoopbackAddress }.map { it.hostAddress } }
        .distinct()
}.getOrDefault(emptyList())
