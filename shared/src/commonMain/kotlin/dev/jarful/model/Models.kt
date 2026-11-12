package dev.jarful.model

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
    /** Last-writer-wins timestamp for sync (FR-14.5). Stamped automatically by the Store. */
    val updatedAt: Long = 0,
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
    /** Last-writer-wins timestamp for sync (FR-14.5). Stamped automatically by the Store. */
    val updatedAt: Long = 0,
)

@Serializable
enum class PaperWidth(val columns: Int, val dots: Int, val mm: Int, val label: String) {
    MM58(32, 384, 58, "58mm"),
    MM80(48, 576, 80, "80mm"),
}

/**
 * Text-mode character sets (FR-9.2 ESC/POS text). [codePage] is the `ESC t n` page number of the
 * Epson table (-1 = do not send). [kanji] switches the printer into 2-byte mode with `FS &`.
 */
@Serializable
enum class PrinterCharset(val label: String, val javaName: String, val codePage: Int, val kanji: Boolean = false) {
    UTF8("UTF-8", "UTF-8", -1),
    SHIFT_JIS("Shift_JIS (日本語)", "Shift_JIS", -1, kanji = true),
    BIG5("Big5 (繁體中文)", "Big5", -1, kanji = true),
    GB18030("GB18030 (简体中文)", "GB18030", -1, kanji = true),
    CP437("CP437 (US)", "IBM437", 0),
    CP850("CP850 (Latin-1)", "IBM850", 2),
    WIN1252("Windows-1252 (Latin-1)", "windows-1252", 16),
    CP858("CP858 (Latin-1 + €)", "IBM00858", 19),
    CP852("CP852 (Latin-2)", "IBM852", 18),
    WIN1250("Windows-1250 (Latin-2: Polski)", "windows-1250", 45),
    CP866("CP866 (Кириллица)", "IBM866", 17),
    WIN1251("Windows-1251 (Кириллица)", "windows-1251", 46),
    CP864("CP864 (العربية)", "IBM864", 37),
    WIN1256("Windows-1256 (العربية)", "windows-1256", 50),
    WIN1258("Windows-1258 (Tiếng Việt)", "windows-1258", 52),
}

/** How bytes reach the printer (FR-9.1). */
@Serializable
enum class PrinterTransport(val label: String) {
    BLUETOOTH("Bluetooth Classic (SPP)"),
    BLUETOOTH_LE("Bluetooth LE (GATT)"),
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
    /** "Cat printer" pocket printers (GB01/GB02/GB03/GT01/MX05/MX06/YT01…): proprietary 0x51 0x78 packets over BLE AE01. */
    CATPRINTER("Cat printer (GB01/GT01/MX06)", true),
    /** Newer cat printers (MXW01, X5h…): `22 21` control packets on AE01, raw rows on AE03, replies on AE02. BLE only. */
    CATPRINTER_MXW01("Cat printer MXW01 (AE03 data)", true),
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

/** UI languages (NFR-4). [tag] is the BCP-47 prefix used to match the system locale; [rtl] flips the layout. */
@Serializable
enum class Language(val tag: String, val nativeName: String, val rtl: Boolean = false) {
    SYSTEM("", "System"),
    JA("ja", "日本語"),
    EN("en", "English"),
    FR("fr", "Français"),
    AR("ar", "العربية", rtl = true),
    RU("ru", "Русский"),
    ES("es", "Español"),
    DE("de", "Deutsch"),
    VI("vi", "Tiếng Việt"),
    PL("pl", "Polski"),
    UK("uk", "Українська"),
    ID("id", "Bahasa Indonesia"),
    ZH_TW("zh-TW", "繁體中文（台灣）"),
}

/** LAN sync configuration (FR-14). Device-local, never synced itself. */
@Serializable
data class SyncSettings(
    val hostEnabled: Boolean = false,
    val port: Int = 47831,
    val pin: String = "",
    val peerHost: String = "",
    val peerPort: Int = 47831,
    val peerPin: String = "",
    val autoSync: Boolean = true,
    val lastSyncAt: Long? = null,
    val lastSyncResult: String? = null,
    val deviceId: String = "",
)

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
    val sync: SyncSettings = SyncSettings(),
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
    /** id -> deletedAt for synced entity types (FR-14.4). */
    val tombstones: Map<String, Long> = emptyMap(),
)
