# Execution Report

## Implementation

- Branch: `feat/既有點位不上傳照片`
- Scope: `feat-0911-1`
- Existing unrelated change preserved: `app/src/main/java/com/example/taoyuangutter/api/GutterApiService.kt`
- Added `StoreDitchNodeRequestMapper` to omit `captured_at` and `img_ids` for existing nodes while preserving new-node behavior.
- Added `PhotoUploadCandidateResolver` and switched `MainActivity` and `MapWorkspaceFragment` to the shared resolver.
- Kept form/draft photo metadata intact; `PhotoUploadManager` now consumes resolved upload candidates.
- Added mapper JSON tests and resolver tests for unchanged photos, slot replacement, and draft resume.

## Validation

| Check | Result | Evidence |
|---|---|---|
| `git diff --check` | PASS | No whitespace errors reported. |
| `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean testDebugUnitTest --tests '*StoreDitchNodeRequestMapperTest' --tests '*PhotoUploadCandidateResolverTest'` | PASS | `BUILD SUCCESSFUL`; targeted mapper and resolver tests passed. |
| `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug` | PASS | `BUILD SUCCESSFUL`; debug APK assembled. |
| Host smoke / MockWebServer | NOT VERIFIED | Not run; implementation environment lacks the Java/Android test runtime. |

## Acceptance-Criteria Evidence Status

- AC-001: mapper JSON unit test PASS; host smoke `NOT VERIFIED`.
- AC-002: resolver unit coverage PASS; host smoke `NOT VERIFIED`.
- AC-003: slot replacement resolver unit coverage PASS; upload integration `NOT VERIFIED`.
- AC-004: new-node mapper JSON unit test PASS.
- AC-005: draft-resume resolver unit coverage PASS; host smoke `NOT VERIFIED`.

## Limitation

Local unit/build validation completed with the Android Studio bundled Java Runtime. Host-level MockWebServer/manual smoke remains `NOT VERIFIED` and must be covered during Verification.

## Follow-up Implementation Validation

- Root cause addressed: the imported-node path in `GutterFormActivity` was not covered by the first resolver extraction.
- `handleImportedNodeDetails()` now persists `NodeDetails.nodeId` as `_nodeId` and keeps it in the session waypoint.
- The form-level `uploadLocalPhotos()` now skips any slot with an existing `photo*ImgId`.
- Added a resolver test confirming imported photo IDs remain available to the upload guard.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew clean testDebugUnitTest --tests '*StoreDitchNodeRequestMapperTest' --tests '*PhotoUploadCandidateResolverTest'`: PASS (`BUILD SUCCESSFUL`).
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`: PASS (`BUILD SUCCESSFUL`).
- Host-level imported-node smoke observing `/v1/node/nodeImage`: NOT VERIFIED.
- Confirmed root-cause fixture: `docs/tasks/dbg-0910/evidence/node_details_A0910pt52.json` provides a `node_img` URL without an image `id`.
- Imported downloaded photos are now marked `UploadState=success`; upload guards accept either an existing `imgId` or success state.
- `PhotoUploadSlotState.isAlreadyUploaded()` now covers both numeric image IDs and imported server-backed photos whose response omits an ID.
- Follow-up targeted tests: PASS (`BUILD SUCCESSFUL`).
- Follow-up debug APK build: PASS (`BUILD SUCCESSFUL`).
