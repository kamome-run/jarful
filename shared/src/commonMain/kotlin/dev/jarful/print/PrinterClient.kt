package dev.jarful.print

import dev.jarful.model.PrinterSettings
import dev.jarful.model.PrinterTransport
import dev.jarful.platform.bleSupported
import dev.jarful.platform.bluetoothSupported
import dev.jarful.platform.ensureBluetoothPermission
import dev.jarful.platform.sendBle
import dev.jarful.platform.sendBluetooth
import dev.jarful.platform.sendRawTcp
import dev.jarful.platform.sendSerial

sealed class PrintResult {
    /** [usedTransport] differs from the configured one when the automatic Classic ⇄ LE fallback kicked in. */
    data class Ok(val usedTransport: PrinterTransport) : PrintResult()
    data class Error(val message: String) : PrintResult()
}

/** Sends encoded bytes through the configured transport (FR-9.1, FR-9.7, FR-9.8). */
class PrinterClient(private val timeoutMs: Int = 5_000, private val chunkSize: Int = 512) {
    suspend fun send(s: PrinterSettings, bytes: ByteArray): PrintResult {
        return try {
            when (s.transport) {
                PrinterTransport.TCP -> {
                    if (s.host.isBlank()) return PrintResult.Error("NO_HOST")
                    sendRawTcp(s.host.trim(), s.port, bytes, timeoutMs)
                    PrintResult.Ok(PrinterTransport.TCP)
                }
                PrinterTransport.SERIAL -> {
                    if (s.serialPort.isBlank()) return PrintResult.Error("NO_PORT")
                    sendSerial(s.serialPort, bytes, timeoutMs, chunkSize)
                    PrintResult.Ok(PrinterTransport.SERIAL)
                }
                PrinterTransport.BLUETOOTH, PrinterTransport.BLUETOOTH_LE -> sendBluetoothWithFallback(s, bytes)
            }
        } catch (e: Throwable) {
            PrintResult.Error(e.message ?: e::class.simpleName ?: "ERROR")
        }
    }

    /**
     * Cheap pocket printers are sometimes LE-only although they pair like a Classic device, and vice
     * versa. Try the configured flavour first, then the other one, and report which one worked.
     */
    private suspend fun sendBluetoothWithFallback(s: PrinterSettings, bytes: ByteArray): PrintResult {
        if (s.bluetoothAddress.isBlank()) return PrintResult.Error("NO_DEVICE")
        if (!ensureBluetoothPermission(scan = false)) return PrintResult.Error("PERMISSION_DENIED")
        val order = if (s.transport == PrinterTransport.BLUETOOTH) listOf(PrinterTransport.BLUETOOTH, PrinterTransport.BLUETOOTH_LE)
                    else listOf(PrinterTransport.BLUETOOTH_LE, PrinterTransport.BLUETOOTH)
        val errors = ArrayList<String>()
        for (t in order) {
            val supported = if (t == PrinterTransport.BLUETOOTH) bluetoothSupported() else bleSupported()
            if (!supported) continue
            try {
                if (t == PrinterTransport.BLUETOOTH) sendBluetooth(s.bluetoothAddress, bytes, timeoutMs.coerceAtLeast(12_000), chunkSize)
                else sendBle(s.bluetoothAddress, bytes, timeoutMs.coerceAtLeast(15_000), chunkSize)
                return PrintResult.Ok(t)
            } catch (e: Throwable) {
                errors += "${t.name}: ${e.message ?: e::class.simpleName}"
            }
        }
        return PrintResult.Error(errors.joinToString(" | ").ifBlank { "BLUETOOTH_UNAVAILABLE" })
    }
}
