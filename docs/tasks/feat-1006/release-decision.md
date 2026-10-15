# Release Decision

- Task: `FEAT-1006`
- Decision: `release_ready`
- Verified revision: `ede6d5b79d26a9bceb8b36fdcfd53791c7736efb`
- Branch: `feat/feat-1006-wmts-offline-data`
- Date: 2026-10-14

## Gate Evidence

- Independent Verification passed for AC-001 through AC-008 on the exact revision above; see `verification.md`.
- GitHub Actions [Android CI run #15](https://github.com/GTYang99/TaoYuanGutter/actions/runs/38526214063) completed successfully for the exact revision. The task-scoped JVM tests and debug APK assembly passed.
- Emulator validation ran only on `emulator-5554` (Android 16/API 36). The three-GPKG package, all four map hosts, local layer behavior, request capture, and lifecycle checks passed. No physical phone was tested, as required.
- Issue-log review found no open P0 or P1 issue. The remote-CI blocker ISS-FEAT-1006-008 is closed.
- The branch includes merge commit `555dea4`, whose second parent is requested commit `a86dc3dd0a3c390a498bbe94cb79e4eb125ed400`.

## Risks and Recovery

- The three GPKGs are static bundled data. The app creates a versioned, app-private derived SQLite index; it rebuilds from the packaged sources when the cache version changes. No server database migration or new permission is introduced.
- Feature data for the three overlays is served locally; unrelated map requests retain their existing behavior. No public API contract change is part of FEAT-1006.
- CI verifies JVM tests and a debug build. A signed Release artifact and production deployment were not produced as part of this task.
- The approved plan's rollback is to revert the offline source and outline changes, restore the previous WMTS providers, and remove the bundled trial assets. Cache cleanup is limited to the task-owned derived data.

## Decision

The verified branch is ready for human release authorization. This decision does not authorize merging the feature branch into `main`, publishing an artifact, or deploying the app.

Next action: `human_release`.
