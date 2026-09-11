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
- next_action: verification
