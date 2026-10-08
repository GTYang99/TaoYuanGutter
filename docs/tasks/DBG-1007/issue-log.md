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
- Title: Emulator mock fix does not demonstrate the edit-map current-location path
- Status: open
- Evidence: On `emulator-5554` (Medium_Phone AVD, Android 14 / API 34), `adb emu geo fix 121.5000 25.0300` returned `OK`; `dumpsys location` showed a mock location at that point in the fused provider. The edit-map instrumentation assertion nevertheless observed the unchanged Taoyuan fallback, about 20.5 km away. The full class run and one isolated retry both reproduced this result. App permissions were granted in those runs. AC-002 independently passed after both permissions were revoked.
- Impact: AC-001 and AC-003 cannot yet be verified, and current evidence cannot distinguish emulator/Google Play Services mock-location behavior from an application-side location request issue.
- Next action: investigation

## ISS-DBG-1007-LOC-003

- Task: DBG-1007
- Category: environment
- Priority: P2
- Title: Native permission prompts need shell-level emulator control for repeatable UI checks
- Status: open
- Evidence: On Android 14 / API 34, Espresso alone could not interact with `com.google.android.permissioncontroller.permission.ui.GrantPermissionsActivity`. A manual emulator procedure using `uiautomator dump` plus `adb shell input tap` successfully completed AC-004. AC-005 and AC-011 have not been exercised with that procedure.
- Impact: AC-005 and AC-011 remain unverified; the current permission tests need a reliable system-dialog interaction mechanism for repeatable automation.
- Next action: infrastructure
