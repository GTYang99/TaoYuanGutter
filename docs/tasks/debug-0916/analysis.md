# Analysis

## Evidence reviewed

- `GutterFormNavigator.buildInspectIntent()` / `GutterFormActivity.newViewIntent()`
- `GutterFormActivity.onPhotoSlotReadyForUpload()`
- `PhotoSlotUploadCoordinator`
- `StoreDitchNodeRequestMapper`
- `GutterRepository.uploadNodeImage()` and `storeDitch()`
- Existing mapper and multipart tests

## Findings

The inspect launch did not include `sessionDraftId`. After entering edit mode,
the upload callback previously returned at the positive-draft guard, so no
upload coordinator task was created. The fix creates and persists an edit draft
when edit mode is entered and clears stale photo metadata in both form and
session snapshots.

After upload, the new image ID was persisted locally, but the update mapper
explicitly set `imgIds` to `null` whenever `requestNodeId` was present. Thus an
update `storeDitch` request could not carry the replacement image association.

The current approved contract also requires `storeDitch` to omit `captured_at`.
Photo multipart upload continues to send its own `captured_at` parameter.
