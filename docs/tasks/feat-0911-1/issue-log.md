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
- resolution:
  - Persist imported `NodeDetails.nodeId` into form/session `_nodeId`.
  - Skip slots with an existing `photo*ImgId` in the form-level uploader.
  - Added imported photo metadata preservation coverage.
- next_action: verification
