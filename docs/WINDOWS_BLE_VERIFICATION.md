# Windows BLE / Phomemo M02 verification

Checked locally on 2026-11-09:

- `powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -File tools/Test-WindowsBle.ps1`: PASS. Compiles the actual C# WinRT event bridge and checks address validation, cache-miss recovery, random address types, missing devices, enumeration failure, deduplication, output escaping and writable-characteristic selection without hardware.
- `jarful-ble.ps1 list`: discovered the nearby `Mr.in_M02` over advertisements. The existing device-enumeration API failed on this machine; the new list still returned the live device.
- `git diff --check`: PASS.
- `gradlew.bat :shared:desktopTest --no-daemon`: at the initial BLE verification, no build JDK was available. JDK 25 has since been installed and the build configuration migrated; see [JDK 25 migration checks](JDK25_MIGRATION.md) for the current network blocker. New Kotlin tests cover exact M02 raster bytes, the 255/256-row boundary, fixed width, paper feed and separate labels; they still need a complete Gradle run. Windows CI also runs the helper regression checks.
- GATT connection to the discovered M02 still returned null locally. Successful GATT writes and visible paper output have **not** been verified. The installed application and its saved settings have not been replaced.

For a full hardware check after building:

1. Run `./gradlew :shared:desktopTest :desktopApp:packageExe --no-daemon` with JDK 25 and the project's Android SDK prerequisites.
2. Start the rebuilt app, turn on M02, disconnect phone printing apps, and refresh **Bluetooth LE (GATT)**.
3. Select `Mr.in_M02` from the scanned list and choose **Phomemo M02 (384px)**. Do not reuse a Classic/COM address or the Cat printer protocol.
4. Run connection diagnostics, then test print. Verify Japanese text, a long ticket crossing a 255-row boundary, and two successive tickets with legible output and paper feed. Keep any failure report; a successful byte transfer alone does not prove paper output.

References: [Windows address-cache behavior](https://learn.microsoft.com/en-us/uwp/api/windows.devices.bluetooth.bluetoothledevice.frombluetoothaddressasync), [M02 raster/feed format](https://github.com/vivier/phomemo-tools/blob/master/cups/filter/rastertopm02_t02.py).
