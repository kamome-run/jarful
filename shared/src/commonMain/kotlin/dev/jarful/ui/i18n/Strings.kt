package dev.jarful.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import dev.jarful.model.Language
import dev.jarful.platform.systemLanguageTag
import kotlinx.datetime.DayOfWeek

/** UI strings (NFR-4). Japanese is the primary language; English is the fallback. */
data class Strings(
    val appName: String = "Jarful",
    val tabToday: String, val tabColumns: String, val tabRoutines: String, val tabStats: String, val tabSettings: String,
    val inbox: String,
    val root: String,
    val newTask: String, val newSubtask: String, val addTaskHint: String, val addSubtaskHint: String,
    val rename: String, val delete: String, val undo: String, val cancel: String, val ok: String, val save: String, val close: String,
    val toToday: String, val columnToToday: String, val print: String, val printColumn: String, val printToday: String, val printTask: String,
    val estimate: String, val timebox: String, val minutes: String, val noEstimate: String,
    val microtaskHint: String, val breakDownHint: String, val breakDownAction: String,
    val pasteLines: String, val pasteLinesHint: String,
    val done: String, val start: String, val stop: String, val complete: String, val extend5: String, val breakDown: String,
    val timeUp: String, val running: String, val elapsed: String, val remaining: String,
    val todayTitle: String, val todayEmpty: String, val todayEmptyHint: String, val loopsToday: (Int) -> String,
    val combo: (Int) -> String, val undoDone: String,
    val carryOverTitle: String, val carryOver: String, val discard: String, val carryOverAll: String, val discardAll: String,
    val routines: String, val routineNew: String, val routineTitle: String, val routineCategory: String, val routineDays: String,
    val routineQuota: String, val routineQuotaHint: String, val routineEnabled: String, val routineEmpty: String, val routineOrderHint: String,
    val routinePrintToday: String, val routineRegenerate: String, val sampleRoutinesTitle: String, val sampleRoutinesBody: String, val sampleRoutinesAdd: String, val sampleRoutinesSkip: String,
    val stats: String, val streak: (Int) -> String, val last90: String, val routineRates: String, val totalLoops: (Int) -> String, val bestDay: (Int) -> String, val noData: String,
    val settings: String, val sound: String, val haptics: String, val prepareTime: String, val prepareTimeHint: String,
    val printer: String, val printerHost: String, val printerPort: String, val paperWidth: String, val charset: String, val testPrint: String,
    val printerNoHost: String, val printOk: String, val printFailed: (String) -> String,
    val language: String, val langSystem: String, val showCompleted: String,
    val dataTitle: String, val exportJson: String, val importJson: String, val importFailed: String, val importOk: String, val dataPath: String, val copied: String, val pasteJsonHint: String,
    val refocus: String, val refocusHint: String, val refocusPlaceholder: String, val refocusGo: String,
    val shortcuts: String, val shortcutsList: List<Pair<String, String>>,
    val quotaProgress: (Int, Int) -> String,
    val dayShort: (DayOfWeek) -> String,
    val about: String, val aboutBody: String,
    val printed: String, val childCount: (Int, Int) -> String,
    val confirmDelete: (String) -> String, val moveUp: String, val moveDown: String, val moveTo: String, val moveToRoot: String,
    val all: String, val subtasksOf: (String) -> String, val empty: String,
    val sampleRoutines: List<Triple<String, String, Int?>>,
    val sync: String, val syncIntro: String, val syncHost: String, val syncHostHint: String, val syncHostAddresses: String, val syncHostNoAddress: String,
    val syncPort: String, val syncPin: String, val syncRegeneratePin: String, val syncPeer: String, val syncPeerHost: String, val syncPeerPin: String,
    val syncAuto: String, val syncNow: String, val syncOk: String, val syncFailed: (String) -> String, val syncLast: String, val syncNever: String,
    val syncHostRunning: String, val syncHostError: (String) -> String, val syncError: (String) -> String,
    val printerDiagnose: String, val printerDiagnoseTitle: String, val printerDiagnoseHint: String, val copy: String, val printSwitchedTransport: (String) -> String,
    val nothingToPrint: String,
    val printBusy: String, val printPaused: (Int) -> String, val printProgress: (Int, Int) -> String, val printResume: String, val printDiscard: String, val printStop: String, val printPaperHint: String,
    // v1.7: date print, bulk toggle, duplicate, drag reorder, depth limit, categories
    val routinePrintDate: String, val printDateTitle: String, val printDateHint: String, val today: String, val tomorrow: String,
    val selectMode: String, val selectAll: String, val selectNone: String, val selectedCount: (Int) -> String, val enableSelected: String, val disableSelected: String,
    val duplicate: String, val duplicateCount: String, val duplicateHint: String,
    val dragToReorder: String, val depthLimitHint: String, val uncategorized: String,
)

val JA = Strings(
    tabToday = "今日", tabColumns = "カラム", tabRoutines = "ルーチン", tabStats = "統計", tabSettings = "設定",
    inbox = "受信箱", root = "すべて",
    newTask = "新しいタスク", newSubtask = "子タスクを追加（分解）", addTaskHint = "タスク名を入力して Enter（Tab で分解）", addSubtaskHint = "子タスクを入力して Enter",
    rename = "名前を変更", delete = "削除", undo = "元に戻す", cancel = "キャンセル", ok = "OK", save = "保存", close = "閉じる",
    toToday = "今日のチケットにする", columnToToday = "この列を今日へ", print = "印刷", printColumn = "この列を印刷", printToday = "今日を全部印刷", printTask = "このタスクを印刷",
    estimate = "所要時間の目安", timebox = "タイムボックス", minutes = "分", noEstimate = "未設定",
    microtaskHint = "目安: 2〜5 分で終わる大きさに", breakDownHint = "3日以上残っています。もっと細かく分解しましょう", breakDownAction = "分解する",
    pasteLines = "複数行から分解", pasteLinesHint = "1 行 1 タスクで貼り付け",
    done = "完了", start = "開始", stop = "中断", complete = "完了 ✓", extend5 = "5 分延長", breakDown = "分解する",
    timeUp = "時間です！", running = "実行中", elapsed = "経過", remaining = "残り",
    todayTitle = "今日のチケット", todayEmpty = "チケットがありません", todayEmptyHint = "カラムからタスクを「今日へ」出すか、Ctrl+K でリフォーカス",
    loopsToday = { "今日 $it ループ" }, combo = { "$it 連続！" }, undoDone = "完了を取り消す",
    carryOverTitle = "持ち越し候補（前日までの未完了）", carryOver = "今日へ", discard = "破棄", carryOverAll = "すべて今日へ", discardAll = "すべて破棄",
    routines = "ルーチン（曜日別の習慣）", routineNew = "ルーチンを追加", routineTitle = "タイトル", routineCategory = "カテゴリ見出し", routineDays = "曜日",
    routineQuota = "クォータ（件数目標）", routineQuotaHint = "「メールを 10 件処理」のように数で区切る習慣に", routineEnabled = "有効", routineEmpty = "ルーチンがありません。朝の簡単な日課から登録しましょう。",
    routineOrderHint = "上から順に今日のチケットになります。簡単なものを先頭に。",
    routinePrintToday = "今日のルーチンを印刷", routineRegenerate = "今日分を再生成",
    sampleRoutinesTitle = "簡単な日課から始めましょう", sampleRoutinesBody = "記事のアドバイスどおり、朝の簡単なルーチンを用意すると勢いがつきます。サンプルを追加しますか？（後で編集できます）",
    sampleRoutinesAdd = "サンプルを追加", sampleRoutinesSkip = "あとで",
    stats = "統計", streak = { "$it 日連続" }, last90 = "過去 90 日のループ数", routineRates = "ルーチン達成率（30 日）", totalLoops = { "合計 $it ループ" }, bestDay = { "最高 $it ループ/日" }, noData = "まだデータがありません",
    settings = "設定", sound = "効果音", haptics = "触覚フィードバック", prepareTime = "翌日分の準備時刻", prepareTimeHint = "この時刻以降にアプリを開くと、翌日のルーチンチケットを準備します",
    printer = "サーマルプリンター", printerHost = "ホスト / IP アドレス", printerPort = "ポート", paperWidth = "用紙幅", charset = "文字コード", testPrint = "テスト印刷",
    printerNoHost = "プリンターのホストを設定してください", printOk = "印刷しました", printFailed = { "印刷に失敗: $it" },
    language = "言語", langSystem = "システムの言語", showCompleted = "完了済みタスクを表示",
    dataTitle = "データ", exportJson = "JSON をエクスポート（コピー）", importJson = "JSON をインポート", importFailed = "読み込めませんでした", importOk = "インポートしました", dataPath = "保存先", copied = "クリップボードにコピーしました", pasteJsonHint = "エクスポートした JSON を貼り付け",
    refocus = "リフォーカス", refocusHint = "先延ばしに気づいたら、次にやる 3〜5 個を 1 行ずつ書いて、すぐ始める。", refocusPlaceholder = "例:\n机の上を片付ける\nメールを 3 通返す\n10 分だけ書く", refocusGo = "始める",
    shortcuts = "キーボードショートカット",
    shortcutsList = listOf(
        "N" to "同じ列に新規タスク", "Tab / Shift+Enter" to "子タスクを追加（分解）", "↑ ↓" to "同じ列で移動", "← →" to "列を移動",
        "Space" to "完了 / 未完了", "T" to "今日のチケットにする", "Shift+T" to "列全体を今日へ", "P" to "選択タスクを印刷", "Shift+P" to "列全体を印刷",
        "Ctrl+P" to "今日を全部印刷", "Ctrl+K" to "リフォーカス", "F2" to "名前を変更", "Delete" to "削除", "Alt+↑ ↓" to "並べ替え", "Ctrl+Z" to "元に戻す",
        "Ctrl+1〜5" to "タブ切替", "Esc" to "キャンセル",
    ),
    quotaProgress = { c, t -> "$c / $t 件" },
    dayShort = { d -> when (d) { DayOfWeek.MONDAY -> "月"; DayOfWeek.TUESDAY -> "火"; DayOfWeek.WEDNESDAY -> "水"; DayOfWeek.THURSDAY -> "木"; DayOfWeek.FRIDAY -> "金"; DayOfWeek.SATURDAY -> "土"; else -> "日" } },
    about = "Jarful について", aboutBody = "「瓶いっぱい」のゲームループ型タスク管理。タスクを 2〜5 分に分解し、完了したらくしゃっと丸めて瓶へ。MIT License / OSS。",
    printed = "印刷済み", childCount = { open, total -> "$open/$total" },
    confirmDelete = { "「$it」と子タスクを削除しますか？（Ctrl+Z で戻せます）" }, moveUp = "上へ", moveDown = "下へ", moveTo = "移動先", moveToRoot = "最上位へ",
    all = "すべて", subtasksOf = { "「$it」の子タスク" }, empty = "（空）",
    sampleRoutines = listOf(
        Triple("コーヒーを淹れる", "朝", 3), Triple("窓を開けて換気", "朝", 1), Triple("ベッドを整える", "朝", 2),
        Triple("2 分ウォームアップ（タイピング練習）", "仕事", 2), Triple("今日のチケットを確認する", "仕事", 2),
        Triple("机の上を片付ける", "夜", 5), Triple("明日のチケットを準備する", "夜", 5),
    ),
    sync = "端末間の同期（同じ Wi-Fi 内）", syncIntro = "クラウドを使わず、同じネットワーク上の端末同士で直接同期します。片方を「ホスト」にして、もう片方にホストのアドレスと PIN を入力してください。",
    syncHost = "この端末をホストにする", syncHostHint = "アプリを開いている間、他の端末からの同期を受け付けます（Windows 側をホストにするのがおすすめ）。",
    syncHostAddresses = "この端末のアドレス", syncHostNoAddress = "ネットワークに接続されていません",
    syncPort = "ポート", syncPin = "PIN", syncRegeneratePin = "PIN を再生成", syncPeer = "接続先（クライアントとして）", syncPeerHost = "ホストの IP アドレス", syncPeerPin = "ホストの PIN",
    syncAuto = "自動同期（起動時・5 分ごと）", syncNow = "今すぐ同期", syncOk = "同期しました", syncFailed = { "同期に失敗: $it" }, syncLast = "最終同期", syncNever = "未同期",
    syncHostRunning = "受付中", syncHostError = { "ホストを開始できません: $it" },
    syncError = { code -> when (code) { "UNREACHABLE" -> "接続できません（同じ Wi-Fi か、ホストが起動しているか確認）"; "PIN_MISMATCH" -> "PIN が違います"; "VERSION_MISMATCH" -> "アプリのバージョンが違います"; "NO_PEER" -> "接続先が未設定"; "BAD_RESPONSE" -> "応答を読めません"; else -> code } },
    printerDiagnose = "接続診断", printerDiagnoseTitle = "Bluetooth 接続診断", printerDiagnoseHint = "機器の種類・SPP の有無・GATT サービス・各接続方式の結果を表示します。うまく接続できないときは、この結果をコピーして GitHub の Issue に貼ってください。", copy = "コピー", printSwitchedTransport = { "接続方式を $it に切り替えました" },
    nothingToPrint = "印刷するものがありません。タスクを「今日のチケットにする」か、タスクの ⋮ → 印刷を使ってください。",
    printBusy = "印刷中です", printPaused = { "印刷が中断されました（残り $it 枚）" }, printProgress = { d, n -> "印刷中 $d / $n 枚" }, printResume = "続きから印刷", printDiscard = "残りを破棄", printStop = "中断", printPaperHint = "用紙切れのときはロールを交換してから「続きから印刷」を押してください。",
    routinePrintDate = "日付を選んで印刷", printDateTitle = "印刷する日付", printDateHint = "翌朝の分を前夜に印刷できます。選んだ日のルーチンをチケットにして印刷します。", today = "今日", tomorrow = "翌日", selectMode = "まとめて選択", selectAll = "すべて選択", selectNone = "選択解除", selectedCount = { "$it 件を選択中" }, enableSelected = "選択をオン", disableSelected = "選択をオフ", duplicate = "複製", duplicateCount = "複製する数", duplicateHint = "子タスクも一緒に複製されます（最大 50）。", dragToReorder = "≡ をドラッグして並べ替え", depthLimitHint = "ひ孫タスクまでです。これ以上は分解できません。", uncategorized = "未分類",
)

val EN = Strings(
    tabToday = "Today", tabColumns = "Columns", tabRoutines = "Routines", tabStats = "Stats", tabSettings = "Settings",
    inbox = "Inbox", root = "All",
    newTask = "New task", newSubtask = "Add subtask (break down)", addTaskHint = "Type a task and press Enter (Tab = break down)", addSubtaskHint = "Type a subtask and press Enter",
    rename = "Rename", delete = "Delete", undo = "Undo", cancel = "Cancel", ok = "OK", save = "Save", close = "Close",
    toToday = "Make today's ticket", columnToToday = "Column → Today", print = "Print", printColumn = "Print column", printToday = "Print all of today", printTask = "Print this task",
    estimate = "Estimate", timebox = "Timebox", minutes = "min", noEstimate = "none",
    microtaskHint = "Aim for 2–5 minutes per task", breakDownHint = "Open for 3+ days. Break it down further.", breakDownAction = "Break down",
    pasteLines = "Break down from lines", pasteLinesHint = "Paste one task per line",
    done = "Done", start = "Start", stop = "Stop", complete = "Complete ✓", extend5 = "+5 min", breakDown = "Break down",
    timeUp = "Time's up!", running = "Running", elapsed = "elapsed", remaining = "left",
    todayTitle = "Today's tickets", todayEmpty = "No tickets", todayEmptyHint = "Send tasks from Columns with “Today”, or press Ctrl+K to refocus",
    loopsToday = { "$it loops today" }, combo = { "$it in a row!" }, undoDone = "Undo complete",
    carryOverTitle = "Carry-over candidates (open from previous days)", carryOver = "To today", discard = "Discard", carryOverAll = "All to today", discardAll = "Discard all",
    routines = "Routines (habits by weekday)", routineNew = "Add routine", routineTitle = "Title", routineCategory = "Category header", routineDays = "Weekdays",
    routineQuota = "Quota (count target)", routineQuotaHint = "For habits like “process 10 emails”", routineEnabled = "Enabled", routineEmpty = "No routines yet. Start with easy morning habits.",
    routineOrderHint = "Tickets are created top to bottom. Put easy wins first.",
    routinePrintToday = "Print today's routines", routineRegenerate = "Regenerate today",
    sampleRoutinesTitle = "Start with easy wins", sampleRoutinesBody = "As the article suggests, easy morning routines build momentum. Add sample routines? (You can edit them later.)",
    sampleRoutinesAdd = "Add samples", sampleRoutinesSkip = "Later",
    stats = "Stats", streak = { "$it-day streak" }, last90 = "Loops over the last 90 days", routineRates = "Routine completion (30 days)", totalLoops = { "$it loops total" }, bestDay = { "best $it loops/day" }, noData = "No data yet",
    settings = "Settings", sound = "Sound effects", haptics = "Haptic feedback", prepareTime = "Prepare tomorrow at", prepareTimeHint = "Opening the app after this time prepares tomorrow's routine tickets",
    printer = "Thermal printer", printerHost = "Host / IP address", printerPort = "Port", paperWidth = "Paper width", charset = "Charset", testPrint = "Test print",
    printerNoHost = "Set the printer host first", printOk = "Printed", printFailed = { "Print failed: $it" },
    language = "Language", langSystem = "System language", showCompleted = "Show completed tasks",
    dataTitle = "Data", exportJson = "Export JSON (copy)", importJson = "Import JSON", importFailed = "Could not read the data", importOk = "Imported", dataPath = "Storage path", copied = "Copied to clipboard", pasteJsonHint = "Paste exported JSON",
    refocus = "Refocus", refocusHint = "Noticed you're procrastinating? Write the next 3–5 tasks, one per line, and start right away.", refocusPlaceholder = "e.g.\nClear the desk\nReply to 3 emails\nWrite for 10 minutes", refocusGo = "Start",
    shortcuts = "Keyboard shortcuts",
    shortcutsList = listOf(
        "N" to "New task in this column", "Tab / Shift+Enter" to "Add subtask (break down)", "↑ ↓" to "Move within column", "← →" to "Move between columns",
        "Space" to "Toggle done", "T" to "Make today's ticket", "Shift+T" to "Whole column → today", "P" to "Print selected task", "Shift+P" to "Print column",
        "Ctrl+P" to "Print all of today", "Ctrl+K" to "Refocus", "F2" to "Rename", "Delete" to "Delete", "Alt+↑ ↓" to "Reorder", "Ctrl+Z" to "Undo",
        "Ctrl+1–5" to "Switch tab", "Esc" to "Cancel",
    ),
    quotaProgress = { c, t -> "$c / $t" },
    dayShort = { d -> d.name.take(3).lowercase().replaceFirstChar { it.uppercase() } },
    about = "About Jarful", aboutBody = "Game-loop task manager. Break tasks into 2–5 minute pieces, crumple them when done, and watch the jar fill up. MIT License / OSS.",
    printed = "printed", childCount = { open, total -> "$open/$total" },
    confirmDelete = { "Delete “$it” and its subtasks? (Ctrl+Z to undo)" }, moveUp = "Move up", moveDown = "Move down", moveTo = "Move to", moveToRoot = "To top level",
    all = "All", subtasksOf = { "Subtasks of “$it”" }, empty = "(empty)",
    sampleRoutines = listOf(
        Triple("Make coffee", "Morning", 3), Triple("Open the window", "Morning", 1), Triple("Make the bed", "Morning", 2),
        Triple("2-minute warm-up (typing practice)", "Work", 2), Triple("Review today's tickets", "Work", 2),
        Triple("Clear the desk", "Evening", 5), Triple("Prepare tomorrow's tickets", "Evening", 5),
    ),
    sync = "Device sync (same Wi-Fi)", syncIntro = "No cloud: devices on the same network sync directly. Make one device the host and enter its address and PIN on the other.",
    syncHost = "Make this device the host", syncHostHint = "Accepts sync from other devices while the app is open (the Windows PC is the recommended host).",
    syncHostAddresses = "This device's addresses", syncHostNoAddress = "Not connected to a network",
    syncPort = "Port", syncPin = "PIN", syncRegeneratePin = "Regenerate PIN", syncPeer = "Connect to (as client)", syncPeerHost = "Host IP address", syncPeerPin = "Host PIN",
    syncAuto = "Auto sync (on launch and every 5 min)", syncNow = "Sync now", syncOk = "Synced", syncFailed = { "Sync failed: $it" }, syncLast = "Last sync", syncNever = "never",
    syncHostRunning = "listening", syncHostError = { "Cannot start host: $it" },
    syncError = { code -> when (code) { "UNREACHABLE" -> "Cannot reach the host (same Wi-Fi? host running?)"; "PIN_MISMATCH" -> "Wrong PIN"; "VERSION_MISMATCH" -> "App versions differ"; "NO_PEER" -> "No host configured"; "BAD_RESPONSE" -> "Unreadable response"; else -> code } },
    printerDiagnose = "Diagnose connection", printerDiagnoseTitle = "Bluetooth diagnostics", printerDiagnoseHint = "Shows the device type, SPP availability, GATT services and the result of every connection strategy. If printing fails, copy this and paste it into a GitHub issue.", copy = "Copy", printSwitchedTransport = { "Switched transport to $it" },
    nothingToPrint = "Nothing to print. Make the task today's ticket, or use the task menu ⋮ → Print.",
    printBusy = "Already printing", printPaused = { "Printing paused ($it left)" }, printProgress = { d, n -> "Printing $d / $n" }, printResume = "Resume printing", printDiscard = "Discard the rest", printStop = "Stop", printPaperHint = "Out of paper? Replace the roll, then press “Resume printing”.",
    routinePrintDate = "Print for a date…", printDateTitle = "Date to print", printDateHint = "Print tomorrow morning's routines tonight: the routines of the chosen day become tickets and are printed.", today = "Today", tomorrow = "Tomorrow", selectMode = "Select several", selectAll = "Select all", selectNone = "Clear selection", selectedCount = { "$it selected" }, enableSelected = "Turn selection on", disableSelected = "Turn selection off", duplicate = "Duplicate", duplicateCount = "Number of copies", duplicateHint = "Subtasks are duplicated too (max 50).", dragToReorder = "Drag ≡ to reorder", depthLimitHint = "Great-grandchild level reached; tasks cannot be broken down further.", uncategorized = "Uncategorized",
)

/** Resolves the effective UI language: explicit choice, or the best match for the system locale. */
fun resolveLanguage(setting: Language): Language {
    if (setting != Language.SYSTEM) return setting
    val tag = systemLanguageTag().lowercase()
    if (tag.startsWith("zh")) return if (tag.contains("tw") || tag.contains("hant") || tag.contains("hk") || tag.contains("mo")) Language.ZH_TW else Language.ZH_TW
    return Language.entries.firstOrNull { it != Language.SYSTEM && tag.startsWith(it.tag.lowercase()) } ?: Language.EN
}

fun stringsFor(lang: Language): Strings = when (resolveLanguage(lang)) {
    Language.JA -> JA
    Language.EN -> EN
    Language.FR -> FR
    Language.AR -> AR
    Language.RU -> RU
    Language.ES -> ES
    Language.DE -> DE
    Language.VI -> VI
    Language.PL -> PL
    Language.UK -> UK
    Language.ID -> ID
    Language.ZH_TW -> ZH_TW
    Language.SYSTEM -> EN
}

val LocalStrings = staticCompositionLocalOf { JA }
