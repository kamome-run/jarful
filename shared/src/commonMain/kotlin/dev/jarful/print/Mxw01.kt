package dev.jarful.print

import dev.jarful.platform.BleWrite
import dev.jarful.platform.MonoBitmap

/**
 * "MXW01" generation of cat printers (also X5h and similar): control packets `22 21 <cmd> 00 <len> 00 <data> <crc8> FF`
 * on characteristic AE01, replies on AE02, raw bitmap rows (48 bytes, LSB-first, 1 = black) streamed to AE03,
 * print-complete notification on AE04. BLE only.
 */
object Mxw01 {
    const val WIDTH = 384
    const val CHAR_CONTROL = "0000ae01-0000-1000-8000-00805f9b34fb"
    const val CHAR_NOTIFY = "0000ae02-0000-1000-8000-00805f9b34fb"
    const val CHAR_DATA = "0000ae03-0000-1000-8000-00805f9b34fb"
    const val CHAR_DONE = "0000ae04-0000-1000-8000-00805f9b34fb"

    private const val CMD_GET_STATUS = 0xA1
    private const val CMD_SET_INTENSITY = 0xA2
    private const val CMD_PRINT = 0xA9
    private const val CMD_FLUSH = 0xAD

    fun packet(cmd: Int, data: ByteArray): ByteArray {
        require(data.size < 256) { "mxw01 payload too long" }
        val out = ByteArray(8 + data.size)
        out[0] = 0x22; out[1] = 0x21; out[2] = cmd.toByte(); out[3] = 0; out[4] = data.size.toByte(); out[5] = 0
        data.copyInto(out, 6)
        out[6 + data.size] = CatPrinter.crc8(data).toByte()
        out[7 + data.size] = 0xFF.toByte()
        return out
    }

    fun getStatus(): ByteArray = packet(CMD_GET_STATUS, byteArrayOf(0x00))
    fun setIntensity(v: Int = 0x5D): ByteArray = packet(CMD_SET_INTENSITY, byteArrayOf(v.toByte()))
    /** Print request: line count (LE), 0x30, mode 0x00 = 1 bpp, 0x02 = 4 bpp grayscale. */
    fun printRequest(lines: Int, mode: Int = 0x00): ByteArray = packet(CMD_PRINT, byteArrayOf((lines and 0xFF).toByte(), ((lines shr 8) and 0xFF).toByte(), 0x30, mode.toByte()))
    fun flush(): ByteArray = packet(CMD_FLUSH, byteArrayOf(0x00))

    /** Bitmap rows (LSB-first like GB01) followed by blank rows used as paper feed. */
    fun rowData(bmp: MonoBitmap, feedRows: Int): ByteArray {
        val rowBytes = WIDTH / 8
        val total = bmp.height + feedRows
        val out = ByteArray(total * rowBytes)
        for (y in 0 until bmp.height) CatPrinter.rowBytes(bmp, y).copyInto(out, y * rowBytes)
        return out
    }

    /** 4-bpp rows: two pixels per byte (even pixel in the low nibble), 0x0 = white, 0xF = black; blank rows as feed. */
    fun rowData4bpp(bmp: MonoBitmap, feedRows: Int): ByteArray {
        val rowBytes = WIDTH / 2
        val out = ByteArray((bmp.height + feedRows) * rowBytes)
        for (y in 0 until bmp.height) for (x in 0 until WIDTH) {
            if (x < bmp.width && bmp.isBlack(x, y)) {
                val i = y * rowBytes + x / 2
                out[i] = (out[i].toInt() or (if (x % 2 == 0) 0x0F else 0xF0)).toByte()
            }
        }
        return out
    }

    /**
     * The BLE job for one bitmap: status → intensity → print request (wait reply) → rows → flush (wait done).
     * Always uses the darkest configuration (FR-9.10): maximum intensity and the 4-bpp mode with full-black pixels.
     */
    fun plan(bmp: MonoBitmap, feedLines: Int, intensity: Int = Density.MXW01_INTENSITY, gray: Boolean = true): List<BleWrite> {
        val feedRows = (feedLines * 24).coerceIn(0, 400)
        val lines = bmp.height + feedRows
        return listOf(
            BleWrite(CHAR_CONTROL, getStatus(), awaitNotify = CHAR_NOTIFY),
            BleWrite(CHAR_CONTROL, setIntensity(intensity), delayMs = 50),
            BleWrite(CHAR_CONTROL, printRequest(lines, if (gray) Density.MXW01_MODE_GRAY else 0x00), awaitNotify = CHAR_NOTIFY),
            BleWrite(CHAR_DATA, if (gray) rowData4bpp(bmp, feedRows) else rowData(bmp, feedRows)),
            BleWrite(CHAR_CONTROL, flush(), awaitNotify = CHAR_DONE, delayMs = 300),
        )
    }
}
