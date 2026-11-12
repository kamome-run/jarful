# JDK 25 build configuration

The build now selects JDK 25 for Gradle, all three module toolchains, tests and desktop packaging. No JDK 17 installation is required. Desktop artifacts target Java 25; the packaged app includes its runtime. Android compilation uses JDK 25 with Java 17-compatible output for D8. Android's `JVM_17` / `VERSION_17` settings describe output compatibility, not an additional installed JDK. Desktop tests explicitly select Kotlin's JUnit 5 adapter and declare the Jupiter engine and Platform launcher, since Gradle 9 no longer supplies framework runtime dependencies implicitly.

Pinned tool versions:

| Tool | Version |
| --- | --- |
| JDK | 25 (latest maintained patch) |
| Gradle wrapper, scripts and distribution | 9.3.1 |
| Kotlin and Compose compiler | 2.3.21 |
| Compose Multiplatform | 1.10.3 |
| Android Gradle Plugin | 8.13.2 |
| Android compile SDK / target SDK | 36 / 35 |
| Android SDK Build Tools | 35.0.0 |
| Desktop JUnit Jupiter / Platform | 5.12.2 / 1.12.2 |
| kotlinx-datetime | 0.7.1 (uses `kotlin.time.Clock` / `Instant`) |

Gradle 9.1 introduced Java 25 support. Kotlin 2.3.20–2.3.21 supports the Gradle 9.3 line. AGP 8.13.2 adds Kotlin 2.3 support. The AGP 8.x module layout is retained. Sources: [Gradle Java support](https://docs.gradle.org/current/userguide/compatibility.html), [Kotlin Multiplatform compatibility](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html), [AGP 8.13.2](https://developer.android.com/build/releases/agp-8-13-0-release-notes), [Compose 1.10.3](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.10.3).

The wrapper JAR and launch scripts are the upstream Gradle `v9.3.1` files. Distribution downloads verify SHA-256 `b266d5ff6b90eada6dc3b20cb090e3731302e553a27c5d3e4df1f0d76beaff06` from the official [distribution release](https://github.com/gradle/gradle-distributions/releases/tag/v9.3.1). Wrapper JAR SHA-256: `b3a875ddc1f044746e1b1a55f645584505f4a10438c1afea9f15e92a7c42ec13`.

## Local verification (2026-11-09)

- Temurin `java` and `javac`: 25.0.5; JVM startup passed.
- Compiled and executed a Java smoke program with JDK 25 using both `--release 25` and `--release 17`. Class-file major versions were 69 and 61 respectively. This verifies the local compiler and cross-compilation capability, not the application's Gradle build.
- `jlink --version` and `jpackage --version`: 25.0.5.
- Wrapper JAR content matches upstream Git blob `61285a659d17295f1de7c53e24fdf13ad755c379`.
- `tools/Verify-Jdk25.ps1 -CheckOnly`: passed; detected the installed JDK 25 and selected workspace paths for the SDK and Gradle cache. Script parsing, native argument forwarding and propagation of native failures were also checked.
- The official Android command-line tools archive was downloaded from a normal PowerShell session and its SHA-256 matched the pinned checksum. PowerShell 5.1 extraction failed on a long path; the script now uses JDK 25's `jar` extractor. The corrected extraction and SDK Manager 22.0 startup both passed locally. The user then installed SDK packages and downloaded the Gradle distribution and plugins through the verification script.
- `gradlew.bat --version --console=plain`: passed using the cached distribution; Gradle 9.3.1 reported both launcher and daemon JDK as Temurin 25.0.5.
- `gradlew.bat help --offline --no-daemon --console=plain`: passed after allowing the Japanese Windows project path with `android.overridePathCheck=true`. All three module build scripts were configured successfully.
- `gradlew.bat :shared:desktopTest :desktopApp:test --offline --no-daemon --console=plain`: reached `:shared:compileKotlinDesktop`, then stopped because the application libraries (Compose, Kotlin standard library, coroutines, etc.) were not cached yet. Application source compilation, test execution, Android compilation and native packaging remain unverified. This environment still cannot download the missing libraries directly. No CI run has been triggered.
- Native-output logging smoke test: stdout, stderr and exit code 7 were all captured correctly. The script writes Gradle output to `build/jdk25-support/gradle.log` using UTF-8 and `--console=plain`, since PowerShell transcription alone omitted parts of native Gradle output.

## Completed local verification (2026-11-12)

The dependency-download blocker recorded above has been resolved. `tools/Verify-Jdk25.ps1` completed successfully with Temurin 25.0.5 and Gradle 9.3.1:

- Shared and desktop test tasks passed (81 tests across 16 suites, no failures, errors or skips in the XML reports; the final build reused their up-to-date results).
- Android debug and unsigned release APKs were generated in `androidApp/build/outputs/apk`.
- `:desktopApp:createDistributable`, `:desktopApp:packageMsi` and `:desktopApp:packageExe` passed. Outputs are `desktopApp/build/compose/binaries/main/app/Jarful/Jarful.exe`, `main/msi/Jarful-1.7.2.msi` and `main/exe/Jarful-1.7.2.exe`.
- `tools/Test-WindowsApp.ps1` passed a 15-second startup check with isolated `APPDATA`. It follows jpackage's child launcher and verifies that the process loads the packaged `runtime/bin/server/jvm.dll` and `app/skiko-windows-x64.dll`. Logs and native module paths are saved under `build/jdk25-support/app-smoke-*`. The check closes only the processes it started.
- `tools/Test-WindowsBle.ps1` and `git diff --check` passed.

Windows packaging needed one additional workaround: Compose 1.10.3 writes its tool argument files as UTF-8, whereas the Windows jlink launcher interprets them in the native code page. A Japanese project path consequently failed in `createRuntimeImage`. The Windows task now passes jlink's short argument list directly, preserving the configured modules, output and free arguments and the current stripping defaults. Gradle retains `file.encoding=COMPAT` for its own Java launcher argument files; source compilation and test text remain UTF-8. See the [pinned Compose task implementation](https://github.com/JetBrains/compose-multiplatform/blob/v1.10.3/gradle-plugins/compose/src/main/kotlin/org/jetbrains/compose/desktop/application/tasks/AbstractJvmToolOperationTask.kt).

WiX linking returned exit code 216 inside the restricted execution environment; the same verification script succeeded when run outside that restriction. No installer was run, no installed application or saved user data was replaced, and no CI run or release was triggered. Physical printer output and jSerialComm hardware operation remain unverified.

## Full verification with network access

Set `JAVA_HOME` to JDK 25 and configure the Android SDK as described in [DEVELOPMENT.md](DEVELOPMENT.md). In PowerShell, for example:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.5.8-hotspot'
.\gradlew.bat --version
.\gradlew.bat :shared:desktopTest :desktopApp:test :androidApp:assembleDebug :androidApp:assembleRelease --no-daemon
.\gradlew.bat :desktopApp:packageMsi :desktopApp:packageExe --no-daemon
```

Confirm Gradle reports JDK 25, all tests and builds pass, and the packaged Windows app starts and can use Skiko and jSerialComm. Native access is enabled for the desktop app and tests because these dependencies load native libraries. The installed Jarful executable has not been rebuilt or replaced by this configuration change.

For Windows setup and validation in one run, use `powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/Verify-Jdk25.ps1` from a shell with network access. The script obtains the Android SDK when needed, lets `sdkmanager` present its license agreements, runs the tests and all packages, and saves a transcript in `build/jdk25-support/verification.log` plus native Gradle output in `build/jdk25-support/gradle.log`. It leaves the Gradle cache and Android user preferences inside the workspace so subsequent checks can reuse the downloaded dependencies. It does not install the rebuilt application or publish anything.
