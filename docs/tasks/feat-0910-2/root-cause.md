# Root Cause Analysis: ISS-0910-2-09

## Confirmed behavior

- The failure occurs in `GutterBasicInfoUiTest.newFormShowsRequiredOrderLabelsButtonsAndDefaults` on Sony XQ-AU52.
- `GutterFormActivity` reaches `RESUMED`, then transitions to `PAUSED` and `STOPPED` about 66 ms later.
- There is no application `FATAL EXCEPTION`, `finish()` log, or stack trace identifying an application crash.
- Espresso waits for the first assertion until the 46-second timeout, after which `ActivityScenario` reports the activity as `DESTROYED` and raises `NoActivityResumedException`.
- The same test uses an activity with a full-screen `SupportMapFragment` and a translucent `FormSheet` theme. Existing-value and `GutterCantOpenUiTest` scenarios do not reproduce this exact failure.

## Root-cause status

The application-side trigger for the immediate `PAUSED/STOPPED` transition is not yet conclusively identified. The evidence rules out the earlier View-reparenting crash and does not justify changing the form window theme or removing the map. This remains an implementation investigation, not a verified environment-only failure.

## Affected acceptance criteria

- AC-001, AC-002, AC-003, AC-005: assertions cannot run because the new-form activity is not resumed.
- AC-006: the connected regression suite fails even though the affected `GutterCantOpenUiTest` cases pass.

## Investigation evidence

- `app/build/outputs/androidTest-results/connected/debug/TEST-XQ-AU52 - 12.xml`: one failure in a 12-test connected run.
- `app/build/outputs/androidTest-results/connected/debug/XQ-AU52 - 12/logcat-com.example.taoyuangutter.GutterBasicInfoUiTest-newFormShowsRequiredOrderLabelsButtonsAndDefaults.txt`: lifecycle sequence and absence of app fatal exception.
- `GutterFormActivity.onCreate()`: initializes the map fragment and form pager before the first assertion.
