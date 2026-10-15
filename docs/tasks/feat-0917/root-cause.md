# Root Cause: ISS-007

## Symptom

After creating and uploading a gutter, opening inspection, and returning to the main map, gutter segments from `/v1/map/scopeSearch` appeared absent.

## Cause

`MapWorkspaceFragment.inspectLauncher` reloads the viewport before reopening the multi-gutter list. However, `showAddGutterList()` immediately and unconditionally hid `scopeGutterPolylineController`. The request and drawing path could therefore complete, but the list transition made the resulting polyline layer invisible.

## Minimum Fix

When the add-gutter list opens, set scope-polyline visibility from `mapOverlayController.currentState().showPlan` rather than forcing it to `false`.

## Scope and Risk

Only the multi-gutter list transition is changed. The normal map layer toggle remains the source of truth; users who manually turn the gutter layer off continue to see it hidden.

# Root Cause: ISS-011 and ISS-012

## ISS-011 — Editable list shows an inspection-only suffix

`WaypointAdapter.buildDisplayLabel()` appends `(銜接點)` for every adapter consumer. Its only production consumer is `AddGutterBottomSheet`, so the editable waypoint list inherits a label that is required only in the inspection screen. The inspection screen does not use this adapter: `GutterInspectPhotosFragment` builds its spinner label independently in `pointLabel()` / `buildPointStatusSuffix()`.

## ISS-012 — Inspection detail order differs from the requested order

`GutterInspectPhotosFragment.renderFields()` creates rows dynamically. The current append sequence is `側溝材質 → 淤積程度 → 連接管 → 溝體結構受損 → 附掛或過路管線 → 補充說明`, while the required sequence is `側溝材質 → 溝體結構受損 → 附掛或過路管線 → 淤積程度 → 連接管 → 補充說明`.

## Minimum Fix Scope

- Remove the tie-in suffix from `WaypointAdapter` without changing inspection spinner labels.
- Reorder only the five dynamic detail-row additions in `GutterInspectPhotosFragment.renderFields()`.
- Add regression coverage for both labels and the exact inspection-field order.

## Confidence

Root-cause confidence: 99%. The observed output maps directly to the only two code paths that build these labels and fields.

# Root Cause: ISS-013 and ISS-014

## ISS-013 — Strict Boolean parsing in the inspection renderer

The inspection renderer evaluates hanging with `details.isHanging == "1"`; connecting delegates to `isConnectingAsBoolean`, which also checks only `"1"`. Values represented as `true`/`"true"`, or supplied under an existing compatibility alias, become false and are rendered as the negative option. The specific live response has not been captured, so confidence is 85%; the raw nodeDetails payload is the remaining evidence needed to distinguish a Boolean-format response from an absent/renamed field.

## ISS-014 — Progress count and actual upload candidate selection are different operations

`countPendingPhotoUploads()` triggers the blocking overlay before `ensureWaypointPhotosUploadedBeforeSubmit()` evaluates all of its later skip conditions. Import writes photo paths first and asynchronously writes server `img_id`/success state afterwards. If that state arrives between count and processing, every counted photo can be skipped as already uploaded, leaving the overlay with `0/X` before it closes.

## Minimum Fix Scope

- Normalize hanging/connecting response values at the NodeDetails boundary using one shared loose Boolean parser, while retaining canonical `"0"`/`"1"` behavior; confirm any required JSON aliases from captured response evidence.
- Build one resolved pending-photo candidate list before showing the overlay, including server image IDs and coordinator-completed slots; use that same list for upload execution.

# Root Cause: ISS-014 (revised after rollback)

## Observed behavior

When an imported point's photos already have server `img_id` values, submitting the gutter can briefly show `照片上傳中…已完成 0/X（失敗 0）` and then close without a photo upload result.

## Cause

`AddGutterBottomSheet.countPendingPhotoUploads()` chooses the overlay total using only the local photo path, unchanged-photo rule, and `PhotoUploadSlotState.isAlreadyUploaded()`.

The later `ensureWaypointPhotosUploadedBeforeSubmit()` repeats those checks but also examines `PhotoSlotUploadCoordinator.completedFor()` and `PhotoSlotUploadCoordinator.isUploading()/awaitCompletion()`. A slot resolved by the coordinator is written with its server image ID and then skipped, but it has already been counted. The host therefore starts the overlay with X candidates; no `onPendingPhotoUploadProgress()` is emitted for coordinator-resolved slots; `finally` calls `onPendingPhotoUploadFinished()`. This is the exact 0/X flash.

The failure is not a requirement to defer all uploads until the final gutter request. Server-owned imported photos must remain excluded by their `img_id`; the defect is that progress creation and actual upload eligibility use different candidate definitions.

## Rejected change

The previous change wrote imported image success state earlier in `GutterFormActivity`. It was reverted in `14f6c78` because it did not make the overlay and actual-upload decisions use the same source of truth, so it could not prove the observed 0/X condition was fixed.

## Confidence and remaining evidence

- Root-cause confidence: 97% for the zero-work overlay. The code path has a direct start-with-count / skip-without-progress / finish sequence matching the text exactly.
- The repository cannot prove which imported slot first reaches the coordinator without a focused trace from the affected flow. Existing `PhotoImgIdTraceDebugger` logging at `countPending.slot*` and `ensurePhotos.slot*` can confirm the slot-level state during the next reproduction, but is not required to identify the mismatch.
