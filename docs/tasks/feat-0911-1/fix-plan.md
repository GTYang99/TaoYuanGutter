# Debug Fix Plan

## Scope

Fix `ISS-002` in `AddGutterBottomSheet` only. No API contract or product
requirement changes.

## Minimum Fix

1. Replace the two bottom-sheet pre-submit numeric-ID checks with
   `PhotoUploadSlotState.isAlreadyUploaded(...)`.
2. Add regression tests for imported photos with `UploadState=success` and no
   image ID, while confirming idle/replaced slots remain uploadable.
3. Run targeted unit tests, full unit tests, debug build, and connected
   instrumentation tests.

## Acceptance Mapping

- AC-002 / AC-005: imported server-backed photos without an ID are not queued
  by the bottom-sheet submit path.
- AC-003: a replaced slot without a successful server state remains eligible
  for upload.
- AC-004: new photos retain existing upload behavior.

## Exit Criteria

- Root cause is documented.
- Tests fail before the fix where practical and pass after it.
- No unrelated production changes are introduced.
