# Focused Debug Fix Plan

## Objective

Restore the intended photo-upload state through form result handling,
waypoint reversal, and final `storeDitch` submission without changing photo
slots, API endpoints, or virtual/cannot-open rules.

## Implementation

1. Preserve current-form photo metadata when a URI changes, while removing
   stale metadata from the replaced photo and clearing metadata for deletion.
2. Wait for an active single-photo coordinator upload before publishing the
   form result, so the live sheet cannot reverse or autosave a stale state.
3. Keep `PhotoUploadSlotState.isAlreadyUploaded()` as the final upload gate.
4. Add focused regression coverage for new upload, replacement, deletion,
   same-photo metadata preservation, and waypoint reversal.

## Validation scope

- Focused unit tests for the changed merge and nearby upload/request mapping.
- `:app:assembleDebug`.
- One focused emulator form-exit smoke test.
- Two focused physical-device instrumentation cases mapped to AC-001 and the
  related waypoint/photo-status regression risk.

Full regression, CI, real camera/network request counting, release, and
deployment are not part of this focused debug run and must remain explicitly
marked `NOT VERIFIED`.
