package dev.jarful

import dev.jarful.data.Store
import dev.jarful.model.PrinterSettings
import dev.jarful.model.PrinterTransport
import dev.jarful.print.PrintResult
import dev.jarful.print.PrinterClient
import dev.jarful.print.TicketFormatter
import dev.jarful.ui.AppState
import dev.jarful.ui.PrintTarget
import dev.jarful.ui.i18n.EN
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** FR-9.12: tickets print one by one; a failure keeps the rest queued; resume prints only the rest. */
class PrintQueueTest {
    private class FakePrinter(var failAt: Int) : PrinterClient() {
        val sent = ArrayList<Int>()
        var calls = 0
        override suspend fun send(s: PrinterSettings, job: TicketFormatter.PrintJob): PrintResult {
            calls++
            if (calls == failAt) return PrintResult.Error("PAPER_OUT")
            sent += calls
            return PrintResult.Ok(PrinterTransport.TCP)
        }
    }

    private suspend fun waitIdle(state: AppState) = withTimeout(20_000) { while (state.printing) delay(50) }

    @Test
    fun transientFailuresAreRetriedBeforePausing() = runBlocking {
        val store = Store(null, TestScope(), "Inbox").also { it.load() }
        store.updateSettings { it.copy(printer = PrinterSettings(transport = PrinterTransport.TCP, host = "127.0.0.1")) }
        val p = store.addTask(null, "P")!!
        listOf("a", "b").forEach { store.addTask(p.id, it) }
        store.ticketizeColumn(p.id)
        // the 2nd label fails twice (dropped link), then prints
        val printer = object : PrinterClient() {
            var calls = 0
            override suspend fun send(s: PrinterSettings, job: TicketFormatter.PrintJob): PrintResult {
                calls++
                return if (calls == 2 || calls == 3) PrintResult.Error("DISCONNECTED(8)") else PrintResult.Ok(PrinterTransport.TCP)
            }
        }
        val state = AppState(store, CoroutineScope(SupervisorJob() + Dispatchers.Default), EN, printer)
        state.printRetryDelaysMs = listOf(10, 10, 10)
        state.print(PrintTarget.Today)
        waitIdle(state)
        assertTrue(state.printQueue.isEmpty(), "retries should have finished the queue")
        assertEquals(4, printer.calls) // ok, fail, fail, ok
        assertEquals(null, state.printLastError)
    }

    @Test
    fun persistentFailurePausesAfterAllRetries() = runBlocking {
        val store = Store(null, TestScope(), "Inbox").also { it.load() }
        store.updateSettings { it.copy(printer = PrinterSettings(transport = PrinterTransport.TCP, host = "127.0.0.1")) }
        val p = store.addTask(null, "P")!!
        listOf("a", "b").forEach { store.addTask(p.id, it) }
        store.ticketizeColumn(p.id)
        val printer = FakePrinter(failAt = 2).also { it.failAt = 2 }
        val always = object : PrinterClient() {
            var calls = 0
            override suspend fun send(s: PrinterSettings, job: TicketFormatter.PrintJob): PrintResult { calls++; return if (calls == 1) PrintResult.Ok(PrinterTransport.TCP) else PrintResult.Error("PAPER_OUT") }
        }
        val state = AppState(store, CoroutineScope(SupervisorJob() + Dispatchers.Default), EN, always)
        state.printRetryDelaysMs = listOf(10, 10, 10)
        state.print(PrintTarget.Today)
        waitIdle(state)
        assertEquals(1, state.printQueue.size)
        assertEquals(5, always.calls) // 1 ok + 1 fail + 3 retries
        assertEquals("PAPER_OUT", state.printLastError)
        @Suppress("UNUSED_VARIABLE") val unused = printer
    }

    @Test
    fun failureKeepsRemainingTicketsAndResumeFinishesThem() = runBlocking {
        val store = Store(null, TestScope(), "Inbox").also { it.load() }
        store.updateSettings { it.copy(printer = PrinterSettings(transport = PrinterTransport.TCP, host = "127.0.0.1")) }
        val p = store.addTask(null, "P")!!
        listOf("a", "b", "c").forEach { store.addTask(p.id, it) }
        store.ticketizeColumn(p.id)
        val printer = FakePrinter(failAt = 2)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val state = AppState(store, scope, EN, printer)
        state.printRetryDelaysMs = emptyList()

        state.print(PrintTarget.Today)
        waitIdle(state)
        assertEquals(2, state.printQueue.size, "two tickets should remain after the failure")
        assertEquals(1, store.data.value.tickets.count { it.printedAt != null })

        printer.failAt = -1
        state.resumePrintQueue()
        waitIdle(state)
        assertTrue(state.printQueue.isEmpty())
        assertEquals(3, store.data.value.tickets.count { it.printedAt != null })
        assertEquals(4, printer.calls) // 1 ok, 1 fail, 2 ok
    }
}
