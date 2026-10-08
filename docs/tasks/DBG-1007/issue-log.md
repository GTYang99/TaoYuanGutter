# Issue Log

## ISS-DBG-1007-LOC-001

- Task: DBG-1007
- Category: enhancement_request (addressed by approved FR-001)
- Priority: P3
- Title: Missing waypoint coordinates use a fixed form-map fallback
- Status: closed
- Evidence: `GutterFormActivity.onMapReady()` explicitly selects `LatLng(24.9929, 121.3011)` when the edited waypoint lacks coordinates; it does not use the host location passed in the intent. The separate main map attempts its own asynchronous location recentering.
- Impact: A point without coordinates opens its form map at the fixed Taoyuan-area location even when the shared main map has recentered on the device.
- Next action: closed
- Resolution: Approved edit-only initial-location behavior was implemented and exercised on the emulator; missing-coordinate edits now use a usable fix only for the form-map camera and retain the Taoyuan fallback on failure. See `execution-report.md` and the AC-001/AC-003 emulator evidence.

## ISS-DBG-1007-LOC-002

- Task: DBG-1007
- Category: environment
- Priority: P2
- Title: Emulator location input does not demonstrate the edit-map Fused Location path
- Status: closed
- Evidence: The earlier `adb emu geo fix` and direct Fused mock probes did not establish the app callback path. After the user authorized selecting the Android test APK, `com.example.taoyuangutter.test` was selected in Developer Options and Android reported `MOCK_LOCATION: allow`. A test-APK Java `BroadcastReceiver` then added a framework GPS test provider. AC-001/003 passed with the injected fix at `25.030000,121.500000`, confirming the form camera moved while node/form coordinates remained unchanged (`emulator-results/AC-001-AC-003-fused-mock.md`). AC-009 passed after manual pan and later fix injection (`emulator-results/AC-009-delayed-fused-callback.md` and XML/logcat evidence).
- Impact: The original emulator input path could not distinguish GMS delivery from an application-side request issue. The selected test-APK framework provider now supplies a reproducible mock fix for developer emulator validation.
- Resolution: Resolved the test-environment blocker with a selected test APK and framework GPS test provider; confirmed both the successful callback and delayed-callback behavior. This issue is closed. Independent verification and CI remain workflow gates.
- Next action: closed

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

## ISS-DBG-1007-LOC-004

- Task: DBG-1007
- Category: environment
- Priority: P2
- Title: Emulator no-fix state was not established for independent verification
- Status: closed
- Evidence: The initial independent run was affected by Fused's cached mock fix. After a normal reboot (no data wipe), `dumpsys location` showed null last locations for fused, GPS, and network providers. `missingCoordinateEditWithoutUsableFixKeepsFallbackAndShowsUnavailablePrompt` then passed on `emulator-5554`; the connected test XML reports one test, zero failures, and 54.298 seconds. Assertions confirmed the flow completed, unavailable prompt appeared, fallback remained, and waypoint/form coordinates stayed empty. Source review confirms a 25-second timeout per attempt and one retry maximum.
- Impact: The emulator cache had prevented clean no-fix verification; AC-006 is now verified.
- Resolution: Rebooted the emulator and reran the targeted no-fix instrumentation case successfully.
- Next action: closed
