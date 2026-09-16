# Verification Report

## Inputs
- Reviewed `AGENTS.md`, requirement, analysis, plan, state, approved Debug records, git diff, existing tests, and verification/testing rules.
- Production revision under test: `eef13b8cec004d5adeaf302717ba83a37eb1a72f`. The later `0523013` changes verification records only.
- The primary worktree contains an unrelated uncommitted `GutterApiService.kt` change. Executable validation used a clean detached worktree at `eef13b8`.

## Physical Device Context

```yaml
revision: "eef13b8cec004d5adeaf302717ba83a37eb1a72f"
device:
  serial: "emulator-5554"
  model: "Medium_Phone (AVD)"
  android_version: "14"
app:
  package: "com.example.taoyuangutter"
  build_variant: "debug"
```

The isolated worktree references the existing local Maps configuration without copying or exposing its value. The emulator could exercise the offline editor, but no authenticated list-flow access was available.

## Acceptance Criteria

| AC | Steps | Result | Actual Result | Evidence | Retry Count |
|----|-------|--------|---------------|----------|-------------|
| AC-001 | Open both target panels and inspect controls | PASS | The editor and authenticated `新增側溝清單` each displayed the existing main measurement button above the panel; no in-panel proxy was present. | Android 14 UI dumps; `list-open.png` | 0 |
| AC-002 | Tap the main button from both target panels | PASS | Corrected editor tap `(975, 148)` entered measurement and hid the editor sheet. From the authenticated list, the main button at x=913–1038, y=21–146 entered measurement and hid the list. | Android 14 UI dumps; `r3-before-tap.png`; `r3-after-tap.png`; `list-measure-open.png` | 0 |
| AC-003 | Run list measurement and restore with close/Back | FAIL | From authenticated `新增側溝清單`, two map taps displayed `90 公尺`. Android Back then returned to the Android launcher instead of restoring the retained list sheet and scope state. A controlled retry after a fresh login reproduced the same result. | Android 14 UI dumps: `90 公尺`; `list-measured.png`; post-Back launcher UI dumps | 1 |
| AC-004 | Run editor measurement and restore same data | NOT VERIFIED | A new offline editor scenario created start/end virtual waypoints through the normal form and map-point-picker paths, with a map pan before confirming the end point. After zooming to 16 or higher, two map taps displayed `13 公尺`; Android Back restored the same `離線草稿` editor with both waypoint rows. The measured dashed line was visible, but the pre-existing working polyline was not rendered in this offline virtual-waypoint scenario, so the full working-line preservation requirement remains unproven. | Android 14 UI dumps; `zoom16-measured.png`; `zoom16-restored.png` | 0 |
| AC-005 | Verify reset, close, listener, drafts, blocking, and controls | NOT VERIFIED | Editor distance display, reset, Android Back, and explicit `關閉測距模式` each restored its two waypoint rows. The list-source Android Back regression fails the related restoration/control expectation; remaining listener, draft recreation, blocking, and normal-editor checks are unexecuted. | 74 unit tests; prior 34-test instrumentation regression; Android 14 UI dumps | 0 |

## Regression
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew --no-daemon --console=plain :app:testDebugUnitTest :app:assembleDebug`: PASS, 74 unit tests, 0 failures.
- Corrected manual editor smoke: PASS for main-button entry, sheet hide, two-point distance display (`646 公尺`), reset, and Android Back restoration.
- Additional non-empty offline-editor smoke: PASS for preserving the start/end waypoint rows across main-button entry and Android Back. The map-point picker was used to pan and confirm the end point; no submit or network operation was performed.
- At zoom 16 or higher, two map taps displayed `13 公尺` and the measurement dashed line was visible. Android Back removed the measurement UI and restored the editor sheet with both rows.
- The emulator was force-stopped and reopened through the normal launcher and offline entry. It loaded the map and scope lines, but this does not provide the authenticated list source: the app has no local auth-preference file, and offline mode explicitly ignores scope-polyline inspection taps.
- Authenticated list smoke: `新增側溝清單` opened from the main `+` action. Its main measurement button entered measurement and two map taps displayed `90 公尺`. Android Back incorrectly exited to launcher rather than restoring the list. A fresh-login controlled retry produced the same `90 公尺` result and the same exit to launcher.
- The existing 34-test instrumentation suite passed on the same emulator in the prior fixed-revision validation, but it does not cover this source-panel measurement flow and is not used as direct AC evidence.

## Issues
- `ISS-FEAT-0916-1-008` was resolved as a verification-evidence error: the previous `(975, 368)` tap missed the real button. The corrected `(975, 148)` tap passed the editor entry case.
- `ISS-FEAT-0916-1-010` is open: list-source Android Back exits the app instead of restoring the list.

## Validation Limitations
- The emulator's app storage contains only Maps preferences and local draft data; it has no separate login-preference file. This was checked by listing filenames only, without reading credential contents.
- The offline virtual-waypoint scenario did not render a pre-existing working polyline even though its start/end rows remained present. A normal editor with a visible working polyline is still required to prove the complete AC-004 preservation condition.
- CI results remain pending in task state.

## Failure Classification
- `implementation`

## Next Action
- `debug`

## Final Result

FAIL

## Re-verification — `a83f745`

### Fixed Revision

- Commit: `a83f745ab05473b382e51b374b05fc59e3fb922b` (`a83f745`, `fix(feat-0916-1): prioritize measurement back handling`).
- Validation worktree: clean detached `/private/tmp/tyg-feat-0916-1-verification-r3` at that commit.
- `:app:testDebugUnitTest :app:assembleDebug`: PASS; generated unit-test XML reports contain no failures or errors.

### AC-003 Re-test

1. Installed the debug APK built from `a83f745` on Android 14 `emulator-5554`.
2. Logged in through the normal app UI, opened `新增側溝清單` using the main `+` action, and recorded the list Fragment instance.
3. Tapped the visible main measurement button, selected two map points, and observed `90 公尺`.
4. Sent Android Back.

**Actual:** the focused window became `NexusLauncherActivity`; `AddGutterListBottomSheet` was not restored. The result reproduces the prior AC-003 failure on the fixed revision.

**Result:** FAIL. `ISS-FEAT-0916-1-010` remains open and routes back to Debug.

Evidence: `ac003-list-open.png`, `ac003-measured.png`, and `ac003-restored.png` under the isolated verification worktree.

## Re-verification — `0c5107d`

### Fixed Revision and Environment

- Commit under test: `0c5107d2cf1705401fb6c550cc42ed55e896e925` (`0c5107d`, `fix(feat-0916-1): prioritize measurement back callback`).
- Worktree: clean detached `/private/tmp/tyg-feat-0916-1-verification-r4`; no tracked source files changed during the test.
- Device: `emulator-5554`, `Medium_Phone (AVD)`, Android 14; package `com.example.taoyuangutter`, debug variant.
- Preconditions: user completed normal login; opened `新增側溝清單` from the main `+` action.

### AC-003 Focused Re-test

1. With `新增側溝清單` open, tapped the exposed main-map `測量距離` control at `(975, 84)`.
2. Confirmed the list hid and the measurement panel appeared while the focused window remained `MainShellActivity`.
3. Sent Android Back (`adb -s emulator-5554 shell input keyevent 4`).
4. Confirmed the focused window remained `MainShellActivity` and the same `新增側溝清單` UI was restored, including its close and add controls.

**Back-restoration subcase: PASS.** This directly resolves the behavior reported in `ISS-FEAT-0916-1-010`; Android Back no longer exits to the launcher.

**AC-003 overall: NOT VERIFIED.** The authenticated list available for this run did not expose pre-existing scope lines or working-layer segments/nodes. Their hide/preserve/restore behavior, including reconciliation with the latest `showPlan` preference, therefore remains unproven.

## Current Final Result

NOT VERIFIED — AC-003’s Android Back failure is resolved at `0c5107d`; remaining AC-003 layer-state evidence, AC-004’s pre-existing working polyline, AC-005’s outstanding regressions, and CI are still required before Release.

## Emulator Verification Round — `0c5107d`

### Environment

- Revision: `0c5107d2cf1705401fb6c550cc42ed55e896e925`, clean detached worktree `/private/tmp/tyg-feat-0916-1-verification-r4`.
- Device: `emulator-5554`, `Medium_Phone (AVD)`, Android 14; package `com.example.taoyuangutter`, debug variant.
- Login: completed through the normal UI. The authenticated `新增側溝清單` contained no rendered item, scope line, or working-layer node available for inspection.

### AC Results

| AC | Result | Emulator evidence |
|----|--------|-------------------|
| AC-001 | PASS | With the authenticated list and the editor each open, `dumpsys activity top` showed only `btnMeasureDistance` visible/enabled; logout, add, layers, drafts, legend, report and location controls were `GONE`. UI dumps contained no in-panel proxy measure control. |
| AC-002 | PASS | From list and editor, the exposed main-map button at `(975, 84)` entered measurement without dismissing either source. The resulting panel displayed `距離`, `重設起點`, and `關閉測距模式`. |
| AC-003 | NOT VERIFIED | The list hid for measurement and Android Back restored `新增側溝清單` while focus stayed in `MainShellActivity`. However, the authenticated data set had no existing scope or working-layer lines/nodes, so their immediate hiding, preservation, and latest-`showPlan` restoration could not be observed. |
| AC-004 | NOT VERIFIED | Editor-source measurement hid the editor, map input showed `45 公尺` then `43 公尺` after reset, and both explicit close and Android Back restored the same editor. The editor had empty start/end rows, so retention of a pre-existing working line/node is unproven. |
| AC-005 | NOT VERIFIED | Reset returned the panel to `點擊地圖設定起點`; close and Back restored the source panel. Closing the list restored all normal map controls, and the restored Layers control opened and closed normally. Emulator instrumentation passed `MainShellActivityTest` (12/12: Back precedence, independent multi-drafts, list close/finalization, blocking-indicator state) and `GutterFormExitUiTest` (7/7: incomplete-form warnings and permitted completed/preview exits). Listener recovery after a normal editor carrying existing working geometry, upload-blocking interaction, and full draft recreation through the production map flow remain unexecuted. |

### Instrumentation Evidence

- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ANDROID_SERIAL=emulator-5554 ./gradlew --no-daemon --console=plain :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.MainShellActivityTest`: PASS. XML: 12 tests, 0 failures, 0 errors.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ANDROID_SERIAL=emulator-5554 ./gradlew --no-daemon --console=plain :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.GutterFormExitUiTest`: PASS. XML: 7 tests, 0 failures, 0 errors; exit-code file `0`.
- A prior unrestricted full-suite attempt did not produce a usable final report after the test environment removed the app and test package. It is excluded from PASS evidence.

### Final Result

NOT VERIFIED. AC-001 and AC-002 pass on the emulator. AC-003 through AC-005 still require a test account or prepared local scenario with visible pre-existing scope/working geometry; AC-005 also requires the remaining production-flow blocking/listener checks. CI is pending.

## Zhongli Data Re-test — `0c5107d`

### Environment and Data Load

- Same fixed revision and detached verification worktree as the preceding emulator round.
- Android 14 `emulator-5554`; mock location set to `24.9537, 121.2240` (Zhongli).
- App `現在位置` action issued `GET /api/v1/map/scopeSearch?minLat=24.951731010701675&maxLat=24.955672082391796&minLng=121.22289743274452&maxLng=121.22510455548763`; response was HTTP 200.
- Before opening the list, the map visibly rendered multiple green and red existing gutter polylines around Zhongli Station.

### AC-003 Re-test: FAIL

1. With the loaded existing gutter polylines visible, opened `新增側溝清單` through the main `+` action.
2. Captured the list-open map state.

**Expected:** all existing scope gutter polylines hide immediately when the list opens.

**Actual:** the same green and red existing gutter polylines remained visible in the map area above the list sheet.

Evidence: `/private/tmp/zhongli-before-list.png` (lines visible before opening the list) and `/private/tmp/zhongli-list-open.png` (same lines still visible with `新增側溝清單` open).

Classification: `implementation`; issue `ISS-FEAT-0916-1-011`; route: Debug.
