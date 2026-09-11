# Fix Plan: ISS-0910-2-09

## Current status

Debug is still investigating. No production fix is approved yet because the trigger for the activity losing resumed state has not been proven.

## Minimum next checks

1. Re-run the single new-form test with map initialization isolated or disabled only in the test harness, to determine whether `SupportMapFragment`/map rendering is the trigger.
2. Capture ActivityTaskManager/WindowManager lifecycle events together with the app logcat to identify what causes the `PAUSED/STOPPED` transition.
3. If map initialization is confirmed, choose the smallest production-safe fix that preserves the translucent map-backed form behavior and then rerun the new-form test.
4. Re-run `GutterBasicInfoUiTest` and `GutterCantOpenUiTest` before updating AC-001/002/003/005/006.

## Guardrails

- Do not remove the map, change the translucent form theme, or alter activity finish behavior without confirming the trigger and reviewing the UI requirement impact.
- Do not mark the affected acceptance criteria PASS based only on the user's manual field-order result.
- Keep the offline existing-data fix and photo lifecycle fix separate from this investigation.
