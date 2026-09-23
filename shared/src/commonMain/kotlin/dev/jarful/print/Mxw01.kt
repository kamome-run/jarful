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
    private const val CMD_SET_QUALITY = 0xA4
    private const val CMD_PRINT = 0xA9
    private const val CMD_FLUSH = 0xAD
    private const val CMD_SPEED = 0xBD

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

    /** 4-bpp rows with inverted polarity (0xF = white, 0x0 = black). */
    fun rowData4bppInverted(bmp: MonoBitmap, feedRows: Int): ByteArray = rowData4bpp(bmp, feedRows).also { for (i in it.indices) it[i] = (it[i].toInt().inv() and 0xFF).toByte() }

    /**
     * The BLE job for one bitmap: status → intensity → print request (wait reply) → rows → flush (wait done).
     * 1 bpp is the mode known to print on real devices; the 4-bpp mode is kept for the darkness probe.
     */
    fun plan(bmp: MonoBitmap, feedLines: Int, intensity: Int = Density.MXW01_INTENSITY, gray: Boolean = false): List<BleWrite> {
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

    /**
     * Darkness probe (temporary diagnostic): candidate configurations, each returned as its own BLE session.
     * For 1-bpp variants the label is stacked above the sample inside the same print job (one request per
     * session, no back-to-back requests). 4-bpp variants print their label in a separate 1-bpp job first.
     */
    fun variantPlans(sample: MonoBitmap, label: (String) -> MonoBitmap): List<Pair<String, List<BleWrite>>> {
        fun stacked(tag: String): MonoBitmap {
            val l = label(tag); val rows = (l.rows.toList() + sample.rows.toList()).toTypedArray()
            return MonoBitmap(WIDTH, rows.size, rows)
        }
        fun oneBpp(tag: String, intensity: Int, extra: List<BleWrite> = emptyList(), intensityAfterRequest: Boolean = false): List<BleWrite> {
            val bmp = stacked(tag)
            val pre = listOf(BleWrite(CHAR_CONTROL, getStatus(), awaitNotify = CHAR_NOTIFY)) +
                (if (intensityAfterRequest) emptyList() else listOf(BleWrite(CHAR_CONTROL, setIntensity(intensity), delayMs = 50))) + extra
            return pre + listOf(BleWrite(CHAR_CONTROL, printRequest(bmp.height + 24, 0x00), awaitNotify = CHAR_NOTIFY)) +
                (if (intensityAfterRequest) listOf(BleWrite(CHAR_CONTROL, setIntensity(intensity), delayMs = 50)) else emptyList()) +
                listOf(BleWrite(CHAR_DATA, rowData(bmp, 24)), BleWrite(CHAR_CONTROL, flush(), awaitNotify = CHAR_DONE, delayMs = 800))
        }
        fun fourBpp(tag: String, mode: Int, inverted: Boolean): List<BleWrite> =
            plan(label(tag), 1, 0xE0, gray = false) + listOf(
                BleWrite(CHAR_CONTROL, getStatus(), awaitNotify = CHAR_NOTIFY, delayMs = 2000),
                BleWrite(CHAR_CONTROL, setIntensity(0xE0), delayMs = 50),
                BleWrite(CHAR_CONTROL, printRequest(sample.height + 24, mode), awaitNotify = CHAR_NOTIFY),
                BleWrite(CHAR_DATA, if (inverted) rowData4bppInverted(sample, 24) else rowData4bpp(sample, 24)),
                BleWrite(CHAR_CONTROL, flush(), awaitNotify = CHAR_DONE, delayMs = 800),
            )
        return listOf(
            "A 1bpp intensity 0xE0" to oneBpp("A", 0xE0),
            "B 1bpp intensity 0xFF" to oneBpp("B", 0xFF),
            "C 1bpp intensity 0x5D + quality 0x35" to oneBpp("C", 0x5D, listOf(BleWrite(CHAR_CONTROL, packet(CMD_SET_QUALITY, byteArrayOf(0x35)), delayMs = 50))),
            "D 1bpp intensity 0xE0 + speed 0x08" to oneBpp("D", 0xE0, listOf(BleWrite(CHAR_CONTROL, packet(CMD_SPEED, byteArrayOf(0x08)), delayMs = 50))),
            "E 1bpp intensity after request" to oneBpp("E", 0xE0, intensityAfterRequest = true),
            "F 4bpp mode 2 black=F" to fourBpp("F", 0x02, false),
            "G 4bpp mode 2 black=0" to fourBpp("G", 0x02, true),
            "H 4bpp mode 1 black=F" to fourBpp("H", 0x01, false),
        )
    }
}
