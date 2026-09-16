# Verification Report

## Inputs
- Requirement, analysis, plan, state, git diff, existing tests, `ai/verification-rules.md`, and `ai/testing-rules.md` reviewed.
- Verified revision: `6838499203c1bc8ed55b79e3c9f65b6537888765` (`6838499`). Production changes under review include `48b8756`.
- The primary worktree had an unrelated uncommitted change in `GutterApiService.kt`, so executable validation used a clean detached worktree at the verified revision. The primary `HEAD` did not change during validation.

## Physical Device Context

```yaml
revision: "6838499203c1bc8ed55b79e3c9f65b6537888765"
device:
  serial: "emulator-5554"
  model: "Medium_Phone (AVD)"
  android_version: "14"
app:
  package: "com.example.taoyuangutter"
  build_variant: "debug"
```

The initial isolated build used a placeholder `MAPS_API_KEY`; it was then rebuilt using a symlink to the existing local configuration without exposing or copying the key. The offline editor path was executable on the emulator. The login-dependent list path was not available.

## Acceptance Criteria

| AC | Steps | Result | Actual Result | Evidence | Retry Count |
|----|-------|--------|---------------|----------|-------------|
| AC-001 | Open each target panel and inspect controls | NOT VERIFIED | The offline editor showed the existing main button at y=305–430, above the sheet at y=702. The list source was not available without login. | Android 14 UI dump; `activity_main.xml`; `MainBlockingUiController.kt` | 0 |
| AC-002 | Tap the main button from each target panel | FAIL | In the offline editor, tapping the main button at (975, 368) did not dismiss the sheet or show measurement. One permitted retry had the same result. | Android 14 UI dump after both attempts; `editor-measure-fail.png`; `AddGutterBottomSheet.kt` | 1 |
| AC-003 | Open list, measure, exit and use Back | NOT VERIFIED | Static review confirms list opening hides scope lines, preserves the working layer, restores scope from current `showPlan`, and restores the retained list sheet. Map interaction and Back flow were not executed. | `MapWorkspaceFragment.kt` | 0 |
| AC-004 | Open editor, measure, exit and use Back | FAIL | Because the only editor measurement entry did not enter measurement after both attempts, the required editor-source hide, preservation, exit, and Back flow could not occur. | Same AC-002 emulator evidence; `MainActivity.kt` entry path | 1 |
| AC-005 | Exercise reset, close, listener recovery, drafts, blocking, and control restoration | NOT VERIFIED | `exitMeasureMode()` reinstalls the normal map-click listener and the existing regression suite passed, but changed measurement behaviours have no focused automated coverage and were not manually executed. | 74 unit tests; 34 instrumentation tests; source review | 0 |

## Regression
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew --no-daemon --console=plain :app:testDebugUnitTest :app:assembleDebug`: PASS in the clean detached worktree.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ANDROID_SERIAL=emulator-5554 ./gradlew --no-daemon --console=plain :app:connectedDebugAndroidTest`: PASS, 34 tests, 0 failures, 0 errors on Android 14.
- The suite covers existing form, draft, authentication, and shell regressions, but no test references `btnMeasureDistance`, `hideForMeasure`, `showAfterMeasure`, or `setTargetPanelVisible`.

## Issues
- `ISS-FEAT-0916-1-008`: the editor sheet's external main-button entry does not transition into measurement. This is an implementation failure for AC-002 and AC-004.

## Validation Limitations
- CI result is not available in the repository state; both CI gates remain pending.
- The login-dependent list flow was not available, so AC-001, AC-003, and list-source portions of AC-005 remain unexecuted.
- No committed focused test covers the changed flow.

## Failure Classification
- `implementation`

## Next Action
- `debug`

---

## Final Result

FAIL

---

## Re-verification — `eef13b8`

The Debug fix was reviewed at committed revision `eef13b8cec004d5adeaf302717ba83a37eb1a72f` in a new clean detached worktree. It changes both sheets to classify external touches using the Material `design_bottom_sheet` bounds.

### Executed Evidence

- `:app:testDebugUnitTest :app:assembleDebug`: PASS, 74 unit tests with 0 failures.
- Device: `emulator-5554`, Medium_Phone (AVD), Android 14.
- Offline editor: opened the sheet, then tapped the main `btnMeasureDistance` at `(975, 368)`, above the sheet. The sheet remained visible and no `measurePanel` appeared. One retry produced the same result.
- Screenshot: `/private/tmp/tyg-feat-0916-1-verification-r2/editor-measure-fail.png`.

### Acceptance Criteria Update

| AC | Result | Actual Result |
|----|--------|---------------|
| AC-002 | FAIL | The only editor measurement entry still does not receive the main-map button tap after the routing fix. |
| AC-004 | FAIL | Editor-source measurement cannot start, so hide/restore and Back behavior cannot execute. |
| AC-001 | NOT VERIFIED | The editor source was visible, but the login-dependent list source was unavailable. |
| AC-003 | NOT VERIFIED | List-source flow was unavailable without authenticated test access. |
| AC-005 | NOT VERIFIED | The changed measurement flow cannot progress to reset, close, and listener-recovery checks. |

### Failure Classification

`implementation`

### Next Action

`debug`

## Re-verification Final Result

FAIL

## Verification Response

The AC-002/AC-004 FAIL classification is withdrawn. The recorded tap `(975, 368)` missed the visible measurement button; the screenshot places the button approximately at x=923–1038 and y=98–198. The tap was in the map area, so the external-touch route was not exercised. AC-002 and AC-004 are `NOT VERIFIED`, not implementation failures. The next run must record the actual button bounds and tap its center (approximately `(975, 148)`).
