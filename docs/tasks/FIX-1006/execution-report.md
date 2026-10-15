# Execution Report

## Change

`AddGutterBottomSheet` now forwards map-area touch events using a copied `MotionEvent` offset from the Dialog decor's screen origin into the Activity decor's coordinate space. Sheet-originated events continue through the original Dialog callback. Route selection remains fixed from ACTION_DOWN until ACTION_UP or ACTION_CANCEL.

- Branch: `fix/FIX-1006-gutter-map-drag`
- Implementation commit: `f503217`

## Validation

| Check | Result | Evidence |
|---|---|---|
| `git diff --check` | PASS | No whitespace errors reported |
| `PATH="/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin:$PATH" ./gradlew :app:compileDebugKotlin` | PASS | `BUILD SUCCESSFUL`; only existing `adapterPosition` deprecation warnings in `AddGutterBottomSheet.kt` |
| `PATH="/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin:$PATH" ./gradlew :app:testDebugUnitTest --tests 'com.example.taoyuangutter.map.NoDitchPointHitTesterTest' :app:assembleDebug` | PASS | Targeted map hit-test unit suite: 2 passed, 0 failed; debug APK assembled |
| AC-001 physical map drag | PASS for shared `AddGutterBottomSheet` routing; edit-mode-specific check NOT VERIFIED | On the attached phone, injected a horizontal drag in the visible map region (`x=250,y=330` to `x=760,y=330`, 650 ms); map content shifted and sheet stayed open. The displayed sheet title was `新增側溝`, so this confirms the shared touch callback, not an existing-gutter edit session. |
| AC-002 physical sheet controls | PASS for shared sheet controls; edit-mode-specific check NOT VERIFIED | Tapped the curve-type radio and restored the default type; opened the start-waypoint editor and backed out without changing fields or submitting. The displayed sheet was in add mode. |

## Physical Device Record

- Revision under test: `7c6db3b` (contains implementation commit `f503217`)
- Device serial: `adb-QV710EDR3A-hF5XZF._adb-tls-connect._tcp`
- Device model: `XQ-AU52`
- Android version: `12`
- Package: `com.example.taoyuangutter`
- Preconditions: app installed with `adb install -r`; user logged in using the already-saved account; no gutter was submitted
- Scope: AC-001 and AC-002 only

## Limitations and Next Action

The exact pre-existing-gutter edit mode was not reached; the phone displayed the add mode of the same `AddGutterBottomSheet` component. The tested touch callback is shared across modes, but edit-mode-only behavior remains `NOT VERIFIED`. Independent Verification should confirm the edit mode if a selectable existing gutter is available. The unit test covers nearby map hit testing, not the cross-window event offset itself.
