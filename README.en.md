# Jarful — a game-loop task manager for ADHD brains

[![CI](https://github.com/kamome-run/jarful/actions/workflows/ci.yml/badge.svg)](https://github.com/kamome-run/jarful/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[日本語](README.md) · **English** · [Français](README.fr.md) · [العربية](README.ar.md) · [Русский](README.ru.md) · [Español](README.es.md) · [Deutsch](README.de.md) · [Tiếng Việt](README.vi.md) · [Polski](README.pl.md) · [Українська](README.uk.md) · [Bahasa Indonesia](README.id.md) · [繁體中文（台灣）](README.zh-TW.md)

"I can focus on a game for hours, but I put off work and chores." Jarful turns the
**sticky notes × transparent jar × thermal printer** method described by ADHD entrepreneur Laurie Hérault
([original article](https://www.laurieherault.com/articles/a-thermal-receipt-printer-cured-my-procrastination))
into an app for Android (including Chromebooks and Google's ChromeOS/Android laptops) and Windows 11.

> **Jarful** = "a jar full". Crumple every finished ticket and watch the jar fill up.

---

## Contents

1. [How it works](#1-how-it-works)
2. [What you need](#2-what-you-need)
3. [Supported platforms](#3-supported-platforms)
4. [Installation](#4-installation)
5. [First launch and the daily flow](#5-first-launch-and-the-daily-flow)
6. [Printer setup (detailed)](#6-printer-setup-detailed)
7. [Android ⇄ Windows sync (detailed)](#7-android--windows-sync-detailed)
8. [Keyboard shortcuts](#8-keyboard-shortcuts)
9. [Touch gestures](#9-touch-gestures)
10. [Data and backups](#10-data-and-backups)
11. [Languages](#11-languages)
12. [Troubleshooting](#12-troubleshooting)
13. [Building from source](#13-building-from-source)
14. [License and disclaimer](#14-license-and-disclaimer)

---

## 1. How it works

| The method | In Jarful |
|-----------|-----------|
| Break tasks into **2–5 minute micro-tasks** so the loop repeats often | Hierarchical tasks in **side-by-side columns**; `Tab` adds a subtask instantly |
| One sticky note = one task; when done, **crumple it into a transparent jar** | Completing a "today ticket" plays a **crumple animation + paper sound + vibration** and drops a paper ball into the jar |
| Start the day with easy habits; **prepare tomorrow the night before** | Weekday **routines** generate tomorrow's tickets automatically after the prepare time (21:00 by default) |
| When you notice procrastination, write **the next 3–5 tasks** and start | `Ctrl+K` **Refocus**: one task per line → instant tickets → the first one starts running |
| Tasks that cannot be split get **split by time** ("just 10 minutes") | Timeboxed tickets count down; at the end choose "complete / +5 min / break down" |
| A backlog (thousands of emails) becomes "**all new + N old, every day**" | **Quota routines** (+1 counter, complete at the target) |
| A **thermal printer** removes the friction | Prints over Bluetooth Classic / Bluetooth LE / COM / TCP using **ESC/POS, TSPL or CPCL**, one ticket per receipt |

## Screenshots

Windows 11 uses the **Fluent Design System** (WinUI 3); Android uses **Material 3**. Brand colors are shared.

| Windows 11 (Fluent) | Windows 11 dark | Windows 11, Traditional Chinese |
|---|---|---|
| ![windows](docs/screenshots/windows-fluent.png) | ![windows dark](docs/screenshots/windows-fluent-dark.png) | ![windows zh-TW](docs/screenshots/windows-fluent-zh-TW.png) |

| Android (Material 3) | Android dark | Android, Arabic (RTL) |
|---|---|---|
| ![phone](docs/screenshots/android-material-phone.png) | ![phone dark](docs/screenshots/android-material-phone-dark.png) | ![phone ar](docs/screenshots/android-material-phone-ar.png) |

| Printed ticket (Japanese) | Arabic | Traditional Chinese | Vietnamese |
|---|---|---|---|
| ![ticket](docs/screenshots/ticket-raster.png) | ![ticket ar](docs/screenshots/ticket-raster-ar.png) | ![ticket zh](docs/screenshots/ticket-raster-zh-TW.png) | ![ticket vi](docs/screenshots/ticket-raster-vi.png) |

## 2. What you need

**Required**
- An Android device (phone / tablet / Chromebook / Google ChromeOS or Android laptop) or a Windows 11 PC.

**Recommended (to reproduce the method fully)**
- **Thermal printer**: any 58 mm or 80 mm ESC/POS-compatible printer (Bluetooth Classic, Bluetooth LE, or wired LAN / Wi-Fi). For label stock, a TSPL- or CPCL-capable label printer.
- **Thermal paper rolls** in the matching width. You handle the tickets a lot, so **bisphenol-free (BPA/BPS-free)** paper is recommended.
- **A whiteboard and magnets**: printed tickets are **pinned to a whiteboard with magnets** so today's work sits right in front of you. Peeling a finished ticket off and crumpling it is the reward. Keep 20–30 small magnets (10–15 mm) handy.
- **A transparent jar** for the crumpled tickets. The in-app jar works on its own, but a real one makes the effect stronger.

## 3. Supported platforms

| Platform | Download | Notes |
|---|---|---|
| Android 8.0+ | `androidApp-debug.apk` / `androidApp-release-unsigned.apk` | Phones and tablets; Material 3, dynamic color on Android 12+ |
| Chromebook (ChromeOS) / Google ChromeOS or Android laptops | same | Installs without a touchscreen; keyboard, mouse and touch all supported |
| Windows 11 (x64) | `Jarful-*.msi` / `Jarful-*.exe` | Fluent Design UI; Bluetooth printers via a virtual COM port |

Downloads are on the [Releases](https://github.com/kamome-run/jarful/releases) page.

## 4. Installation

### 4.1 Android (phone / tablet)

1. Open [Releases](https://github.com/kamome-run/jarful/releases) in the device browser and download the latest `androidApp-debug.apk`.
2. Tap the APK from the notification or the Files app.
3. If Android warns about unknown apps, tap **Settings → Allow from this source** and go back (one-time permission for the browser / Files app).
4. Tap **Install**, then **Open**.
5. On first launch, choose **Add samples** when asked to "start with easy wins" to get a morning routine you can edit later.

> `release-unsigned.apk` is for developers who sign and distribute the app themselves. Use `debug.apk` normally.

### 4.2 Chromebook / Google ChromeOS or Android laptops

ChromeOS can install APKs that are not on Google Play in two ways.

**Option A: Linux development environment + adb (recommended)**
1. **Settings → Advanced → Developers → Linux development environment → Turn on** (the first time takes a few minutes).
2. In the same screen enable **Develop Android apps → ADB debugging**, then restart.
3. In the Linux terminal install adb and connect to the device:
   ```bash
   sudo apt update && sudo apt install -y adb
   adb connect 100.115.92.2:5555      # accept the on-screen prompt
   adb install ~/Downloads/androidApp-debug.apk
   ```
4. **Jarful** appears in the launcher. The window is resizable; at 840 dp or wider it switches to the three-pane layout.

**Option B: managed Play Store distribution** (school / work devices): an administrator can publish the APK as a private app.

Pair Bluetooth printers in **ChromeOS Settings → Bluetooth** first, then pick them in the app (Classic and LE both work).

### 4.3 Windows 11

1. Download `Jarful-<version>.msi` from [Releases](https://github.com/kamome-run/jarful/releases).
2. Double-click the installer. If the blue **SmartScreen** dialog appears, click **More info → Run anyway** (the installer is not code-signed; the source is public in this repository).
3. Confirm the destination and click **Install**. It installs per user; no administrator rights needed.
4. Launch **Jarful** from the Start menu.
5. Data lives in `%APPDATA%\Jarful\jarful-data.json` (shown under "Storage path" in Settings).

Uninstall via **Settings → Apps → Installed apps → Jarful**. The data file is kept; delete it manually if you want.

## 5. First launch and the daily flow

1. **Set up routines** (Routines tab): list easy morning habits (make coffee, open the window…) from top to bottom. Toggle weekdays per routine; for counted habits such as "process 10 emails", enter the number as a **quota**.
2. **Prepared the night before**: opening the app after the "prepare tomorrow at" time (21:00 by default) generates tomorrow's routine tickets. Opening it in the morning generates today's if they are missing.
3. **Break tasks down** (Columns tab): create a big task ("Clean the house") in the left column, select it and press `Tab` (or "Add subtask") to add "Kitchen", "Bathroom"… in the next column, then split further to **2–5 minute** pieces such as "Wash dishes". Tasks open for 3+ days show a "break it down further" hint.
4. **Make today's tickets**: select a task and press `T`; for a whole column press `Shift+T` (or use the column menu). They appear as receipt-style cards in the Today tab.
5. **Print and pin** (optional): `Ctrl+P` prints all of today's tickets; tear them off and **pin them to the whiteboard with magnets**.
6. **Do it → complete**: **Start** shows elapsed time (a countdown for timeboxed tickets). Press **Done** (or swipe the card right) and it crumples into the jar with sound and vibration. Peel off the paper ticket, crumple it, and drop it into the real jar.
7. **When you catch yourself procrastinating**: `Ctrl+K` (⚡ Refocus), write the next 3–5 tasks one per line and press **Start**. They become tickets immediately and the first one starts running.
8. **Stats**: loops per day (90 days), streak and routine completion.

## 6. Printer setup (detailed)

Settings tab → **Thermal printer**.

### 6.1 Choose the transport

| Transport | OS | Typical printers |
|---|---|---|
| **Bluetooth Classic (SPP)** | Android / Chromebook | Dual-mode printers, Bluetooth 2.1–5.x (usually ask for a PIN when pairing) |
| **Bluetooth LE (GATT)** | Android / Chromebook | LE-only pocket printers, Bluetooth 4.0–5.x (sold as "app-only" printers) |
| **Serial / COM** | Windows 11 | Bluetooth Classic printers through a virtual COM port; USB-serial adapters |
| **TCP/IP** | Android / Windows | Wired LAN / Wi-Fi receipt printers (port 9100) |

Not sure which Bluetooth you have? If the OS Bluetooth settings can **pair** the printer (PIN or confirmation prompt) it is Classic; if pairing fails and the manual says "connect from the app", it is probably LE. Try both and keep the one whose **Test print** works.

### 6.2 Choose the print protocol

| Protocol | Use |
|---|---|
| **ESC/POS raster (default)** | Most 58/80 mm receipt printers. The ticket is sent as an image, so **every language prints correctly regardless of the printer's built-in fonts** |
| ESC/POS bit image | Older printers without `GS v 0` raster support |
| ESC/POS text | Printing with the printer's built-in font. Set the charset (UTF-8 / Shift_JIS / Big5 / GB18030 / Windows-125x / CP8xx…) to match the printer |
| TSPL | Label printers (set label height and gap in mm) |
| CPCL | Label printers using CPCL |
| Cat printer | 57 mm pocket printers that do not speak ESC/POS (GB01 / GT01 / MX06 family, sold with the “iPrint” / “Fun Print” apps). Connects over BLE |
| Cat printer MXW01 | Newer generation of the above (MXW01 / X5h family; the diagnostics list AE01–AE04). BLE only |

Paper width is **58 mm (384 dots)** or **80 mm (576 dots)**. Output always uses the maximum density (no setting). Pocket printers without a cutter should keep **Cut off**; 3–5 feed lines is a good default.

### 6.3 Android / Chromebook — Bluetooth Classic

1. Turn the printer on; hold its Bluetooth button if it needs pairing mode.
2. **Device Settings → Bluetooth → Pair new device**, pick the printer and enter the PIN from its manual (`0000` or `1234` are common).
3. Jarful → Settings → Thermal printer → **Bluetooth Classic (SPP)**.
4. Pick the printer from the list of paired devices (🔄 refreshes). On Android 12+ allow the **Nearby devices** permission when asked.
5. Press **Test print**. "Jarful / Test print OK" should appear within a few seconds.
6. If printing stops halfway, increase the feed lines or print tickets one at a time (ticket ⋮ → Print).

### 6.4 Android / Chromebook — Bluetooth LE

1. Turn **Bluetooth on** (most LE printers need no pairing). On Android 11 and older also turn **Location on** (required for BLE scanning).
2. Jarful → Settings → Thermal printer → **Bluetooth LE (GATT)**.
3. Press 🔄 to scan for about 4 seconds; named printers appear in the list. Select yours.
4. **Test print**. The first connection can take 5–10 seconds.
5. If nothing prints: restart the printer, fully close the vendor app (an LE printer accepts one connection at a time), or toggle the device's Bluetooth.

### 6.5 Windows 11 — Bluetooth (virtual COM port)

1. **Settings → Bluetooth & devices → Add device → Bluetooth**, pair the printer (PIN from the manual).
2. **Settings → Bluetooth & devices → Devices**, scroll to the bottom and open **More devices and printer settings**.
3. Right-click the printer → **Properties → Services**, tick **Serial port (SPP)** and press **OK**.
4. In the same window open **More Bluetooth settings → COM Ports** and note the `COMx` listed as **Outgoing** for the printer. If none exists, **Add → Outgoing → select the printer → SPP**.
5. Jarful → Settings → Thermal printer → **Serial / COM** → pick `COMx` → **Test print**.
6. "PORT_OPEN_FAILED" means another program holds the port (vendor utility) or the printer is off; close it and power-cycle the printer.

> The Windows build cannot talk to LE-only printers. Print from an Android device or use a TCP-capable printer instead.

### 6.6 Network printers (TCP/IP)

1. Connect the printer to the LAN and print its **self-test page** (usually hold the feed button while powering on) to find its IP address.
2. Jarful → Settings → **TCP/IP** → enter the IP; port `9100` (default).
3. **Test print**. Reserving the IP in your router (DHCP reservation) keeps it stable.

### 6.7 Print-and-pin routine

- Morning: Today tab → **Print all of today** (`Ctrl+P`) → tear → **pin to the whiteboard with magnets**, top to bottom.
- During the day: after each ticket, peel it off, crumple it and drop it **into the transparent jar**. Pressing Done in the app adds a paper ball to the virtual jar too.
- Evening: tomorrow's tickets are prepared automatically; in the morning just print.

## 7. Android ⇄ Windows sync (detailed)

No cloud, no account. **Devices on the same Wi-Fi sync directly** (PIN-protected, default port 47831).

### 7.1 Host (a Windows PC is recommended)

1. Settings → **Device sync** → turn on **Make this device the host**.
2. Note **This device's addresses** (e.g. `192.168.1.20`) and the **6-digit PIN**.
3. If Windows Firewall asks whether Jarful may access the network, allow it on **private networks**.
4. While the app is open it accepts sync from other devices ("● listening").

### 7.2 Client (Android and others)

1. Settings → **Device sync** → under **Connect to** enter the **host IP address** and **PIN**.
2. Press **Sync now**. "Synced" confirms it; the 🔄 button in the top bar does the same.
3. Leave **Auto sync** on to sync at launch and every 5 minutes.

### 7.3 How it works and caveats

- Synced: **tasks, tickets, routines**. Device-specific settings (printer etc.) are not synced.
- If both devices change the same item, **the later change wins**. Deletions propagate too (a later edit revives the item).
- Traffic is plain HTTP inside the LAN. Use it on trusted home/office networks and switch the host off on public Wi-Fi.
- Three or more devices work as long as they all connect to the same host.

## 8. Keyboard shortcuts

| Key | Action |
|-----|--------|
| `N` / `Enter` | New task in this column |
| `Tab` / `Shift+Enter` | Add subtask (break down) |
| `↑ ↓` | Move within the column |
| `← →` | Move between columns |
| `Space` | Toggle done |
| `T` / `Shift+T` | Make today's ticket / whole column → today |
| `P` / `Shift+P` / `Ctrl+P` | Print task / column / all of today |
| `Ctrl+K` | Refocus |
| `F2` | Rename |
| `Delete` | Delete (`Ctrl+Z` undoes) |
| `Alt+↑ ↓` | Reorder |
| `Ctrl+Z` | Undo |
| `Ctrl+1–5` | Switch tab |
| `Esc` | Cancel |

## 9. Touch gestures

| Gesture | Action |
|---|---|
| **Swipe a ticket right** | Complete (past 40% of the width) |
| **Tap** a task | Select (on phones: open its subtasks) |
| **Long-press** a task | Menu (add subtask / today / print / rename / move / delete) |
| **Double-tap** | Rename |
| **←** at the top left | Back to the parent column |

Works on Chromebook and Google laptop touchscreens and on tablets, alongside mouse and keyboard.

## 10. Data and backups

- Location: Android `filesDir/jarful-data.json` (app-private); Windows `%APPDATA%\Jarful\jarful-data.json`.
- **Backup**: Settings → Data → **Export JSON (copy)** copies everything to the clipboard; paste it into a note.
- **Restore**: paste into **Import JSON**. Existing data is replaced (`Ctrl+Z` undoes once).
- Nothing is sent off the device automatically; the only peer is the sync host you configure.

## 11. Languages

日本語 / English / Français / العربية / Русский / Español / Deutsch / Tiếng Việt / Polski / Українська / Bahasa Indonesia / 繁體中文（台灣）.
The app follows the system language by default; change it in Settings → Language. Arabic switches the whole layout to right-to-left.
Raster printing works in every language (a suitable font is chosen per line; Arabic is shaped and right-aligned).

## 12. Troubleshooting

| Symptom | Fix |
|---|---|
| APK will not install | Allow unknown apps; check the device runs Android 8.0+ |
| Bluetooth device not listed | Pair in the OS first (Classic). For LE press 🔄 again and enable Location (Android 11 and older) |
| Test print times out | Check printer power, distance and other apps holding the connection; close the vendor app for LE |
| Garbled text (text mode) | Match the charset to the printer's font, or switch to **ESC/POS raster** |
| Faint print | The glossy side of the thermal paper must face the print head |
| No COM port on Windows | Add an **Outgoing** port as in 6.5; re-pair the printer |
| Sync says "cannot reach the host" | Same Wi-Fi? Host app open? Firewall allowed? |
| Sync says "wrong PIN" | Re-enter the PIN shown on the host's settings screen |
| Pairs but never connects (vendor app fails too) | Some **LE-only printers stop connecting once paired in the OS**. Unpair in the OS Bluetooth settings → power-cycle the printer → choose **Bluetooth LE** in Jarful → 🔄 → Test print. Fully close the vendor app. If it still fails, paste the **Diagnose connection** report into an issue |
| Cannot connect on OPPO / Xiaomi / Huawei | Grant “Nearby devices” and “Location”, turn Location on, exempt Jarful from battery optimisation. Classic failures fall back to LE automatically and the working transport is saved |
| Routine tickets missing | Check the weekdays and the Enabled switch; use "Regenerate today" |

## 13. Building from source

```bash
# unit tests (domain, print encoding, sync, screenshots)
./gradlew :shared:desktopTest :desktopApp:test
# Android APK (debug)
./gradlew :androidApp:assembleDebug
# Windows installer (run on Windows)
./gradlew :desktopApp:packageMsi
# run the desktop app
./gradlew :desktopApp:run
```

Requirements: JDK 17 and the Android SDK (API 35). See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md);
the specification is [docs/SPEC.md](docs/SPEC.md) and the article notes are [docs/SOURCES.md](docs/SOURCES.md) (Japanese).

## 14. License and disclaimer

MIT License. Jarful is an **independent, unofficial implementation** inspired by a publicly available article.
It is not affiliated with or endorsed by Laurie Hérault, his app Colonnes, or the Nazology editorial team, and contains none of
their text, images or software. There is no affiliation with or warranty for any printer product.
