package dev.jarful.sync

import dev.jarful.model.SyncSettings
import dev.jarful.platform.HttpResult
import dev.jarful.platform.httpPostJson
import kotlinx.serialization.json.Json

sealed class SyncOutcome {
    data class Ok(val merged: SyncPayload) : SyncOutcome()
    data class Error(val code: String, val detail: String = "") : SyncOutcome()
}

/** Pushes the local payload to a peer host and returns the merged result (FR-14.6, FR-14.8). */
class SyncClient(private val json: Json, private val timeoutMs: Int = 5_000) {
    suspend fun sync(s: SyncSettings, local: SyncPayload): SyncOutcome {
        if (s.peerHost.isBlank()) return SyncOutcome.Error("NO_PEER")
        val body = json.encodeToString(SyncRequest.serializer(), SyncRequest(pin = s.peerPin, deviceId = s.deviceId, data = local))
        val url = "http://${s.peerHost.trim()}:${s.peerPort}/sync"
        val res: HttpResult = try { httpPostJson(url, body, timeoutMs) } catch (e: Throwable) {
            return SyncOutcome.Error("UNREACHABLE", e.message ?: "")
        }
        return when (res.status) {
            200 -> runCatching { json.decodeFromString(SyncResponse.serializer(), res.body) }
                .fold({ SyncOutcome.Ok(it.data) }, { SyncOutcome.Error("BAD_RESPONSE", it.message ?: "") })
            403 -> SyncOutcome.Error("PIN_MISMATCH")
            426 -> SyncOutcome.Error("VERSION_MISMATCH")
            else -> SyncOutcome.Error("HTTP_${res.status}", res.body.take(200))
        }
    }
}
