# Issue Log

## ISS-001

```yaml
issue_id: ISS-001
task_id: debug-0929-photo-upload-count
phase: investigation
category: implementation_regression
priority: P1
title: Successful nodeImage response without img_id can be silently omitted from storeDitch
status: open
impact: A required photo can have a usable local path and success state but no numeric server image ID; the final img_ids list can therefore contain fewer photos than the mode requires.
repro_steps:
  - Provide a required photo path for a special or normal waypoint.
  - Make nodeImage return HTTP success with success=true and data.img_id missing, null, or non-numeric.
  - Observe AddGutterBottomSheet writing UploadState=success and no photo{slot}ImgId.
  - Build StoreDitchNodeRequestMapper output and inspect img_ids.
expected: A successful required upload has a valid server image ID before storeDitch, or the flow blocks with an explicit error.
actual: The client accepts success=true without validating img_id, and mapNotNull omits the slot from img_ids.
evidence:
  - app/src/main/java/com/example/taoyuangutter/api/GutterRepository.kt:648-655
  - app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt:2205-2215
  - app/src/main/java/com/example/taoyuangutter/common/PhotoUploadSlotState.kt:23-25
  - app/src/main/java/com/example/taoyuangutter/api/StoreDitchNodeRequestMapper.kt:27-33
next_action: debug
owner: developer
```

## ISS-002

```yaml
issue_id: ISS-002
task_id: debug-0929-photo-upload-count
phase: investigation
category: unknown
priority: P1
title: The reported live case is not correlated to a nodeImage response and final storeDitch request
status: open
impact: Static source evidence proves a possible loss path, but cannot prove that the backend returned the triggering response for the user's report.
repro_steps:
  - Obtain the reported operation's request/response trace and local draft snapshot.
  - Correlate each required photo slot from nodeImage through storeDitch to the server record.
expected: A complete trace identifies whether the loss happened at local validation, nodeImage response handling, request mapping, or backend persistence.
actual: Repository evidence contains one successful img_id sample and a separate URL-only import fixture, but no trace for this report.
evidence:
  - docs/tasks/debug-0919-2/verification.md:66-68
  - docs/tasks/dbg-0910/evidence/node_details_A0910pt52.json
  - docs/tasks/debug-0929-photo-upload-count/root-cause.md
next_action: investigation
owner: verifier
```

## ISS-003

```yaml
issue_id: ISS-003
task_id: debug-0929-photo-upload-count
phase: investigation
category: implementation_regression
priority: P2
title: Photo progress total can differ from the slots actually processed
status: open
impact: The overlay can show a 0/X completion flash when coordinator state resolves between counting and execution; this harms observability but does not by itself prove fewer server photos.
repro_steps:
  - Start submit with a pending-photo count.
  - Resolve the slot through coordinator state before ensure processes it.
  - Observe the slot skipped without a corresponding progress callback.
expected: Progress total and progress callbacks represent the same resolved candidate set.
actual: countPendingPhotoUploads() and ensureWaypointPhotosUploadedBeforeSubmit() use different candidate conditions.
evidence:
  - docs/tasks/feat-0917/root-cause.md:47-68
  - app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt:2111-2144
next_action: debug
owner: developer
```
