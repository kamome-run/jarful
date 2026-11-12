# Contributing

Issues and pull requests are welcome, in Japanese or English.

This repository uses Git-flow. Read [the complete workflow](docs/GIT_FLOW.md) before opening a PR.

1. Create `feature/<topic>` from `develop`; open the PR against `develop`.
2. Keep behaviour aligned with `docs/SPEC.md`; include spec changes in the same PR.
3. Run `./gradlew :shared:desktopTest :desktopApp:test` before pushing. For workflow-only changes run `node --test tools/check-git-flow.test.mjs`.
4. Keep feature, release, and hotfix boundaries visible with merge commits. Do not squash or rebase merges.
5. Maintainers create `release/<version>` from `develop` and `hotfix/<version>` from `main`. Both finish with PRs into `main` and back into `develop` (also an active release when needed).
6. Only tag a tested merge on `main` as `v<version>`. All three app versions must match. CI builds the APK and Windows installers and publishes the release.

`main` and `develop` require PRs and passing checks. Do not force-push shared branches or move release tags during normal development.
