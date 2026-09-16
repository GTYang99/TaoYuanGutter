# Debug Root Cause

## Issue

- Issue ID: `ISS-002`
- Verification review: AC-005 regression coverage
- Category: `implementation_regression`
- Priority: `P1`

## Root Cause

`AddGutterBottomSheet` has a submission-time upload path that is separate from
`PhotoUploadManager` and `GutterFormActivity.uploadLocalPhotos()`. Its
`countPendingPhotoUploads()` and `ensureWaypointPhotosUploadedBeforeSubmit()`
functions only check whether `photo*ImgId` can be parsed as an integer.

Imported `nodeDetails` photos can legitimately have no image ID in the API
response, while the app marks the downloaded photo as
`photo*UploadState=success`. The shared upload-state rule already treats that
state as server-backed, but the bottom-sheet path bypasses that rule. Therefore
an imported photo without an ID can be counted and uploaded again before
`storeDitch`.

## Evidence

- `PhotoUploadSlotState.isAlreadyUploaded()` returns true for either a numeric
  image ID or `UploadState=success`.
- `PhotoUploadManager` and `GutterFormActivity` use that rule.
- `AddGutterBottomSheet.kt` at the two pre-submit checks used only
  `readImgId(...) == null`.
- Verification marked AC-005 `NOT VERIFIED` because this alternate submit path
  was not covered by the existing end-to-end matrix.

## Regression Risk

The fix must preserve uploads for a newly replaced photo: only a slot with a
server image ID or an explicit successful server state is skipped. Failed,
idle, or newly replaced slots remain upload candidates.

## Follow-up Root Cause: ISS-003

### Finding

Replacing a photo does **not** clear the original server `img_id` in the
current code. An explicit delete does clear it, but the capture-and-replace
path only changes the local URI.

### Causal Chain

1. The camera result invokes `applyPhotoToSlot(slot, newUri, true)`.
2. That function stores the new URI but leaves the slot's existing image ID and
   `success` upload state unchanged.
3. `updateCurrentFormPhotos()` / `syncCurrentWaypointFromCurrentFormData()`
   preserve metadata for every non-empty photo slot, including the replacement.
4. `onPhotoSlotReadyForUpload()` sees the old ID or success state through
   `PhotoUploadSlotState.isAlreadyUploaded()` and returns before creating a
   `PhotoSlotUploadCoordinator` job.

The early guard was introduced in `72492cb` to stop *unchanged* imported
photos from being uploaded again. It lacks a replacement transition that clears
only the server-upload metadata while retaining the newly captured URI.

### Minimum Fix Direction

Before notifying the upload host of a successful replacement capture, reset
only that slot's upload state, image ID, and error; retain the new URI and its
capture timestamp. Do not call the generic deletion helper because it also
removes the newly captured photo. Add a regression test that starts from a
server-backed slot, replaces it, and asserts that an upload job is enqueued.
