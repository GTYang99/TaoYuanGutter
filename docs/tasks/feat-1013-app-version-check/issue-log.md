# Issue Log

## ISS-001 — Required update signing key unavailable

- Task: `feat-1013-app-version-check`
- Phase: `verification`
- Category: `environment`
- Priority: `P1`
- Status: `resolved`
- Affected acceptance criteria: AC-008
- Evidence: the checkout's `local.properties` lacks `UPDATE_*` properties, but the user-provided JKS was found outside Git and used through mode-0600 temporary configuration in isolated build copies. Its certificate fingerprint matched the code-4 and code-5 Release APKs; same-key `adb install -r` update passed on `emulator-5554`. No credential values were recorded.
- Next action: none for AC-008.

## ISS-002 — Installer/settings UI checks were initially blocked by the Mac lock

- Task: `feat-1013-app-version-check`
- Phase: `verification`
- Category: `environment`
- Priority: `P2`
- Status: `resolved`
- Affected acceptance criteria: AC-004, AC-007
- Evidence: after the user unlocked the Mac, the installer/settings flow ran on `emulator-5554`. The correct app-specific settings page opened, permission grant automatically resumed Android's installer, and Cancel returned to Login without an immediate prompt. The original APK and permission state were restored. AC-004 cadence is covered by the request-count case under ISS-005.
- Next action: none for the computer-use availability blocker.

## ISS-003 — Controlled HTTPS downgrade redirect case unavailable

- Task: `feat-1013-app-version-check`
- Phase: `verification`
- Category: `environment`
- Priority: `P2`
- Status: `resolved`
- Affected acceptance criteria: AC-009
- Evidence: a temporary host-side HTTPS fixture redirected to a local HTTP target while exercising the fixed-revision `AppUpdateDownloader`. The downloader raised the expected HTTPS-only `IOException`, the HTTP target received no request, and no APK destination was created. Existing offline and failure-flow evidence confirms app use remains available.
- Next action: none for AC-009.

## ISS-004 — CI evidence unavailable for the fixed revision

- Task: `feat-1013-app-version-check`
- Phase: `verification`
- Category: `environment`
- Priority: `P1`
- Status: `resolved`
- Affected gates: CI build and test
- Evidence: with user authorization, exact commit `02ad7e5370a9a103182b0cb908e1175e39803248` was pushed to `origin` branch `codex/feat-1013-app-version-check-implementation`. [GitHub Actions Android CI run #11](https://github.com/GTYang99/TaoYuanGutter/actions/runs/38355107296) completed successfully; the task-scoped JVM tests and `:app:assembleDebug` both passed.
- Next action: none for CI evidence. Merge remains outside Verification.

## ISS-005 — Post-installer foreground request cadence is not observable

- Task: `feat-1013-app-version-check`
- Phase: `verification`
- Category: `environment`
- Priority: `P2`
- Status: `resolved`
- Affected acceptance criteria: AC-004
- Evidence: a temporary instrumentation case in an isolated copy of the fixed revision set the same persisted flags written before installer handoff. Returning immediately made 0 repository calls; after an ordinary background/foreground transition the count became exactly 1. The actual settings/installer/cancel path was separately observed under AC-007.
- Next action: none for AC-004.
