# Fix Plan

## Issue and Root Cause

- Issue: ISS-1012-001 (implementation_regression, P2).
- Root cause: performEditSubmit enables host loading before storeDitch but does not clear it in its API-error or generic-exception paths. Choosing save draft can then dismiss the sheet through a host callback that does not clear loading for single-gutter flows.

## Minimum Fix Scope

1. In AddGutterBottomSheet.performEditSubmit(), clear the host submission loading state on the failed edit request path before showing the retry/save-draft dialog. Use the existing onGutterSubmitFailed callback so MainActivity and MapWorkspaceFragment keep their established loading policy.
2. Apply the same balanced cleanup to the generic exception path that displays the same failure dialog.
3. Preserve the current 403 classification, dialog actions, draft storage, and API request/response behavior.
4. Confirm retry re-enters performEditSubmit and turns loading back on through onGutterSubmitting.

## Acceptance Mapping

- User-reported behavior: after a 403 and choosing “存入草稿”, the spinner must stop. The edit path currently fails this expected terminal state because inspectLoadingVisible remains true after sheet dismissal.
- Formal AC mapping is pending: task-specific requirement.md, plan.md, and verification.md were not present in the reviewed revision.

## Affected Production Scope

- app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt

Existing host implementations are already present in MainActivity.kt and MapWorkspaceFragment.kt. No API or data-model change is indicated by this root cause.

## Validation Needed Before Closure

- Exercise an existing-gutter update with a controlled 403 response and choose “存入草稿”; confirm the overlay and rotation stop, the draft is available, and the user can continue using the map.
- Choose “重傳” after a controlled failure; confirm the loading animation restarts and a successful retry completes normally.
- Check both MainActivity and MapWorkspaceFragment single-gutter host paths.
- Record the fixed committed revision and run the validation required by the approved task plan.

Implementation is not started in this debug phase. The missing approved task inputs must be resolved before re-implementation under the repository workflow.


## Scope Clarification from Cross-Path Review

The spinner fix remains the missing edit-flow cleanup callback. During implementation planning, also confirm the expected persistence contract for the save-draft action in the default single-gutter host: its current callback may only dismiss, and autosave depends on a current or matching draft record. Keep this as a separate acceptance check until product requirements confirm whether the dialog action must create a draft in all cases.

The normal product route is MapWorkspaceFragment with isMultiGutterSession false. Validation should cover that default route in addition to the legacy MainActivity host and the multi-gutter branch, which already clears loading.
