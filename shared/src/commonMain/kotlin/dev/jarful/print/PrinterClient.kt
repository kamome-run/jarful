package dev.jarful.print

import dev.jarful.model.PrinterSettings
import dev.jarful.model.PrinterTransport
import dev.jarful.platform.ensureBluetoothPermission
import dev.jarful.platform.sendBle
import dev.jarful.platform.sendBluetooth
import dev.jarful.platform.sendRawTcp
import dev.jarful.platform.sendSerial

sealed class PrintResult {
    data object Ok : PrintResult()
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
                }
                PrinterTransport.BLUETOOTH -> {
                    if (s.bluetoothAddress.isBlank()) return PrintResult.Error("NO_DEVICE")
                    if (!ensureBluetoothPermission()) return PrintResult.Error("PERMISSION_DENIED")
                    sendBluetooth(s.bluetoothAddress, bytes, timeoutMs, chunkSize)
                }
                PrinterTransport.BLUETOOTH_LE -> {
                    if (s.bluetoothAddress.isBlank()) return PrintResult.Error("NO_DEVICE")
                    if (!ensureBluetoothPermission(scan = false)) return PrintResult.Error("PERMISSION_DENIED")
                    sendBle(s.bluetoothAddress, bytes, timeoutMs.coerceAtLeast(15_000), chunkSize)
                }
                PrinterTransport.SERIAL -> {
                    if (s.serialPort.isBlank()) return PrintResult.Error("NO_PORT")
                    sendSerial(s.serialPort, bytes, timeoutMs, chunkSize)
                }
            }
            PrintResult.Ok
        } catch (e: Throwable) {
            PrintResult.Error(e.message ?: e::class.simpleName ?: "ERROR")
        }
    }
}
