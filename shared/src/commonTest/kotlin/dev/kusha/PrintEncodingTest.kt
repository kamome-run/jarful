package dev.kusha

import dev.kusha.model.PaperWidth
import dev.kusha.model.PrintProtocol
import dev.kusha.model.PrinterCharset
import dev.kusha.model.PrinterSettings
import dev.kusha.model.Ticket
import dev.kusha.platform.MonoBitmap
import dev.kusha.print.Cpcl
import dev.kusha.print.EscPos
import dev.kusha.print.TicketFormatter
import dev.kusha.print.Tspl
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrintEncodingTest {
    private val ticket = Ticket(id = "k1", date = "2026-09-22", category = "KITCHEN", title = "Wash the dishes", estimateMin = 5)

    private fun ByteArray.indexOf(seq: ByteArray): Int {
        outer@ for (i in 0..(size - seq.size)) { for (j in seq.indices) if (this[i + j] != seq[j]) continue@outer; return i }
        return -1
    }
    private fun b(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    @Test
    fun escposTextOrderAndCut() { // AC-7
        val bytes = TicketFormatter.encodeTicketText(ticket, PaperWidth.MM58, PrinterCharset.UTF8, "2026-09-22 (Tue)", feedLines = 3, cut = true)
        val init = bytes.indexOf(b(0x1B, 0x40)); val center = bytes.indexOf(b(0x1B, 0x61, 1)); val dbl = bytes.indexOf(b(0x1D, 0x21, 0x11))
        val cat = bytes.indexOf("KITCHEN".encodeToByteArray()); val left = bytes.indexOf(b(0x1B, 0x61, 0)); val title = bytes.indexOf("Wash the dishes".encodeToByteArray())
        val cut = bytes.indexOf(b(0x1D, 0x56, 66, 0))
        assertTrue(init == 0 && init < center && center < dbl && dbl < cat && cat < left && left < title && title < cut)
        assertEquals(bytes.size - 4, cut) // cut is the last command
        val noCut = TicketFormatter.encodeTicketText(ticket, PaperWidth.MM58, PrinterCharset.UTF8, "d", 3, cut = false)
        assertEquals(-1, noCut.indexOf(b(0x1D, 0x56, 66, 0)))
    }

    @Test
    fun shiftJisEnablesKanjiMode() {
        val bytes = EscPos(PrinterCharset.SHIFT_JIS).init().bytes()
        assertContentEquals(b(0x1B, 0x40, 0x1C, 0x26), bytes)
    }

    @Test
    fun wrapCountsFullWidthAsTwo() {
        assertEquals(listOf("日本語日", "本語"), TicketFormatter.wrap("日本語日本語", 8))
        assertEquals(listOf("abcdefgh", "ij"), TicketFormatter.wrap("abcdefghij", 8))
    }

    private fun sampleBitmap(): MonoBitmap {
        // 16 x 3: row0 all black, row1 alternating, row2 white
        val rows = arrayOf(b(0xFF, 0xFF), b(0xAA, 0xAA), b(0x00, 0x00))
        return MonoBitmap(16, 3, rows)
    }

    @Test
    fun rasterHeader() { // §10.2
        val bmp = sampleBitmap()
        val bytes = EscPos().raster(bmp).bytes()
        assertContentEquals(b(0x1D, 0x76, 0x30, 0x00, 2, 0, 3, 0), bytes.copyOfRange(0, 8))
        assertContentEquals(bmp.packed(), bytes.copyOfRange(8, 8 + 6))
        assertEquals(8 + 6, bytes.size)
    }

    @Test
    fun rasterSplitsIntoBands() {
        val rows = Array(600) { b(0xFF) }
        val bytes = EscPos().raster(MonoBitmap(8, 600, rows), bandRows = 256).bytes()
        // 256 + 256 + 88 rows => 3 headers of 8 bytes + 600 data bytes
        assertEquals(3 * 8 + 600, bytes.size)
        assertContentEquals(b(0x1D, 0x76, 0x30, 0x00, 1, 0, 0, 1), bytes.copyOfRange(0, 8)) // 256 = 0x0100
    }

    @Test
    fun bitImageColumnMajor() { // §10.3
        val bmp = sampleBitmap()
        val bytes = EscPos().bitImage(bmp).bytes()
        // ESC 3 24, ESC * 33 nL nH, then 16 columns x 3 bytes, LF, ESC 2
        assertContentEquals(b(0x1B, 0x33, 24, 0x1B, 0x2A, 33, 16, 0), bytes.copyOfRange(0, 8))
        val col0 = bytes.copyOfRange(8, 11)
        // column 0: row0 black (bit7), row1 black (bit6), row2 white => 0b11000000
        assertContentEquals(b(0xC0, 0x00, 0x00), col0)
        val col1 = bytes.copyOfRange(11, 14)
        assertContentEquals(b(0x80, 0x00, 0x00), col1) // row1 alternating: x=1 white
        assertEquals(8 + 16 * 3 + 1 + 2, bytes.size)
    }

    @Test
    fun tsplInvertsBitsAndHasHeader() { // §10.4
        val bytes = Tspl.label(sampleBitmap(), 58, 40, 2)
        val text = bytes.decodeToString()
        assertTrue(text.startsWith("SIZE 58 mm,40 mm\r\nGAP 2 mm,0 mm\r\nCLS\r\nBITMAP 0,0,2,3,0,"))
        val head = "SIZE 58 mm,40 mm\r\nGAP 2 mm,0 mm\r\nCLS\r\nBITMAP 0,0,2,3,0,".length
        assertContentEquals(b(0x00, 0x00, 0x55, 0x55, 0xFF, 0xFF), bytes.copyOfRange(head, head + 6))
        assertTrue(text.endsWith("\r\nPRINT 1,1\r\n"))
    }

    @Test
    fun cpclHexData() { // §10.5
        val text = Cpcl.label(sampleBitmap()).decodeToString()
        assertEquals("! 0 200 200 3 1\r\nEG 2 3 0 0 FFFFAAAA0000\r\nFORM\r\nPRINT\r\n", text)
    }

    @Test
    fun encodeAllUsesProtocol() {
        val s = PrinterSettings(protocol = PrintProtocol.ESCPOS_TEXT, cutEnabled = true)
        val two = TicketFormatter.encodeAll(listOf(ticket, ticket.copy(id = "k2")), s) { "d" }
        var count = 0; var i = 0
        while (true) { val j = two.copyOfRange(i, two.size).indexOf(b(0x1D, 0x56, 66, 0)); if (j < 0) break; count++; i += j + 4 }
        assertEquals(2, count)
        assertFalse(PrintProtocol.ESCPOS_TEXT.raster)
    }
}
