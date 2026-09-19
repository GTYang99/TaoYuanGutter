# Root Cause Analysis

## Task

- Task: `debug-0919-2`
- Branch: `fix/debug-0919-2-照片上傳流程`
- Scope: root-cause analysis only; no production code changed

## Common root cause for AC-001 and AC-002

`AddGutterBottomSheet.updateWaypointBasicData()` clears successful upload
metadata after merging the form result. It first executes `merged.putAll(data)`
and then, when the incoming URI differs from the old URI, removes the new
`photo{slot}ImgId`, `photo{slot}UploadState`, captured time, and error.

The branch was intended to remove stale metadata from the old photo, but its
placement after the merge removes the newly returned successful ID as well.
This applies both to a previously empty slot receiving a new photo and to an
existing photo being replaced.

The waypoint then contains a usable photo URI but no successful
`photo{slot}ImgId` or upload-success state. The submit gate correctly treats
that state as not uploaded and calls `nodeImage` again.

## AC-001 mapping

After all single-photo uploads complete, reversing the waypoint order must
preserve each photo's `img_id` with its owning waypoint and must not trigger a
second upload. The actual path is:

1. The completed form upload returns URI + `img_id` + success state.
2. `updateWaypointBasicData()` removes that ID/state because the URI differs.
3. `reverseWaypoints()` moves the `Waypoint` objects but cannot restore the
   removed metadata.
4. Submit sees a usable URI and `isAlreadyUploaded == false`.
5. The sheet calls `uploadNodeImage()` again.

The user confirmed that all individual uploads have completed before reversal.
This rules out an in-flight coordinator completion as the primary cause. The
reversal operation itself is not destructive: `waypoints.reverse()` preserves
each object's `basicData`.

### Deterministic state transition

For a newly filled slot, the code produces this state transition:

```text
form result: photo1=URI, photo1ImgId=123, photo1UploadState=success
       ↓ updateWaypointBasicData()
live waypoint: photo1=URI, photo1ImgId=<absent>, photo1UploadState=<absent>
       ↓ reverseWaypoints()
same photo object, same incomplete basicData
       ↓ isAlreadyUploaded()
false → uploadNodeImage()
```

If the form returns before the coordinator finishes, the second deterministic
path is that `GutterFormActivity.onDestroy()` unregisters its listener while
the application-scoped coordinator continues. The coordinator may write a
success ID into the persisted draft, but the live sheet still lacks it; the
reversal callback then autosaves the live list and can overwrite the draft
success state. This also satisfies the reported premise that the upload is
complete before reversal.

## AC-002 mapping

After inspect → edit → single-photo replacement succeeds, the returned
`img_id` must reach the final `storeDitch` request without another
`nodeImage` upload. The form clears old metadata before upload and the
coordinator writes the new ID/state. The sheet-side merge then removes that
new ID/state on the URI-difference branch. The final upload gate therefore
starts a duplicate upload, and the request mapper can only use the metadata
that remains.

## Identity and response-order findings

- `Waypoint.uid` is normally a client-generated local/session identity. Some
  legacy conversion code copies an API node ID into it, but that does not make
  it the canonical update field.
- `basicData["_nodeId"]` / API `nodeId` is the backend identity used for
  updates. It is not interchangeable with `uid`.
- The user confirmed that `storeDitch` response order equals submitted
  waypoint order. `StoreDitchResponseWaypointMapper.mapIndexed` is therefore
  accepted for this task and is not a root cause.

## Evidence

- `AddGutterBottomSheet.kt:2112-2128` merges the result and then removes the
  new photo metadata whenever the URI differs.
- `GutterFormActivity.kt:1931-1940` stores the coordinator's success ID/state
  in `currentFormData`; `GutterFormActivity.kt:1971-1988` snapshots it for the
  result; and `GutterFormActivity.kt:2294-2320` dispatches it.
- `GutterFormContract.kt:191-205,242-256` exports and reads the photo ID/state
  fields. `MainActivity.kt:459-468` and `MapWorkspaceFragment.kt:1306-1309`
  pass the result through the sheet merge.
- `GutterFormActivity.kt:157-176` clears old replacement metadata before
  upload and `GutterFormActivity.kt:1421-1432` applies the new upload state.
- `PhotoSlotUploadCoordinator.kt` writes successful `img_id` and state.
- `AddGutterBottomSheet.kt:1477-1478` reverses the list without clearing
  `basicData`.
- `AddGutterBottomSheet.kt:2142-2146` immediately emits the merged result to
  the host autosave callback, persisting the erased state.
- `MainActivity.kt:588-637` autosaves every reversal callback, and
  `GutterFormActivity.kt:1444-1471` removes the form listener on destruction.
- `AddGutterBottomSheet.kt:1965-2027` reuploads a usable photo when the ID or
  success state is absent.
- Commit `934d139` introduced the URI-difference clearing branch.

## Regression risk

The fix must distinguish old-photo invalidation from new-result acceptance:

- old metadata must not survive a real replacement;
- newly returned successful metadata must not be erased;
- deletion followed by a new capture must not inherit the deleted ID;
- reversal must preserve the photo-to-waypoint association.

The index-based coordinator and draft merge remain secondary risks for a
separate in-flight or stale-snapshot reproduction. They should be covered by
tests but are not part of the minimum confirmed cause for this task.

## Status

- Root cause: identified and mapped to both acceptance criteria.
- AC-001 code-level confidence: 95% under the confirmed completed-before-
  reversal premise.
- Minimum fix scope: documented in `fix-plan.md`.
- Runtime duplicate-request count: `NOT VERIFIED` because no runtime/network
  trace was supplied.
