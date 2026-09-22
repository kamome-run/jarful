package dev.kusha.model

import kotlinx.datetime.DayOfWeek
import kotlinx.serialization.Serializable

/** Schema version of the persisted JSON file (FR-12.3). */
const val SCHEMA_VERSION = 1

/** Fixed id of the root-level "Inbox" project that receives refocus tasks (§9). */
const val INBOX_ID = "inbox"

@Serializable
data class Task(
    val id: String,
    val parentId: String? = null,
    val title: String,
    val order: Int = 0,
    val estimateMin: Int? = null,
    val timeboxMin: Int? = null,
    val done: Boolean = false,
    val doneAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
enum class TicketState { PENDING, RUNNING, DONE }

@Serializable
data class Ticket(
    val id: String,
    /** ISO date `yyyy-MM-dd` (local). */
    val date: String,
    val taskId: String? = null,
    val routineId: String? = null,
    val category: String,
    val title: String,
    val estimateMin: Int? = null,
    val timeboxMin: Int? = null,
    val quotaTarget: Int? = null,
    val quotaCount: Int = 0,
    val order: Int = 0,
    val state: TicketState = TicketState.PENDING,
    val startedAt: Long? = null,
    val doneAt: Long? = null,
    val printedAt: Long? = null,
) {
    val isDone: Boolean get() = state == TicketState.DONE
    val isQuota: Boolean get() = quotaTarget != null
}

@Serializable
data class Routine(
    val id: String,
    val title: String,
    val category: String = "",
    val weekdays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val order: Int = 0,
    val estimateMin: Int? = null,
    val timeboxMin: Int? = null,
    val quotaTarget: Int? = null,
    val enabled: Boolean = true,
)

@Serializable
enum class PaperWidth(val columns: Int, val dots: Int, val mm: Int, val label: String) {
    MM58(32, 384, 58, "58mm"),
    MM80(48, 576, 80, "80mm"),
}

@Serializable
enum class PrinterCharset(val label: String) {
    UTF8("UTF-8"),
    SHIFT_JIS("Shift_JIS"),
}

/** How bytes reach the printer (FR-9.1). */
@Serializable
enum class PrinterTransport(val label: String) {
    BLUETOOTH("Bluetooth (SPP)"),
    SERIAL("Serial / COM"),
    TCP("TCP/IP"),
}

/** Command set used to encode a ticket (FR-9.2). */
@Serializable
enum class PrintProtocol(val label: String, val raster: Boolean) {
    ESCPOS_TEXT("ESC/POS text", false),
    ESCPOS_RASTER("ESC/POS raster (GS v 0)", true),
    ESCPOS_BITIMAGE("ESC/POS bit image (ESC *)", true),
    TSPL("TSPL (label)", true),
    CPCL("CPCL (label)", true),
}

@Serializable
data class PrinterSettings(
    val transport: PrinterTransport = PrinterTransport.BLUETOOTH,
    val host: String = "",
    val port: Int = 9100,
    val bluetoothAddress: String = "",
    val bluetoothName: String = "",
    val serialPort: String = "",
    val protocol: PrintProtocol = PrintProtocol.ESCPOS_RASTER,
    val paperWidth: PaperWidth = PaperWidth.MM58,
    val charset: PrinterCharset = PrinterCharset.UTF8,
    val feedLines: Int = 3,
    val cutEnabled: Boolean = false,
    val labelHeightMm: Int = 40,
    val labelGapMm: Int = 2,
)

@Serializable
enum class Language { SYSTEM, JA, EN }

@Serializable
data class Settings(
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val prepareHour: Int = 21,
    val prepareMinute: Int = 0,
    val printer: PrinterSettings = PrinterSettings(),
    val language: Language = Language.SYSTEM,
    val showCompleted: Boolean = true,
    /** Set once the first-run routine suggestion has been shown (FR-6.6). */
    val onboardingDone: Boolean = false,
)

@Serializable
data class DayStat(val date: String, val loops: Int)

@Serializable
data class AppData(
    val schemaVersion: Int = SCHEMA_VERSION,
    val tasks: List<Task> = emptyList(),
    val tickets: List<Ticket> = emptyList(),
    val routines: List<Routine> = emptyList(),
    val settings: Settings = Settings(),
    val dayStats: List<DayStat> = emptyList(),
    /** Dates (yyyy-MM-dd) for which routine tickets were already generated (FR-6.3). */
    val routineGeneratedDates: Set<String> = emptySet(),
)
