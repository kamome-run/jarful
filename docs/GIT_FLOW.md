# Git-flow

`main` はリリース済みの系統、`develop` は次のリリースの統合先です。既定ブランチは `develop`。Git-flow 拡張コマンドを追加インストールする必要はありません。

| ブランチ | 分岐元 | PR の統合先 | 用途 |
|---|---|---|---|
| `feature/<topic>` | `develop` | `develop` | 新機能、通常の修正、ビルド・文書の変更 |
| `release/<MAJOR.MINOR.PATCH>` | `develop` | `main` と `develop` | リリース準備・安定化 |
| `hotfix/<MAJOR.MINOR.PATCH>` | `main` | `main` と `develop`、必要なら進行中の `release/*` | 公開版への緊急修正 |

`main` / `develop` は PR と必須チェックを通し、マージコミットで統合します。通常運用では force-push、squash merge、rebase merge、公開タグの移動を行いません。完了した作業ブランチは、必要なすべての統合先への反映後に削除します。リリースの戻し PR が残っている間は削除しません。

## 開発

```sh
git fetch origin
git switch -c feature/example origin/develop
# 実装、テスト、コミット
git push -u origin feature/example
gh pr create --base develop
```

fork からの `feature/*` PR も `develop` を対象にします。`main` へ直接出せるのは本体リポジトリの `release/<version>` / `hotfix/<version>` だけです。

## リリース

```sh
git fetch origin
git switch -c release/1.8.0 origin/develop
# バージョン更新、リリース準備、テスト
git push -u origin release/1.8.0
gh pr create --base main --head release/1.8.0
```

更新する値は Android の versionCode / versionName、desktop の packageVersion、共通の AppVersion.NAME。CI のテストと Android / Windows のビルドが成功してから `main` に **Create a merge commit** で統合します。同じ `release/1.8.0` を `develop` に戻す PR も作成・マージします。`main` のリリースマージコミットに注釈付きタグを付けます。

```sh
git fetch origin
git tag -a v1.8.0 <mainのリリースマージSHA> -m 'Jarful 1.8.0'
git push origin refs/tags/v1.8.0
```

タグは `vMAJOR.MINOR.PATCH`。`main` へ統合済みで、3か所のバージョンがタグと一致する場合だけ配布物を公開します。途中の版はブランチの CI 成果物で確認し、正式な公開タグを付けません。

## 緊急修正

```sh
git fetch origin
git switch -c hotfix/1.8.1 origin/main
# 修正、3か所の版更新、テスト、コミット
git push -u origin hotfix/1.8.1
gh pr create --base main --head hotfix/1.8.1
gh pr create --base develop --head hotfix/1.8.1
```

進行中のリリースがある場合は、その `release/*` へも同じ修正を戻します。GitHub が差分なしの PR を作成できない場合は、その統合先が修正コミットを祖先として含むことを `git merge-base --is-ancestor` で確認して記録します。`main` のマージに `v1.8.1` を付けます。

## CI と保護設定

- `main` / `develop` / `feature/*` / `release/*` / `hotfix/*` の push と PR でビルドします。
- `Git-flow policy` は信頼済みの統合先コードだけを実行し、PR の経路を検査します。PR 元コードを `pull_request_target` で実行しません。
- 必須チェックは `Git-flow policy`、`Unit tests (shared)`、`Android APK`、`Windows 11 installer`。保護ブランチの削除・force-push・直接 push を拒否し、管理者にも適用します。
- `main` / `develop` の線形履歴の強制は無効です。マージコミットが Git-flow の分岐と統合を記録します。
- リポジトリでは merge commit のみを許可し、作業ブランチの自動削除を無効にします。複数の統合先に反映してから手動削除します。
- 1人で管理できるよう、PR は必須ですが別の承認者の人数は強制しません。共同管理を始める場合は保護設定で必要承認数を増やしてください。

## 過去履歴の移行

2026年11月の移行では、旧 `main` の直線状の履歴を公開タグと PR の内容に沿って再構成します。元の変更コミットは作者・日時・メッセージとファイルツリーを保持して再配置し、追加マージは「Reconstructed」と明示します。元の squash 以前の開発途中コミットやブランチ作成日時を復元したものではありません。

機能追加は `feature/*` → `develop` → `release/*` → `main`、公開版修正は `hotfix/*` → `main` / `develop` に対応させます。昔のパッチ版にも機能追加が含まれるため、旧版番号だけで hotfix と判定しません。

旧25タグの各ツリーは移行前と完全一致させます。タグ名と既存 Release・配布資産を維持しますが、コミット SHA とタグオブジェクト SHA は変わります。署名を変更後のコミットに移植しません。旧署名と旧履歴はバックアップに保持します。既存 Release の配布物は再ビルド・再アップロードしません。

CI と保護ルールをすべての統合先に配置するため、履歴再構成後の `main` / `develop` に、一度だけ版番号を変えない運用設定の bootstrap merge を加えます。これは製品リリースではなく、既存の公開タグはその直前の再構成済みリリースマージを指します。

旧履歴は `archive/pre-git-flow-20261112/main`、`archive/pre-git-flow-20261112/pr27` と同じ接頭辞の退避タグに保持します。完全な Git bundle、タグ・コミット対応表、GitHub 設定と Release 資産のメタデータも管理者が保管します。閉じた過去 PR は書き換えられず、元コミットと旧差分を引き続き参照します。

既存の PR #27 は `feature/windows-ble-discovery` → `develop` として作り直します。変更の統合や新しい製品版の公開は移行に含めません。

移行後は新規 clone を推奨します。未公開作業は旧 clone に保存し、必要な自分の変更だけを新 clone に cherry-pick してください。旧 `main` を新 `main` / `develop` へ merge すると旧履歴が再流入するため行わないでください。

参考: [Git-flow の原案](https://nvie.com/posts/a-successful-git-branching-model/)。
