# Root Cause

## Issue 1: Imported photos appear to require upload

### Primary cause

The import flow downloads each existing remote photo to a local URI, then
marks the slot `success` even when the API response does not contain a server
image ID. `PhotoUploadSlotState.isAlreadyUploaded()` treats `success` alone as
an uploaded slot, so the submit path skips multipart upload. The mapper also
cannot send `img_ids` when `photo{slot}ImgId` is absent.

This is proven by `docs/tasks/dbg-0910/evidence/node_details_A0910pt52.json`:
the response contains a category-1 URL but no `id`/`img_id`. The current
`handleImportedNodeDetails()` then calls `updatePhotoUploadState()` with the
download result as `success` and the absent ID as `null`.

Therefore the current behavior for this real response is: download locally,
do not re-upload, and send no image ID in `storeDitch`. This matches the
approved existing-point contract recorded in `docs/tasks/feat-0911-1/`: an
unchanged downloaded photo must not call `nodeImage`, and existing-point
`storeDitch` must not carry unchanged photo metadata. It is not accurately
described as “an existing `img_id` prevents upload”; the `success` state is
also an intentional server-backed guard for imported photos without an ID.

### Affected files

- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/common/PhotoUploadSlotState.kt`
- `app/src/main/java/com/example/taoyuangutter/api/StoreDitchNodeRequestMapper.kt`
- `app/src/main/java/com/example/taoyuangutter/api/GutterApiModels.kt`

### Regression risk

No production fix is indicated for the unchanged-import path. The required
distinction is already implemented: imported/downloaded success is skipped;
replacement capture clears the old metadata, uploads the new local photo, and
the returned new ID is mapped into the existing-node `storeDitch` update.
Further backend capture is useful integration evidence but is not needed to
resolve whether unchanged imported photos should be re-uploaded.

## Issue 2: `0910刪除資料` is on by default

### Primary cause

The same default is independently encoded as `true` in
`MapOverlayController.showDeletedAreaOverlay`, `LayersBottomSheet`'s
`ARG_SHOW_DELETED_AREA` fallback and `newInstance()` default, and
`sheet_layers.xml`'s `cbDeletedArea` checked value. The first map overlay
state therefore requests and creates `Wms3826TileProvider()` for
`deleted_area`.

### Affected files

- `app/src/main/java/com/example/taoyuangutter/map/MapOverlayController.kt`
- `app/src/main/java/com/example/taoyuangutter/map/LayersBottomSheet.kt`
- `app/src/main/res/layout/sheet_layers.xml`

### Regression risk

Only the initial default should change. Explicit state passed from the map
host must continue to round-trip through the bottom sheet and controller.

## Issue 3: `既有點位資料` is not visually centered

### Primary cause

The header's title TextView is weighted and centered only within the space
remaining after the back button and the right-side location button. The
location button is `gone` for the current import flow, so its 48dp width is
removed from layout measurement. The title's mathematical center is thus to
the right of the full row center.

### Affected file

- `app/src/main/res/layout/bottom_sheet_import_existing_waypoint.xml`

### Regression risk

The correction must preserve a stable full-row title center while retaining
the current left-button behavior and the optional right-button visibility.
