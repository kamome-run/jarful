# Jarful — 為 ADHD 大腦設計的「遊戲循環」任務管理工具

[![CI](https://github.com/kamome-run/jarful/actions/workflows/ci.yml/badge.svg)](https://github.com/kamome-run/jarful/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[日本語](README.md) · [English](README.en.md) · [Français](README.fr.md) · [العربية](README.ar.md) · [Русский](README.ru.md) · [Español](README.es.md) · [Deutsch](README.de.md) · [Tiếng Việt](README.vi.md) · [Polski](README.pl.md) · [Українська](README.uk.md) · [Bahasa Indonesia](README.id.md) · **繁體中文（台灣）**

「玩遊戲可以專注好幾個小時，工作和家事卻一直拖。」Jarful 把 ADHD 創業者 Laurie Hérault 提出的
**便利貼 × 透明罐子 × 熱感應印表機** 方法
（[原文](https://www.laurieherault.com/articles/a-thermal-receipt-printer-cured-my-procrastination)）
做成了 Android（含 Chromebook 與 Google 的 ChromeOS／Android 筆電）和 Windows 11 的 App。

> **Jarful** = 「裝滿的罐子」。把每張完成的票券揉成一團，看著罐子慢慢裝滿。

---

## 目錄

1. [運作原理](#1-運作原理)
2. [需要準備的東西](#2-需要準備的東西)
3. [支援平台](#3-支援平台)
4. [安裝](#4-安裝)
5. [第一次啟動與一天的流程](#5-第一次啟動與一天的流程)
6. [印表機設定（詳細）](#6-印表機設定詳細)
7. [Android ⇄ Windows 同步（詳細）](#7-android--windows-同步詳細)
8. [鍵盤快速鍵](#8-鍵盤快速鍵)
9. [觸控操作](#9-觸控操作)
10. [資料與備份](#10-資料與備份)
11. [語言](#11-語言)
12. [疑難排解](#12-疑難排解)
13. [從原始碼建置](#13-從原始碼建置)
14. [授權與免責聲明](#14-授權與免責聲明)

---

## 1. 運作原理

| 文章的方法 | 在 Jarful 中 |
|-----------|-----------|
| 把任務拆成 **2〜5 分鐘的微任務**，讓循環頻繁重複 | 階層任務以 **並排的欄位** 顯示；按 `Tab` 立即新增子任務 |
| 一張便利貼＝一件任務；完成後 **揉成一團丟進透明罐子** | 完成「今天的票券」會播放 **揉紙動畫＋紙張音效＋震動**，並有紙球掉進罐子 |
| 一天從簡單的習慣開始；**前一晚就把明天準備好** | 依星期設定的 **例行事項** 會在準備時間（預設 21:00）之後自動產生明天的票券 |
| 發現自己在拖延時，寫下 **接下來的 3〜5 件事** 馬上開始 | `Ctrl+K` **重新聚焦**：一行一件 → 立刻變成票券 → 第一張自動開始 |
| 拆不開的任務就 **用時間來拆**（「只做 10 分鐘」） | 有時間盒的票券會倒數；結束時選「完成／延長 5 分鐘／拆解」 |
| 堆積的事（幾千封郵件）變成「**所有新的＋N 件舊的，每天做**」 | **配額型例行事項**（+1 計數，達標即完成） |
| 用 **熱感應印表機** 消除阻力 | 透過 Bluetooth Classic／Bluetooth LE／COM／TCP 以 **ESC/POS、TSPL 或 CPCL** 列印，一張票券一張紙 |

## 畫面截圖

Windows 11 版採用 **Fluent Design System**（WinUI 3），Android 版採用 **Material 3**，品牌色彩一致。

| Windows 11（Fluent） | Windows 11 深色 | Windows 11 繁體中文 |
|---|---|---|
| ![windows](docs/screenshots/windows-fluent.png) | ![windows dark](docs/screenshots/windows-fluent-dark.png) | ![windows zh-TW](docs/screenshots/windows-fluent-zh-TW.png) |

| Android（Material 3） | Android 深色 | Android 阿拉伯文（RTL） |
|---|---|---|
| ![phone](docs/screenshots/android-material-phone.png) | ![phone dark](docs/screenshots/android-material-phone-dark.png) | ![phone ar](docs/screenshots/android-material-phone-ar.png) |

| 列印票券（日文） | 阿拉伯文 | 繁體中文 | 越南文 |
|---|---|---|---|
| ![ticket](docs/screenshots/ticket-raster.png) | ![ticket ar](docs/screenshots/ticket-raster-ar.png) | ![ticket zh](docs/screenshots/ticket-raster-zh-TW.png) | ![ticket vi](docs/screenshots/ticket-raster-vi.png) |

## 2. 需要準備的東西

**必要**
- Android 裝置（手機／平板／Chromebook／Google 的 ChromeOS 或 Android 筆電）或 Windows 11 電腦。

**建議（完整重現這套方法）**
- **熱感應印表機**：任何相容 ESC/POS 的 58 mm 或 80 mm 機型（Bluetooth Classic、Bluetooth LE，或有線網路／Wi-Fi）。若要用標籤紙，需支援 TSPL 或 CPCL 的標籤機。
- **感熱紙卷**：符合寬度的紙卷。票券會經常拿在手上，建議選 **不含雙酚（無 BPA／BPS）** 的紙。
- **白板與磁鐵**：列印出來的票券要 **用磁鐵貼在白板上**，讓今天要做的事就在眼前。把完成的票券撕下來揉掉，就是獎勵。準備 20〜30 顆小磁鐵（直徑 10〜15 mm）會很方便。
- **透明罐子**：用來裝揉成一團的票券。App 內的罐子也可以，但實體罐子效果更好。

## 3. 支援平台

| 平台 | 檔案 | 說明 |
|---|---|---|
| Android 8.0 以上 | `androidApp-debug.apk`／`androidApp-release-unsigned.apk` | 手機與平板；Material 3，Android 12 以上支援動態色彩 |
| Chromebook（ChromeOS）／Google 的 ChromeOS 或 Android 筆電 | 同上 | 沒有觸控螢幕也能安裝；支援鍵盤、滑鼠與觸控 |
| Windows 11（x64） | `Jarful-*.msi`／`Jarful-*.exe` | Fluent Design 介面；藍牙印表機透過虛擬 COM 埠 |

檔案請至 [Releases](https://github.com/kamome-run/jarful/releases) 頁面下載。

## 4. 安裝

### 4.1 Android（手機／平板）

1. 用裝置的瀏覽器開啟 [Releases](https://github.com/kamome-run/jarful/releases)，下載最新的 `androidApp-debug.apk`。
2. 從通知或「檔案」App 點選 APK。
3. 若出現「不明來源應用程式」警告，點 **設定 → 允許來自這個來源的應用程式** 後返回（只需對瀏覽器／檔案 App 允許一次）。
4. 點 **安裝**，完成後點 **開啟**。
5. 第一次啟動時會出現「從簡單的小勝利開始」，選 **加入範例** 即可建立早晨的例行事項（之後可修改）。

> `release-unsigned.apk` 是給要自行簽章並上架的開發者用的，一般請使用 `debug.apk`。

### 4.2 Chromebook／Google 的 ChromeOS 或 Android 筆電

ChromeOS 安裝 Google Play 以外的 APK 有兩種方式。

**方式 A：Linux 開發環境 + adb（建議）**
1. **設定 → 進階 → 開發人員 → Linux 開發環境 → 開啟**（第一次需要幾分鐘）。
2. 在同一畫面開啟 **開發 Android 應用程式 → ADB 偵錯**，然後重新啟動。
3. 在 Linux 終端機安裝 adb 並連線：
   ```bash
   sudo apt update && sudo apt install -y adb
   adb connect 100.115.92.2:5555      # 在畫面上出現的對話框按「允許」
   adb install ~/Downloads/androidApp-debug.apk
   ```
4. 啟動器中會出現 **Jarful**。視窗可調整大小，寬度達 840 dp 以上會切換成三欄版面。

**方式 B：由管理者透過 Play 商店發布**（學校／公司的受管理裝置）：管理員可將 APK 以私人應用程式發布。

藍牙印表機請先在 **ChromeOS 設定 → 藍牙** 配對，再到 App 內選擇（Classic 與 LE 都支援）。

### 4.3 Windows 11

1. 從 [Releases](https://github.com/kamome-run/jarful/releases) 下載 `Jarful-<版本>.msi`。
2. 雙擊啟動安裝程式。若出現藍色的 **SmartScreen** 畫面，請點 **其他資訊 → 仍要執行**（安裝程式未做程式碼簽章；原始碼公開於本儲存庫）。
3. 確認安裝位置後按 **Install**。以使用者為單位安裝，不需要系統管理員權限。
4. 從開始功能表啟動 **Jarful**。
5. 資料儲存在 `%APPDATA%\Jarful\jarful-data.json`（可在設定的「儲存位置」確認）。

解除安裝：**設定 → 應用程式 → 已安裝的應用程式 → Jarful**。資料檔會保留，需要時請手動刪除。

## 5. 第一次啟動與一天的流程

1. **整理例行事項**（例行事項分頁）：把早晨簡單的習慣（泡咖啡、開窗…）由上而下排好。可依星期開關；「處理 10 封郵件」這類要計數的習慣，把數量填進 **配額**。
2. **前一晚自動準備**：在設定的「準備明天票券的時間」（預設 21:00）之後開啟 App，就會自動建立明天的例行事項票券。早上開啟時若尚未產生，也會立刻產生當天的。
3. **拆解任務**（欄位分頁）：在最左欄建立大任務（「打掃家裡」），選取後按 `Tab`（或「新增子任務」）在右欄拆成「廚房」「浴室」…，再拆到「洗碗」「擦流理台」這種 **2〜5 分鐘** 的大小。擱置超過 3 天的任務會提示「請再拆得更細」。
4. **加入今天的票券**：選取任務按 `T`，整欄則按 `Shift+T`（或選單的「整欄 → 今天」）。它們會以收據樣式的卡片出現在「今天」分頁。
5. **列印並貼上**（選用）：`Ctrl+P` 列印今天全部票券，一張張撕下 **用磁鐵貼在白板上**。
6. **執行 → 完成**：按卡片的 **開始** 會顯示經過時間（有時間盒則倒數）。做完按 **完成**（或把卡片向右滑），卡片會揉成一團掉進罐子，並有音效與震動。實體票券也撕下來揉掉丟進罐子。
7. **發現自己在拖延時**：`Ctrl+K`（⚡ 重新聚焦），把接下來要做的 3〜5 件事一行一件寫下並按 **開始**。會立刻變成票券，第一張自動進入進行中。
8. **統計**：可查看每日循環數（90 天）、連續天數與例行事項達成率。

## 6. 印表機設定（詳細）

設定分頁 → **熱感應印表機**。

### 6.1 選擇連線方式

| 連線方式 | 支援系統 | 適用的印表機 |
|---|---|---|
| **Bluetooth Classic (SPP)** | Android／Chromebook | Bluetooth 2.1〜5.x 的雙模機型（配對時多半要輸入 PIN） |
| **Bluetooth LE (GATT)** | Android／Chromebook | Bluetooth 4.0〜5.x 的 LE 專用口袋印表機（以「專用 App」形式販售的機型） |
| **Serial / COM** | Windows 11 | 透過虛擬 COM 埠使用 Bluetooth Classic 機型；USB 轉序列埠 |
| **TCP/IP** | Android／Windows | 有線網路／Wi-Fi 的收據印表機（連接埠 9100） |

不確定是哪一種藍牙？如果在系統的藍牙設定中 **可以配對**（會要求輸入 PIN 或確認），就是 Classic；若無法配對、說明書寫「請從專用 App 連線」，多半是 LE。兩種都試試，用 **測試列印** 成功的那一種。

### 6.2 選擇列印通訊協定

| 協定 | 用途 |
|---|---|
| **ESC/POS 點陣（預設）** | 大多數 58／80 mm 收據印表機。票券以影像傳送，所以 **任何語言都能正確列印，不受印表機內建字型影響** |
| ESC/POS 位元影像 | 不支援 `GS v 0` 點陣的舊機型 |
| ESC/POS 文字 | 用印表機內建字型列印。請把字元編碼（UTF-8／Shift_JIS／Big5／GB18030／Windows-125x／CP8xx 等）設成與印表機相同 |
| TSPL | 標籤機（以 mm 設定標籤高度與間距） |
| CPCL | 使用 CPCL 的標籤機 |
| 貓咪印表機 | 不支援 ESC/POS 的 57 mm 口袋印表機（GB01 / GT01 / MX06 系列，搭配「iPrint」「Fun Print」等 App 販售）。以 BLE 連線 |
| 貓咪印表機 MXW01 | 上述的新世代機型（MXW01 / X5h 系列；連線診斷會列出 AE01〜AE04）。僅限 BLE |

紙張寬度為 **58 mm（384 點）** 或 **80 mm（576 點）**。 **列印濃度**（1〜5，預設 4）可讓太淡的列印變濃。沒有裁刀的口袋印表機請把 **Cut 關閉**，進紙行數設 3〜5 即可。

### 6.3 Android／Chromebook — Bluetooth Classic

1. 開啟印表機電源；需要的話長按藍牙鍵進入配對模式。
2. **裝置的設定 → 藍牙 → 配對新裝置**，選擇印表機並輸入說明書上的 PIN（常見為 `0000` 或 `1234`）。
3. Jarful → 設定 → 熱感應印表機 → 選 **Bluetooth Classic (SPP)**。
4. 從已配對裝置清單選擇印表機（🔄 可重新整理）。Android 12 以上第一次會要求 **鄰近裝置** 權限，請允許。
5. 按 **測試列印**。幾秒內印出「Jarful / Test print OK」即完成。
6. 若列印到一半停住，請增加進紙行數，或改為一張張列印（票券的 ⋮ → 列印）。

### 6.4 Android／Chromebook — Bluetooth LE

1. 開啟裝置的 **藍牙**（多數 LE 機型不需配對）。Android 11 以下請同時開啟 **位置資訊**（BLE 掃描所需）。
2. Jarful → 設定 → 熱感應印表機 → 選 **Bluetooth LE (GATT)**。
3. 按 🔄 會掃描約 4 秒，找到的印表機會以名稱列出。選擇你的印表機。
4. **測試列印**。第一次連線可能需要 5〜10 秒。
5. 若沒有印出：重新啟動印表機、完全關閉原廠 App（LE 印表機一次只能接受一個連線）、或關閉再開啟藍牙。

### 6.5 Windows 11 — 藍牙（虛擬 COM 埠）

1. **設定 → 藍牙與裝置 → 新增裝置 → 藍牙**，配對印表機（PIN 依說明書）。
2. **設定 → 藍牙與裝置 → 裝置**，捲到最下方開啟 **更多裝置和印表機設定**。
3. 在印表機上按右鍵 → **內容 → 服務**，勾選 **序列埠 (SPP)** 後按 **確定**。
4. 在同一視窗開啟 **其他藍牙設定 → COM 連接埠**，記下印表機 **外送（Outgoing）** 的 `COMx`。若沒有，請 **新增 → 外送 → 選擇裝置 → SPP**。
5. Jarful → 設定 → 熱感應印表機 → **Serial / COM** → 從清單選 `COMx` → **測試列印**。
6. 出現「PORT_OPEN_FAILED」表示其他程式（原廠工具等）佔用了連接埠，或印表機未開機：關閉該程式並重新開啟印表機電源。

> Windows 版無法連線 LE 專用機型。請改用 Android 裝置列印，或使用支援 TCP 的印表機。

### 6.6 網路印表機（TCP/IP）

1. 將印表機接上區域網路，列印 **自我測試頁**（通常是開機時長按進紙鍵）確認 IP 位址。
2. Jarful → 設定 → **TCP/IP** → 主機填 IP，連接埠 `9100`（預設）。
3. **測試列印**。在路由器固定 IP（DHCP 保留）會更穩定。

### 6.7 「列印後貼上」的日常運作

- 早上：今天分頁 → **列印今天全部**（`Ctrl+P`）→ 撕下 → **由上而下用磁鐵貼在白板上**。
- 白天：每做完一張就撕下揉掉，丟進 **透明罐子**。App 內按完成，罐子裡的紙球也會增加。
- 晚上：明天的票券會自動準備好，隔天早上直接列印即可。

## 7. Android ⇄ Windows 同步（詳細）

不用雲端也不用帳號。**同一個 Wi-Fi 內的裝置直接互相同步**（PIN 驗證，預設連接埠 47831）。

### 7.1 主機端（建議用 Windows 電腦）

1. 設定 → **裝置同步** → 開啟 **將此裝置設為主機**。
2. 記下顯示的 **此裝置的位址**（例如 `192.168.1.20`）與 **6 位數 PIN 碼**。
3. 若 Windows **防火牆** 詢問是否允許 Jarful 存取網路，請允許 **私人網路**。
4. App 開啟期間會接受其他裝置的同步（顯示「● 等待連線中」）。

### 7.2 用戶端（Android 等）

1. 設定 → **裝置同步** → 在 **連線到** 輸入 **主機的 IP 位址** 與 **PIN 碼**。
2. 按 **立即同步**。顯示「已同步」即完成；上方列的 🔄 也可同步。
3. 開啟 **自動同步**，就會在啟動時與每 5 分鐘自動同步。

### 7.3 運作方式與注意事項

- 同步的是 **任務、票券、例行事項**；印表機設定等裝置專屬設定不會同步。
- 兩邊同時修改同一項目時，**以較晚修改的為準**。刪除也會傳到另一端（之後再編輯的話會復原）。
- 通訊是區域網路內的明文 HTTP。請在家裡、公司等可信任的網路使用，公共 Wi-Fi 請關閉主機。
- 三台以上也可以，所有裝置連到同一台主機即可。

## 8. 鍵盤快速鍵

| 按鍵 | 動作 |
|-----|--------|
| `N`／`Enter` | 在同一欄新增任務 |
| `Tab`／`Shift+Enter` | 新增子任務（拆解） |
| `↑ ↓` | 在欄內移動 |
| `← →` | 切換欄位 |
| `Space` | 完成／取消完成 |
| `T`／`Shift+T` | 加入今天的票券／整欄 → 今天 |
| `P`／`Shift+P`／`Ctrl+P` | 列印（任務／欄位／今天全部） |
| `Ctrl+K` | 重新聚焦 |
| `F2` | 重新命名 |
| `Delete` | 刪除（可用 `Ctrl+Z` 復原） |
| `Alt+↑ ↓` | 調整順序 |
| `Ctrl+Z` | 復原 |
| `Ctrl+1〜5` | 切換分頁 |
| `Esc` | 取消 |

## 9. 觸控操作

| 操作 | 動作 |
|---|---|
| 將票券 **向右滑動** | 完成（滑過寬度的 40% 即確定） |
| **點一下** 任務 | 選取（手機上會進入子任務的欄位） |
| **長按** 任務 | 選單（新增子任務／今天／列印／重新命名／移動／刪除） |
| **點兩下** | 重新命名 |
| 左上角的 **←** | 回到上一層欄位 |

適用於 Chromebook 與 Google 筆電的觸控螢幕以及平板，可與滑鼠、鍵盤並用。

## 10. 資料與備份

- 儲存位置：Android 為 `filesDir/jarful-data.json`（App 專屬區域），Windows 為 `%APPDATA%\Jarful\jarful-data.json`。
- **備份**：設定 → 資料 → **匯出 JSON（複製）** 會複製到剪貼簿，貼到筆記等處保存。
- **還原**：貼到 **匯入 JSON**。既有資料會被取代（可用 `Ctrl+Z` 復原一次）。
- 不會自動傳送到裝置以外的地方；唯一的對象是你自己設定的同步主機。

## 11. 語言

日本語／English／Français／العربية／Русский／Español／Deutsch／Tiếng Việt／Polski／Українська／Bahasa Indonesia／繁體中文（台灣）。
預設會自動配合系統語言，也可在設定 → 語言切換。阿拉伯文會將整個介面改為由右至左的版面。
以點陣方式列印時，任何語言都能直接印出（每行自動選擇合適字型，阿拉伯文會靠右並正確連字）。

## 12. 疑難排解

| 症狀 | 處理方式 |
|---|---|
| 無法安裝 APK | 允許「不明來源應用程式」；確認是 Android 8.0 以上 |
| 藍牙裝置沒出現在清單 | Classic 請先在系統配對；LE 請按 🔄 重新掃描，並開啟位置資訊（Android 11 以下） |
| 測試列印逾時 | 確認印表機電源、距離、其他 App 是否佔用連線；LE 請關閉原廠 App |
| 印出亂碼（文字模式） | 把字元編碼設成與印表機內建字型一致，或改用 **ESC/POS 點陣** |
| 印得很淡 | 確認感熱紙正反面（光滑面朝向列印頭） |
| Windows 沒有 COM 埠 | 依 6.5 的步驟新增 **外送** 埠；重新配對 |
| 同步顯示「無法連線到主機」 | 確認是否同一個 Wi-Fi、主機端 App 是否開著、防火牆是否已允許 |
| 同步顯示「PIN 碼錯誤」 | 重新輸入主機設定畫面顯示的 PIN 碼 |
| 可以配對卻無法連線（原廠 App 也不行） | 有些 **LE 專用機在系統配對後反而無法連線**。到系統藍牙設定解除配對 → 重新開啟印表機電源 → 在 Jarful 選 **Bluetooth LE** → 🔄 → 測試列印。並完全關閉原廠 App。仍不行時，請把 **連線診斷** 的結果貼到 Issue |
| OPPO / Xiaomi / Huawei 無法連線 | 允許「鄰近裝置」與「位置資訊」權限、開啟位置資訊、把 Jarful 從電池最佳化排除。Classic 失敗時會自動改試 LE，並記住成功的方式 |
| 沒有產生例行事項票券 | 確認例行事項的星期設定與「啟用」開關；使用例行事項分頁的「重新產生今天的票券」 |

## 13. 從原始碼建置

```bash
# 單元測試（領域邏輯、列印編碼、同步、截圖）
./gradlew :shared:desktopTest :desktopApp:test
# Android APK（debug）
./gradlew :androidApp:assembleDebug
# Windows 安裝程式（需在 Windows 上執行）
./gradlew :desktopApp:packageMsi
# 直接啟動桌面版
./gradlew :desktopApp:run
```

需求：JDK 17、Android SDK（API 35）。詳見 [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)；
規格書在 [docs/SPEC.md](docs/SPEC.md)，參考文章摘要在 [docs/SOURCES.md](docs/SOURCES.md)（日文）。

## 14. 授權與免責聲明

MIT License。本專案是受公開文章啟發的 **非官方獨立實作**，
與 Laurie Hérault 本人、其應用程式 Colonnes 及 Nazology 編輯部沒有任何關係，也未獲其認可或合作。
不包含文章本文、圖片或其軟體。與任何印表機產品亦無合作或保證關係。
