package dev.jarful.print

/** Maps the 1–5 density setting onto each protocol's own scale (FR-9.10). */
object Density {
    fun clamp(d: Int) = d.coerceIn(1, 5)

    /** MXW01 `A2` intensity byte. 0x5D is the vendor default; higher = darker. */
    fun mxw01Intensity(d: Int): Int = intArrayOf(0x3A, 0x5D, 0x80, 0xB0, 0xE0)[clamp(d) - 1]

    /** GB01-family `AF` energy (16-bit). 0x2EE0 is the vendor default. */
    fun catEnergy(d: Int): Int = intArrayOf(0x1F40, 0x2EE0, 0x4E20, 0x7530, 0xA028)[clamp(d) - 1]

    /** ESC/POS `GS ( K` fn=49 density byte: 0x80 = 100 %, ±0x03 per step (70 %–130 %). */
    fun escposGsK(d: Int): Int = 0x80 + (clamp(d) - 3) * 3

    /** `ESC 7 n1 n2 n3` heating time (n2) used by many 58 mm pocket printers; 0x50 is a common default. */
    fun escposHeatTime(d: Int): Int = intArrayOf(0x30, 0x50, 0x80, 0xB0, 0xF0)[clamp(d) - 1]

    /** `DC2 # n` print density (low 5 bits) with a short break time (high 3 bits). */
    fun escposDc2(d: Int): Int = intArrayOf(0x08, 0x0F, 0x14, 0x1A, 0x1F)[clamp(d) - 1] or 0x20
}
