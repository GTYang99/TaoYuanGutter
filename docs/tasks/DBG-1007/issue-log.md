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
- Title: Emulator automation cannot complete the system location-permission prompt
- Status: open
- Evidence: On Android 14 / API 34, the system displayed `com.google.android.permissioncontroller.permission.ui.GrantPermissionsActivity`. The first Espresso attempt had no resumed app Activity; one accessibility-based retry could not find the system “Don't allow” action. The emulator package was removed after the run, restoring its permission state.
- Impact: AC-004, AC-005, and AC-011 cannot be fully verified with the current instrumentation setup.
- Next action: infrastructure
