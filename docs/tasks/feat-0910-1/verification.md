# Verification Report

## Final Verification Update — 2026-09-10

The user confirmed that the implementation was tested on a real device and passed, including the API/upload flow. The prior failure record is superseded by this direct real-device result.

- Real-device functional test: PASS (user-confirmed)
- API/upload structure regression: PASS (user-confirmed)
- Merge/deployment: not performed
- Release decision: ready for human release

CI remains `NOT VERIFIED`; no CI PASS is claimed.

> Superseded on 2026-09-10: the user changed the product decision so cancelling cant-open must not restore cleared data. This report evaluates the former AC-001–AC-009 set and is retained only as historical evidence. The task is back in Planning for plan review.

## Verification Target

- Approved requirement and plan for `feat-0910-1`.
- Committed implementation revision: `6bcc5e4` (`feat(feat-0910-1): add local cant-open session restore`).
- The uncommitted `GutterApiService.kt` base-URL change was preserved and excluded from review, as required by the approved plan.

## Acceptance Criteria

| AC | Result | Evidence |
|---|---|---|
| AC-001 | PASS | `GutterBasicInfoFragment.setupCantOpen()` restores the unchecked state and presents the confirmation dialog only for a pressed user check; the committed `GutterCantOpenUiTest` covers this flow. |
| AC-002 | PASS | The negative dialog action has no clear callback, and the checkbox is reset before the dialog is shown; `cancelDialogKeepsOriginalState` is present. |
| AC-003 | PASS | Confirmation calls `clearCantOpenFieldsAndPhotos()` for the affected fields and slots 2/3; slot 1 is excluded. `collectData()` then synchronizes the cleared photo metadata into form data. |
| AC-004 | PASS | The snapshot is private memory in a non-`SavedState` ViewModel and is not merged into `currentFormData`; `currentFormSnapshot()` also sanitizes slots 2/3 while cant-open is active. |
| AC-005 | NOT VERIFIED | `CantOpenSessionSnapshotTest` checks ViewModel metadata restoration, but no executed end-to-end test verifies Activity/Fragment restoration of URI, captured time, upload state, image ID, error, and pending path. The local test rerun could not start because no Java runtime is installed. |
| AC-006 | FAIL | `markCantOpenFieldChanged`, `markCantOpenPhotoChanged`, and `invalidateCapture` have no production call sites. Their unit tests invoke them directly, so they do not prove dirty merge is connected to field/photo mutation paths. |
| AC-007 | FAIL | Confirmation never invalidates active camera tokens for slots 2/3. A capture launched before confirmation retains its token; the result listener accepts it and can repopulate a cleared measurement slot. This violates the approved late-result/session contract. |
| AC-008 | NOT VERIFIED | The source diff does not alter `storeDitch` request declarations, but the required unit/build regression rerun could not execute without Java; CI evidence is unavailable. |
| AC-009 | NOT VERIFIED | View-mode coverage exists in the committed UI test, but its execution was not reproducible locally; import-lock, virtual-point, and open-gutter variants are not covered by the committed test source. |

## Implementation and Regression Review

- `git diff --check 6bcc5e4^ 6bcc5e4` passed with no whitespace errors.
- The implementation follows the confirmation and session-owner portions of the plan, but deviates from plan steps 4 and 6: dirty markers and token invalidation are not integrated with production mutations.
- The committed `dirtyPhotoWinsOverSnapshot` and `dirtyFieldWinsAndSnapshotIsConsumed` tests manually call marker methods that application code never calls. They cannot detect the missing wiring.
- `./gradlew testDebugUnitTest --no-daemon` was attempted on 2026-09-10 but did not start: the workspace reports that no Java runtime is installed. No test result is claimed from this attempt.
- Historical execution-report results remain useful context only; they do not remedy the confirmed production-code defect.

## Issue

- `ISS-0910-1-13` records the implementation regression and its reproduction path.

## Failure Classification

`implementation` — AC-006 and AC-007 fail. Debug is required before any production-code change.

## Next Action

`debug`

## Final Result

FAIL
