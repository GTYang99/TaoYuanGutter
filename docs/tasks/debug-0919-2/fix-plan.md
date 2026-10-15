# Minimum Fix Plan

This is the minimum implementation scope derived from the confirmed root
cause. It does not authorize unrelated refactoring.

1. Fix `AddGutterBottomSheet.updateWaypointBasicData()` so URI replacement
   invalidates old metadata without deleting a successful `img_id` and
   upload-success state supplied by the current form result.
2. Close the form-to-sheet completion gap for AC-001. If a single-photo upload
   is still completing when the form returns, its successful state must be
   rebound to the live waypoint before reversal, or reversal must merge the
   authoritative completion by waypoint identity instead of overwriting it
   with the stale live snapshot. The implementation must not rely on the form
   Activity listener remaining alive after `onDestroy()`.
3. Preserve the existing replacement transition in `GutterFormActivity`: old
   metadata must be cleared before the new upload begins. A genuinely new
   successful result must then be accepted by the sheet merge.
4. Keep `PhotoUploadSlotState.isAlreadyUploaded()` as the shared upload gate.
   Do not infer completion from a URI alone, and do not change endpoint names,
   `fileCategory` values, or virtual/cannot-open exclusions.
5. Keep `reverseWaypoints()` behavior limited to moving waypoint objects and
   renumbering display fields. Verify that each `img_id` remains attached to
   its waypoint after reversal.
6. Add focused regression coverage for:
   - a newly uploaded photo returned to an initially empty slot;
   - a successful replacement result with a new `img_id`;
   - deletion followed by a new capture in the same slot;
   - completed uploads followed by order reversal and submit, with zero
     duplicate `nodeImage` calls;
   - form returns before coordinator completion, coordinator completes before
     reversal, and reversal does not overwrite the completed state;
   - final `storeDitch` mapping using retained photo IDs;
   - the confirmed `storeDitch` response-order contract.
7. During implementation validation, capture the photo URI, waypoint identity,
   upload state, `img_id`, and request counts for `nodeImage` and `storeDitch`.
   Runtime evidence is currently unavailable and must not be reported as
   passed without a reproduction.

## Implemented choices

- `AddGutterBottomSheet` now uses `PhotoResultMetadataMerger`: changed photo
  URIs invalidate old metadata while retaining metadata supplied by the current
  form result; deleted URIs clear the former server metadata.
- `GutterFormActivity` waits for in-flight single-photo work before publishing
  a form result, then refreshes the session draft so the live sheet receives
  the completed state before reversal.
- The existing `PhotoUploadSlotState.isAlreadyUploaded()` gate and API request
  contracts are unchanged.

## Explicitly out of minimum scope

- changing `storeDitch` response mapping away from list order;
- replacing all index-based coordinator keys with a new identity model;
- broad draft/session synchronization refactoring;
- endpoint, payload contract, or special-mode behavior changes.
