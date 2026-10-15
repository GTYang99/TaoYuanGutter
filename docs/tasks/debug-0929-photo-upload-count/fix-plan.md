# Fix Plan

## Issue and root cause

- Issue: `ISS-001` (`implementation_regression`, P1)
- Root cause: `GutterRepository.uploadNodeImage()` currently returns `ApiResult.Success` for any
  HTTP-success response with `success=true`, even when `data.img_id` is null, zero, or otherwise
  unusable. Downstream flows then either mark the slot successful or count it as completed, while
  `StoreDitchNodeRequestMapper` can omit the slot from `img_ids`.

## Approved scope

The user explicitly requested implementation after the investigation. The scope is limited to the
photo-upload boundary and its regression tests:

1. Treat a node-image response as upload success only when `data.img_id` is a positive server image ID.
2. Convert a success response without a valid ID into the existing `ApiResult.Error` path so all current
   callers stop before `storeDitch`, preserve draft failure state, and expose the existing retry/save-draft UI.
3. Keep URL-only imported existing photos unchanged: they are downloaded from `nodeDetails`, marked as
   imported success, and do not call `uploadNodeImage()` unless the user replaces them.
4. Add boundary tests for valid ID, missing ID, and invalid/non-positive ID; retain existing mapper and
   imported-photo regression tests.

## Affected files

- `app/src/main/java/com/example/taoyuangutter/api/GutterRepository.kt`
- `app/src/test/java/com/example/taoyuangutter/api/GutterRepositoryNodeImageBoundaryTest.kt`
- `app/src/test/java/com/example/taoyuangutter/api/NodeImgDeserializationTest.kt`
- `app/src/test/java/com/example/taoyuangutter/api/StoreDitchNodeRequestMapperTest.kt`

## Acceptance criteria for the fix

- FIX-001: A positive `img_id` remains `ApiResult.Success` and is available to the existing callers.
- FIX-002: A `success=true` response with missing, null, zero, or negative `img_id` becomes
  `ApiResult.Error`; no caller can proceed as if the required upload completed.
- FIX-003: Existing URL-only imported photos still remain displayable and are not re-uploaded merely
  because their import response lacks an image ID.
- FIX-004: Special mode still requires one photo and normal mode still requires three; the fix does not
  weaken local validation or change `storeDitch` payload rules.
- FIX-005: Targeted JVM tests, affected debug compilation/build, and `git diff --check` pass; unavailable
  device/backend/CI evidence is recorded as `NOT VERIFIED`.

## Verification and rollback boundary

- The fixed revision must be committed before Independent Verification.
- No backend data will be created by local tests.
- If the backend contract intentionally permits success without an ID for newly uploaded photos, this
  fix will surface that contract mismatch as an explicit upload failure rather than silently losing a photo.
