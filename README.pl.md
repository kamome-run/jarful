# Jarful — menedżer zadań oparty na pętli gry dla mózgów z ADHD

[![CI](https://github.com/kamome-run/jarful/actions/workflows/ci.yml/badge.svg)](https://github.com/kamome-run/jarful/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[日本語](README.md) · [English](README.en.md) · [Français](README.fr.md) · [العربية](README.ar.md) · [Русский](README.ru.md) · [Español](README.es.md) · [Deutsch](README.de.md) · [Tiếng Việt](README.vi.md) · **Polski** · [Українська](README.uk.md) · [Bahasa Indonesia](README.id.md) · [繁體中文（台灣）](README.zh-TW.md)

„W grę potrafię wsiąknąć na godziny, a pracę i obowiązki domowe odkładam.” Jarful zamienia metodę
**karteczki samoprzylepne × przezroczysty słoik × drukarka termiczna**, opisaną przez przedsiębiorcę z ADHD Laurie Héraulta
([oryginalny artykuł](https://www.laurieherault.com/articles/a-thermal-receipt-printer-cured-my-procrastination)),
w aplikację na Androida (w tym Chromebooki i laptopy Google z ChromeOS/Androidem) oraz Windows 11.

> **Jarful** = „pełny słoik”. Zgniataj każdy wykonany bilet i patrz, jak słoik się zapełnia.

---

## Spis treści

1. [Jak to działa](#1-jak-to-działa)
2. [Co będzie potrzebne](#2-co-będzie-potrzebne)
3. [Obsługiwane platformy](#3-obsługiwane-platformy)
4. [Instalacja](#4-instalacja)
5. [Pierwsze uruchomienie i rytm dnia](#5-pierwsze-uruchomienie-i-rytm-dnia)
6. [Konfiguracja drukarki (szczegółowo)](#6-konfiguracja-drukarki-szczegółowo)
7. [Synchronizacja Android ⇄ Windows (szczegółowo)](#7-synchronizacja-android--windows-szczegółowo)
8. [Skróty klawiszowe](#8-skróty-klawiszowe)
9. [Gesty dotykowe](#9-gesty-dotykowe)
10. [Dane i kopie zapasowe](#10-dane-i-kopie-zapasowe)
11. [Języki](#11-języki)
12. [Rozwiązywanie problemów](#12-rozwiązywanie-problemów)
13. [Budowanie ze źródeł](#13-budowanie-ze-źródeł)
14. [Licencja i zastrzeżenia](#14-licencja-i-zastrzeżenia)

---

## 1. Jak to działa

| Metoda | W Jarful |
|-----------|-----------|
| Dzielić zadania na **mikrozadania po 2–5 minut**, aby pętla powtarzała się często | Hierarchia zadań w **kolumnach obok siebie**; `Tab` natychmiast dodaje podzadanie |
| Jedna karteczka = jedno zadanie; po wykonaniu **zgnieć ją i wrzuć do przezroczystego słoika** | Zakończenie „biletu na dziś” odtwarza **animację zgniatania + szelest papieru + wibrację**, a do słoika wpada papierowa kulka |
| Zaczynać dzień od łatwych nawyków; **przygotować jutro wieczorem** | **Rutyny** według dni tygodnia tworzą jutrzejsze bilety po godzinie przygotowania (domyślnie 21:00) |
| Gdy zauważysz zwlekanie, zapisz **kolejne 3–5 zadań** i zacznij | `Ctrl+K` **Skup się**: jedno zadanie w wierszu → natychmiast bilety → pierwszy rusza |
| Zadania niepodzielne **dzieli się czasem** („tylko 10 minut”) | Bilety z blokiem czasu odliczają; na koniec: „zakończ / +5 min / podziel” |
| Zaległości (tysiące e-maili) stają się „**wszystko nowe + N starych, codziennie**” | **Rutyny z limitem** (licznik +1, zakończenie po osiągnięciu celu) |
| **Drukarka termiczna** usuwa tarcie | Druk przez Bluetooth Classic / Bluetooth LE / COM / TCP w **ESC/POS, TSPL lub CPCL**, jeden bilet na paragon |

## Zrzuty ekranu

Windows 11 używa **Fluent Design System** (WinUI 3), Android — **Material 3**. Kolory marki są wspólne.

| Windows 11 (Fluent) | Windows 11, tryb ciemny | Windows 11, chiński tradycyjny |
|---|---|---|
| ![windows](docs/screenshots/windows-fluent.png) | ![windows dark](docs/screenshots/windows-fluent-dark.png) | ![windows zh-TW](docs/screenshots/windows-fluent-zh-TW.png) |

| Android (Material 3) | Android, tryb ciemny | Android, arabski (RTL) |
|---|---|---|
| ![phone](docs/screenshots/android-material-phone.png) | ![phone dark](docs/screenshots/android-material-phone-dark.png) | ![phone ar](docs/screenshots/android-material-phone-ar.png) |

| Wydrukowany bilet (japoński) | Arabski | Chiński tradycyjny | Wietnamski |
|---|---|---|---|
| ![ticket](docs/screenshots/ticket-raster.png) | ![ticket ar](docs/screenshots/ticket-raster-ar.png) | ![ticket zh](docs/screenshots/ticket-raster-zh-TW.png) | ![ticket vi](docs/screenshots/ticket-raster-vi.png) |

## 2. Co będzie potrzebne

**Wymagane**
- Urządzenie z Androidem (telefon / tablet / Chromebook / laptop Google z ChromeOS lub Androidem) albo komputer z Windows 11.

**Zalecane (aby odtworzyć całą metodę)**
- **Drukarka termiczna**: dowolna drukarka zgodna z ESC/POS o szerokości 58 mm lub 80 mm (Bluetooth Classic, Bluetooth LE albo sieć przewodowa / Wi-Fi). Do etykiet — drukarka obsługująca TSPL lub CPCL.
- **Rolki papieru termicznego** w odpowiedniej szerokości. Bilety często bierze się do ręki, dlatego zalecany jest papier **bez bisfenolu (bez BPA/BPS)**.
- **Tablica suchościeralna i magnesy**: wydrukowane bilety **przypina się magnesami do tablicy**, aby dzisiejsze zadania były przed oczami. Zdjęcie wykonanego biletu i zgniecenie go to nagroda. Miej pod ręką 20–30 małych magnesów (10–15 mm).
- **Przezroczysty słoik** na zgniecione bilety. Słoik w aplikacji wystarczy, ale prawdziwy wzmacnia efekt.

## 3. Obsługiwane platformy

| Platforma | Plik | Uwagi |
|---|---|---|
| Android 8.0 lub nowszy | `androidApp-debug.apk` / `androidApp-release-unsigned.apk` | Telefony i tablety; Material 3, kolory dynamiczne od Androida 12 |
| Chromebook (ChromeOS) / laptopy Google z ChromeOS lub Androidem | te same | Instaluje się bez ekranu dotykowego; obsługa klawiatury, myszy i dotyku |
| Windows 11 (x64) | `Jarful-*.msi` / `Jarful-*.exe` | Interfejs Fluent Design; drukarki Bluetooth przez wirtualny port COM |

Pliki znajdują się na stronie [Releases](https://github.com/kamome-run/jarful/releases).

## 4. Instalacja

### 4.1 Android (telefon / tablet)

1. Otwórz [Releases](https://github.com/kamome-run/jarful/releases) w przeglądarce urządzenia i pobierz najnowszy `androidApp-debug.apk`.
2. Dotknij pliku APK w powiadomieniu lub w aplikacji Pliki.
3. Jeśli Android ostrzeże przed nieznaną aplikacją, dotknij **Ustawienia → Zezwól z tego źródła** i wróć (jednorazowe zezwolenie dla przeglądarki / Plików).
4. Dotknij **Zainstaluj**, a potem **Otwórz**.
5. Przy pierwszym uruchomieniu wybierz **Dodaj przykłady**, aby otrzymać poranną rutynę, którą później można edytować.

> `release-unsigned.apk` jest dla deweloperów, którzy sami podpisują i dystrybuują aplikację. Normalnie używaj `debug.apk`.

### 4.2 Chromebook / laptopy Google z ChromeOS lub Androidem

ChromeOS pozwala zainstalować APK spoza Google Play na dwa sposoby.

**Sposób A: środowisko programistyczne Linux + adb (zalecany)**
1. **Ustawienia → Zaawansowane → Programiści → Środowisko programistyczne Linux → Włącz** (za pierwszym razem trwa to kilka minut).
2. Na tym samym ekranie włącz **Tworzenie aplikacji na Androida → Debugowanie ADB** i uruchom ponownie.
3. W terminalu Linux zainstaluj adb i połącz się z urządzeniem:
   ```bash
   sudo apt update && sudo apt install -y adb
   adb connect 100.115.92.2:5555      # zatwierdź komunikat na ekranie
   adb install ~/Downloads/androidApp-debug.apk
   ```
4. **Jarful** pojawi się w programie uruchamiającym. Okno można skalować; od szerokości 840 dp przełącza się w układ trzech paneli.

**Sposób B: zarządzany Sklep Play** (urządzenia szkolne / firmowe): administrator może opublikować APK jako aplikację prywatną.

Drukarkę Bluetooth najpierw sparuj w **Ustawienia ChromeOS → Bluetooth**, a potem wybierz ją w aplikacji (działają Classic i LE).

### 4.3 Windows 11

1. Pobierz `Jarful-<wersja>.msi` ze strony [Releases](https://github.com/kamome-run/jarful/releases).
2. Kliknij dwukrotnie instalator. Jeśli pojawi się niebieskie okno **SmartScreen**, kliknij **Więcej informacji → Uruchom mimo to** (instalator nie jest podpisany; kod źródłowy jest publiczny w tym repozytorium).
3. Potwierdź folder i kliknij **Install**. Instalacja jest dla bieżącego użytkownika, bez uprawnień administratora.
4. Uruchom **Jarful** z menu Start.
5. Dane są w `%APPDATA%\Jarful\jarful-data.json` (pozycja „Lokalizacja” w ustawieniach).

Odinstalowanie: **Ustawienia → Aplikacje → Zainstalowane aplikacje → Jarful**. Plik danych pozostaje; w razie potrzeby usuń go ręcznie.

## 5. Pierwsze uruchomienie i rytm dnia

1. **Ustaw rutyny** (karta Rutyny): wypisz od góry do dołu łatwe poranne nawyki (zaparzyć kawę, otworzyć okno…). Włącz dni tygodnia dla każdej rutyny; przy nawykach liczonych, jak „przetworzyć 10 e-maili”, wpisz liczbę jako **limit**.
2. **Przygotowanie wieczorem**: otwarcie aplikacji po godzinie „Przygotuj jutro o” (domyślnie 21:00) tworzy jutrzejsze bilety rutyn. Rano powstają dzisiejsze, jeśli ich brakuje.
3. **Dziel zadania** (karta Kolumny): utwórz duże zadanie („Sprzątanie domu”) w lewej kolumnie, zaznacz je i naciśnij `Tab` (albo „Dodaj podzadanie”), aby w następnej kolumnie dodać „Kuchnia”, „Łazienka”…, i dziel dalej do kawałków po **2–5 minut** („Zmyć naczynia”). Zadania otwarte ponad 3 dni pokazują podpowiedź „podziel dalej”.
4. **Utwórz dzisiejsze bilety**: zaznacz zadanie i naciśnij `T`; dla całej kolumny `Shift+T` (lub menu kolumny). Pojawią się jako karty w stylu paragonu na karcie Dziś.
5. **Wydrukuj i przypnij** (opcjonalnie): `Ctrl+P` drukuje wszystkie dzisiejsze bilety; oderwij je i **przypnij magnesami do tablicy**.
6. **Zrób → zakończ**: **Start** pokazuje upływający czas (przy bloku czasu — odliczanie). Naciśnij **Gotowe** (lub przesuń kartę w prawo): zgniata się i wpada do słoika z dźwiękiem i wibracją. Zdejmij papierowy bilet, zgnieć i wrzuć do prawdziwego słoika.
7. **Gdy przyłapiesz się na zwlekaniu**: `Ctrl+K` (⚡ Skup się), zapisz kolejne 3–5 zadań po jednym w wierszu i naciśnij **Zacznij**. Od razu stają się biletami, a pierwszy rusza.
8. **Statystyki**: pętle dziennie (90 dni), seria i realizacja rutyn.

## 6. Konfiguracja drukarki (szczegółowo)

Karta Ustawienia → **Drukarka termiczna**.

### 6.1 Wybierz sposób połączenia

| Połączenie | Systemy | Typowe drukarki |
|---|---|---|
| **Bluetooth Classic (SPP)** | Android / Chromebook | Drukarki dwutrybowe, Bluetooth 2.1–5.x (przy parowaniu zwykle proszą o PIN) |
| **Bluetooth LE (GATT)** | Android / Chromebook | Kieszonkowe drukarki tylko LE, Bluetooth 4.0–5.x (sprzedawane jako „drukarka do aplikacji”) |
| **Serial / COM** | Windows 11 | Drukarki Bluetooth Classic przez wirtualny port COM; przejściówki USB-serial |
| **TCP/IP** | Android / Windows | Drukarki paragonowe po kablu / Wi-Fi (port 9100) |

Nie wiesz, który Bluetooth masz? Jeśli w ustawieniach Bluetooth systemu da się drukarkę **sparować** (PIN lub potwierdzenie), to Classic; jeśli parowanie się nie udaje, a instrukcja mówi „połącz się z aplikacji”, to prawdopodobnie LE. Wypróbuj oba i zostaw ten, w którym działa **wydruk testowy**.

### 6.2 Wybierz protokół druku

| Protokół | Zastosowanie |
|---|---|
| **ESC/POS raster (domyślny)** | Większość drukarek paragonowych 58/80 mm. Bilet jest wysyłany jako obraz, więc **każdy język drukuje się poprawnie niezależnie od wbudowanych czcionek** |
| ESC/POS bit image | Starsze modele bez rastra `GS v 0` |
| ESC/POS tekst | Druk wbudowaną czcionką drukarki. Dopasuj zestaw znaków (UTF-8 / Shift_JIS / Big5 / GB18030 / Windows-125x / CP8xx…) do drukarki |
| TSPL | Drukarki etykiet (wysokość etykiety i odstęp w mm) |
| CPCL | Drukarki etykiet z CPCL |
| Cat printer | Drukarki kieszonkowe 57 mm bez ESC/POS (rodzina GB01 / GT01 / MX06, sprzedawane z aplikacjami „iPrint” / „Fun Print”). Łączą się przez BLE |

Szerokość papieru to **58 mm (384 punkty)** lub **80 mm (576 punktów)**. W drukarkach kieszonkowych bez obcinacza zostaw **Cut wyłączone**; 3–5 wierszy wysuwu to dobra wartość.

### 6.3 Android / Chromebook — Bluetooth Classic

1. Włącz drukarkę; jeśli trzeba, przytrzymaj przycisk Bluetooth, aby wejść w tryb parowania.
2. **Ustawienia urządzenia → Bluetooth → Sparuj nowe urządzenie**, wybierz drukarkę i wpisz PIN z instrukcji (często `0000` lub `1234`).
3. Jarful → Ustawienia → Drukarka termiczna → **Bluetooth Classic (SPP)**.
4. Wybierz drukarkę z listy sparowanych urządzeń (🔄 odświeża). Na Androidzie 12+ zezwól na dostęp do **urządzeń w pobliżu**.
5. Naciśnij **Wydruk testowy**: w kilka sekund powinno wyjść „Jarful / Test print OK”.
6. Jeśli druk urywa się w połowie, zwiększ liczbę wierszy wysuwu lub drukuj bilety pojedynczo (⋮ → Drukuj).

### 6.4 Android / Chromebook — Bluetooth LE

1. Włącz **Bluetooth** (większość drukarek LE nie wymaga parowania). Na Androidzie 11 i starszych włącz też **lokalizację** (potrzebną do skanowania BLE).
2. Jarful → Ustawienia → Drukarka termiczna → **Bluetooth LE (GATT)**.
3. Naciśnij 🔄: przez około 4 sekundy trwa skanowanie, a znalezione drukarki pojawiają się na liście. Wybierz swoją.
4. **Wydruk testowy**. Pierwsze połączenie może potrwać 5–10 sekund.
5. Nic się nie drukuje: uruchom drukarkę ponownie, całkowicie zamknij aplikację producenta (drukarka LE przyjmuje jedno połączenie) albo wyłącz i włącz Bluetooth.

### 6.5 Windows 11 — Bluetooth (wirtualny port COM)

1. **Ustawienia → Bluetooth i urządzenia → Dodaj urządzenie → Bluetooth**, sparuj drukarkę (PIN z instrukcji).
2. **Ustawienia → Bluetooth i urządzenia → Urządzenia**, przewiń na dół i otwórz **Więcej ustawień urządzeń i drukarek**.
3. Kliknij drukarkę prawym przyciskiem → **Właściwości → Usługi**, zaznacz **Port szeregowy (SPP)** i naciśnij **OK**.
4. W tym samym oknie otwórz **Więcej ustawień Bluetooth → Porty COM** i zapisz `COMx` wymieniony dla drukarki jako **wychodzący**. Jeśli go nie ma: **Dodaj → Wychodzący → wybierz drukarkę → SPP**.
5. Jarful → Ustawienia → Drukarka termiczna → **Serial / COM** → wybierz `COMx` → **Wydruk testowy**.
6. „PORT_OPEN_FAILED” oznacza, że port zajmuje inny program (narzędzie producenta) albo drukarka jest wyłączona: zamknij go i zrestartuj drukarkę.

> Wersja na Windows nie łączy się z drukarkami tylko LE. Drukuj z urządzenia z Androidem albo użyj drukarki z TCP.

### 6.6 Drukarki sieciowe (TCP/IP)

1. Podłącz drukarkę do sieci i wydrukuj **stronę autotestu** (zwykle przytrzymując przycisk wysuwu przy włączaniu), aby odczytać adres IP.
2. Jarful → Ustawienia → **TCP/IP** → wpisz IP; port `9100` (domyślny).
3. **Wydruk testowy**. Zarezerwuj IP w routerze (rezerwacja DHCP), aby się nie zmieniał.

### 6.7 Rytm „wydrukuj i przypnij”

- Rano: karta Dziś → **Drukuj cały dzień** (`Ctrl+P`) → oderwij → **przypnij magnesami do tablicy** od góry do dołu.
- W ciągu dnia: po każdym bilecie zdejmij go, zgnieć i wrzuć **do przezroczystego słoika**. Naciśnięcie „Gotowe” w aplikacji dodaje kulkę także do wirtualnego słoika.
- Wieczorem: jutrzejsze bilety przygotują się same; rano wystarczy wydrukować.

## 7. Synchronizacja Android ⇄ Windows (szczegółowo)

Bez chmury i bez konta. **Urządzenia w tej samej sieci Wi-Fi synchronizują się bezpośrednio** (ochrona PIN-em, domyślny port 47831).

### 7.1 Host (zalecany komputer z Windows)

1. Ustawienia → **Synchronizacja urządzeń** → włącz **To urządzenie jako host**.
2. Zapisz **adresy tego urządzenia** (np. `192.168.1.20`) i **6-cyfrowy PIN**.
3. Jeśli Zapora Windows zapyta, czy Jarful może korzystać z sieci, zezwól w **sieciach prywatnych**.
4. Gdy aplikacja jest otwarta, przyjmuje synchronizację z innych urządzeń („● nasłuchuje”).

### 7.2 Klient (Android i inne)

1. Ustawienia → **Synchronizacja urządzeń** → w polu **Połącz z** wpisz **adres IP hosta** i **PIN**.
2. Naciśnij **Synchronizuj teraz**. „Zsynchronizowano” potwierdza sukces; przycisk 🔄 na górnym pasku robi to samo.
3. Zostaw włączoną **automatyczną synchronizację** — przy starcie i co 5 minut.

### 7.3 Jak to działa i uwagi

- Synchronizowane są **zadania, bilety, rutyny**. Ustawienia urządzenia (drukarka itp.) — nie.
- Jeśli oba urządzenia zmienią ten sam element, **wygrywa późniejsza zmiana**. Usunięcia też się propagują (późniejsza edycja przywraca element).
- Ruch to nieszyfrowany HTTP w sieci lokalnej. Używaj w zaufanych sieciach i wyłączaj hosta w publicznym Wi-Fi.
- Trzy lub więcej urządzeń działa, o ile wszystkie łączą się z tym samym hostem.

## 8. Skróty klawiszowe

| Klawisz | Działanie |
|-----|--------|
| `N` / `Enter` | Nowe zadanie w tej kolumnie |
| `Tab` / `Shift+Enter` | Dodaj podzadanie (podziel) |
| `↑ ↓` | Poruszanie w kolumnie |
| `← →` | Zmiana kolumny |
| `Spacja` | Gotowe / niegotowe |
| `T` / `Shift+T` | Dzisiejszy bilet / cała kolumna → dziś |
| `P` / `Shift+P` / `Ctrl+P` | Drukuj zadanie / kolumnę / cały dzień |
| `Ctrl+K` | Skup się |
| `F2` | Zmień nazwę |
| `Delete` | Usuń (`Ctrl+Z` cofa) |
| `Alt+↑ ↓` | Zmień kolejność |
| `Ctrl+Z` | Cofnij |
| `Ctrl+1–5` | Przełącz kartę |
| `Esc` | Anuluj |

## 9. Gesty dotykowe

| Gest | Działanie |
|---|---|
| **Przesuń bilet w prawo** | Zakończ (ponad 40 % szerokości) |
| **Dotknij** zadania | Zaznacz (na telefonie: otwórz podzadania) |
| **Przytrzymaj** zadanie | Menu (podzadanie / dziś / drukuj / zmień nazwę / przenieś / usuń) |
| **Dwukrotne dotknięcie** | Zmień nazwę |
| **←** w lewym górnym rogu | Wróć do kolumny nadrzędnej |

Działa na ekranach dotykowych Chromebooków i laptopów Google oraz na tabletach — równolegle z myszą i klawiaturą.

## 10. Dane i kopie zapasowe

- Lokalizacja: Android `filesDir/jarful-data.json` (prywatny katalog aplikacji); Windows `%APPDATA%\Jarful\jarful-data.json`.
- **Kopia zapasowa**: Ustawienia → Dane → **Eksportuj JSON (kopiuj)** kopiuje wszystko do schowka; wklej do notatki.
- **Przywracanie**: wklej w **Importuj JSON**. Obecne dane zostają zastąpione (`Ctrl+Z` cofa raz).
- Nic nie jest wysyłane z urządzenia automatycznie; jedynym rozmówcą jest skonfigurowany przez Ciebie host synchronizacji.

## 11. Języki

日本語 / English / Français / العربية / Русский / Español / Deutsch / Tiếng Việt / Polski / Українська / Bahasa Indonesia / 繁體中文（台灣）.
Domyślnie aplikacja podąża za językiem systemu; zmień go w Ustawienia → Język. Arabski przełącza cały interfejs na układ od prawej do lewej.
Druk rastrowy działa w każdym języku (dla każdego wiersza dobierana jest odpowiednia czcionka; arabski jest łączony i wyrównany do prawej).

## 12. Rozwiązywanie problemów

| Objaw | Rozwiązanie |
|---|---|
| APK nie chce się zainstalować | Zezwól na nieznane aplikacje; sprawdź, czy to Android 8.0+ |
| Urządzenia Bluetooth nie ma na liście | Najpierw sparuj w systemie (Classic). Przy LE naciśnij 🔄 ponownie i włącz lokalizację (Android 11 i starsze) |
| Wydruk testowy przekracza limit czasu | Sprawdź zasilanie, odległość i inne połączone aplikacje; przy LE zamknij aplikację producenta |
| Krzaczki (tryb tekstowy) | Dopasuj zestaw znaków do czcionki drukarki albo przełącz na **ESC/POS raster** |
| Blady wydruk | Błyszcząca strona papieru termicznego musi być zwrócona do głowicy |
| Brak portu COM w Windows | Dodaj port **wychodzący** jak w 6.5; sparuj ponownie |
| Synchronizacja: „brak połączenia z hostem” | Ta sama sieć Wi-Fi? Aplikacja hosta otwarta? Zapora zezwoliła? |
| Synchronizacja: „błędny PIN” | Wpisz ponownie PIN wyświetlany na ekranie ustawień hosta |
| Paruje się, ale nigdy nie łączy (aplikacja producenta też nie) | Niektóre **drukarki tylko LE przestają się łączyć po sparowaniu w systemie**. Rozparuj w ustawieniach Bluetooth → zrestartuj drukarkę → wybierz w Jarful **Bluetooth LE** → 🔄 → Wydruk testowy. Całkowicie zamknij aplikację producenta. Jeśli nadal nie działa, wklej raport **Diagnozuj połączenie** do zgłoszenia |
| Brak połączenia na OPPO / Xiaomi / Huawei | Zezwól na „Urządzenia w pobliżu” i „Lokalizację”, włącz lokalizację, wyłącz optymalizację baterii dla Jarful. Gdy Classic zawiedzie, automatycznie próbowane jest LE, a działający tryb jest zapisywany |
| Brak biletów rutyn | Sprawdź dni tygodnia i przełącznik „Włączona”; użyj „Wygeneruj dziś ponownie” |

## 13. Budowanie ze źródeł

```bash
# testy jednostkowe (domena, kodowanie druku, synchronizacja, zrzuty ekranu)
./gradlew :shared:desktopTest :desktopApp:test
# APK na Androida (debug)
./gradlew :androidApp:assembleDebug
# instalator Windows (uruchamiać w Windows)
./gradlew :desktopApp:packageMsi
# uruchomienie aplikacji desktopowej
./gradlew :desktopApp:run
```

Wymagania: JDK 17 i Android SDK (API 35). Zobacz [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md);
specyfikacja jest w [docs/SPEC.md](docs/SPEC.md), a notatki z artykułów w [docs/SOURCES.md](docs/SOURCES.md) (po japońsku).

## 14. Licencja i zastrzeżenia

Licencja MIT. Jarful to **niezależna, nieoficjalna implementacja** zainspirowana publicznie dostępnym artykułem.
Nie jest powiązany z Laurie Héraultem, jego aplikacją Colonnes ani redakcją Nazology i nie jest przez nich firmowany; nie zawiera ich tekstów,
obrazów ani oprogramowania. Brak powiązań z jakimkolwiek producentem drukarek i gwarancji na jego produkty.
