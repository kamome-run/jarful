# 開発ガイド

## 構成

```
jarful/
├── shared/       Kotlin Multiplatform 共通コード（ドメイン、UI、印刷、プラットフォーム抽象）
│   ├── commonMain/   model / data(Store) / domain / print / sound / sync / ui (ui/ds = design-system primitives)
│   ├── androidMain/  FileStore, Bluetooth SPP, AudioTrack, Vibrator, Canvas ラスタライズ
│   ├── desktopMain/  FileStore, jSerialComm(COM), javax.sound, Java2D ラスタライズ
│   └── commonTest/   ツリー操作・ルーチン生成・印刷エンコード・統計・Store のテスト
├── androidApp/   Android アプリ（MainActivity、マニフェスト、アイコン）
├── desktopApp/   Compose Desktop アプリ（Windows MSI/EXE パッケージ設定）
└── docs/         仕様書、参照記事要約、本ガイド
```

## 必要なもの

- **JDK 25**（推奨: Eclipse Temurin 25 の最新パッチ）。`JAVA_HOME` をそのインストール先に設定する。Gradle 実行、Kotlin/Java コンパイル、テスト、Windows 配布物の作成はすべて JDK 25 を使い、JDK 17 のインストールは不要。
- Android SDK（`platforms;android-36`, `build-tools;35.0.0`）。`local.properties` に `sdk.dir` を書くか `ANDROID_HOME` を設定。
- Windows インストーラ（MSI/EXE）の作成は Windows 上でのみ可能（jpackage の制約）。CI が生成します。

Windows で必要な SDK の取得から検証まで進めるには、通信可能な通常の PowerShell で次を実行する。インストール済みの Temurin JDK 25 を検出し、Android SDK がなければ公式コマンドラインツールを SHA-256 検証後に展開する。SDK のライセンスは `sdkmanager` の画面で確認する。

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/Verify-Jdk25.ps1
```

`-JdkHome` と `-AndroidSdk` で既存のインストール先も指定できる。`-CheckOnly` はダウンロードやビルドをせず設定だけ確認する。実行ログは `build/jdk25-support/verification.log`、Gradle の標準出力・標準エラーは `build/jdk25-support/gradle.log`、Gradle キャッシュは `build/jdk25-gradle-cache` に保存される。Android の設定・デバッグ鍵の保存先も `build/jdk25-support/android-user-home` に設定する。完了するとテスト・Android APK・Windows アプリと MSI/EXE が生成される。Jarful のインストールや保存データの変更は行わない。

Windows の日本語ユーザーフォルダで Android プラグインが構成処理を止めないよう、`gradle.properties` に `android.overridePathCheck=true` を指定している。コンパイラ・パッケージツールで発生した実際のエラーは通常どおり検出する。

Windows の `jlink` には引数を直接渡し、Compose 1.10.3 の UTF-8 引数ファイルと Windows のコードページの不一致を回避する。生成したアプリの起動確認には `powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/Test-WindowsApp.ps1` を使用できる。一時的な `APPDATA` で15秒起動し、同梱 JVM と Skiko の DLL を読み込んだことを確認して終了する。ログは `build/jdk25-support/app-smoke-*` に保存される。制限された実行環境では WiX のリンクや GUI 起動が失敗する場合があるため、その場合は通常の PowerShell で実行する。

## よく使うコマンド

| 目的 | コマンド |
|------|----------|
| 単体・デスクトップテスト | `./gradlew :shared:desktopTest :desktopApp:test` |
| Windows BLE ヘルパー回帰テスト（実機不要） | `powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/Test-WindowsBle.ps1` |
| Android debug APK | `./gradlew :androidApp:assembleDebug` |
| Android release APK（未署名） | `./gradlew :androidApp:assembleRelease` |
| デスクトップ起動 | `./gradlew :desktopApp:run` |
| 実行可能 JAR | `./gradlew :desktopApp:packageUberJarForCurrentOS` |
| Windows MSI/EXE | `./gradlew :desktopApp:packageMsi :desktopApp:packageExe`（Windows 上） |

## 設計メモ

- **ビルド環境**: Gradle 9.3.1 / Kotlin 2.3.21 / Compose Multiplatform 1.10.3 / Android Gradle Plugin 8.13.2。全モジュールで `jvmToolchain(25)` を指定し、CI も JDK 25 を使用する。デスクトップの生成コード・同梱ランタイムは Java 25。Android は同じ JDK 25 で Java 17 互換のバイトコードを生成し、D8 で Android 用に変換する。`JVM_17` / `VERSION_17` は出力形式の指定であり、JDK 17 を取得する指定ではない。Android の target SDK は 35 を維持する。

- **状態**: `Store` が `StateFlow<AppData>` を持ち、すべての変更は `mutate {}` を通る。150ms デバウンスで JSON に原子的書き込み。Undo はスナップショット方式（最大 50）。
- **UI 状態**: `AppState`（選択パス、フォーカス列、ダイアログ、コンボ、瓶の投入シグナル）。
- **印刷**: `TicketFormatter` が `PrinterSettings.protocol` に応じて ESC/POS テキスト、または 1bit 画像（`renderTextBitmap` の expect/actual）を各プロトコルにエンコード。`PrinterClient` がトランスポートを切り替え、512 バイト単位で送信。
- **デザインシステム**: `ui/ds` の `Ds*` プリミティブ（Button / IconButton / Switch / Checkbox / TextField / Card / ListItem / Dialog / Menu / Segmented / Chip / Progress / Divider / AppShell）が `LocalDesignSystem` に応じて Material 3（Android）または自前実装の Fluent / WinUI 3（デスクトップ）で描画する。画面は `Ds*` と foundation のレイアウトだけを使う。Fluent のトークンは `DesignSystem.kt` の `fluentTokens()`。
- **多言語**: `ui/i18n/Strings.kt` の `Strings` データクラスに全文言を持ち、`Strings.<lang>.kt` が 1 言語 1 ファイル（ja / en / fr / ar / ru / es / de / vi / pl / uk / id / zh-TW）。`resolveLanguage()` がシステムロケールを解決し、`Language.rtl` が `LocalLayoutDirection` を切り替える。文言を追加するときは全ファイルに同じ名前付き引数を追加する（コンパイルエラーで漏れが分かる）。README も `README.<lang>.md` で 1 言語 1 ファイル。
- **印刷の多言語**: ラスター描画は行ごとに `printerFontFamily()`（デスクトップ）／Android の標準フォールバックでフォントを選び、`isRtlText()` で右寄せ。テキストモードは `PrinterCharset.codePage`（`ESC t n`）と `kanji`（`FS &`）で切り替える。
- **Bluetooth**: Classic は `sendBluetooth()`（RFCOMM/SPP）、LE は `sendBle()`（GATT、既知キャラクタリスティック優先、MTU 分割書き込み）。Windows は `sendSerial()`（jSerialComm）。
- **同期**: `SyncMerge`（純粋関数、最終更新優先＋トゥームストーン）、`SyncServer`/`SyncClient`（`src/jvmShared` の最小 HTTP 実装を Android・デスクトップで共用）。`Store.mutate` が変更差分から `updatedAt` とトゥームストーンを自動付与する。
- **効果音**: `CrumpleSound` が PCM を手続き生成（バイナリ資産なし）。
- **Chromebook**: `uses-feature touchscreen required=false`、`resizeableActivity`、幅 840dp 以上で 3 ペイン。

## 新しいプリンタープロトコルを追加するには

1. `PrintProtocol` に列挙子を追加（`raster = true` なら画像ベース）。
2. `print/EscPos.kt` に近いエンコーダオブジェクトを追加し、`TicketFormatter.encodeBitmap` に分岐を追加。
3. `PrintEncodingTest` にヘッダ・データ長のテストを追加。

## リリース

### この Windows 環境での更新

この作業コピーは `%LOCALAPPDATA%/Jarful/source` にあり、インストール先の配下にある。旧 1.7.2 のアンインストーラーは `WixRemoveFoldersEx` でインストール先全体を再帰処理するため、**この配置のまま MSI 更新・アンインストールを再実行しない**。2026-11-12 の 1.7.3 更新では、この再帰処理が Gradle キャッシュの長いパスでエラー 2318（終了コード 1603）になった。ソースと保存データに削除・変更がないことを確認したうえで、旧 `app`・`runtime`・`Jarful.exe` のみを `build/jdk25-support/installed-app-backup-*` に移動して保存し、テスト済みの 1.7.3 の同じ3項目を配置した。配置後は全ファイルの SHA-256 一致と、保存データが更新前のままであることを確認した。

今日のチケットの一括削除を追加した 1.7.4 も、同じ方法でアプリ本体を更新した。全94件のテスト、Windows/Android のビルド、生成アプリの15秒起動確認が成功し、配置後の全ファイル一致と保存データの保持を確認済み。アプリ本体・設定画面のバージョンは 1.7.4。MSI の登録は更新していないため、Windows のインストール済みアプリ一覧には旧版の番号が残る。今後通常のインストーラーを使う前に、作業コピーと必要なバックアップをインストール先の外へ保全・移設する必要がある。

バージョンは 3 か所を同時に上げる: `androidApp/build.gradle.kts`（versionCode / versionName）、`desktopApp/build.gradle.kts`（packageVersion）、`shared/.../AppVersion.kt`（設定画面に表示）。


`v*` タグを push すると CI が APK と Windows インストーラを GitHub Release に添付します。
