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

The newly supplied `storeDitch` response changes the evidence assessment:
`data.nodes[].url[].id` is the server image ID (`id=12339` is the same value
conceptually referred to as `img_id`). The client already parses and persists
this ID after a successful `storeDitch` through
`StoreDitchResponseWaypointMapper`.

Therefore the previous conclusion that the server response lacks an image ID
was too broad. It was only true for the earlier `nodeDetails` fixture used in
the import investigation. The concrete loss boundary is the subsequent
`nodeDetails` / `closestNodeDetails` refresh and handoff: when that response
omits `nodeImg[].id`, the import code must not replace an ID already obtained
from `storeDitch` with `null`. The `storeDitch` response mapping itself is
correct; the fix is to prefer a newly returned ID and otherwise retain the
existing `photo{slot}ImgId`.

### Affected files

- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/common/PhotoUploadSlotState.kt`
- `app/src/main/java/com/example/taoyuangutter/api/StoreDitchNodeRequestMapper.kt`
- `app/src/main/java/com/example/taoyuangutter/api/GutterApiModels.kt`
- `app/src/main/java/com/example/taoyuangutter/common/PhotoImgIdResolver.kt`

### Regression risk

The production fix now protects the handoff even when the import endpoint
omits the ID: a non-null ID from the refreshed response wins, otherwise the
existing form-state ID is retained. A fully captured `nodeDetails` /
`closestNodeDetails` payload would still be useful for endpoint-level
verification, but it is no longer required to infer the save-response
mapping.

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
