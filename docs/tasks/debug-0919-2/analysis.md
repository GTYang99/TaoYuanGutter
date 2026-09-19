# Repository Analysis

## Scope

This task covers the two photo paths described in the attached debug note:
waypoint order reversal before gutter submission, and inspect → edit →
single-photo upload → gutter update. It includes the authorized minimum fix
and focused validation; it does not claim full regression or release
readiness.

## Evidence reviewed

- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormContract.kt`
- `app/src/main/java/com/example/taoyuangutter/common/PhotoSlotUploadCoordinator.kt`
- `app/src/main/java/com/example/taoyuangutter/common/PhotoUploadSlotState.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/PhotoUploadManager.kt`
- `app/src/main/java/com/example/taoyuangutter/api/StoreDitchNodeRequestMapper.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/StoreDitchResponseWaypointMapper.kt`
- `app/src/main/java/com/example/taoyuangutter/MainActivity.kt`
- existing photo-upload tests and prior debug records under `docs/tasks/`

## Current behavior

### 1. Common form-result merge defect

The form/coordinator returns a photo URI together with its successful
`photo{slot}ImgId` and `photo{slot}UploadState`. `MainActivity` and
`MapWorkspaceFragment` pass that result to
`AddGutterBottomSheet.updateWaypointBasicData()`.

That method first executes `merged.putAll(data)`, then compares the incoming
URI with the previous URI. If the URI differs, it removes
`photo{slot}CapturedAt`, `photo{slot}ImgId`, `photo{slot}UploadState`, and
`photo{slot}UploadError` from the already merged map. The branch was intended
to clear stale metadata, but it also clears the newly returned successful ID
and state. This occurs both when a previously empty slot receives a new photo
and when an existing photo is replaced.

At submit, the URI remains usable but `PhotoUploadSlotState.isAlreadyUploaded()`
returns false, so the sheet calls `uploadNodeImage()` again.

The successful result path is independently visible in the source: the form
stores success state in `currentFormData`, `currentFormSnapshot()` copies that
map into the result, `GutterFormContract.putResultData()` exports the
`photo{slot}ImgId` and `photo{slot}UploadState` extras, and the host reads them
before calling `updateWaypointBasicData()`. Therefore the missing metadata is
not absent at the upload/result boundary; it is removed by the sheet merge.

### 2. Waypoint order reversal

`reverseWaypoints()` calls `waypoints.reverse()` and renumbers only display
type/label. The `Waypoint` object and its `basicData` move together; reversal
does not itself remove an ID. The user confirmed that every individual upload
has completed before reversal, so an in-flight completion race is excluded as
the primary explanation.

The repeated upload is observed after reversal because the earlier merge has
already removed the success metadata. Reversal preserves that incomplete
state, and the submit gate treats the photo as a new upload candidate.

`updateWaypointBasicData()` immediately invokes `onWaypointsChanged`, so the
incomplete map is also passed to the host's draft autosave before the user
reverses the list. This makes the loss persistent across the subsequent
submit-time synchronization.

The coordinator's position-based task key remains a future risk if an upload
is genuinely in flight during reorder, but it is not the primary cause under
the confirmed completed-upload premise.

### AC-001 deterministic state proof

For a newly filled slot, the static state transition is:

```text
form result: photo1=URI, photo1ImgId=123, photo1UploadState=success
       ↓ updateWaypointBasicData()
live waypoint: photo1=URI, photo1ImgId=<absent>, photo1UploadState=<absent>
       ↓ reverseWaypoints()
same photo object, same incomplete basicData
       ↓ isAlreadyUploaded()
false → uploadNodeImage()
```

There is also a second timing path consistent with the user observation. If
the form returns before the coordinator finishes, `GutterFormActivity` later
unregisters its listener in `onDestroy()`. The coordinator can still write the
successful ID to the persisted draft, but the live sheet remains without that
state. The reversal callback then autosaves the live waypoint list and can
overwrite the draft success state. In both timing paths, the upload is
complete before reversal but the submit-time waypoint lacks the success gate.

### 3. Identity and response order

`Waypoint.uid` is normally a client-generated local/session identity. Some
legacy conversion code copies an API node ID into `uid`, but that is an
implementation shortcut, not the canonical update field. The API update
identity is `basicData["_nodeId"]` / `nodeId`, which is passed to the request
mapper. The user confirmed that `storeDitch` response order equals submitted
waypoint order, so the existing index-based response mapper is valid and is
not a root cause.

## Expected behavior

- A waypoint's stable identity and photo metadata must survive list reordering;
  only its display position/type/label should change.
- A replacement transition must clear old metadata before the replacement
  upload, but a successful result must retain the new `img_id` and success
  state when it returns to the gutter sheet.
- `PhotoUploadSlotState.isAlreadyUploaded()` must be the same gate used by the
  form, gutter-sheet submit, and batch manager.

## Affected modules

- Waypoint identity and reorder persistence:
  `AddGutterBottomSheet`, `MainActivity`, `GutterDraftCoordinator`,
  `PhotoSlotUploadCoordinator`.
- Form-to-sheet metadata handoff:
  `GutterFormActivity`, `GutterFormContract`, `AddGutterBottomSheet`.
- Final upload gates and request mapping:
  `PhotoUploadSlotState`, `PhotoUploadManager`,
  `StoreDitchNodeRequestMapper`.
- Regression tests for reorder, successful replacement, and no duplicate
  `nodeImage` calls.

## Dependencies

- `photo{slot}ImgId` and `photo{slot}UploadState` propagation through the
  activity result contract.
- The distinction between old-photo invalidation and new-result acceptance.
- `storeDitch` request `img_ids` mapping under the confirmed response-order
  contract.

## Risks

- Fixing only the UI list order will not restore metadata already erased by
  the form-result merge.
- Comparing raw URI strings can classify a normalized copy of the same photo as
  a replacement and clear valid metadata.
- A broad metadata merge fix could preserve an old ID for a genuinely replaced
  photo; old metadata must be cleared before upload, while new successful
  metadata must be accepted afterward.
- Changing node/waypoint pairing is unnecessary under the confirmed API
  ordering contract and would broaden the fix scope.

## Evidence limitations

- No runtime trace or network capture for debug-0919-2 was provided.
  Code-level root cause and failed-AC mapping are confirmed at 95% confidence
  under the completed-before-reversal premise; duplicate `nodeImage` request
  count is `NOT VERIFIED`.
- Focused unit, build, emulator, and physical-device evidence exists. Full
  regression, CI, independent verification, and release evidence remain
  pending.
