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
