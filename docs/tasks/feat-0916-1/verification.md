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
