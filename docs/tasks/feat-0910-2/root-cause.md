# Root Cause Analysis: ISS-0910-2-09

## Confirmed behavior

- The failure occurs in `GutterBasicInfoUiTest.newFormShowsRequiredOrderLabelsButtonsAndDefaults` on Sony XQ-AU52.
- `GutterFormActivity` reaches `RESUMED`, then transitions to `PAUSED` and `STOPPED` about 66 ms later.
- There is no application `FATAL EXCEPTION`, `finish()` log, or stack trace identifying an application crash.
- Espresso waits for the first assertion until the 46-second timeout, after which `ActivityScenario` reports the activity as `DESTROYED` and raises `NoActivityResumedException`.
- The same test uses an activity with a full-screen `SupportMapFragment` and a translucent `FormSheet` theme. Existing-value and `GutterCantOpenUiTest` scenarios do not reproduce this exact failure.

## Root-cause status

Root cause identified: `GutterFormActivity` does not complete its launch lifecycle before the Sony XQ-AU52 system's top-resumed timeout. The synchronous `setContentView()` path inflates the full form and the XML-declared `SupportMapFragment`; the map's `onCreateView()` blocks the main thread while the Activity is still starting. Android then reports `Activity top resumed state loss timeout` and `Activity pause timeout`, moves the form out of the foreground, and Espresso later reports `NoActivityResumedException`.

This is an application startup performance/lifecycle regression, not an app `finish()` call or the `onPause()` draft-sync path. The translucent form theme increases the consequence because the form is expected to remain a foreground overlay, but changing that theme is not required by the root-cause evidence.

## Latest isolation result (2026-09-11)

- The same targeted test was executed twice on the same `XQ-AU52 - 12` device and the same committed revision `4aa4d06`.
- Run 1 passed: `1 test, 0 failures`, completed in about 5 seconds.
- Run 2 failed: `1 test, 1 failure`, `NoActivityResumedException` after 45.5 seconds.
- The failing run again reached `RESUMED`, then `PAUSED/STOPPED` about 63 ms later; `DESTROYED` occurred during failure cleanup.
- Both runs show `SupportMapFragment.onCreateView()` blocking the main thread for about 203–226 ms. The pass run remained resumed long enough for the assertions; the fail run did not.
- No app `FATAL EXCEPTION`, explicit `finish()`, or logged second application Activity was captured.

The earlier pass/fail variation is explained by whether the device completed the startup work before the top-resumed deadline. The minimum safe production fix is to keep form UI startup independent from map creation: remove automatic XML map-fragment inflation and schedule `SupportMapFragment` creation/`getMapAsync()` after the form Activity has reached its first resumed frame. The map remains part of the product UI; only its startup timing changes.

## Affected acceptance criteria

- AC-001, AC-002, AC-003, AC-005: assertions cannot run because the new-form activity is not resumed.
- AC-006: the connected regression suite fails even though the affected `GutterCantOpenUiTest` cases pass.

## Investigation evidence

- `app/build/outputs/androidTest-results/connected/debug/TEST-XQ-AU52 - 12.xml`: one failure in a 12-test connected run.
- `app/build/outputs/androidTest-results/connected/debug/XQ-AU52 - 12/logcat-com.example.taoyuangutter.GutterBasicInfoUiTest-newFormShowsRequiredOrderLabelsButtonsAndDefaults.txt`: lifecycle sequence and absence of app fatal exception.
- `GutterFormActivity.onCreate()`: initializes the map fragment and form pager before the first assertion.


# Root Cause Analysis: ISS-0910-2-11

## Confirmed behavior

- The original virtual-point flow allowed the user to turn virtual mode off and restore the normal form navigation behavior.
- The current form still exposes `cbIsVirtual` and its listener, but turning it off does not restore the original page-navigation UI/interaction state.

## Root cause

This is a regression from the UI layout redesign in commit `cb3b90f` (`fix: UI 版面改版`). The original `applyVirtualModeUi(isVirtual)` used the virtual-state argument to control both behaviors:

- `switchPageBar.visibility = if (isVirtual) View.GONE else View.VISIBLE`
- `viewPager.isUserInputEnabled = !isVirtual`

The redesign replaced both state-dependent expressions with unconditional values:

- `binding.switchPageBar.visibility = View.GONE`
- `binding.viewPager.isUserInputEnabled = false`

As a result, the checkbox can change `isVirtualMode` and the form fields can be restored by `GutterBasicInfoFragment.setVirtualMode(false)`, but the Activity-level navigation state remains permanently disabled/hidden. The close/off action therefore appears to be missing even though the checkbox listener still exists.

## Affected files and risk

- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`: `applyVirtualModeUi()` and its initial `setupTabButtons()` interaction.
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt`: existing field visibility toggle remains present and is not the root cause.
- Risk: restoring the original conditional behavior may expose the page switch bar in non-virtual mode, so the implementation must preserve the current single-page product intent if that visibility was intentionally removed; the required behavior decision is specifically that virtual mode can be turned off and normal form interaction restored.

## Additional field-visibility finding

The virtual-mode field filter also omitted `cbCantOpen`. Because the measurement-status row contains both `btnPendingDeploy` and `cbCantOpen`, virtual mode still displayed the inapplicable 「無法開蓋」 option. The expected virtual form keeps the measurement-status title and 「待架站」 only, plus the measurement-coordinate number field and location section.

## Minimum fix scope

Restore the virtual-state transition contract at the Activity layer: when virtual mode is turned off, re-enable the normal form navigation/close path and keep virtual mode's hidden/disabled state while it is on. Hide `cbCantOpen` while virtual mode is on. Add a regression test that toggles `cbIsVirtual` on and off and verifies both the checkbox state and the restored controls. Do not change API keys, field order, photo slots, or map startup behavior.
