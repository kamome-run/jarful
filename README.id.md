# Jarful — pengelola tugas berbasis putaran permainan untuk otak ADHD

[![CI](https://github.com/kamome-run/jarful/actions/workflows/ci.yml/badge.svg)](https://github.com/kamome-run/jarful/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[日本語](README.md) · [English](README.en.md) · [Français](README.fr.md) · [العربية](README.ar.md) · [Русский](README.ru.md) · [Español](README.es.md) · [Deutsch](README.de.md) · [Tiếng Việt](README.vi.md) · [Polski](README.pl.md) · [Українська](README.uk.md) · **Bahasa Indonesia** · [繁體中文（台灣）](README.zh-TW.md)

"Main game bisa fokus berjam-jam, tapi kerjaan dan urusan rumah selalu ditunda." Jarful mengubah metode
**sticky note × stoples bening × printer termal** dari pengusaha dengan ADHD Laurie Hérault
([artikel asli](https://www.laurieherault.com/articles/a-thermal-receipt-printer-cured-my-procrastination))
menjadi aplikasi untuk Android (termasuk Chromebook dan laptop ChromeOS/Android dari Google) dan Windows 11.

> **Jarful** = "stoples penuh". Remas setiap tiket yang selesai dan lihat stoplesnya terisi.

---

## Daftar isi

1. [Cara kerja](#1-cara-kerja)
2. [Yang perlu disiapkan](#2-yang-perlu-disiapkan)
3. [Platform yang didukung](#3-platform-yang-didukung)
4. [Instalasi](#4-instalasi)
5. [Pertama kali dibuka dan alur harian](#5-pertama-kali-dibuka-dan-alur-harian)
6. [Pengaturan printer (rinci)](#6-pengaturan-printer-rinci)
7. [Sinkronisasi Android ⇄ Windows (rinci)](#7-sinkronisasi-android--windows-rinci)
8. [Pintasan keyboard](#8-pintasan-keyboard)
9. [Gestur sentuh](#9-gestur-sentuh)
10. [Data dan cadangan](#10-data-dan-cadangan)
11. [Bahasa](#11-bahasa)
12. [Pemecahan masalah](#12-pemecahan-masalah)
13. [Membangun dari kode sumber](#13-membangun-dari-kode-sumber)
14. [Lisensi dan sangkalan](#14-lisensi-dan-sangkalan)

---

## 1. Cara kerja

| Metodenya | Di Jarful |
|-----------|-----------|
| Pecah tugas menjadi **tugas mikro 2–5 menit** supaya putaran sering berulang | Tugas berjenjang dalam **kolom berdampingan**; `Tab` langsung menambah subtugas |
| Satu sticky note = satu tugas; setelah selesai **diremas dan dimasukkan ke stoples bening** | Menyelesaikan "tiket hari ini" memutar **animasi meremas + suara kertas + getaran** dan bola kertas jatuh ke stoples |
| Mulai hari dengan kebiasaan mudah; **siapkan hari esok pada malam sebelumnya** | **Rutinitas** per hari membuat tiket esok otomatis setelah jam persiapan (bawaan 21:00) |
| Saat sadar sedang menunda, tulis **3–5 tugas berikutnya** lalu mulai | `Ctrl+K` **Fokus ulang**: satu tugas per baris → langsung jadi tiket → yang pertama mulai berjalan |
| Tugas yang tak bisa dipecah **dipecah berdasarkan waktu** ("10 menit saja") | Tiket dengan batas waktu menghitung mundur; di akhir pilih "selesaikan / +5 menit / pecah" |
| Tumpukan pekerjaan (ribuan email) jadi "**semua yang baru + N yang lama, setiap hari**" | **Rutinitas kuota** (penghitung +1, selesai saat target tercapai) |
| **Printer termal** menghilangkan hambatan | Mencetak lewat Bluetooth Classic / Bluetooth LE / COM / TCP dengan **ESC/POS, TSPL, atau CPCL**, satu tiket per struk |

## Tangkapan layar

Windows 11 memakai **Fluent Design System** (WinUI 3); Android memakai **Material 3**. Warna merek sama.

| Windows 11 (Fluent) | Windows 11 gelap | Windows 11, Mandarin Tradisional |
|---|---|---|
| ![windows](docs/screenshots/windows-fluent.png) | ![windows dark](docs/screenshots/windows-fluent-dark.png) | ![windows zh-TW](docs/screenshots/windows-fluent-zh-TW.png) |

| Android (Material 3) | Android gelap | Android, Arab (RTL) |
|---|---|---|
| ![phone](docs/screenshots/android-material-phone.png) | ![phone dark](docs/screenshots/android-material-phone-dark.png) | ![phone ar](docs/screenshots/android-material-phone-ar.png) |

| Tiket tercetak (Jepang) | Arab | Mandarin Tradisional | Vietnam |
|---|---|---|---|
| ![ticket](docs/screenshots/ticket-raster.png) | ![ticket ar](docs/screenshots/ticket-raster-ar.png) | ![ticket zh](docs/screenshots/ticket-raster-zh-TW.png) | ![ticket vi](docs/screenshots/ticket-raster-vi.png) |

## 2. Yang perlu disiapkan

**Wajib**
- Perangkat Android (ponsel / tablet / Chromebook / laptop ChromeOS atau Android dari Google) atau PC Windows 11.

**Disarankan (untuk menjalankan metode secara utuh)**
- **Printer termal**: printer apa pun yang kompatibel ESC/POS ukuran 58 mm atau 80 mm (Bluetooth Classic, Bluetooth LE, atau LAN kabel / Wi-Fi). Untuk kertas label, printer yang mendukung TSPL atau CPCL.
- **Gulungan kertas termal** dengan lebar yang sesuai. Tiket sering dipegang, jadi pilih kertas **bebas bisfenol (bebas BPA/BPS)**.
- **Papan tulis putih dan magnet**: tiket yang dicetak **ditempel di papan tulis putih dengan magnet** agar pekerjaan hari ini ada di depan mata. Mencopot tiket yang selesai lalu meremasnya adalah hadiahnya. Siapkan 20–30 magnet kecil (10–15 mm).
- **Stoples bening** untuk tiket yang diremas. Stoples di aplikasi sudah cukup, tetapi stoples sungguhan membuat efeknya lebih kuat.

## 3. Platform yang didukung

| Platform | Unduhan | Catatan |
|---|---|---|
| Android 8.0 ke atas | `androidApp-debug.apk` / `androidApp-release-unsigned.apk` | Ponsel dan tablet; Material 3, warna dinamis di Android 12+ |
| Chromebook (ChromeOS) / laptop ChromeOS atau Android dari Google | sama | Bisa dipasang tanpa layar sentuh; keyboard, mouse, dan sentuh didukung |
| Windows 11 (x64) | `Jarful-*.msi` / `Jarful-*.exe` | Antarmuka Fluent Design; printer Bluetooth lewat port COM virtual |

Unduhan tersedia di halaman [Releases](https://github.com/kamome-run/jarful/releases).

## 4. Instalasi

### 4.1 Android (ponsel / tablet)

1. Buka [Releases](https://github.com/kamome-run/jarful/releases) di browser perangkat dan unduh `androidApp-debug.apk` terbaru.
2. Ketuk APK dari notifikasi atau aplikasi File.
3. Jika Android memperingatkan aplikasi tidak dikenal, ketuk **Setelan → Izinkan dari sumber ini**, lalu kembali (izin sekali untuk browser / aplikasi File).
4. Ketuk **Instal**, lalu **Buka**.
5. Saat pertama dibuka, pilih **Tambahkan contoh** untuk mendapatkan rutinitas pagi yang bisa diubah nanti.

> `release-unsigned.apk` untuk pengembang yang menandatangani dan mendistribusikan sendiri. Biasanya gunakan `debug.apk`.

### 4.2 Chromebook / laptop ChromeOS atau Android dari Google

ChromeOS bisa memasang APK di luar Google Play dengan dua cara.

**Cara A: lingkungan pengembangan Linux + adb (disarankan)**
1. **Setelan → Lanjutan → Developer → Lingkungan pengembangan Linux → Aktifkan** (pertama kali butuh beberapa menit).
2. Di layar yang sama, aktifkan **Kembangkan aplikasi Android → Debugging ADB**, lalu mulai ulang.
3. Di terminal Linux, pasang adb dan hubungkan ke perangkat:
   ```bash
   sudo apt update && sudo apt install -y adb
   adb connect 100.115.92.2:5555      # setujui permintaan yang muncul di layar
   adb install ~/Downloads/androidApp-debug.apk
   ```
4. **Jarful** muncul di peluncur. Jendela bisa diubah ukurannya; pada lebar 840 dp atau lebih tampilan beralih ke tiga panel.

**Cara B: distribusi Play Store terkelola** (perangkat sekolah / kantor): admin dapat menerbitkan APK sebagai aplikasi privat.

Sambungkan printer Bluetooth dulu di **Setelan ChromeOS → Bluetooth**, lalu pilih di aplikasi (Classic dan LE sama-sama bisa).

### 4.3 Windows 11

1. Unduh `Jarful-<versi>.msi` dari [Releases](https://github.com/kamome-run/jarful/releases).
2. Klik dua kali penginstal. Jika muncul layar biru **SmartScreen**, klik **More info → Run anyway** (penginstal belum ditandatangani; kode sumbernya terbuka di repositori ini).
3. Konfirmasi folder tujuan dan klik **Install**. Terpasang per pengguna, tanpa hak administrator.
4. Jalankan **Jarful** dari menu Start.
5. Data disimpan di `%APPDATA%\Jarful\jarful-data.json` (tampil sebagai "Lokasi penyimpanan" di Pengaturan).

Untuk mencopot: **Settings → Apps → Installed apps → Jarful**. Berkas data tetap ada; hapus manual bila perlu.

## 5. Pertama kali dibuka dan alur harian

1. **Atur rutinitas** (tab Rutinitas): tulis kebiasaan pagi yang mudah dari atas ke bawah (membuat kopi, membuka jendela…). Aktifkan hari per rutinitas; untuk kebiasaan berjumlah seperti "proses 10 email", isi angkanya sebagai **kuota**.
2. **Disiapkan malam sebelumnya**: membuka aplikasi setelah jam "Siapkan hari esok pukul" (bawaan 21:00) membuat tiket rutinitas untuk besok. Pagi hari, tiket hari ini dibuat jika belum ada.
3. **Pecah tugas** (tab Kolom): buat tugas besar ("Bersihkan rumah") di kolom kiri, pilih lalu tekan `Tab` (atau "Tambah subtugas") untuk menambah "Dapur", "Kamar mandi"… di kolom berikutnya, lalu pecah lagi sampai **2–5 menit** ("Cuci piring"). Tugas yang terbuka lebih dari 3 hari menampilkan saran "pecah lebih kecil".
4. **Buat tiket hari ini**: pilih tugas lalu tekan `T`; untuk seluruh kolom tekan `Shift+T` (atau menu kolom). Tiket muncul sebagai kartu mirip struk di tab Hari ini.
5. **Cetak dan tempel** (opsional): `Ctrl+P` mencetak semua tiket hari ini; sobek lalu **tempel di papan tulis putih dengan magnet**.
6. **Kerjakan → selesaikan**: **Mulai** menampilkan waktu berjalan (hitung mundur jika ada batas waktu). Tekan **Selesai** (atau geser kartu ke kanan): kartu meremas dan jatuh ke stoples dengan suara dan getaran. Copot tiket kertasnya, remas, dan masukkan ke stoples sungguhan.
7. **Saat tersadar sedang menunda**: `Ctrl+K` (⚡ Fokus ulang), tulis 3–5 tugas berikutnya satu per baris lalu tekan **Mulai**. Langsung jadi tiket dan yang pertama mulai berjalan.
8. **Statistik**: putaran per hari (90 hari), rekor beruntun, dan penyelesaian rutinitas.

## 6. Pengaturan printer (rinci)

Tab Pengaturan → **Printer termal**.

### 6.1 Pilih jenis koneksi

| Koneksi | OS | Printer yang umum |
|---|---|---|
| **Bluetooth Classic (SPP)** | Android / Chromebook | Printer dual-mode, Bluetooth 2.1–5.x (biasanya minta PIN saat penyandingan) |
| **Bluetooth LE (GATT)** | Android / Chromebook | Printer saku khusus LE, Bluetooth 4.0–5.x (dijual sebagai "printer khusus aplikasi") |
| **Serial / COM** | Windows 11 | Printer Bluetooth Classic lewat port COM virtual; adaptor USB-serial |
| **TCP/IP** | Android / Windows | Printer struk LAN kabel / Wi-Fi (port 9100) |

Tidak yakin Bluetooth yang mana? Jika pengaturan Bluetooth OS bisa **menyandingkan** printer (minta PIN atau konfirmasi), itu Classic; jika penyandingan gagal dan buku panduan berkata "hubungkan dari aplikasi", kemungkinan LE. Coba keduanya dan pakai yang **cetak uji**-nya berhasil.

### 6.2 Pilih protokol cetak

| Protokol | Kegunaan |
|---|---|
| **ESC/POS raster (bawaan)** | Sebagian besar printer struk 58/80 mm. Tiket dikirim sebagai gambar, jadi **semua bahasa tercetak benar tanpa bergantung pada font bawaan printer** |
| ESC/POS bit image | Printer lama tanpa dukungan raster `GS v 0` |
| ESC/POS teks | Mencetak dengan font bawaan printer. Sesuaikan set karakter (UTF-8 / Shift_JIS / Big5 / GB18030 / Windows-125x / CP8xx…) dengan printer |
| TSPL | Printer label (tinggi label dan jarak dalam mm) |
| CPCL | Printer label yang memakai CPCL |
| Cat printer | Printer saku 57 mm yang tidak mendukung ESC/POS (keluarga GB01 / GT01 / MX06, dijual dengan aplikasi “iPrint” / “Fun Print”). Terhubung lewat BLE |

Lebar kertas **58 mm (384 titik)** atau **80 mm (576 titik)**. Printer saku tanpa pemotong: biarkan **Cut mati**; 3–5 baris umpan kertas sudah pas.

### 6.3 Android / Chromebook — Bluetooth Classic

1. Nyalakan printer; tahan tombol Bluetooth jika perlu mode penyandingan.
2. **Setelan perangkat → Bluetooth → Sambungkan perangkat baru**, pilih printer dan masukkan PIN dari buku panduan (`0000` atau `1234` umum dipakai).
3. Jarful → Pengaturan → Printer termal → **Bluetooth Classic (SPP)**.
4. Pilih printer dari daftar perangkat tersanding (🔄 untuk menyegarkan). Di Android 12+ izinkan **Perangkat di sekitar** saat diminta.
5. Tekan **Cetak uji**. "Jarful / Test print OK" akan tercetak dalam beberapa detik.
6. Jika cetakan berhenti di tengah, tambah baris umpan atau cetak tiket satu per satu (⋮ → Cetak).

### 6.4 Android / Chromebook — Bluetooth LE

1. **Nyalakan Bluetooth** (kebanyakan printer LE tidak perlu penyandingan). Di Android 11 ke bawah nyalakan juga **Lokasi** (diperlukan untuk pemindaian BLE).
2. Jarful → Pengaturan → Printer termal → **Bluetooth LE (GATT)**.
3. Tekan 🔄 untuk memindai sekitar 4 detik; printer bernama muncul di daftar. Pilih milikmu.
4. **Cetak uji**. Koneksi pertama bisa memakan 5–10 detik.
5. Jika tidak mencetak: mulai ulang printer, tutup sepenuhnya aplikasi vendor (printer LE hanya menerima satu koneksi), atau matikan-nyalakan Bluetooth.

### 6.5 Windows 11 — Bluetooth (port COM virtual)

1. **Settings → Bluetooth & devices → Add device → Bluetooth**, sandingkan printer (PIN dari buku panduan).
2. **Settings → Bluetooth & devices → Devices**, gulir ke bawah dan buka **More devices and printer settings**.
3. Klik kanan printer → **Properties → Services**, centang **Serial port (SPP)**, lalu **OK**.
4. Di jendela yang sama buka **More Bluetooth settings → COM Ports** dan catat `COMx` yang tertulis **Outgoing** untuk printer. Jika belum ada: **Add → Outgoing → pilih printer → SPP**.
5. Jarful → Pengaturan → Printer termal → **Serial / COM** → pilih `COMx` → **Cetak uji**.
6. "PORT_OPEN_FAILED" berarti program lain memegang port (utilitas vendor) atau printer mati: tutup program itu dan nyalakan ulang printer.

> Versi Windows tidak bisa terhubung ke printer khusus LE. Cetak dari perangkat Android atau gunakan printer yang mendukung TCP.

### 6.6 Printer jaringan (TCP/IP)

1. Hubungkan printer ke LAN dan cetak **halaman uji mandiri** (biasanya tahan tombol umpan saat dinyalakan) untuk melihat alamat IP.
2. Jarful → Pengaturan → **TCP/IP** → masukkan IP; port `9100` (bawaan).
3. **Cetak uji**. Cadangkan IP di router (DHCP reservation) agar tidak berubah.

### 6.7 Rutinitas cetak dan tempel

- Pagi: tab Hari ini → **Cetak semua hari ini** (`Ctrl+P`) → sobek → **tempel di papan tulis putih dengan magnet** dari atas ke bawah.
- Siang: setiap tiket selesai, copot, remas, dan masukkan **ke stoples bening**. Menekan Selesai di aplikasi juga menambah bola ke stoples virtual.
- Malam: tiket esok disiapkan otomatis; paginya tinggal cetak.

## 7. Sinkronisasi Android ⇄ Windows (rinci)

Tanpa cloud, tanpa akun. **Perangkat di Wi-Fi yang sama menyinkronkan langsung** (dilindungi PIN, port bawaan 47831).

### 7.1 Host (PC Windows disarankan)

1. Pengaturan → **Sinkronisasi perangkat** → aktifkan **Jadikan perangkat ini host**.
2. Catat **Alamat perangkat ini** (mis. `192.168.1.20`) dan **PIN 6 digit**.
3. Jika Windows Firewall bertanya apakah Jarful boleh mengakses jaringan, izinkan pada **jaringan privat**.
4. Selama aplikasi terbuka, ia menerima sinkronisasi dari perangkat lain ("● menunggu koneksi").

### 7.2 Klien (Android dan lainnya)

1. Pengaturan → **Sinkronisasi perangkat** → di **Hubungkan ke**, masukkan **alamat IP host** dan **PIN**.
2. Tekan **Sinkronkan sekarang**. "Tersinkron" menandakan berhasil; tombol 🔄 di bilah atas melakukan hal yang sama.
3. Biarkan **Sinkronisasi otomatis** aktif untuk sinkron saat dibuka dan setiap 5 menit.

### 7.3 Cara kerja dan catatan

- Yang disinkronkan: **tugas, tiket, rutinitas**. Pengaturan khusus perangkat (printer, dll.) tidak.
- Jika kedua perangkat mengubah item yang sama, **perubahan terakhir yang menang**. Penghapusan juga ikut tersebar (pengeditan setelahnya memulihkan item).
- Lalu lintas berupa HTTP polos di dalam LAN. Gunakan di jaringan tepercaya dan matikan host di Wi-Fi publik.
- Tiga perangkat atau lebih tetap bisa, asalkan semua terhubung ke host yang sama.

## 8. Pintasan keyboard

| Tombol | Aksi |
|-----|--------|
| `N` / `Enter` | Tugas baru di kolom ini |
| `Tab` / `Shift+Enter` | Tambah subtugas (pecah) |
| `↑ ↓` | Pindah dalam kolom |
| `← →` | Pindah antar kolom |
| `Spasi` | Selesai / belum |
| `T` / `Shift+T` | Tiket hari ini / seluruh kolom → hari ini |
| `P` / `Shift+P` / `Ctrl+P` | Cetak tugas / kolom / semua hari ini |
| `Ctrl+K` | Fokus ulang |
| `F2` | Ubah nama |
| `Delete` | Hapus (`Ctrl+Z` untuk mengurungkan) |
| `Alt+↑ ↓` | Ubah urutan |
| `Ctrl+Z` | Urungkan |
| `Ctrl+1–5` | Ganti tab |
| `Esc` | Batal |

## 9. Gestur sentuh

| Gestur | Aksi |
|---|---|
| **Geser tiket ke kanan** | Selesaikan (lebih dari 40% lebar) |
| **Ketuk** tugas | Pilih (di ponsel: buka subtugasnya) |
| **Tekan lama** tugas | Menu (subtugas / hari ini / cetak / ubah nama / pindah / hapus) |
| **Ketuk dua kali** | Ubah nama |
| **←** di kiri atas | Kembali ke kolom induk |

Berfungsi di layar sentuh Chromebook dan laptop Google serta di tablet, berdampingan dengan mouse dan keyboard.

## 10. Data dan cadangan

- Lokasi: Android `filesDir/jarful-data.json` (privat aplikasi); Windows `%APPDATA%\Jarful\jarful-data.json`.
- **Cadangan**: Pengaturan → Data → **Ekspor JSON (salin)** menyalin semuanya ke papan klip; tempel ke catatan.
- **Pulihkan**: tempel ke **Impor JSON**. Data yang ada akan diganti (`Ctrl+Z` mengurungkan sekali).
- Tidak ada yang dikirim keluar perangkat secara otomatis; satu-satunya mitra adalah host sinkronisasi yang kamu atur.

## 11. Bahasa

日本語 / English / Français / العربية / Русский / Español / Deutsch / Tiếng Việt / Polski / Українська / Bahasa Indonesia / 繁體中文（台灣）.
Aplikasi mengikuti bahasa sistem secara bawaan; ubah di Pengaturan → Bahasa. Bahasa Arab mengubah seluruh tata letak menjadi kanan-ke-kiri.
Cetak raster berfungsi di semua bahasa (font yang sesuai dipilih per baris; huruf Arab disambung dan rata kanan).

## 12. Pemecahan masalah

| Gejala | Solusi |
|---|---|
| APK tidak bisa dipasang | Izinkan aplikasi tidak dikenal; pastikan Android 8.0+ |
| Perangkat Bluetooth tidak muncul | Sandingkan dulu di OS (Classic). Untuk LE tekan 🔄 lagi dan nyalakan Lokasi (Android 11 ke bawah) |
| Cetak uji habis waktu | Periksa daya printer, jarak, dan aplikasi lain yang memegang koneksi; tutup aplikasi vendor untuk LE |
| Teks kacau (mode teks) | Sesuaikan set karakter dengan font printer, atau ganti ke **ESC/POS raster** |
| Cetakan pudar | Sisi mengilap kertas termal harus menghadap kepala cetak |
| Tidak ada port COM di Windows | Tambahkan port **Outgoing** seperti di 6.5; sandingkan ulang |
| Sinkronisasi "host tidak terjangkau" | Wi-Fi sama? Aplikasi host terbuka? Firewall diizinkan? |
| Sinkronisasi "PIN salah" | Masukkan ulang PIN yang tampil di layar pengaturan host |
| Bisa disandingkan tapi tidak pernah terhubung (aplikasi vendor juga gagal) | Sebagian **printer khusus LE berhenti terhubung setelah disandingkan di sistem**. Lepaskan sandingan di pengaturan Bluetooth → nyalakan ulang printer → pilih **Bluetooth LE** di Jarful → 🔄 → Cetak uji. Tutup sepenuhnya aplikasi vendor. Jika masih gagal, tempel laporan **Diagnosis koneksi** ke issue |
| Tidak bisa terhubung di OPPO / Xiaomi / Huawei | Izinkan “Perangkat di sekitar” dan “Lokasi”, nyalakan Lokasi, kecualikan Jarful dari optimasi baterai. Jika Classic gagal, LE dicoba otomatis dan mode yang berhasil disimpan |
| Tiket rutinitas tidak muncul | Periksa hari dan sakelar "Aktif"; gunakan "Buat ulang hari ini" |

## 13. Membangun dari kode sumber

```bash
# uji unit (domain, pengodean cetak, sinkronisasi, tangkapan layar)
./gradlew :shared:desktopTest :desktopApp:test
# APK Android (debug)
./gradlew :androidApp:assembleDebug
# penginstal Windows (jalankan di Windows)
./gradlew :desktopApp:packageMsi
# menjalankan aplikasi desktop
./gradlew :desktopApp:run
```

Persyaratan: JDK 17 dan Android SDK (API 35). Lihat [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md);
spesifikasinya di [docs/SPEC.md](docs/SPEC.md) dan catatan artikel di [docs/SOURCES.md](docs/SOURCES.md) (bahasa Jepang).

## 14. Lisensi dan sangkalan

Lisensi MIT. Jarful adalah **implementasi independen dan tidak resmi** yang terinspirasi dari artikel yang tersedia untuk umum.
Tidak berafiliasi dengan atau didukung oleh Laurie Hérault, aplikasinya Colonnes, maupun tim redaksi Nazology, dan tidak memuat
teks, gambar, atau perangkat lunak mereka. Tidak ada afiliasi atau jaminan untuk produk printer mana pun.
