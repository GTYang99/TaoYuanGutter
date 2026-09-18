# Analysis

## Scope

Debug 0918 covers the current behavior of importing an existing waypoint,
the default visibility of the main-map `0910刪除資料` layer, and the title
alignment in the existing-waypoint import sheet.

## Evidence reviewed

- `GutterFormActivity.handleImportedNodeDetails()`
- `GutterRepository.downloadImageToLocalContentUri()`
- `PhotoUploadSlotState`
- `StoreDitchNodeRequestMapper`
- `MapOverlayController`
- `LayersBottomSheet` and `sheet_layers.xml`
- `bottom_sheet_import_existing_waypoint.xml`

## Current behavior

### Existing-waypoint photo import

`handleImportedNodeDetails()` resolves photo URLs by `fileCategory` 1, 2,
and 3, then calls `downloadImageToLocalContentUri()` sequentially. The local
URI is used to prefill the photo fields. The import APIs are
`getClosestNodeDetails` and `getNodeDetailsByXyNum`, and the selected object is
`NodeDetails`. The authoritative import fixture has `node_img[].url` and
`fileCategory`, but no `id`/`img_id`, so the URL download succeeds while no ID
can be written into form state. The client additionally accepts `url[].id`
when present, but cannot derive an ID from a URL alone.

The later upload gate uses `PhotoUploadSlotState.isAlreadyUploaded()`: only a
slot with a numeric `photo{slot}ImgId` is skipped. A `success` state without an
ID is not sufficient because `storeDitch` cannot reference that photo.
The `StoreDitchNodeRequestMapper` reads numeric IDs and maps them to `img_ids`,
for both create and update requests, except for virtual points and the
excluded slots of cannot-open points.

The supplied `A0910pt52` response is decisive for the edge case: its
`node_img[0]` contains `url` and `fileCategory`, but no `id` or `img_id`.
The import flow nevertheless writes `success` when the local download
succeeds. That means the current implementation skips multipart upload even
though no server image ID is available, and the mapper emits no `img_ids` for
that slot.

Therefore the intended rule is “a slot with `img_id` does not upload; a slot
without `img_id` must upload if it has a usable local photo.” For an imported
response that has no image ID, the downloaded photo remains a multipart upload
candidate; the returned new `img_id` is then sent in the existing-node
`storeDitch` update. A response that already has `url[].id` is preserved and
skips the redundant upload.

### Deleted-area layer

The default is `true` in the overlay controller state, the bottom-sheet
argument default, and the XML checkbox. The map controller consequently
creates the deleted-area WMS tile overlay during its initial overlay apply.

### Existing-waypoint title

The title row uses a horizontal layout with a fixed 48dp back button, a
weighted title TextView, and a right location button. The location button is
`gone` in this flow. Since `gone` removes its width, the weighted title is
centered in the remaining space rather than in the full title row.

## Expected behavior

- A downloaded unchanged existing photo must not be uploaded again. A new or
  replaced photo must be uploaded, and the returned `img_id` must be included
  in the existing-node `storeDitch` update.
- `0910刪除資料` must be unchecked and absent from the main map on first
  launch, while remaining user-toggleable.
- `既有點位資料` must be visually centered against the complete title row.

## Affected modules

- Import and photo state: `GutterFormActivity`, `PhotoUploadSlotState`,
  `StoreDitchNodeRequestMapper`.
- Map layer defaults: `MapOverlayController`, `LayersBottomSheet`,
  `sheet_layers.xml`.
- Import sheet header: `bottom_sheet_import_existing_waypoint.xml`.

## Risks

- Clearing or failing to preserve an imported `img_id` can cause duplicate
  photo uploads or a store request that no longer references the existing
  image.
- Changing the deleted-area default must not reset an explicitly selected
  layer state during recreation.
- A title-row layout fix must keep the back and location controls clickable
  without changing their hit targets.

## New API evidence: `storeDitch` response

The user-provided response for `POST /api/v1/ditch/storeDitch` contains the
authoritative server image IDs under:

`data.nodes[].url[].id`

For example, node `10979`, `fileCategory=1`, has `id=12339`. In this response
shape, `id` is the same server image identifier referred to elsewhere as
`img_id`.

The current client already parses this shape through `DitchDetails.nodes` →
`DitchNode.url` → `NodeImageUrl.id`. `StoreDitchResponseWaypointMapper` then
maps each `url[].id` by `fileCategory` into `photo1ImgId` / `photo2ImgId` /
`photo3ImgId`. The mapping is exercised by
`StoreDitchResponseParsingTest` and `StoreDitchResponseWaypointMapperTest`.

The remaining distinction is flow ownership: this mapper runs after a
successful `storeDitch` save in the map hosts. The existing-point import picker
itself queries `nodeDetails` / `closestNodeDetails`, whose model now accepts
both `NodeDetails.nodeImg` and `NodeDetails.url`. If the endpoint returns an
image ID in either shape, the ID is routed into the form state. The observed
fixture returns neither ID field, so the client-only fix cannot prevent
re-upload in that case.

## Updated conclusion

The correct root-cause boundary is the missing ID in the direct-import API
response. `storeDitch.data.nodes[].url[].id` proves the server has the ID in
that response, but does not make it available during pre-submit import. The
client can complete this fix only after the import endpoint returns the same
ID or provides a lookup endpoint.
