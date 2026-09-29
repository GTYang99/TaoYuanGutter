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

## NOT VERIFIED

- No live response for the reported operation proving `success=true` with missing/invalid `data.img_id`.
- No correlated `nodeImage` → `storeDitch` → server-record trace for the reported node.
- No CI workflow/result was available in this worktree.
- No physical-device reproduction was run in this investigation.

## Decision

The codebase makes incomplete final photo upload **possible** under the conditional response shape above.
The reported case itself remains **NOT VERIFIED** until the required request/response correlation evidence is
available. Route ISS-001 to `debug` if a production fix is authorized; keep ISS-002 in `investigation` until
the live contract/case is captured.
