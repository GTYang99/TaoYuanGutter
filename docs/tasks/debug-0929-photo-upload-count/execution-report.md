# Investigation Evidence

## Fixed revision and scope

- Revision: `fbb198370a2dbf7bad38876f2d1e21be5bca75f1`
- Worktree: `/Users/a10362/.codex/worktrees/photo-upload-investigation/TaoYuanGutter`
- Production code changes: none.
- Investigation boundary: required-photo validation, pre-submit photo upload, upload-state persistence,
  `storeDitch` image-ID mapping, coordinator path, and historical evidence.

## Source review result

- Required-slot policy: PASS by source review. Special mode is `[1]`; normal mode is `[1,2,3]`.
- Missing local path: PASS by source review. Form and bottom-sheet validation block the submit.
- Success-without-ID handling: RISK FOUND. `success=true` does not require `data.img_id`; state can become
  success while the request mapper omits the slot.
- Progress mismatch: KNOWN SECONDARY ISSUE. It can explain `0/X`, not enough to explain fewer server photos.
- Historical contract evidence: the original `nodeImage` success model was URL-only; `img_id` was added
  later as nullable, while the repository success predicate remained `HTTP success && success=true`.

## Automated validation

Command executed with the Android Studio bundled JBR:

```text
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest \
  --tests com.example.taoyuangutter.gutter.GutterCompletionPolicyTest \
  --tests com.example.taoyuangutter.api.StoreDitchNodeRequestMapperTest \
  --tests com.example.taoyuangutter.gutter.PhotoUploadCandidateResolverTest \
  --tests com.example.taoyuangutter.gutter.PhotoResultMetadataMergerTest \
  --tests com.example.taoyuangutter.gutter.SubmittedRetrySnapshotTest \
  --console=plain
```

Result: `BUILD SUCCESSFUL in 50s`; 32 actionable tasks; no test failure reported.

The first attempt was blocked by the repository's required `MAPS_API_KEY` placeholder. The targeted tests
were then run with a temporary ignored `local.properties` placeholder, which was removed after the run.
No production source or tracked configuration was changed.

## Additional evidence tests

After the initial report, the following non-production tests were added and passed on the same worktree:

```text
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest \
  --tests com.example.taoyuangutter.api.NodeImgDeserializationTest \
  --tests com.example.taoyuangutter.api.StoreDitchNodeRequestMapperTest \
  --tests com.example.taoyuangutter.gutter.GutterCompletionPolicyTest \
  --console=plain
```

Result: `BUILD SUCCESSFUL`; JUnit XML reports 4 + 9 + 6 tests, all with zero failures and zero errors.
The added tests demonstrate that a successful response without `img_id` is accepted by the model and that
the mapper produces no ID for the special mode or silently drops one slot in the normal mode. They validate
the code-level possibility, not the reported user's live backend response.

## Confidence update

- Code-level incomplete-upload path: **99% credible**.
- Local missing-photo validation path being bypassed: **1% or less** based on current source/tests.
- Backend sometimes returning a valid ID: **95% credible** from the authenticated live sample; this does
  not establish that every success response has an ID.
- Attribution to the specific user report: **below 95%**, pending correlated runtime evidence.

The temporary ignored `local.properties` used for the Gradle `MAPS_API_KEY` placeholder was removed after
the run.

## NOT VERIFIED

- No live response for the reported operation proving `success=true` with missing/invalid `data.img_id`.
- No correlated `nodeImage` → `storeDitch` → server-record trace for the reported node.
- No CI workflow/result was available in this worktree.
- No physical-device reproduction was run in this investigation.

## Implementation update

- Production fix: `GutterRepository.uploadNodeImage()` now returns success only when the response
  contains a positive `data.img_id`; missing, zero, or negative IDs use the existing `ApiResult.Error`
  path. This is the shared boundary used by the direct submit, background coordinator, and batch manager.
- URL-only imported photos remain unchanged because the import flow downloads them and does not call
  `uploadNodeImage()` unless the user replaces the photo.
- Added `GutterRepositoryNodeImageBoundaryTest` covering valid, missing, and non-positive IDs.
- Production fix commit: `ccaab159bd4fad9768a7044482fab12882ca42f5` on
  `codex/debug-0929-photo-upload-count`.

## Implementation validation

Targeted regression command:

```text
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest \
  --tests com.example.taoyuangutter.api.GutterRepositoryNodeImageBoundaryTest \
  --tests com.example.taoyuangutter.api.NodeImgDeserializationTest \
  --tests com.example.taoyuangutter.api.StoreDitchNodeRequestMapperTest \
  --tests com.example.taoyuangutter.gutter.GutterCompletionPolicyTest \
  --tests com.example.taoyuangutter.gutter.PhotoUploadCandidateResolverTest \
  --tests com.example.taoyuangutter.gutter.PhotoResultMetadataMergerTest \
  --tests com.example.taoyuangutter.gutter.SubmittedRetrySnapshotTest \
  --console=plain
```

Result: `BUILD SUCCESSFUL`; 38 tests, 0 failures, 0 errors.

Build command:

```text
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain
```

Result: `BUILD SUCCESSFUL` in 11s; debug APK generated at
`app/build/outputs/apk/debug/app-debug.apk`. The Android Gradle build reported only existing deprecation
warnings and native-library strip warnings; no build failure.

## Verification handoff

- Worktree must remain unchanged for Independent Verification at commit
  `ccaab159bd4fad9768a7044482fab12882ca42f5`.
- Package: `com.example.taoyuangutter`.
- Build variant: `debug`.
- APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Required runtime cases: special mode with missing-ID response, normal mode with one missing-ID response,
  valid-ID success, URL-only imported photo without replacement, and retry after the explicit upload error.
- No test account or secret is recorded in this artifact.

## Decision

The codebase makes incomplete final photo upload **possible** under the conditional response shape above;
the production fix now prevents that response from being treated as a completed upload. The reported case
itself remains **NOT VERIFIED** until the required request/response correlation evidence is available.
The implementation is ready for Independent Verification; CI and physical-device evidence remain pending.
