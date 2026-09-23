package dev.jarful.print

import dev.jarful.model.PaperWidth
import dev.jarful.model.PrintProtocol
import dev.jarful.model.PrinterCharset
import dev.jarful.model.PrinterSettings
import dev.jarful.model.Ticket
import dev.jarful.platform.MonoBitmap
import dev.jarful.platform.TextLine
import dev.jarful.platform.isRtlText
import dev.jarful.platform.renderTextBitmap

/** Turns tickets into printer bytes for every supported protocol (FR-9.2, §10). */
object TicketFormatter {

    /** Approximate display width: CJK/full-width chars count as 2 columns. */
    fun displayWidth(ch: Char): Int {
        val c = ch.code
        return if (c in 0x1100..0x115F || c in 0x2E80..0xA4CF || c in 0xAC00..0xD7A3 ||
            c in 0xF900..0xFAFF || c in 0xFE30..0xFE4F || c in 0xFF00..0xFF60 || c in 0xFFE0..0xFFE6) 2 else 1
    }

    /** Greedy wrap by display columns. */
    fun wrap(text: String, columns: Int): List<String> {
        if (text.isEmpty()) return listOf("")
        val lines = ArrayList<String>()
        val sb = StringBuilder(); var w = 0
        for (ch in text) {
            if (ch == '\n') { lines.add(sb.toString()); sb.clear(); w = 0; continue }
            val cw = displayWidth(ch)
            if (w + cw > columns) { lines.add(sb.toString()); sb.clear(); w = 0 }
            sb.append(ch); w += cw
        }
        lines.add(sb.toString())
        return lines
    }

    fun metaLine(t: Ticket): String {
        val parts = ArrayList<String>()
        t.estimateMin?.let { parts.add("[~${it}min]") }
        t.timeboxMin?.let { parts.add("[BOX ${it}min]") }
        t.quotaTarget?.let { parts.add("[x${it}]") }
        return parts.joinToString(" ")
    }

    // ---------- ESC/POS text (§10.1) ----------

    fun encodeTicketText(t: Ticket, paper: PaperWidth, charset: PrinterCharset, dateLabel: String, feedLines: Int, cut: Boolean): ByteArray {
        val e = EscPos(charset).init()
        val cols = paper.columns
        e.alignCenter().doubleSize(true)
        wrap(t.category.ifBlank { "-" }, cols / 2).forEach { e.line(it) }
        e.doubleSize(false).alignLeft().line("-".repeat(cols))
        e.bold(true)
        wrap(t.title, cols).forEach { e.line(it) }
        e.bold(false)
        val meta = metaLine(t)
        if (meta.isNotEmpty()) e.line(meta)
        e.line(dateLabel)
        e.feed(feedLines)
        if (cut) e.partialCut()
        return e.bytes()
    }

    // ---------- Bitmap-based protocols (§10.2–10.5) ----------

    /** Layout used by every raster protocol so the ticket looks the same everywhere. */
    fun ticketLines(t: Ticket, dateLabel: String): List<TextLine> {
        val lines = ArrayList<TextLine>()
        val rtl = isRtlText(t.title) || isRtlText(t.category)
        lines.add(TextLine(t.category.ifBlank { "-" }, sizePx = 40f, bold = true, center = true, rtl = isRtlText(t.category)))
        lines.add(TextLine("", sizePx = 0f)) // rule
        lines.add(TextLine(t.title, sizePx = 30f, bold = true, rtl = isRtlText(t.title)))
        val meta = metaLine(t)
        if (meta.isNotEmpty()) lines.add(TextLine(meta, sizePx = 22f, rtl = rtl))
        lines.add(TextLine(dateLabel, sizePx = 20f, rtl = rtl))
        return lines
    }

    fun renderTicket(t: Ticket, paper: PaperWidth, dateLabel: String): MonoBitmap =
        renderTextBitmap(ticketLines(t, dateLabel), widthPx = paper.dots, paddingPx = 8)

    /** Cat printers are always 384 px wide regardless of the configured paper width. */
    private fun renderFor(t: Ticket, s: PrinterSettings, dateLabel: String): MonoBitmap =
        if (s.protocol == PrintProtocol.CATPRINTER) renderTextBitmap(ticketLines(t, dateLabel), widthPx = CatPrinter.WIDTH, paddingPx = 8)
        else renderTicket(t, s.paperWidth, dateLabel)

    fun encodeBitmap(bmp: MonoBitmap, s: PrinterSettings): ByteArray = when (s.protocol) {
        PrintProtocol.ESCPOS_RASTER -> EscPos(s.charset).init().alignCenter().raster(bmp).feed(s.feedLines).also { if (s.cutEnabled) it.partialCut() }.bytes()
        PrintProtocol.ESCPOS_BITIMAGE -> EscPos(s.charset).init().alignCenter().bitImage(bmp).feed(s.feedLines).also { if (s.cutEnabled) it.partialCut() }.bytes()
        PrintProtocol.TSPL -> Tspl.label(bmp, s.paperWidth.mm, s.labelHeightMm, s.labelGapMm)
        PrintProtocol.CPCL -> Cpcl.label(bmp)
        PrintProtocol.CATPRINTER -> CatPrinter.encode(bmp, s.feedLines)
        PrintProtocol.ESCPOS_TEXT -> error("not a bitmap protocol")
    }

    fun encodeTicket(t: Ticket, s: PrinterSettings, dateLabel: String): ByteArray =
        if (s.protocol == PrintProtocol.ESCPOS_TEXT) encodeTicketText(t, s.paperWidth, s.charset, dateLabel, s.feedLines, s.cutEnabled)
        else encodeBitmap(renderFor(t, s, dateLabel), s)

    fun encodeAll(tickets: List<Ticket>, s: PrinterSettings, dateLabel: (Ticket) -> String): ByteArray {
        var out = ByteArray(0)
        tickets.forEach { t -> out += encodeTicket(t, s, dateLabel(t)) }
        return out
    }

    fun encodeTestPage(s: PrinterSettings): ByteArray {
        if (s.protocol == PrintProtocol.ESCPOS_TEXT) {
            val e = EscPos(s.charset).init()
            e.alignCenter().doubleSize(true).line("Jarful").doubleSize(false)
            e.line("Test print OK / テスト印刷")
            e.alignLeft().line("-".repeat(s.paperWidth.columns))
            e.line("Paper: ${s.paperWidth.label}  Charset: ${s.charset.label}")
            e.feed(s.feedLines)
            if (s.cutEnabled) e.partialCut()
            return e.bytes()
        }
        val width = if (s.protocol == PrintProtocol.CATPRINTER) CatPrinter.WIDTH else s.paperWidth.dots
        val bmp = renderTextBitmap(
            listOf(
                TextLine("Jarful", 40f, bold = true, center = true),
                TextLine("", 0f),
                TextLine("Test print OK / テスト印刷", 26f),
                TextLine("${s.protocol.label} / ${s.paperWidth.label} / ${width}px", 20f),
            ), width, 8,
        )
        return encodeBitmap(bmp, s)
    }
}
