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
- Added an early guard in `GutterFormActivity.onPhotoSlotReadyForUpload()` so an existing photo cannot enqueue `PhotoSlotUploadCoordinator`.
- Follow-up targeted tests: PASS (`BUILD SUCCESSFUL`).
- Follow-up debug APK build: PASS (`BUILD SUCCESSFUL`).

## API Contract Follow-up Validation

- Added Gson compatibility mapping so `NodeImg.id` accepts both `id` (ditchDetails contract) and `img_id` (upload-style contract).
- Added `NodeImgDeserializationTest` covering both response field names.
- Emulator `ty04` direct-import evidence: the supplied `/v1/node/nodeDetails?XY_NUM=ty04` response contains `node_img` entries with only `url` and `fileCategory`; runtime recorded downloaded photos with `imgId=null` and `UploadState=success`.
- Emulator observed no `/v1/node/nodeImage` request during the import flow.
- Targeted mapping/resolver/mapper tests: PASS (`BUILD SUCCESSFUL`).
- Debug APK build: PASS (`BUILD SUCCESSFUL`).
- `photo*ImgId` population for the current ty04 direct-import response: NOT VERIFIED / unavailable because that response omits the ID; backend must return `node_img[].id` (or `img_id`) or provide a lookup endpoint.

## StoreDitch Response Persistence Follow-up

- Root cause fixed: `persistServerIdsIntoDraft()` previously persisted only `SPI_NUM` and `_nodeId`; it now applies `storeDitch.data.nodes[].url[].id` by `fileCategory` to `photo1ImgId`/`photo2ImgId`/`photo3ImgId`.
- Fixed both `MainActivity` and `MapWorkspaceFragment` save callbacks to use the same response mapper; either map save entry now persists the returned photo IDs.
- Added `StoreDitchResponseWaypointMapper` and tests for all three photo categories, node ID persistence, and preservation when a response item has no ID.
- Added a controlled Gson response fixture matching `storeDitch.data.nodes[].url[].id`; it verifies the API response model preserves all three IDs and the mapper writes them to the draft photo slots.
- Targeted storeDitch mapper, response model, request mapper, resolver tests: PASS (`BUILD SUCCESSFUL`).
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest --tests '*StoreDitchResponseParsingTest' --tests '*StoreDitchResponseWaypointMapperTest' --tests '*NodeDetailsExtensionsTest' --tests '*NodeImgDeserializationTest' --tests '*PhotoUploadCandidateResolverTest'`: PASS (`BUILD SUCCESSFUL`).
- Debug APK build: PASS (`BUILD SUCCESSFUL`).
- Full `./gradlew testDebugUnitTest`: PASS (`BUILD SUCCESSFUL`).
- Full `./gradlew assembleDebug`: PASS (`BUILD SUCCESSFUL`).
- Emulator ty04 import: PASS for reaching the imported form and displaying all three downloaded photos; the live trace showed `nodeDetails` photos have `imgId` empty because that response contains no ID.
- Emulator AA0019 import on the null-safe APK: PASS; all three downloaded photo slots reached `state=success`, no `captured_at unavailable`/`NullPointerException` was logged, and no `/v1/node/nodeImage` request was observed during the import flow.
- Emulator storeDitch save-and-reopen smoke: NOT VERIFIED. After filling the intermediate rows, the only available fixture remained `ty04`; using it for all positions triggered the app's `座標編號重複` validation before `storeDitch`. No claim is made for runtime response-ID persistence from this run.
- Emulator nodeImage observation: PASS for the completed ty04 form-import flow; no `/v1/node/nodeImage` request was observed before the blocked bottom-sheet save. StoreDitch post-response behavior remains covered by mapper unit tests, not by this emulator run.
- The final `storeDitch` submit action was not repeated after the successful unique-point setup because it creates persistent backend data; runtime response-ID persistence remains covered by mapper tests and requires one authorized controlled backend save.
- The controlled response fixture closes the model/mapping validation gap without writing a duplicate record to the demo backend; an end-to-end emulator observation of a real `storeDitch` success response remains `NOT VERIFIED`.
- Added backend-free Android instrumentation coverage using the `storeDitch` success-response fixture; it runs Gson parsing and `StoreDitchResponseWaypointMapper` on the emulator and asserts `_nodeId` plus all three `photo*ImgId` values.
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew connectedDebugAndroidTest`: PASS (`BUILD SUCCESSFUL`; 14 instrumentation tests passed on `Medium_Phone(AVD) - 14`). No backend request is made by this test.
- The real demo-backend `storeDitch` submit remains `NOT VERIFIED`; the emulator instrumentation test verifies the same response-to-draft mapping in the APK runtime without creating persistent data.
