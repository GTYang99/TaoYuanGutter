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
- title: Maps SDK test credential blocks no-fix map interaction verification
- status: open
- impact: AC-005 map interaction runtime evidence is incomplete; the task cannot advance to Release.
- repro_steps:
  - Run `missingCoordinateEditWithoutUsableFixKeepsFallbackAndShowsUnavailablePrompt` on `emulator-5554` with location disabled.
  - Retry once after disabling emulator animations.
  - Run `returningFromSettingsWithoutPermissionKeepsFallbackUsable` on the same emulator.
- expected: The no-fix prompt can be dismissed and the fallback map responds to a manual swipe.
- actual: The initial Continue click failed because the emulator had a stale injected pointer. After reboot, prompt dismissal and fallback assertions passed, but the test failed at the manual-pan assertion. Google Maps SDK logcat reports an `INVALID_ARGUMENT` token request and authorization failure for the deliberately non-real `debug-placeholder-not-a-real-key`. A valid authorized test Maps SDK key is unavailable. The separate permission-denial case was killed by Android 16 when its helper revoked runtime permissions; the same case passed on an Android 14 emulator.
- evidence:
  - `docs/tasks/debug1016-1/execution-report.md`
  - `docs/tasks/debug1016-1/verification.md`
  - 2026-10-16 post-reboot Infrastructure rerun: `missingCoordinateEditWithoutUsableFixKeepsFallbackAndShowsUnavailablePrompt` passed Continue/dismissal and fallback assertions but failed the manual-pan assertion at line 128.
  - Emulator logcat at 2026-10-16 17:11: Maps API token request returned `INVALID_ARGUMENT`; Google Maps Android SDK logged authorization failure for the non-real placeholder key.
  - The first attempt logged InputDispatcher `Invalid DOWN event - pointers already down`; emulator reboot cleared it and the button click then succeeded.
  - Emulator logcat at 2026-10-16 16:45:02: ActivityManager killed `com.example.taoyuangutter` with reason `permissions revoked`.
- Android 14 emulator run: `returningFromSettingsWithoutPermissionKeepsFallbackUsable` passed (`OK (1 test)`).
- next_action: infrastructure
- owner: verifier
