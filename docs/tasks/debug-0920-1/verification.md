# Verification Report

## Revision and working-tree boundary

- Production implementation and regression-test revision under review: `5e4f85d`
- Branch: `fix/debug-0920-1-表單問題`
- The earlier `d3dffbd` commit contains the production fix; `5e4f85d` adds the focused silt, import, and edit-entry regression coverage.
- The later documentation commits only record task state and verification evidence; they do not change production or test code.
- Existing untracked `.worktrees/` was not modified or included.

## Acceptance criteria

| Criterion | Result | Evidence and limitation |
|---|---|---|
| AC-001 | PASS | `GutterCompletionPolicyTest`, the new tie-in UI test, and `GutterCantOpenUiTest` passed. Source review confirms the same exemption reaches form validation, photo requirements, and submit validation. |
| AC-002 | PASS | `GutterImportExistingWaypointUiTest` invoked the existing import handler with an empty photo set on `Medium_Phone`; the specified missing-photo Toast was absent. |
| AC-003 | PASS | `GutterInspectEditEntryUiTest` used a no-server-photo inspection fixture and confirmed edit entry completed without the `進入編輯確認` dialog; detail-load failure routing remains source-reviewed. |
| AC-004 | PASS | `InspectionPresentationTest` passed for codes `0`, `1`, `2`, and legacy `3`; `GutterBasicInfoUiTest` also passed for the severe form selection. |
| AC-005 | PASS | The new `GutterBasicInfoUiTest` passed before and after entering edit: backend `XY_NUM` is visible, populated, not enabled, and has no required marker. Mapper and completion-policy JVM tests passed. |

## Checks performed

- `git diff --check`: PASS.
- Static forbidden-text regression search for the removed Toast, dialog title, and `2 -> 中度` mapping: PASS.
- Targeted JVM tests: PASS. `GutterCompletionPolicyTest`, `StoreDitchNodeRequestMapperTest`, and `InspectionPresentationTest` passed.
- Full JVM suite: PASS. `./gradlew testDebugUnitTest` completed successfully.
- Debug build: PASS. `assembleDebug` completed successfully.
- Android UI/device checks: PASS. Full `./gradlew connectedDebugAndroidTest` completed successfully on `Medium_Phone` Android 14, including the AC-scoped tests and nearby regression tests.

## Regression review

- Import photo synchronization, loading cleanup, and exception Toast were left intact.
- Genuine node-detail preload failure still blocks edit and offers retry.
- Tie-in exemption logic was not duplicated or rewritten.
- Backend-provided `XY_NUM` is still collected and preserved for update payloads.

## Result

`PASS`

All five acceptance criteria have evidence. The repository's separate remote CI/release record has not been run in this local session.
