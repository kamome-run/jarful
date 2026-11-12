## Change

Describe the problem, resulting behaviour, and relevant issue.

## Git-flow

- [ ] `feature/*` targets `develop`, or this is a maintainer `release/<version>` / `hotfix/<version>` PR.
- [ ] Release/hotfix: update Android versionCode/versionName, desktop packageVersion, and AppVersion.NAME together.
- [ ] Release/hotfix: link the PR that integrates the same branch back into `develop` (and any active release).
- [ ] Keep merge commits; do not squash or rebase when merging this PR.

## Validation

List completed checks and any manual checks still needed.
