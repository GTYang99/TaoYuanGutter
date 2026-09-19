# Debug Execution Report

## Scope

Implemented and validated the minimum fix from `fix-plan.md` on branch
`fix/debug-0919-2-照片上傳流程`. The change is limited to photo-result metadata
merging, the form-result completion boundary, and focused regression coverage.

## Checks performed

- Reviewed the attached debug note as the task problem statement.
- Traced the form-result path from single-photo upload through
  `updateWaypointBasicData()` and final upload gating.
- Confirmed `Waypoint.uid` versus API `_nodeId` ownership.
- Applied the user-confirmed `storeDitch` response-order contract.
- Added `PhotoResultMetadataMerger` so a changed URI removes stale metadata but
  retains current-form `img_id` and upload state; deletion removes the former
  server metadata.
- Updated the form exit paths to wait for active single-photo coordinator work
  before publishing the result, then refresh the draft and dispatch the
  completed state to the live sheet.
- Added focused regression tests for new upload, replacement, deletion, same
  URI metadata preservation, and reversal attachment.
- Ran repository whitespace validation with `git diff --check`.

## Results

- Code-level root cause identified for AC-001 and AC-002. Debug confidence is
  95% at code level; actual runtime duplicate-request counting remains
  `NOT VERIFIED`.
- Focused unit tests passed: 22 tests across the photo merge, upload candidate,
  `storeDitch` response, and request-mapper paths, plus a dedicated reversal
  pending-count regression test; aggregate focused coverage is 23 tests.
- Debug APK build passed with `:app:assembleDebug`.
- Focused emulator instrumentation passed: 1 test,
  `GutterFormExitUiTest#completedVirtualFormLeavesWithoutWarning`.
- Focused physical-device instrumentation passed on Sony XQ-AU52 / Android 12:
  - `GutterFormExitUiTest#completedVirtualFormLeavesWithoutWarning`: 1/1.
  - `Debug0919WaypointAdapterUiTest#listShowsNoDataForBlankAndPartialRowsButFilledForUploadCompleteRow`: 1/1.
- The first physical-device case initially waited because the device display
  was asleep; after waking the device, the same run completed successfully.
- Full regression, real camera/network upload reproduction, CI, independent
  verification, and release were not run. They remain `NOT VERIFIED`.
