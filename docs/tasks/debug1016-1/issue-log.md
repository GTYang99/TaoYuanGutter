# Issue Log

## ISS-DBG-1016-1-001

- task_id: debug1016-1
- phase: implementation
- category: enhancement_request
- priority: P2
- title: Empty add form opens its mini-map at the Taoyuan fallback
- status: resolved
- impact: The form's initial map viewport may be unrelated to the user's work location when the new/add waypoint has no saved coordinates.
- repro_steps:
  - Open the new/add gutter sheet.
  - Select a waypoint that has no saved coordinates to open GutterFormActivity.
  - Observe the form mini-map before selecting a coordinate.
- expected: The mini-map viewport starts at the user's current location when a usable location is available.
- actual: A fresh eligible add form now runs the fused-location flow and centers the mini-map on the emulator GPS fix. The fix remains camera-only.
- evidence:
  - AddGutterBottomSheet.kt:1212-1219 reports add mode as not edit mode.
  - GutterFormActivity.kt:1663-1685 selects the Taoyuan fallback and invokes location only through the edit-only policy.
  - EditMapLocationPolicy.kt:5-12 excludes new/add mode.
  - EditMapLocationPolicyTest.kt:7-15 asserts the exclusion.
  - The prior DBG-1007 requirement FR-006 / AC-008 left new-point behavior unchanged.
- Emulator evidence: The location callback ran with a usable fix and the apply guard allowed it, but `animateCamera()` left the map target unchanged. Replacing that update with `moveCamera()` made the focused test pass.
- next_action: verification
- owner: developer

## ISS-DBG-1016-1-002

- task_id: debug1016-1
- phase: verification
- category: environment
- priority: P2
- title: No-fix fallback interaction remains unverified after emulator test failures
- status: open
- impact: AC-005 runtime evidence is incomplete; the task cannot advance to Release.
- repro_steps:
  - Run `missingCoordinateEditWithoutUsableFixKeepsFallbackAndShowsUnavailablePrompt` on `emulator-5554` with location disabled.
  - Retry once after disabling emulator animations.
  - Run `returningFromSettingsWithoutPermissionKeepsFallbackUsable` on the same emulator.
- expected: The no-fix prompt can be dismissed and the existing permission-denial fallback case completes.
- actual: The first no-fix run reached the prompt and fallback assertions but Espresso could not click the button while animations were enabled. The single retry did not find the expected prompt title. The permission-denial test process was killed by Android 16 when its helper revoked runtime permissions; the same permission-denial case passed on an Android 14 emulator. The no-fix continue interaction remains unverified.
- evidence:
  - `docs/tasks/debug1016-1/execution-report.md`
  - `docs/tasks/debug1016-1/verification.md`
  - Emulator logcat at 2026-10-16 16:45:02: ActivityManager killed `com.example.taoyuangutter` with reason `permissions revoked`.
- Android 14 emulator run: `returningFromSettingsWithoutPermissionKeepsFallbackUsable` passed (`OK (1 test)`).
- next_action: infrastructure
- owner: verifier
