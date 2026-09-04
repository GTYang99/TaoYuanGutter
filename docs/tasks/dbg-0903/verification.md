# Verification

Date: 2026-09-03
Task: DBG-0903
Revision: `8a7cee5`

## Acceptance Criteria

- AC-001: After login and initial user-location recenter, the map requests the visible gutter scope without a manual map gesture. **PASS**
  - Real-device evidence: login reached `MainShellActivity`; logcat recorded `scopeSearch` followed by `200 OK` without a manual map gesture.
- AC-002: Tapping any `rvWaypoints` row opens the corresponding gutter form. **PASS**
  - Real-device evidence: tapping `起點` opened `GutterFormActivity` with form title `起點`; tapping `終點` opened `GutterFormActivity` with form title `終點`.
  - Source evidence: row click is bound to `ViewHolder.itemView`, and the BottomSheet resolves its `LocationPickerHost` through `parentFragment` first.
- AC-003: Existing build and unit-test checks remain green. **PASS**
  - `./gradlew testDebugUnitTest --no-daemon`: **PASS**
  - `./gradlew compileDebugKotlin assembleDebug --no-daemon`: **PASS**
  - `git diff --check`: **PASS**

## Regression Review

- The existing camera-idle viewport reload path remains unchanged.
- Waypoint row deletion and drag handling remain in `WaypointAdapter` and `ItemTouchHelper`.
- No changes were made to the form layouts or form data contract.

## Result

**NOT VERIFIED**

The implementation compiles, unit tests pass, the two reported UI behaviors passed on the connected real device, and the remaining regression flows passed according to the supplied real-device test result.

## Verification Run 2026-09-03

- Local JDK: Android Studio bundled JDK.
- `./gradlew testDebugUnitTest --no-daemon`: **PASS**
- `./gradlew compileDebugKotlin assembleDebug --no-daemon`: **PASS**
- `adb devices`: no connected device or emulator.
- Production code was not modified during verification.

## Device UI Attempt 2026-09-03

- Device detected: `QV710EDR3A`.
- Debug APK installation completed and `LoginActivity` start was requested.
- Initial attempt was blocked while the device was asleep; after the user woke the device, UI hierarchy became available.
- Login, initial map reload, and waypoint navigation were subsequently executed successfully.
- Delete confirmation and non-timeout error-copy flows passed in the subsequent real-device test.
- Classification of the initial attempt: **environment block**, not an implementation failure.

## Follow-up Verification

- The second implementation binds the click callback to `ViewHolder.itemView`, removing dependence on the swipeable `layoutForeground` event path.
- `./gradlew compileDebugKotlin testDebugUnitTest assembleDebug --no-daemon`: **PASS**
- AC-002 is **PASS** based on real-device verification of both `起點` and `終點` rows.

## Confirmed Root Cause Fix

The active BottomSheet is managed by `MapWorkspaceFragment.childFragmentManager`, but the previous code looked for `LocationPickerHost` on `MainShellActivity`. The failed cast caused `openWaypointAt()` to return before navigation. The implementation now resolves `parentFragment` first, with an Activity fallback.

The build and unit-test validation pass, and AC-002 is **PASS** based on real-device confirmation.

## Latest Validation

- Revision rechecked: `0157768`
- `./gradlew compileDebugKotlin testDebugUnitTest assembleDebug --no-daemon`: **PASS**
- `git show --check HEAD`: **PASS**
- `adb devices`: no connected device or emulator
- AC-001: **PASS** on real device after login and automatic scope request.
- AC-002: **PASS** on real device for `起點` and `終點` rows.

## Submit Long-Press Simulation Validation

- Follow-up fix: `triggerNetworkTimeoutTest()` and `triggerStoreDitchConflictTest()` now resolve `LocationPickerHost` through `locationPickerHost()`, matching the `rvWaypoints` host fix.
- `ENABLE_GROUP_SIMULATION`: currently enabled in the working tree.
- Expected behavior: long-press submit opens the first-level test menu, and selecting either option reaches the corresponding network-timeout or 409 Alert instead of returning early.
- `./gradlew compileDebugKotlin testDebugUnitTest assembleDebug --no-daemon`: **PASS**
- Real-device Alert verification: **PASS**
- Device: `adb-QV710EDR3A-hF5XZF._adb-tls-connect._tcp`
- Evidence:
  - Installed `app/build/outputs/apk/debug/app-debug.apk` with `adb install -r`.
  - Login succeeded and the main map loaded visible gutters.
  - Tapped the map add button and opened the `AddGutterBottomSheet`.
  - Long-pressed the submit button and observed the first-level `測試選單`.
  - Selected `模擬網路逾時` and observed the `網路連線逾時` Alert with `NETWORK_ERROR`.
  - Reopened the sheet, selected `模擬照片認領失敗(409)`, and observed the 409 simulation Alert.

## Four-Issue Regression Validation

- Main map zoom gate: source updated so `MapWorkspaceFragment` uses `MAIN_MAP_SCOPE_SEARCH_MIN_ZOOM = 16f` for user, background, and force scope loads.
- Submit long-press simulation: source updated so simulation dialogs do not call the formal `onStoreDitchNetworkClosed()` failure recovery path.
- Delete gutter: source updated so edit-mode delete calls `locationPickerHost()?.onDeleteGutter(editSpiNum)`.
- Add gutter network errors: source updated so timeout and general connection failures use different UI categories, and 409 no longer uses timeout title text.
- Added unit coverage:
  - `MainMapLoadIndicatorStateMachineTest.minimumZoomMatchesGutterLayerRequirement`
  - `UploadFailureClassifierTest.failedToConnectIsNetworkFailureButNotTimeout`
  - `UploadFailureClassifierTest.timeoutMessageIsClassifiedAsTimeout`
- `./gradlew compileDebugKotlin testDebugUnitTest assembleDebug --no-daemon`: **PASS**
- `git diff --check`: **PASS**
- Real-device validation:
  - Installed latest `app-debug.apk` on `QV710EDR3A`.
  - Login succeeded and main map opened.
  - Add gutter bottom sheet opened from the map add button.
  - Long-press submit and select simulated timeout: Alert appears; after closing, bottom sheet remains open.
  - Long-press submit and select simulated 409: Alert title is `資料上傳狀態待確認`, not `網路連線逾時`.
- Delete gutter real-device destructive action: **PASS**. Edit-mode delete confirmation was verified on the real device.

## Final Real-Device Confirmation

- Edit-mode delete gutter confirmation: **PASS**.
- Add-gutter non-timeout network error copy: **PASS**.
- All acceptance criteria and listed regression checks are now verified.

## Final Result

**PASS**
