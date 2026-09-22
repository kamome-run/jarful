# 開発ガイド

## 構成

```
jarful/
├── shared/       Kotlin Multiplatform 共通コード（ドメイン、UI、印刷、プラットフォーム抽象）
│   ├── commonMain/   model / data(Store) / domain / print / sound / ui
│   ├── androidMain/  FileStore, Bluetooth SPP, AudioTrack, Vibrator, Canvas ラスタライズ
│   ├── desktopMain/  FileStore, jSerialComm(COM), javax.sound, Java2D ラスタライズ
│   └── commonTest/   ツリー操作・ルーチン生成・印刷エンコード・統計・Store のテスト
├── androidApp/   Android アプリ（MainActivity、マニフェスト、アイコン）
├── desktopApp/   Compose Desktop アプリ（Windows MSI/EXE パッケージ設定）
└── docs/         仕様書、参照記事要約、本ガイド
```

## 必要なもの

- JDK 17
- Android SDK（`platforms;android-35`, `build-tools;35.0.0`）。`local.properties` に `sdk.dir` を書くか `ANDROID_HOME` を設定。
- Windows インストーラ（MSI/EXE）の作成は Windows 上でのみ可能（jpackage の制約）。CI が生成します。

## よく使うコマンド

| 目的 | コマンド |
|------|----------|
| 単体テスト | `./gradlew :shared:desktopTest` |
| Android debug APK | `./gradlew :androidApp:assembleDebug` |
| Android release APK（未署名） | `./gradlew :androidApp:assembleRelease` |
| デスクトップ起動 | `./gradlew :desktopApp:run` |
| 実行可能 JAR | `./gradlew :desktopApp:packageUberJarForCurrentOS` |
| Windows MSI/EXE | `./gradlew :desktopApp:packageMsi :desktopApp:packageExe`（Windows 上） |

## 設計メモ

- **状態**: `Store` が `StateFlow<AppData>` を持ち、すべての変更は `mutate {}` を通る。150ms デバウンスで JSON に原子的書き込み。Undo はスナップショット方式（最大 50）。
- **UI 状態**: `AppState`（選択パス、フォーカス列、ダイアログ、コンボ、瓶の投入シグナル）。
- **印刷**: `TicketFormatter` が `PrinterSettings.protocol` に応じて ESC/POS テキスト、または 1bit 画像（`renderTextBitmap` の expect/actual）を各プロトコルにエンコード。`PrinterClient` がトランスポートを切り替え、512 バイト単位で送信。
- **効果音**: `CrumpleSound` が PCM を手続き生成（バイナリ資産なし）。
- **Chromebook**: `uses-feature touchscreen required=false`、`resizeableActivity`、幅 840dp 以上で 3 ペイン。

## 新しいプリンタープロトコルを追加するには

1. `PrintProtocol` に列挙子を追加（`raster = true` なら画像ベース）。
2. `print/EscPos.kt` に近いエンコーダオブジェクトを追加し、`TicketFormatter.encodeBitmap` に分岐を追加。
3. `PrintEncodingTest` にヘッダ・データ長のテストを追加。

## リリース

`v*` タグを push すると CI が APK と Windows インストーラを GitHub Release に添付します。
