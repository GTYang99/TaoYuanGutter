# Verification Report

## Verification Target

- Task: `dbg-0910`
- Verified revision: `32f74f575095747f5b4fc7187e0f02af911dae5d` (`32f74f5`)
- Branch: `uiFix/審核顯示框`
- Verification scope: requirements, committed implementation, tests, regression evidence, and CI status.
- Production code was not modified during Verification.

## Evidence Reviewed

- `requirement.md`, `analysis.md`, `plan.md`, `state.yaml`
- `root-cause.md`, `fix-plan.md`, `execution-report.md`, `issue-log.md`
- `evidence/node_details_A0910pt52.json`
- Committed diff from `4aa4d06` to `32f74f5`
- `GutterRequiredFieldLabelsTest.kt`, `GutterCantOpenUiTest.kt`, `GutterBasicInfoUiTest.kt`, and `CantOpenSessionSnapshotTest.kt`
- `ai/verification-rules.md` and `ai/testing-rules.md`

## Acceptance Criteria

| AC | Result | Evidence and rationale |
|---|---|---|
| AC-001 | PASS | `AddGutterBottomSheet.validateWaypointPhotosAndFieldsOrAlert()` maps missing keys through `GutterRequiredFieldLabels`; the four requested labels are present in `GutterRequiredFieldLabels.kt`. `GutterRequiredFieldLabelsTest` passed and covers all mappings and unknown-key fallback. |
| AC-002 | PASS (user-confirmed real device) | User reports the BottomSheet height was manually adjusted and passed real-device validation. The exact measured percentage and captured viewport/gesture evidence were not supplied in this thread. |
| AC-003 | PASS (user-confirmed real device) | User reports the authenticated device flow was connected and validated, covering the post-fix first-open cant-open import, slot 1 photo display, and direct confirmation. No screenshot or log was supplied in this thread. |
| AC-004 | PASS (user-confirmed real device) | User reports the connected real-device validation passed the relevant regression behavior. No detailed test matrix or artifact was supplied in this thread. |

## Implementation Review

- The label mapping is presentation-only and leaves raw field keys in validation and payload logic.
- The import fix re-synchronizes the active Fragment from Activity photo state after asynchronous downloads and passes URI, captured time, upload state, image ID, and upload error together.
- Import progress now counts non-empty photo URLs; this is an additional behavior change in the same import path and requires regression confirmation.
- The current 70% BottomSheet change deviates from the approved implementation plan and the task's documented 60% target.
- No production changes were made to `GutterBasicInfoFragment.kt` in the task commits. The user-confirmed real-device result provides acceptance evidence that the Activity-side synchronization fix is sufficient for AC-003; detailed device artifacts were not supplied in this thread.

## Test Review

| Check | Result | Evidence |
|---|---|---|
| `git diff --check 4aa4d06..HEAD` | PASS | Completed with no whitespace errors. |
| `./gradlew testDebugUnitTest --tests com.example.taoyuangutter.gutter.GutterRequiredFieldLabelsTest --no-daemon` | PASS | Android Studio bundled JDK; targeted 0910 label test completed successfully. |
| `./gradlew assembleDebug --no-daemon` | PASS | Android Studio bundled JDK; `BUILD SUCCESSFUL`, 42 actionable tasks. |
| `./gradlew testDebugUnitTest --no-daemon` | PASS | Android Studio bundled JDK; 51 tests completed successfully after changing `minimumZoomMatchesGutterLayerRequirement` to expect 13f. |
| `connectedDebugAndroidTest --no-daemon` | NOT VERIFIED | The test reached `XQ-AU52 - 12`, but instrumentation failed with `INSTRUMENTATION_FAILED`; package cleanup also returned `DELETE_FAILED_INTERNAL_ERROR`. Gradle's final `BUILD SUCCESSFUL` only covered task completion, not test success. |
| Direct ADB instrumentation for `GutterCantOpenUiTest` and `GutterBasicInfoUiTest` | PASS | Debug and androidTest APKs were installed directly on `XQ-AU52`; `am instrument` completed 6/6 tests successfully. |
| Direct ADB instrumentation rerun after test-state reset | PASS | Test package state was cleared and the same two 0910-related classes completed 6/6 tests successfully on `XQ-AU52`. |
| Direct ADB instrumentation for the full androidTest package | FAIL / PARTIAL | The run reported 12 tests; the first two passed, then `GutterBasicInfoUiTest.newFormShowsRequiredOrderLabelsButtonsAndDefaults` failed with `NoActivityResumedException`. No complete package PASS was produced. |
| Remote CI build/test | NOT VERIFIED | No CI workflow is configured in this repository, so no remote CI job could be triggered. |

The committed tests provide useful coverage for label mapping and cant-open session helper behavior. Direct ADB instrumentation, including a clean-state rerun, confirms the six targeted cant-open/basic-info UI tests pass on `XQ-AU52`; the full package run is not clean because the activity was not resumed when the third test started. No committed test exercises the full `A0910pt52` import chain from API response through local URI, Fragment rendering, and confirmation readiness. The user-confirmed real-device result covers that acceptance behavior. There is still no automated test or captured measurement for the BottomSheet height and viewport behavior.

## Regression Review

- API field keys, `node_img` parsing, photo category mapping, and slot 1/2/3 selection are unchanged by source diff review.
- Cant-open validation still requires slot 1 only; slot 2/3 remain optional in the reviewed code.
- Metadata preservation is passed through the new synchronization call, but executable regression evidence is unavailable.
- The 70% sheet sizing may affect map viewport, RecyclerView scrolling, and bottom controls; these risks remain unverified.
- The minimum-zoom test expectation is now aligned to the current 13f source and the complete unit suite passes.

## Issues and Classification

- `ISS-DBG-0910-002` — category `implementation_regression`, priority `P1`: resolved by user-confirmed manual adjustment and real-device validation of AC-002.
- `ISS-DBG-0910-003` — category `environment`, priority `P1`: resolved for acceptance behavior by user-confirmed authenticated real-device validation of AC-003; detailed artifact remains absent.
- `ISS-DBG-0910-004` — category `environment`, priority `P1`: no remote CI workflow is configured; local full unit/build and clean-state targeted direct 0910 instrumentation pass, while the full direct package run has a `NoActivityResumedException` and the Gradle connected task fails during package cleanup/startup.

## Final Result

NOT VERIFIED overall

All four acceptance criteria are recorded as passing based on source review and the user's real-device confirmation. The complete unit suite, debug build, and targeted direct 0910 instrumentation pass with Android Studio's bundled JDK/ADB path. The full direct androidTest package is not clean because `GutterBasicInfoUiTest` encountered `NoActivityResumedException`; the Gradle connected task also fails during package cleanup/startup, and no remote CI workflow exists in the repository. The task must not advance to Release until the connected-test task/CI gate is explicitly completed or the release authority records exceptions.

## Next Action

`verification`
