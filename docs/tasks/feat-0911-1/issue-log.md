# Issue Log

## ISS-001

- task_id: feat-0911-1
- phase: verification
- category: implementation_regression
- priority: P1
- title: Imported existing waypoint still uploads existing photos
- status: resolved
- impact: Existing imported photos may be sent again to `/v1/node/nodeImage`.
- evidence:
  - `GutterFormActivity.handleImportedNodeDetails()` was outside the previous resolver coverage.
  - Imported `NodeDetails.nodeId` was not copied into form/session `_nodeId`.
  - `GutterFormActivity.uploadLocalPhotos()` did not reject slots with an existing `photo*ImgId`.
  - `docs/tasks/dbg-0910/evidence/node_details_A0910pt52.json` contains an imported `node_img` photo URL without an image `id`.
- resolution:
  - Persist imported `NodeDetails.nodeId` into form/session `_nodeId`.
  - Skip slots with an existing `photo*ImgId` in the form-level uploader.
  - Added imported photo metadata preservation coverage.
  - Treat imported/downloaded photos with `UploadState=success` as already uploaded even when the API omits image IDs.
  - Add an early callback guard before `PhotoSlotUploadCoordinator.enqueueUpload()`.
  - Accept both `node_img[].id` and `node_img[].img_id` in the Android model; current ty04 response omits both fields, so ID population remains an API-contract limitation.
  - Persist `storeDitch.data.nodes[].url[].id` into the corresponding waypoint photo ID fields by `fileCategory`.
  - Apply the same persistence mapping from the `MapWorkspaceFragment` save callback; otherwise the alternate map save path could still lose the returned photo IDs.
  - Make `NodeDetails.safeCapturedAt()` tolerate an explicit `captured_at: null`; emulator evidence showed Gson can bypass the Kotlin non-null default and otherwise produce a caught `NullPointerException` during import.
- next_action: verification

## ISS-003 Implementation Result

- Status: resolved_pending_validation
- Fix: Reset replacement-slot server metadata before upload eligibility is evaluated, and execute the explicit-delete cleanup before the existing-server guard.
- Validation: PASS — target/full unit tests and debug/Android-test APK builds; backend replacement-request smoke is NOT VERIFIED because it would persist test data.
- Next action: verification

## ISS-003

- task_id: feat-0911-1
- phase: debug
- category: implementation_regression
- priority: P1
- title: Replacing a server-backed photo preserves its upload identity and skips re-upload
- status: identified
- impact: A user can see a newly captured replacement photo locally, but the server retains the old photo because no new `nodeImage` request is queued.
- repro_steps:
  - Open an existing gutter with a photo that has `photo*ImgId` or `photo*UploadState=success`.
  - Enter edit mode and capture a replacement for that same slot.
  - Save or return from the form.
- expected: The replacement clears the former server-upload metadata for that slot, enters the pending/uploading state, and is uploaded.
- actual: The replacement path retains the former `img_id` / successful upload state, so `onPhotoSlotReadyForUpload()` returns before enqueueing an upload.
- evidence:
  - `GutterBasicInfoFragment.applyPhotoToSlot()` writes the new URI but does not reset `photoImgId*` or `photoUploadState*`.
  - `GutterFormActivity.updateCurrentFormPhotos()` and `syncCurrentWaypointFromCurrentFormData()` preserve non-empty photo metadata whenever the slot still has a URI.
  - `GutterFormActivity.onPhotoSlotReadyForUpload()` uses `PhotoUploadSlotState.isAlreadyUploaded()` as an early guard, which returns true for the preserved metadata.
- next_action: debug

## ISS-002

- task_id: feat-0911-1
- phase: debug
- category: implementation_regression
- priority: P1
- title: Bottom-sheet pre-submit guard ignores successful imported photos without img ID
- status: resolved
- impact: Imported photos whose API response omits `id` can be uploaded again by the bottom-sheet submit path.
- repro_steps:
  - Import an existing point whose downloaded photo has no `node_img[].id`.
  - Confirm the photo is stored with `photo*UploadState=success`.
  - Submit through `AddGutterBottomSheet`.
- expected: The successful imported photo is skipped and no `nodeImage` request is queued.
- actual: The bottom-sheet path checks only `readImgId(...)`; with no numeric ID it can treat the photo as pending.
- evidence:
  - `AddGutterBottomSheet.countPendingPhotoUploads()` and `ensureWaypointPhotosUploadedBeforeSubmit()` used numeric-ID checks only.
  - `PhotoUploadSlotState.isAlreadyUploaded()` already defines the correct combined rule.
- next_action: verification
