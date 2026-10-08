# Issue Log

## ISS-DBG-1007-LOC-001

- Task: DBG-1007
- Category: enhancement_request (classification: current behavior confirmed; defect status depends on product requirement)
- Priority: P3 pending product impact confirmation
- Title: Missing waypoint coordinates use a fixed form-map fallback
- Status: open
- Evidence: `GutterFormActivity.onMapReady()` explicitly selects `LatLng(24.9929, 121.3011)` when the edited waypoint lacks coordinates; it does not use the host location passed in the intent. The separate main map attempts its own asynchronous location recentering.
- Impact: A point without coordinates opens its form map at the fixed Taoyuan-area location even when the shared main map has recentered on the device.
- Next action: investigation

## ISS-DBG-1007-LOC-002

- Task: DBG-1007
- Category: unknown
- Priority: P2
- Title: Emulator location input does not demonstrate the edit-map Fused Location path
- Status: open
- Evidence: On `emulator-5554` (Medium_Phone AVD, Android 14 / API 34), `adb emu geo fix 121.5000 25.0300` returned `OK`; `dumpsys location` showed a mock location in the fused provider, but the Activity camera remained at the Taoyuan fallback. A test-only `FusedLocationProviderClient.setMockMode` probe was rejected by Android because the caller was not selected as the mock location app, including after the target package was written to the secure setting. The separate AC-009 mock harness did not reach its behavior assertion: its first attempt lacked an Application context and the one retry lacked Google Play Services version metadata in the test APK. AC-002, AC-010, AC-011 and the no-fix flow passed independently.
- Impact: AC-001, AC-003, and AC-009 still lack the required successful Fused callback evidence. The emulator mock path cannot distinguish GMS delivery from an application-side request issue.
- Next action: investigation

## ISS-DBG-1007-LOC-003

- Task: DBG-1007
- Category: environment
- Priority: P2
- Title: Native permission prompts need shell-level emulator control for repeatable UI checks
- Status: closed
- Evidence: On Android 14 / API 34, Espresso alone cannot interact with `com.google.android.permissioncontroller.permission.ui.GrantPermissionsActivity`. Shell tapping the native denial button on the fixed 1080x2400 AVD, followed by Espresso for app dialogs, completed AC-004 and the AC-005/AC-011 Settings-return cases. Results: `emulator-results/AC-004.xml` and `emulator-results/AC-011-denied.xml`.
- Impact: No remaining acceptance-criterion blocker from native permission-dialog interaction.
- Resolution: Added a fixed-AVD shell interaction helper to the instrumentation case; verified Settings opens and returning without permission retains the fallback. The Settings-granted return path is captured in `emulator-results/AC-011-granted.xml`.
- Next action: closed
