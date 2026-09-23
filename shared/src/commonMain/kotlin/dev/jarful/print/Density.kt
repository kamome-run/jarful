package dev.jarful.print

/**
 * Fixed "as dark as possible" settings per protocol (FR-9.10). There is deliberately no user-facing
 * adjustment: tickets must be readable on a whiteboard from a distance.
 */
object Density {
    /** MXW01 `A2` intensity byte. 0x5D is the vendor default and the only value confirmed to print; higher values made the printer drop the job. */
    const val MXW01_INTENSITY = 0x5D
    /** MXW01 print mode 0x02 = 4 bpp grayscale. Rejected by some firmware (nothing prints); 1 bpp is the default. */
    const val MXW01_MODE_GRAY = 0x02
    /**
     * GB01-family darkest configuration confirmed on a real device (probe variant H): energy 0x4E20,
     * quality 0x35 and the speed command 0xBD = 0x0A, which is what actually makes the output dark.
     * (0x7530 energy made the same device drop the job; the vendor default is 0x2EE0.)
     */
    const val CAT_ENERGY = 0x4E20
    const val CAT_QUALITY = 0x35
    const val CAT_SPEED = 0x0A
    /** ESC/POS `GS ( K` fn=49 density byte: 0x80 = 100 %, 0x86 = 130 %. */
    const val ESCPOS_GS_K = 0x86
    /** `ESC 7 n1 n2 n3` heating time byte for pocket printers (0x50 common default). */
    const val ESCPOS_HEAT_TIME = 0xB0
    /** `DC2 # n`: density 31/31 with a short break time. */
    const val ESCPOS_DC2 = 0x3F
}
