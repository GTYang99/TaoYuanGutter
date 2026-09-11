# Execution Report

## Implementation

- Branch: `uiFix/既有點位不上傳照片`
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
