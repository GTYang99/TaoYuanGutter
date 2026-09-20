# Verification Report

## Revision and working-tree boundary

- Production implementation and regression-test revision under review: `739cfa9`
- Branch: `fix/debug-0920-1-表單問題`
- The earlier `d3dffbd` commit contains the production fix; `739cfa9` adds the focused silt and UI regression coverage.
- The later documentation commits only record task state and verification evidence; they do not change production or test code.
- Existing untracked `.worktrees/` was not modified or included.

## Acceptance criteria

| Criterion | Result | Evidence and limitation |
|---|---|---|
| AC-001 | PASS | `GutterCompletionPolicyTest`, the new tie-in UI test, and `GutterCantOpenUiTest` passed. Source review confirms the same exemption reaches form validation, photo requirements, and submit validation. |
| AC-002 | NOT VERIFIED | Diff review confirms only the post-import missing-photo Toast was removed and photo state/error handling remains. A live existing-waypoint import fixture was not available on the emulator. |
| AC-003 | NOT VERIFIED | Diff review confirms no-photo/photo-issue warnings now continue to preload and detail-load failure still routes to retry. A live authenticated inspection/preload fixture was not available on the emulator. |
| AC-004 | PASS | `InspectionPresentationTest` passed for codes `0`, `1`, `2`, and legacy `3`; `GutterBasicInfoUiTest` also passed for the severe form selection. |
| AC-005 | PASS | The new `GutterBasicInfoUiTest` passed before and after entering edit: backend `XY_NUM` is visible, populated, not enabled, and has no required marker. Mapper and completion-policy JVM tests passed. |

## Checks performed

- `git diff --check`: PASS.
- Static forbidden-text regression search for the removed Toast, dialog title, and `2 -> 中度` mapping: PASS.
- Targeted JVM tests: PASS. `GutterCompletionPolicyTest`, `StoreDitchNodeRequestMapperTest`, and `InspectionPresentationTest` passed.
- Debug build: PASS. `assembleDebug` completed successfully.
- Android UI/device checks: PASS for `GutterBasicInfoUiTest`, `GutterFormExitUiTest`, `GutterCantOpenUiTest`, and `Debug0919ImportedWaypointUiTest` on `Medium_Phone` Android 14.

## Regression review

- Import photo synchronization, loading cleanup, and exception Toast were left intact.
- Genuine node-detail preload failure still blocks edit and offers retry.
- Tie-in exemption logic was not duplicated or rewritten.
- Backend-provided `XY_NUM` is still collected and preserved for update payloads.

## Result

`NOT VERIFIED`

Category: `environment`.

Required follow-up: execute AC-002 with an existing-waypoint import fixture and AC-003 with an authenticated inspection/preload fixture, or retain those two criteria as `NOT VERIFIED`.
