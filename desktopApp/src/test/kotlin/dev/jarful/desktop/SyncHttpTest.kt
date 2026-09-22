package dev.jarful.desktop

import dev.jarful.data.Store
import dev.jarful.data.json
import dev.jarful.model.SyncSettings
import dev.jarful.sync.SyncClient
import dev.jarful.sync.SyncOutcome
import dev.jarful.sync.SyncServer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** End-to-end LAN sync over real sockets on localhost (AC-12, FR-14). */
class SyncHttpTest {
    private fun freePort() = ServerSocket(0).use { it.localPort }

    private fun store(): Store = Store(null, TestScope(), "Inbox").also { it.load() }

    @Test
    fun hostAndClientConverge() = runBlocking {
        val host = store(); val client = store()
        val port = freePort()
        val server = SyncServer(json, pin = { "123456" }, onMerge = { host.mergeFromPeer(it) })
        server.start(port)
        try {
            host.addTask(null, "From host")
            val ct = client.addTask(null, "From client")!!
            client.ticketize(ct.id)
            val outcome = SyncClient(json).sync(SyncSettings(peerHost = "127.0.0.1", peerPort = port, peerPin = "123456", deviceId = "c"), client.syncPayload())
            assertTrue(outcome is SyncOutcome.Ok, "outcome=$outcome")
            client.applySynced((outcome as SyncOutcome.Ok).merged)
            val h = host.data.value.tasks.map { it.title }.toSet(); val c = client.data.value.tasks.map { it.title }.toSet()
            assertEquals(h, c)
            assertTrue("From host" in c && "From client" in h)
            assertEquals(1, host.data.value.tickets.size)
        } finally { server.stop() }
    }

    @Test
    fun wrongPinIsRejectedAndDataUntouched() = runBlocking { // AC-12
        val host = store(); val port = freePort()
        var merged = false
        val server = SyncServer(json, pin = { "123456" }, onMerge = { merged = true; host.mergeFromPeer(it) })
        server.start(port)
        try {
            val client = store(); client.addTask(null, "secret")
            val outcome = SyncClient(json).sync(SyncSettings(peerHost = "127.0.0.1", peerPort = port, peerPin = "000000", deviceId = "c"), client.syncPayload())
            assertEquals(SyncOutcome.Error("PIN_MISMATCH"), outcome)
            assertTrue(!merged); assertTrue(host.data.value.tasks.none { it.title == "secret" })
        } finally { server.stop() }
    }

    @Test
    fun unreachableHostReportsError() = runBlocking {
        val port = freePort()
        val outcome = SyncClient(json, timeoutMs = 1500).sync(SyncSettings(peerHost = "127.0.0.1", peerPort = port, peerPin = "1", deviceId = "c"), store().syncPayload())
        assertTrue(outcome is SyncOutcome.Error && outcome.code == "UNREACHABLE", "outcome=$outcome")
    }
}
