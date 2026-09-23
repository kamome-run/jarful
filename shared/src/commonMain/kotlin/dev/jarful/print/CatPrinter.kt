package dev.jarful.print

import dev.jarful.platform.MonoBitmap

/**
 * Protocol of the "cat printer" family (GB01/GB02/GB03/GT01/MX05/MX06/YT01 and clones sold as
 * 57 mm pocket printers with the iPrint / Fun Print apps). Packets are `51 78 <cmd> 00 <len> 00 <data> <crc8> FF`
 * written to BLE characteristic AE01 (service AE30). Bitmap rows are 384 px, LSB-first per byte, 1 = black.
 */
object CatPrinter {
    const val WIDTH = 384
    private const val CMD_GET_DEV_STATE = 0xA3
    private const val CMD_SET_QUALITY = 0xA4
    private const val CMD_LATTICE = 0xA6
    private const val CMD_FEED_PAPER = 0xA1
    private const val CMD_PRINT_ROW = 0xA2
    private const val CMD_SET_ENERGY = 0xAF
    private const val CMD_DRAWING_MODE = 0xBE
    private const val CMD_SPEED = 0xBD

    private val CRC8_TABLE: IntArray = IntArray(256).also { t ->
        for (i in 0 until 256) {
            var c = i
            repeat(8) { c = if (c and 0x80 != 0) ((c shl 1) xor 0x07) and 0xFF else (c shl 1) and 0xFF }
            t[i] = c
        }
    }

    fun crc8(data: ByteArray): Int {
        var crc = 0
        for (b in data) crc = CRC8_TABLE[(crc xor (b.toInt() and 0xFF)) and 0xFF]
        return crc
    }

    fun packet(cmd: Int, data: ByteArray): ByteArray {
        require(data.size < 256) { "cat printer payload too long" }
        val out = ByteArray(8 + data.size)
        out[0] = 0x51; out[1] = 0x78; out[2] = cmd.toByte(); out[3] = 0; out[4] = data.size.toByte(); out[5] = 0
        data.copyInto(out, 6)
        out[6 + data.size] = crc8(data).toByte()
        out[7 + data.size] = 0xFF.toByte()
        return out
    }

    private val LATTICE_START = byteArrayOf(0xAA.toByte(), 0x55, 0x17, 0x38, 0x44, 0x5F, 0x5F, 0x5F, 0x44, 0x38, 0x2C)
    private val LATTICE_END = byteArrayOf(0xAA.toByte(), 0x55, 0x17, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x17)

    /** Converts one MSB-first packed row (as in [MonoBitmap]) to the printer's LSB-first row of exactly 48 bytes. */
    fun rowBytes(bmp: MonoBitmap, y: Int): ByteArray {
        val out = ByteArray(WIDTH / 8)
        for (x in 0 until WIDTH) {
            if (x < bmp.width && bmp.isBlack(x, y)) out[x / 8] = (out[x / 8].toInt() or (1 shl (x % 8))).toByte()
        }
        return out
    }

    /** Full print job: header, one packet per row, paper feed, footer (FR-9.2). [speed] (0xBD) is sent only when given. */
    fun encode(bmp: MonoBitmap, feedLines: Int, energy: Int = Density.CAT_ENERGY, quality: Int = 0x33, speed: Int? = null, drawingMode: Int = 0x00): ByteArray {
        val parts = ArrayList<ByteArray>()
        parts += packet(CMD_GET_DEV_STATE, byteArrayOf(0x00))
        parts += packet(CMD_SET_QUALITY, byteArrayOf(quality.toByte()))
        parts += packet(CMD_LATTICE, LATTICE_START)
        parts += packet(CMD_DRAWING_MODE, byteArrayOf(drawingMode.toByte()))
        if (speed != null) parts += packet(CMD_SPEED, byteArrayOf(speed.toByte()))
        parts += packet(CMD_SET_ENERGY, byteArrayOf((energy and 0xFF).toByte(), ((energy shr 8) and 0xFF).toByte()))
        for (y in 0 until bmp.height) parts += packet(CMD_PRINT_ROW, rowBytes(bmp, y))
        val feed = (feedLines * 24).coerceIn(0, 65535)
        parts += packet(CMD_FEED_PAPER, byteArrayOf((feed and 0xFF).toByte(), ((feed shr 8) and 0xFF).toByte()))
        parts += packet(CMD_LATTICE, LATTICE_END)
        parts += packet(CMD_GET_DEV_STATE, byteArrayOf(0x00))
        val total = parts.sumOf { it.size }
        val out = ByteArray(total); var off = 0
        for (p in parts) { p.copyInto(out, off); off += p.size }
        return out
    }

    /**
     * Darkness probe (temporary diagnostic) for GB01-family printers: each variant is one complete job with
     * its label stacked above the sample, so the user can report which one prints darkest.
     */
    fun variantJobs(sample: MonoBitmap, label: (String) -> MonoBitmap): List<Pair<String, ByteArray>> {
        fun stacked(tag: String): MonoBitmap {
            val l = label(tag); val rows = (l.rows.toList() + sample.rows.toList()).toTypedArray()
            return MonoBitmap(WIDTH, rows.size, rows)
        }
        return listOf(
            "A energy 0x2EE0 (baseline)" to encode(stacked("A"), 1, 0x2EE0),
            "B energy 0x4E20" to encode(stacked("B"), 1, 0x4E20),
            "C energy 0x7530" to encode(stacked("C"), 1, 0x7530),
            "D energy 0xA028" to encode(stacked("D"), 1, 0xA028),
            "E energy 0xFFFF" to encode(stacked("E"), 1, 0xFFFF),
            "F energy 0x2EE0 + quality 0x35" to encode(stacked("F"), 1, 0x2EE0, quality = 0x35),
            "G energy 0x2EE0 + speed 0x0A" to encode(stacked("G"), 1, 0x2EE0, speed = 0x0A),
            "H energy 0x4E20 + quality 0x35 + speed 0x0A" to encode(stacked("H"), 1, 0x4E20, quality = 0x35, speed = 0x0A),
        )
    }
}
