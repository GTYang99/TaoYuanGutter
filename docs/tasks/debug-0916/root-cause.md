# Root Cause

## Primary cause

`StoreDitchNodeRequestMapper.map()` used `requestNodeId != null` as a reason to
set `imgIds = null`. Existing-node updates therefore omitted the new
`photoXImgId` values returned by the photo upload API. The backend received the
node update without the replacement image IDs, so the old image association was
not replaced.

## Related flow cause

Inspect mode did not carry a draft ID. `onPhotoSlotReadyForUpload()` returned
when `sessionDraftId <= 0`, preventing the coordinator from enqueueing the
replacement upload. The fix creates a persisted local edit draft on entering
edit mode and removes stale slot metadata from the session snapshot.

## Affected files

- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/api/StoreDitchNodeRequestMapper.kt`
- `app/src/main/java/com/example/taoyuangutter/common/PhotoSlotUploadCoordinator.kt`
- `app/src/test/java/com/example/taoyuangutter/api/StoreDitchNodeRequestMapperTest.kt`

## Regression risk

The mapper now sends the current completed image IDs for both create and update,
while preserving existing exclusions for virtual and cannot-open slots. The
`captured_at` change is limited to `storeDitch`; multipart photo upload is
unchanged.
