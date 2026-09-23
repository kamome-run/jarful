# Jarful — Aufgabenverwaltung nach dem Spielschleifen-Prinzip für ADHS-Gehirne

[![CI](https://github.com/kamome-run/jarful/actions/workflows/ci.yml/badge.svg)](https://github.com/kamome-run/jarful/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[日本語](README.md) · [English](README.en.md) · [Français](README.fr.md) · [العربية](README.ar.md) · [Русский](README.ru.md) · [Español](README.es.md) · **Deutsch** · [Tiếng Việt](README.vi.md) · [Polski](README.pl.md) · [Українська](README.uk.md) · [Bahasa Indonesia](README.id.md) · [繁體中文（台灣）](README.zh-TW.md)

„Ein Spiel fesselt mich stundenlang, aber Arbeit und Haushalt schiebe ich auf.“ Jarful macht aus der Methode
**Haftnotizen × durchsichtiges Glas × Thermodrucker** des ADHS-Unternehmers Laurie Hérault
([Originalartikel](https://www.laurieherault.com/articles/a-thermal-receipt-printer-cured-my-procrastination))
eine App für Android (einschließlich Chromebooks und Googles ChromeOS/Android-Laptops) und Windows 11.

> **Jarful** = „ein volles Glas“. Zerknülle jedes erledigte Ticket und sieh zu, wie sich das Glas füllt.

---

## Inhalt

1. [So funktioniert es](#1-so-funktioniert-es)
2. [Was du brauchst](#2-was-du-brauchst)
3. [Unterstützte Plattformen](#3-unterstützte-plattformen)
4. [Installation](#4-installation)
5. [Erster Start und Tagesablauf](#5-erster-start-und-tagesablauf)
6. [Drucker einrichten (ausführlich)](#6-drucker-einrichten-ausführlich)
7. [Android ⇄ Windows synchronisieren (ausführlich)](#7-android--windows-synchronisieren-ausführlich)
8. [Tastenkürzel](#8-tastenkürzel)
9. [Touch-Gesten](#9-touch-gesten)
10. [Daten und Sicherung](#10-daten-und-sicherung)
11. [Sprachen](#11-sprachen)
12. [Fehlerbehebung](#12-fehlerbehebung)
13. [Aus dem Quellcode bauen](#13-aus-dem-quellcode-bauen)
14. [Lizenz und Hinweis](#14-lizenz-und-hinweis)

---

## 1. So funktioniert es

| Die Methode | In Jarful |
|-----------|-----------|
| Aufgaben in **2–5-Minuten-Mikroaufgaben** zerlegen, damit sich die Schleife oft wiederholt | Hierarchische Aufgaben in **nebeneinanderliegenden Spalten**; `Tab` fügt sofort eine Teilaufgabe hinzu |
| Eine Haftnotiz = eine Aufgabe; erledigt wird sie **zerknüllt ins durchsichtige Glas geworfen** | Ein „Heute-Ticket“ abzuschließen spielt eine **Zerknüll-Animation + Papiergeräusch + Vibration** und lässt eine Papierkugel ins Glas fallen |
| Den Tag mit leichten Gewohnheiten beginnen; **morgen schon am Vorabend vorbereiten** | **Routinen** je Wochentag erzeugen die Tickets für morgen nach der Vorbereitungszeit (Standard 21:00) |
| Beim Aufschieben **die nächsten 3–5 Aufgaben** aufschreiben und loslegen | `Strg+K` **Neu fokussieren**: eine Aufgabe pro Zeile → sofort Tickets im Tab Heute |
| Unteilbare Aufgaben werden **nach Zeit zerlegt** („nur 10 Minuten“) | Tickets mit Zeitbox zählen herunter; am Ende: „abschließen / +5 Min / zerlegen“ |
| Ein Rückstau (tausende E-Mails) wird zu „**alles Neue + N alte, jeden Tag**“ | **Kontingent-Routinen** (+1-Zähler, abgeschlossen beim Ziel) |
| Ein **Thermodrucker** nimmt die Reibung | Druck über Bluetooth Classic / Bluetooth LE / COM / TCP mit **ESC/POS, TSPL oder CPCL**, ein Ticket pro Bon |

## Screenshots

Windows 11 verwendet das **Fluent Design System** (WinUI 3), Android **Material 3**. Die Markenfarben sind gemeinsam.

| Windows 11 (Fluent) | Windows 11 dunkel | Windows 11, traditionelles Chinesisch |
|---|---|---|
| ![windows](docs/screenshots/windows-fluent.png) | ![windows dark](docs/screenshots/windows-fluent-dark.png) | ![windows zh-TW](docs/screenshots/windows-fluent-zh-TW.png) |

| Android (Material 3) | Android dunkel | Android, Arabisch (RTL) |
|---|---|---|
| ![phone](docs/screenshots/android-material-phone.png) | ![phone dark](docs/screenshots/android-material-phone-dark.png) | ![phone ar](docs/screenshots/android-material-phone-ar.png) |

| Gedrucktes Ticket (Japanisch) | Arabisch | Traditionelles Chinesisch | Vietnamesisch |
|---|---|---|---|
| ![ticket](docs/screenshots/ticket-raster.png) | ![ticket ar](docs/screenshots/ticket-raster-ar.png) | ![ticket zh](docs/screenshots/ticket-raster-zh-TW.png) | ![ticket vi](docs/screenshots/ticket-raster-vi.png) |

## 2. Was du brauchst

**Erforderlich**
- Ein Android-Gerät (Smartphone / Tablet / Chromebook / ChromeOS- oder Android-Laptop von Google) oder ein Windows-11-PC.

**Empfohlen (für die vollständige Methode)**
- **Thermodrucker**: ein beliebiger ESC/POS-kompatibler Drucker mit 58 mm oder 80 mm (Bluetooth Classic, Bluetooth LE oder LAN / WLAN). Für Etiketten ein TSPL- oder CPCL-fähiger Etikettendrucker.
- **Thermopapierrollen** in passender Breite. Die Tickets werden viel angefasst, daher ist **bisphenolfreies (BPA/BPS-freies)** Papier empfehlenswert.
- **Ein Whiteboard und Magnete**: Gedruckte Tickets werden **mit Magneten ans Whiteboard geheftet**, damit die Tagesaufgaben direkt vor dir hängen. Ein erledigtes Ticket abzunehmen und zu zerknüllen ist die Belohnung. Halte 20–30 kleine Magnete (10–15 mm) bereit.
- **Ein durchsichtiges Glas** für die zerknüllten Tickets. Das Glas in der App genügt, ein echtes verstärkt den Effekt.

## 3. Unterstützte Plattformen

| Plattform | Download | Hinweise |
|---|---|---|
| Android 8.0 oder neuer | `androidApp-debug.apk` / `androidApp-release-unsigned.apk` | Smartphones und Tablets; Material 3, dynamische Farben ab Android 12 |
| Chromebook (ChromeOS) / ChromeOS- oder Android-Laptops von Google | dito | Installierbar ohne Touchscreen; Tastatur, Maus und Touch werden unterstützt |
| Windows 11 (x64) | `Jarful-*.msi` / `Jarful-*.exe` | Fluent-Design-Oberfläche; Bluetooth-Drucker über virtuellen COM-Port |

Downloads gibt es auf der Seite [Releases](https://github.com/kamome-run/jarful/releases).

## 4. Installation

### 4.1 Android (Smartphone / Tablet)

1. Öffne [Releases](https://github.com/kamome-run/jarful/releases) im Browser des Geräts und lade die neueste `androidApp-debug.apk` herunter.
2. Tippe die APK in der Benachrichtigung oder in der Dateien-App an.
3. Warnt Android vor unbekannten Apps, tippe auf **Einstellungen → Aus dieser Quelle zulassen** und gehe zurück (einmalig für Browser / Dateien-App).
4. Tippe auf **Installieren**, dann auf **Öffnen**.
5. Beim ersten Start wählst du **Beispiele hinzufügen**, um eine Morgenroutine zu bekommen, die du später anpassen kannst.

> `release-unsigned.apk` richtet sich an Entwickler, die die App selbst signieren und verteilen. Normalerweise nimmst du `debug.apk`.

### 4.2 Chromebook / ChromeOS- oder Android-Laptops von Google

ChromeOS installiert APKs außerhalb von Google Play auf zwei Wegen.

**Variante A: Linux-Entwicklungsumgebung + adb (empfohlen)**
1. **Einstellungen → Erweitert → Entwickler → Linux-Entwicklungsumgebung → Aktivieren** (beim ersten Mal dauert es einige Minuten).
2. Auf derselben Seite **Android-Apps entwickeln → ADB-Debugging** einschalten und neu starten.
3. Im Linux-Terminal adb installieren und mit dem Gerät verbinden:
   ```bash
   sudo apt update && sudo apt install -y adb
   adb connect 100.115.92.2:5555      # die Abfrage auf dem Bildschirm bestätigen
   adb install ~/Downloads/androidApp-debug.apk
   ```
4. **Jarful** erscheint im Launcher. Das Fenster lässt sich skalieren; ab 840 dp Breite wechselt es in die Drei-Spalten-Ansicht.

**Variante B: verwalteter Play Store** (Schul-/Firmengeräte): Ein Administrator kann die APK als private App verteilen.

Bluetooth-Drucker zuerst in **ChromeOS-Einstellungen → Bluetooth** koppeln, dann in der App auswählen (Classic und LE funktionieren).

### 4.3 Windows 11

1. Lade `Jarful-<Version>.msi` von [Releases](https://github.com/kamome-run/jarful/releases) herunter.
2. Doppelklicke auf den Installer. Erscheint der blaue **SmartScreen**-Dialog, klicke auf **Weitere Informationen → Trotzdem ausführen** (der Installer ist nicht signiert; der Quellcode ist in diesem Repository öffentlich).
3. Zielordner bestätigen und **Install** klicken. Die Installation erfolgt pro Benutzer, ohne Administratorrechte.
4. Starte **Jarful** über das Startmenü.
5. Die Daten liegen in `%APPDATA%\Jarful\jarful-data.json` (in den Einstellungen unter „Speicherort“).

Deinstallation über **Einstellungen → Apps → Installierte Apps → Jarful**. Die Datendatei bleibt erhalten; bei Bedarf manuell löschen.

## 5. Erster Start und Tagesablauf

1. **Routinen anlegen** (Tab Routinen): Liste leichte Morgengewohnheiten von oben nach unten auf (Kaffee kochen, Fenster öffnen …). Wochentage je Routine ein- oder ausschalten; für gezählte Gewohnheiten wie „10 E-Mails abarbeiten“ die Zahl als **Kontingent** eintragen. Routinen sind unter **Kategorie-Überschriften** gruppiert; ziehe am Griff ≡, um zu sortieren (Ziehen unter eine andere Überschrift ändert die Kategorie), **dupliziere** eine Routine mit Anzahl oder **wähle mehrere aus** und schalte sie gemeinsam ein oder aus.
2. **Am Vorabend vorbereitet**: Öffnest du die App nach der Zeit „Morgen vorbereiten um“ (Standard 21:00), entstehen die Routine-Tickets für morgen. Morgens werden die heutigen erzeugt, falls sie fehlen. Um **abends die Tickets für morgen früh zu drucken**, wähle im Tab Routinen „Für ein Datum drucken…“ und dann morgen.
3. **Aufgaben zerlegen** (Tab Spalten): Lege in der linken Spalte eine große Aufgabe an („Wohnung putzen“), wähle sie und drücke `Tab` (oder „Teilaufgabe hinzufügen“), um in der nächsten Spalte „Küche“, „Bad“ … anzulegen, und zerlege weiter bis zu **2–5-Minuten**-Stücken („Geschirr spülen“). Aufgaben, die länger als 3 Tage offen sind, zeigen den Hinweis „weiter zerlegen“. Die Hierarchie reicht bis zur **Urenkel-Ebene** (vier Spalten). Ziehe am Griff ≡, um Zeilen zu sortieren, und nutze **Duplizieren** im Menü, um eine Aufgabe samt Unteraufgaben beliebig oft zu kopieren. Lange Titel werden umgebrochen.
4. **Heute-Tickets anlegen**: Aufgabe wählen und `T` drücken; für eine ganze Spalte `Umschalt+T` (oder das Spaltenmenü). Sie erscheinen als Bon-Karten im Tab Heute.
5. **Drucken und anheften** (optional): `Strg+P` druckt alle heutigen Tickets; abreißen und **mit Magneten ans Whiteboard heften**.
6. **Machen → abschließen**: **Erledigt** drücken (oder die Karte nach rechts wischen): Sie zerknüllt mit Geräusch und Vibration ins Glas. Das Papierticket abnehmen, zerknüllen und ins echte Glas werfen.
7. **Wenn du dich beim Aufschieben ertappst**: `Strg+K` (⚡ Neu fokussieren), die nächsten 3–5 Aufgaben je Zeile aufschreiben und **Loslegen** drücken. Sie werden sofort Tickets im Tab Heute.
8. **Statistik**: Runden pro Tag (90 Tage), Serie und Routinen-Erfüllung.

## 6. Drucker einrichten (ausführlich)

Tab Einstellungen → **Thermodrucker**.

### 6.1 Verbindungsart wählen

| Verbindung | Systeme | Typische Drucker |
|---|---|---|
| **Bluetooth Classic (SPP)** | Android / Chromebook | Dual-Mode-Drucker, Bluetooth 2.1–5.x (verlangen beim Koppeln meist eine PIN) |
| **Bluetooth LE (GATT)** | Android / Chromebook | Reine LE-Taschendrucker, Bluetooth 4.0–5.x (als „App-Drucker“ verkauft) |
| **Serial / COM** | Windows 11 | Bluetooth-Classic-Drucker über virtuellen COM-Port; USB-Seriell-Adapter |
| **TCP/IP** | Android / Windows | Bondrucker mit LAN / WLAN (Port 9100) |

Unsicher, welches Bluetooth du hast? Lässt sich der Drucker in den Bluetooth-Einstellungen des Systems **koppeln** (PIN oder Bestätigung), ist es Classic; schlägt das Koppeln fehl und die Anleitung sagt „über die App verbinden“, ist es vermutlich LE. Probiere beides und behalte die Variante, deren **Testdruck** klappt.

### 6.2 Druckprotokoll wählen

| Protokoll | Verwendung |
|---|---|
| **ESC/POS Raster (Standard)** | Die meisten 58/80-mm-Bondrucker. Das Ticket wird als Bild gesendet, sodass **jede Sprache unabhängig von den Druckerschriften korrekt gedruckt wird** |
| ESC/POS Bitbild | Ältere Geräte ohne `GS v 0`-Raster |
| ESC/POS Text | Druck mit der eingebauten Druckerschrift. Zeichensatz (UTF-8 / Shift_JIS / Big5 / GB18030 / Windows-125x / CP8xx …) passend zum Drucker wählen |
| TSPL | Etikettendrucker (Etikettenhöhe und Abstand in mm) |
| CPCL | Etikettendrucker mit CPCL |
| Cat printer | 57-mm-Taschendrucker ohne ESC/POS (Familie GB01 / GT01 / MX06, mit den Apps „iPrint“ / „Fun Print“ verkauft). Verbindung über BLE |
| Cat printer MXW01 | Neuere Generation (Familie MXW01 / X5h; die Diagnose listet AE01–AE04). Nur BLE |

Papierbreite **58 mm (384 Punkte)** oder **80 mm (576 Punkte)**. Gedruckt wird immer mit maximaler Dichte (keine Einstellung). Taschendrucker ohne Schneidwerk: **Cut aus** lassen; 3–5 Vorschubzeilen sind ein guter Wert.

### 6.3 Android / Chromebook — Bluetooth Classic

1. Drucker einschalten; falls nötig die Bluetooth-Taste für den Kopplungsmodus gedrückt halten.
2. **Geräteeinstellungen → Bluetooth → Neues Gerät koppeln**, Drucker wählen und die PIN aus der Anleitung eingeben (`0000` oder `1234` sind üblich).
3. Jarful → Einstellungen → Thermodrucker → **Bluetooth Classic (SPP)**.
4. Drucker aus der Liste der gekoppelten Geräte wählen (🔄 aktualisiert). Ab Android 12 die Berechtigung **Geräte in der Nähe** erteilen.
5. **Testdruck** drücken: „Jarful / Test print OK“ sollte in wenigen Sekunden erscheinen.
6. Bricht der Druck mittendrin ab, erhöhe die Vorschubzeilen oder drucke Tickets einzeln (⋮ → Drucken).

### 6.4 Android / Chromebook — Bluetooth LE

1. **Bluetooth einschalten** (die meisten LE-Drucker brauchen keine Kopplung). Bis Android 11 zusätzlich den **Standort** einschalten (für BLE-Scans nötig).
2. Jarful → Einstellungen → Thermodrucker → **Bluetooth LE (GATT)**.
3. 🔄 drücken: Etwa 4 Sekunden Scan, benannte Drucker erscheinen in der Liste. Deinen auswählen.
4. **Testdruck**. Die erste Verbindung kann 5–10 Sekunden dauern.
5. Kommt nichts: Drucker neu starten, die Hersteller-App vollständig beenden (ein LE-Drucker erlaubt nur eine Verbindung) oder Bluetooth aus- und einschalten.

### 6.5 Windows 11 — Bluetooth (virtueller COM-Port)

1. **Einstellungen → Bluetooth und Geräte → Gerät hinzufügen → Bluetooth**, Drucker koppeln (PIN aus der Anleitung).
2. **Einstellungen → Bluetooth und Geräte → Geräte**, ganz nach unten scrollen und **Weitere Geräte- und Druckereinstellungen** öffnen.
3. Rechtsklick auf den Drucker → **Eigenschaften → Dienste**, **Serieller Anschluss (SPP)** ankreuzen, **OK**.
4. Im selben Fenster **Weitere Bluetooth-Optionen → COM-Anschlüsse** öffnen und den als **Ausgehend** gelisteten `COMx` des Druckers notieren. Fehlt er: **Hinzufügen → Ausgehend → Drucker wählen → SPP**.
5. Jarful → Einstellungen → Thermodrucker → **Serial / COM** → `COMx` wählen → **Testdruck**.
6. „PORT_OPEN_FAILED“ bedeutet, dass ein anderes Programm den Port belegt (Hersteller-Tool) oder der Drucker aus ist: Programm schließen, Drucker neu starten.

> Die Windows-Version kann keine reinen LE-Drucker ansprechen. Drucke von einem Android-Gerät oder nutze einen TCP-fähigen Drucker.

### 6.6 Netzwerkdrucker (TCP/IP)

1. Drucker ins LAN hängen und die **Selbsttestseite** drucken (meist Vorschubtaste beim Einschalten halten), um die IP-Adresse zu erfahren.
2. Jarful → Einstellungen → **TCP/IP** → IP eintragen; Port `9100` (Standard).
3. **Testdruck**. Eine feste IP im Router (DHCP-Reservierung) hält die Verbindung stabil.

### 6.7 Ablauf „drucken und anheften“

- Morgens: Tab Heute → **Ganzen Tag drucken** (`Strg+P`) → abreißen → **mit Magneten von oben nach unten ans Whiteboard heften**.
- Tagsüber: Nach jedem Ticket abnehmen, zerknüllen und **ins durchsichtige Glas** werfen. „Erledigt“ in der App legt zusätzlich eine Kugel ins virtuelle Glas.
- Abends: Die Tickets für morgen bereiten sich automatisch vor; morgens nur noch drucken.

## 7. Android ⇄ Windows synchronisieren (ausführlich)

Keine Cloud, kein Konto. **Geräte im selben WLAN synchronisieren sich direkt** (PIN-geschützt, Standardport 47831).

### 7.1 Host (Windows-PC empfohlen)

1. Einstellungen → **Geräte-Sync** → **Dieses Gerät als Host verwenden** einschalten.
2. **Adressen dieses Geräts** (z. B. `192.168.1.20`) und die **6-stellige PIN** notieren.
3. Fragt die Windows-Firewall, ob Jarful auf das Netzwerk zugreifen darf, in **privaten Netzwerken** zulassen.
4. Solange die App geöffnet ist, nimmt sie Synchronisierungen anderer Geräte an („● wartet auf Verbindungen“).

### 7.2 Client (Android und andere)

1. Einstellungen → **Geräte-Sync** → unter **Verbinden mit** die **IP-Adresse des Hosts** und die **PIN** eingeben.
2. **Jetzt synchronisieren** drücken. „Synchronisiert“ bestätigt es; der 🔄-Knopf in der oberen Leiste tut dasselbe.
3. **Automatisch synchronisieren** eingeschaltet lassen: beim Start und alle 5 Minuten.

### 7.3 Funktionsweise und Hinweise

- Synchronisiert werden **Aufgaben, Tickets, Routinen**. Gerätespezifische Einstellungen (Drucker usw.) nicht.
- Ändern beide Geräte dasselbe Element, **gewinnt die spätere Änderung**. Löschungen werden ebenfalls übertragen (eine spätere Bearbeitung stellt das Element wieder her).
- Der Datenverkehr ist unverschlüsseltes HTTP im LAN. Nur in vertrauenswürdigen Netzen nutzen und den Host in öffentlichem WLAN ausschalten.
- Drei oder mehr Geräte funktionieren, solange sich alle mit demselben Host verbinden.

## 8. Tastenkürzel

| Taste | Aktion |
|-----|--------|
| `N` / `Enter` | Neue Aufgabe in dieser Spalte |
| `Tab` / `Umschalt+Enter` | Teilaufgabe hinzufügen (zerlegen) |
| `↑ ↓` | In der Spalte bewegen |
| `← →` | Spalte wechseln |
| `Leertaste` | Erledigt / offen |
| `T` / `Umschalt+T` | Heute-Ticket / ganze Spalte → heute |
| `P` / `Umschalt+P` / `Strg+P` | Aufgabe / Spalte / ganzen Tag drucken |
| `Strg+K` | Neu fokussieren |
| `F2` | Umbenennen |
| `Entf` | Löschen (`Strg+Z` macht es rückgängig) |
| `Alt+↑ ↓` | Umsortieren |
| `Strg+Z` | Rückgängig |
| `Strg+1–5` | Tab wechseln |
| `Esc` | Abbrechen |

## 9. Touch-Gesten

| Geste | Aktion |
|---|---|
| **Ticket nach rechts wischen** | Abschließen (mehr als 40 % der Breite) |
| Aufgabe **antippen** | Auswählen (am Smartphone: Teilaufgaben öffnen) |
| Aufgabe **lange drücken** | Menü (Teilaufgabe / heute / drucken / umbenennen / verschieben / löschen) |
| **Doppeltippen** | Umbenennen |
| **←** oben links | Zurück zur übergeordneten Spalte |

Funktioniert auf den Touchscreens von Chromebooks und Google-Laptops sowie auf Tablets, parallel zu Maus und Tastatur.

## 10. Daten und Sicherung

- Speicherort: Android `filesDir/jarful-data.json` (app-privat); Windows `%APPDATA%\Jarful\jarful-data.json`.
- **Sicherung**: Einstellungen → Daten → **JSON exportieren (kopieren)** kopiert alles in die Zwischenablage; in eine Notiz einfügen.
- **Wiederherstellen**: in **JSON importieren** einfügen. Vorhandene Daten werden ersetzt (`Strg+Z` macht es einmal rückgängig).
- Nichts verlässt das Gerät automatisch; die einzige Gegenstelle ist der von dir eingerichtete Sync-Host.

## 11. Sprachen

日本語 / English / Français / العربية / Русский / Español / Deutsch / Tiếng Việt / Polski / Українська / Bahasa Indonesia / 繁體中文（台灣）.
Die App folgt standardmäßig der Systemsprache; ändern unter Einstellungen → Sprache. Arabisch schaltet die gesamte Oberfläche auf Rechts-nach-links.
Rasterdruck funktioniert in jeder Sprache (pro Zeile wird eine passende Schrift gewählt; Arabisch wird verbunden und rechtsbündig gesetzt).

## 12. Fehlerbehebung

| Symptom | Lösung |
|---|---|
| APK lässt sich nicht installieren | Unbekannte Apps zulassen; Android 8.0+ prüfen |
| Bluetooth-Gerät nicht in der Liste | Zuerst im System koppeln (Classic). Bei LE 🔄 erneut drücken und Standort aktivieren (bis Android 11) |
| Testdruck läuft in ein Timeout | Stromversorgung, Abstand und andere verbundene Apps prüfen; bei LE die Hersteller-App schließen |
| Zeichensalat (Textmodus) | Zeichensatz an die Druckerschrift anpassen oder auf **ESC/POS Raster** wechseln |
| Blasser Druck | Die glänzende Seite des Thermopapiers muss zum Druckkopf zeigen |
| Kein COM-Port unter Windows | **Ausgehenden** Port wie in 6.5 anlegen; neu koppeln |
| Sync: „Host nicht erreichbar“ | Gleiches WLAN? Host-App offen? Firewall erlaubt? |
| Sync: „Falsche PIN“ | Die auf dem Host angezeigte PIN erneut eingeben |
| Koppelt, verbindet aber nie (Hersteller-App scheitert ebenfalls) | Manche **reinen LE-Drucker verbinden sich nach dem Koppeln im System nicht mehr**. In den Bluetooth-Einstellungen entkoppeln → Drucker neu starten → in Jarful **Bluetooth LE** wählen → 🔄 → Testdruck. Hersteller-App vollständig beenden. Hilft das nicht, den Bericht aus **Verbindung diagnostizieren** in ein Issue einfügen |
| Keine Verbindung auf OPPO / Xiaomi / Huawei | „Geräte in der Nähe“ und „Standort“ erlauben, Standort einschalten, Jarful von der Akku-Optimierung ausnehmen. Scheitert Classic, wird automatisch LE versucht und die funktionierende Art gespeichert |
| Routine-Tickets fehlen | Wochentage und Schalter „Aktiv“ prüfen; „Heute neu erzeugen“ verwenden |

## 13. Aus dem Quellcode bauen

```bash
# Unit-Tests (Domäne, Druckkodierung, Sync, Screenshots)
./gradlew :shared:desktopTest :desktopApp:test
# Android-APK (Debug)
./gradlew :androidApp:assembleDebug
# Windows-Installer (unter Windows ausführen)
./gradlew :desktopApp:packageMsi
# Desktop-App starten
./gradlew :desktopApp:run
```

Voraussetzungen: JDK 17 und Android SDK (API 35). Siehe [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md);
die Spezifikation steht in [docs/SPEC.md](docs/SPEC.md), die Artikelnotizen in [docs/SOURCES.md](docs/SOURCES.md) (japanisch).

## 14. Lizenz und Hinweis

MIT-Lizenz. Jarful ist eine **unabhängige, inoffizielle Implementierung**, inspiriert von einem öffentlich zugänglichen Artikel.
Es besteht keine Verbindung zu Laurie Hérault, seiner App Colonnes oder der Nazology-Redaktion und keine Billigung durch sie; Texte,
Bilder oder Software von ihnen sind nicht enthalten. Es besteht keine Verbindung zu und keine Gewähr für Druckerprodukte.
