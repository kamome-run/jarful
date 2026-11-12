package dev.jarful.sync

import dev.jarful.platform.HttpRequest
import dev.jarful.platform.HttpResponse
import dev.jarful.platform.RawHttpServer
import kotlinx.serialization.json.Json

/**
 * Hosts the /sync and /ping endpoints (FR-14.1, FR-14.7). [onMerge] receives the remote payload,
 * must merge it into local state and return the merged payload that both sides will keep.
 */
class SyncServer(
    private val json: Json,
    private val pin: () -> String,
    private val onMerge: (SyncPayload) -> SyncPayload,
) {
    private var server: RawHttpServer? = null
    val running: Boolean get() = server != null

    fun start(port: Int) {
        if (server != null) return
        server = RawHttpServer(port, ::handle).also { it.start() }
    }

    fun stop() { server?.stop(); server = null }

    private fun jsonResponse(status: Int, body: String) = HttpResponse(status, body, "application/json; charset=utf-8")

    fun handle(req: HttpRequest): HttpResponse {
        return when {
            req.method == "GET" && req.path == "/ping" ->
                jsonResponse(200, """{"app":"jarful","protocolVersion":$SYNC_PROTOCOL_VERSION}""")
            req.method == "POST" && req.path == "/sync" -> {
                val r = runCatching { json.decodeFromString(SyncRequest.serializer(), req.body) }.getOrNull()
                    ?: return jsonResponse(400, """{"error":"BAD_REQUEST"}""")
                if (r.protocolVersion != SYNC_PROTOCOL_VERSION) return jsonResponse(426, """{"error":"VERSION_MISMATCH"}""")
                val expected = pin()
                if (expected.isBlank() || r.pin != expected) return jsonResponse(403, """{"error":"PIN_MISMATCH"}""")
                val merged = onMerge(r.data)
                jsonResponse(200, json.encodeToString(SyncResponse.serializer(), SyncResponse(data = merged)))
            }
            else -> jsonResponse(404, """{"error":"NOT_FOUND"}""")
        }
    }
}
