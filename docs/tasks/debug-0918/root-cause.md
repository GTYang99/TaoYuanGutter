# Root Cause

## Issue 1: Imported photos appear to require upload

### Primary cause

There are two separate API paths:

1. The direct existing-point import response can contain only a remote URL and
   `fileCategory`, without `id`/`img_id`. The app downloads that URL and marks
   the local slot `success`, but this does not give `storeDitch` a server image
   ID.
2. `ditchDetails` is a whole-ditch response and can expose IDs under
   `data.nodes[].url[].id`; `nodeDetails` is a single-node response and the
   observed contract exposes only `node_img[].url` and `fileCategory`. The two
   responses are not interchangeable and must not be joined by response order
   or by an unrelated `ditch_id`.

The correct invariant is: a downloaded unchanged imported photo is marked
`success` and must not be uploaded again; a photo that the user replaces or
captures clears the old state and becomes an upload candidate. The absence of
an ID in `nodeDetails` is not a reason for the client to invent a cross-API
mapping.

This is proven by `docs/tasks/dbg-0910/evidence/node_details_A0910pt52.json`:
the response contains a category-1 URL but no `id`/`img_id`. The current
`handleImportedNodeDetails()` then calls `updatePhotoUploadState()` with the
download result as `success` and the absent ID as `null`.

The newly supplied `ditchDetails`/`storeDitch` response changes the evidence assessment:
`data.nodes[].url[].id` is the server image ID (`id=12339` is the same value
conceptually referred to as `img_id`). The client parses this ID after a
successful `storeDitch` through `StoreDitchResponseWaypointMapper`, but that
callback happens after the form's photo-upload phase. It cannot populate a
direct-import form retroactively before that form decides whether to call
`nodeImage`.

Therefore the previous conclusion that the server response lacks an image ID
was too broad. It was only true for the earlier `nodeDetails` fixture used in
the import investigation. The concrete loss boundary for direct import is
the import API contract: `GutterFormActivity.handleImportedNodeDetails()`
reads the selected `NodeDetails` returned by `getClosestNodeDetails` or
`getNodeDetailsByXyNum`. The authoritative fixture contains
`node_img[].url` and `fileCategory`, but no `id` or `img_id`. The client can
download the photo, but has no server image ID to place in
`photo{slot}ImgId`.

The supplied `storeDitch` response is a different contract and is available
only after the form's photo-upload phase. Its
`data.nodes[].url[].id` cannot retroactively populate the pre-submit import
form.

For the inspect → edit flow, `GutterInspectActivity.preloadEditableWaypoints()`
has a fallback from `nodeDetails.nodeImg[].id` to
`DitchDetails.nodes[].url[].id`. The supplied fixture proves that this path
can work when the `url[].id` values are present in the `getDitchDetails`
payload. A second loss point was found in the main-map `NodeDetails →
Waypoint` mapper: it dropped the photo IDs before opening the edit form. That
mapper now preserves the IDs and marks only ID-backed slots as uploaded.

### Affected files

- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/common/PhotoUploadSlotState.kt`
- `app/src/main/java/com/example/taoyuangutter/api/StoreDitchNodeRequestMapper.kt`
- `app/src/main/java/com/example/taoyuangutter/api/GutterApiModels.kt`
- `app/src/main/java/com/example/taoyuangutter/common/PhotoImgIdResolver.kt`
- `app/src/main/java/com/example/taoyuangutter/common/PhotoUploadSlotState.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterInspectActivity.kt`

### Regression risk

The client now defensively accepts both `node_img[]` and `url[]` when an ID is
actually present. For the observed URL-only import fixture, the client keeps
the downloaded photo as an unchanged successful import and does not invent an
ID or merge an unrelated `ditchDetails` response. Only a user replacement or
new capture enters the upload path.

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
