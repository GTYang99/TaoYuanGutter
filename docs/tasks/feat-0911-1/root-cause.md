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
