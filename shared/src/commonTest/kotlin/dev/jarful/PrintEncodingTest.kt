package dev.jarful

import dev.jarful.model.PaperWidth
import dev.jarful.model.PrintProtocol
import dev.jarful.model.PrinterCharset
import dev.jarful.model.PrinterSettings
import dev.jarful.model.Ticket
import dev.jarful.platform.MonoBitmap
import dev.jarful.print.Cpcl
import dev.jarful.print.EscPos
import dev.jarful.print.TicketFormatter
import dev.jarful.print.Tspl
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
    fun codePagesSelectEscT() { // FR-9.2 text mode for Cyrillic / Arabic / Latin-2 / Vietnamese
        assertContentEquals(b(0x1B, 0x40, 0x1B, 0x74, 46), EscPos(PrinterCharset.WIN1251).init().bytes())
        assertContentEquals(b(0x1B, 0x40, 0x1B, 0x74, 50), EscPos(PrinterCharset.WIN1256).init().bytes())
        assertContentEquals(b(0x1B, 0x40, 0x1B, 0x74, 45), EscPos(PrinterCharset.WIN1250).init().bytes())
        assertContentEquals(b(0x1B, 0x40, 0x1B, 0x74, 52), EscPos(PrinterCharset.WIN1258).init().bytes())
        assertContentEquals(b(0x1B, 0x40, 0x1C, 0x26), EscPos(PrinterCharset.BIG5).init().bytes())
        assertContentEquals(b(0x1B, 0x40), EscPos(PrinterCharset.UTF8).init().bytes())
        // Cyrillic text really is single-byte in windows-1251
        val ru = EscPos(PrinterCharset.WIN1251).text("Привет").bytes()
        assertEquals(6, ru.size)
        assertContentEquals(b(0xCF, 0xF0, 0xE8, 0xE2, 0xE5, 0xF2), ru)
        // Polish in windows-1250: ł = 0xB3
        assertContentEquals(b(0xB3), EscPos(PrinterCharset.WIN1250).text("ł").bytes())
    }

    @Test
    fun rtlDetectionAndAlignment() { // FR-9.4
        assertTrue(dev.jarful.platform.isRtlText("غسل الأطباق"))
        assertFalse(dev.jarful.platform.isRtlText("Wash the dishes"))
        assertFalse(dev.jarful.platform.isRtlText("皿を洗う"))
        val lines = TicketFormatter.ticketLines(Ticket(id = "a", date = "2026-09-23", category = "المطبخ", title = "غسل الأطباق"), "2026-09-23")
        assertTrue(lines.first { it.sizePx == 30f }.rtl)
        assertTrue(lines.last().rtl) // date line follows the ticket's direction
        val ltr = TicketFormatter.ticketLines(Ticket(id = "b", date = "2026-09-23", category = "Küche", title = "Geschirr spülen"), "2026-09-23")
        assertFalse(ltr.any { it.rtl })
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
    fun catPrinterPacketsAndCrc() { // FR-9.2 cat printer
        val p = dev.jarful.print.CatPrinter.packet(0xA1, b(0x19, 0x00))
        assertContentEquals(b(0x51, 0x78, 0xA1, 0x00, 0x02, 0x00, 0x19, 0x00), p.copyOfRange(0, 8))
        assertEquals(0xFF, p.last().toInt() and 0xFF)
        assertEquals(dev.jarful.print.CatPrinter.crc8(b(0x19, 0x00)), p[8].toInt() and 0xFF)
        assertEquals(0xF4, dev.jarful.print.CatPrinter.crc8("123456789".encodeToByteArray())) // CRC-8 poly 0x07 check value
        val rows = arrayOf(ByteArray(48).also { it[0] = 0x80.toByte() })
        val row = dev.jarful.print.CatPrinter.rowBytes(MonoBitmap(384, 1, rows), 0)
        assertEquals(0x01, row[0].toInt() and 0xFF) // leftmost pixel -> bit 0 (LSB first)
        val job = dev.jarful.print.CatPrinter.encode(MonoBitmap(384, 2, arrayOf(ByteArray(48), ByteArray(48))), feedLines = 1)
        assertContentEquals(b(0x51, 0x78, 0xA3), job.copyOfRange(0, 3))
        assertEquals(1, countSeq(job, b(0x51, 0x78, 0xBD, 0x00, 0x01, 0x00, 0x0A))) // speed command precedes the energy packet
        assertEquals(2, countSeq(job, b(0x51, 0x78, 0xA2, 0x00, 48, 0x00)))
    }

    @Test
    fun mxw01PacketsAndPlan() { // FR-9.2 MXW01
        val req = dev.jarful.print.Mxw01.printRequest(300)
        assertContentEquals(b(0x22, 0x21, 0xA9, 0x00, 0x04, 0x00, 0x2C, 0x01, 0x30, 0x00), req.copyOfRange(0, 10))
        assertEquals(dev.jarful.print.CatPrinter.crc8(b(0x2C, 0x01, 0x30, 0x00)), req[10].toInt() and 0xFF)
        assertEquals(0xFF, req.last().toInt() and 0xFF)
        val bmp = MonoBitmap(384, 3, arrayOf(ByteArray(48), ByteArray(48).also { it[0] = 0x80.toByte() }, ByteArray(48)))
        val plan = dev.jarful.print.Mxw01.plan(bmp, feedLines = 1, gray = false)
        assertEquals(5, plan.size)
        assertEquals(dev.jarful.print.Mxw01.CHAR_CONTROL, plan[0].characteristic)
        assertEquals(dev.jarful.print.Mxw01.CHAR_NOTIFY, plan[0].awaitNotify)
        assertEquals(dev.jarful.print.Mxw01.CHAR_DATA, plan[3].characteristic)
        assertEquals((3 + 24) * 48, plan[3].bytes.size) // rows + feed rows, 48 bytes each
        assertEquals(0x01, plan[3].bytes[48].toInt() and 0xFF) // second row, leftmost pixel -> bit 0
        val lines = plan[2].bytes[6].toInt() and 0xFF or ((plan[2].bytes[7].toInt() and 0xFF) shl 8)
        assertEquals(27, lines)
        assertEquals(dev.jarful.print.Mxw01.CHAR_DONE, plan[4].awaitNotify)
    }

    @Test
    fun alwaysPrintsAtMaximumDarkness() { // FR-9.10
        val esc = EscPos().init().maxDensity().bytes()
        assertContentEquals(b(0x1D, 0x28, 0x4B, 0x02, 0x00, 0x31, 0x86), esc.copyOfRange(2, 9))
        assertContentEquals(b(0x1B, 0x37, 0x07, 0xB0, 0x02), esc.copyOfRange(9, 14))
        val rows = arrayOf(ByteArray(48).also { it[0] = 0xC0.toByte() }) // pixels 0 and 1 black
        val plan = dev.jarful.print.Mxw01.plan(MonoBitmap(384, 1, rows), 0)
        assertEquals(0x5D, plan[1].bytes[6].toInt() and 0xFF) // vendor-default intensity (only value confirmed to print)
        assertEquals(0x00, plan[2].bytes[9].toInt() and 0xFF) // 1-bpp mode (the one known to print)
        assertEquals(48, plan[3].bytes.size)
        val gray = dev.jarful.print.Mxw01.plan(MonoBitmap(384, 1, rows), 0, gray = true)
        assertEquals(192, gray[3].bytes.size); assertEquals(0xFF, gray[3].bytes[0].toInt() and 0xFF)

        // GB01: the darkest configuration confirmed on a real device — energy 0x4E20, quality 0x35, speed 0x0A
        val cat = dev.jarful.print.CatPrinter.encode(MonoBitmap(384, 1, arrayOf(ByteArray(48))), 0)
        assertEquals(1, countSeq(cat, b(0x51, 0x78, 0xAF, 0x00, 0x02, 0x00, 0x20, 0x4E)))
        assertEquals(1, countSeq(cat, b(0x51, 0x78, 0xBD, 0x00, 0x01, 0x00, 0x0A)))
        assertEquals(1, countSeq(cat, b(0x51, 0x78, 0xA4, 0x00, 0x01, 0x00, 0x35)))
    }

    @Test
    fun catPrinterSendsTicketsAsSeparateJobs() {
        val s = PrinterSettings(protocol = PrintProtocol.CATPRINTER)
        val two = TicketFormatter.encodeJob(listOf(ticket, ticket.copy(id = "k2", title = "Second")), s) { "d" }
        assertTrue(two is TicketFormatter.PrintJob.Batches && two.batches.size == 2)
        val one = TicketFormatter.encodeJob(listOf(ticket), s) { "d" }
        assertTrue(one is TicketFormatter.PrintJob.Bytes)
    }

    private fun countSeq(hay: ByteArray, needle: ByteArray): Int { var n = 0; var i = 0; while (true) { val j = hay.copyOfRange(i, hay.size).indexOf(needle); if (j < 0) return n; n++; i += j + needle.size } }

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
