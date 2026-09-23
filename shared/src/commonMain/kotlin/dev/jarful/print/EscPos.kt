package dev.jarful.print

import dev.jarful.model.PrinterCharset
import dev.jarful.platform.MonoBitmap
import dev.jarful.platform.encodeText

/** Minimal ESC/POS byte builder (§10.1–10.3). */
class EscPos(private val charset: PrinterCharset = PrinterCharset.UTF8) {
    private val out = ArrayList<Byte>()

    fun raw(vararg b: Int): EscPos = apply { b.forEach { out.add(it.toByte()) } }
    fun raw(bytes: ByteArray): EscPos = apply { bytes.forEach { out.add(it) } }

    fun init(): EscPos = apply {
        raw(0x1B, 0x40) // ESC @
        if (charset.codePage >= 0) raw(0x1B, 0x74, charset.codePage) // ESC t n: select code page
        if (charset.kanji) raw(0x1C, 0x26) // FS &: 2-byte (kanji/hanzi) mode
    }
    fun alignCenter(): EscPos = apply { raw(0x1B, 0x61, 1) }
    fun alignLeft(): EscPos = apply { raw(0x1B, 0x61, 0) }
    fun doubleSize(on: Boolean): EscPos = apply { raw(0x1D, 0x21, if (on) 0x11 else 0x00) }
    fun bold(on: Boolean): EscPos = apply { raw(0x1B, 0x45, if (on) 1 else 0) }
    fun text(s: String): EscPos = apply { raw(encodeText(s, charset.javaName)) }
    fun line(s: String = ""): EscPos = apply { text(s); raw(0x0A) }
    fun feed(n: Int): EscPos = apply { repeat(n.coerceAtLeast(0)) { raw(0x0A) } }
    fun partialCut(): EscPos = apply { raw(0x1D, 0x56, 66, 0) } // GS V 66 0

    /** GS v 0: raster bit image, split into bands of at most [bandRows] rows (§10.2). */
    fun raster(bmp: MonoBitmap, bandRows: Int = 256): EscPos = apply {
        val wb = bmp.widthBytes
        var y = 0
        while (y < bmp.height) {
            val h = minOf(bandRows, bmp.height - y)
            raw(0x1D, 0x76, 0x30, 0x00, wb and 0xFF, (wb shr 8) and 0xFF, h and 0xFF, (h shr 8) and 0xFF)
            for (r in y until y + h) raw(bmp.rows[r])
            y += h
        }
    }

    /** ESC * 33: 24-dot double-density bit image, column-major, 3 bytes per column (§10.3). */
    fun bitImage(bmp: MonoBitmap): EscPos = apply {
        raw(0x1B, 0x33, 24) // line spacing 24 dots
        val w = bmp.width
        var y = 0
        while (y < bmp.height) {
            raw(0x1B, 0x2A, 33, w and 0xFF, (w shr 8) and 0xFF)
            for (x in 0 until w) {
                for (b in 0 until 3) {
                    var v = 0
                    for (bit in 0 until 8) {
                        val row = y + b * 8 + bit
                        if (row < bmp.height && bmp.isBlack(x, row)) v = v or (0x80 shr bit)
                    }
                    raw(v)
                }
            }
            raw(0x0A)
            y += 24
        }
        raw(0x1B, 0x32) // default line spacing
    }

    fun bytes(): ByteArray = out.toByteArray()
}

fun MonoBitmap.isBlack(x: Int, y: Int): Boolean {
    if (x < 0 || y < 0 || x >= width || y >= height) return false
    val b = rows[y][x / 8].toInt()
    return (b and (0x80 shr (x % 8))) != 0
}

/** TSPL label encoder (§10.4). Note TSPL BITMAP uses 0 = black, so bits are inverted. */
object Tspl {
    fun label(bmp: MonoBitmap, widthMm: Int, heightMm: Int, gapMm: Int): ByteArray {
        val sb = StringBuilder()
        sb.append("SIZE $widthMm mm,$heightMm mm\r\n")
        sb.append("GAP $gapMm mm,0 mm\r\n")
        sb.append("CLS\r\n")
        sb.append("BITMAP 0,0,${bmp.widthBytes},${bmp.height},0,")
        val head = encodeText(sb.toString(), "UTF-8")
        val data = bmp.packed().map { (it.toInt().inv() and 0xFF).toByte() }.toByteArray()
        val tail = encodeText("\r\nPRINT 1,1\r\n", "UTF-8")
        return head + data + tail
    }
}

/** CPCL label encoder (§10.5). EG uses hex ASCII, 1 = black. */
object Cpcl {
    private const val HEX = "0123456789ABCDEF"
    fun label(bmp: MonoBitmap): ByteArray {
        val sb = StringBuilder()
        sb.append("! 0 200 200 ${bmp.height} 1\r\n")
        sb.append("EG ${bmp.widthBytes} ${bmp.height} 0 0 ")
        for (b in bmp.packed()) { val v = b.toInt() and 0xFF; sb.append(HEX[v shr 4]); sb.append(HEX[v and 0xF]) }
        sb.append("\r\nFORM\r\nPRINT\r\n")
        return encodeText(sb.toString(), "UTF-8")
    }
}
