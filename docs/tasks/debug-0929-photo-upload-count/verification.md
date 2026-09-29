# Verification Report

## Verification round

- Date: `2026-09-29`
- Production revision under test: `ccaab159bd4fad9768a7044482fab12882ca42f5`
- Verification worktree: `/Users/a10362/.codex/worktrees/photo-upload-investigation/TaoYuanGutter`
- Branch: `codex/debug-0929-photo-upload-count`
- `git diff ccaab159..HEAD -- app/src/main app/src/test`: empty
- Worktree source/test state: clean after removing the temporary ignored `local.properties`
- Package: `com.example.taoyuangutter`
- Build variant: `debug`
- Device: `emulator-5554`, `sdk_gphone64_arm64`, Android 14 / API 34

Independent Verification used the committed production fix revision. The later branch commits only
contain task artifacts; no production or test source differs from `ccaab15`.

## Implementation review

- `GutterRepository.uploadNodeImage()` is the shared boundary for direct submit, background
  coordination, form activity upload, and batch upload manager paths.
- A response is successful only when `success=true` and `data.img_id` is a positive integer.
- Missing, zero, or negative IDs use the existing error path and cannot be treated as a completed
  upload before `storeDitch`.
- Unchanged URL-only imported photos remain on the import/download path and are not forced through
  `uploadNodeImage()`.

## Acceptance criteria

### Original investigation criteria

| AC | Result | Evidence |
|---|---|---|
| AC-001 | PASS | Source review and `GutterCompletionPolicyTest`; special mode requires slot 1, normal mode requires slots 1–3, virtual mode requires none. Device `GutterBasicInfoUiTest` passed 11/11. |
| AC-002 | PASS | Source review traces `nodeImage` through repository result handling to `storeDitch.img_ids`; boundary and mapper tests pass. Device mapper test passed 1/1. |
| AC-003 | PASS | `analysis.md`, `root-cause.md`, and `issue-log.md` distinguish local photo validation, payload loss, and the separate progress-count issue. |
| AC-004 | NOT VERIFIED | No correlated live `nodeImage` response, `storeDitch` request, and server record exists for the reported operation; backend success-without-ID contract is still unconfirmed. |
| AC-005 | PASS | Targeted JVM suite and full `:app:testDebugUnitTest` pass with 136 tests and no failures. |

### Fix criteria

| FIX | Result | Evidence |
|---|---|---|
| FIX-001 | PASS | `GutterRepositoryNodeImageBoundaryTest.positiveImageIdRemainsSuccessful` passes; positive ID remains `ApiResult.Success`. |
| FIX-002 | PASS | Boundary tests for missing and non-positive IDs pass; the shared repository boundary returns `ApiResult.Error`. |
| FIX-003 | PASS | Source review, `PhotoUploadCandidateResolverTest`, and device `GutterImportExistingWaypointUiTest` 1/1 pass; unchanged URL-only import remains unaffected. |
| FIX-004 | PASS | Completion-policy, mapper, form UI, and device contract tests preserve special/normal photo requirements. |
| FIX-005 | PASS | Targeted tests, full unit tests, debug APK assembly, instrumentation compilation, and `git diff --check` pass. Unavailable CI/backend evidence is recorded separately as `NOT VERIFIED`. |

## Validation executed

### JVM and build

```text
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest \
  --tests com.example.taoyuangutter.api.GutterRepositoryNodeImageBoundaryTest \
  --tests com.example.taoyuangutter.api.NodeImgDeserializationTest \
  --tests com.example.taoyuangutter.api.StoreDitchNodeRequestMapperTest \
  --tests com.example.taoyuangutter.gutter.GutterCompletionPolicyTest \
  --tests com.example.taoyuangutter.gutter.PhotoUploadCandidateResolverTest \
  --tests com.example.taoyuangutter.gutter.PhotoResultMetadataMergerTest \
  --console=plain --no-daemon
```

Result: `BUILD SUCCESSFUL`.

```text
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug :app:compileDebugAndroidTestKotlin --console=plain --no-daemon
```

Result: `BUILD SUCCESSFUL`; debug APK and instrumentation sources compiled.

```text
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest --console=plain --no-daemon
```

Result: `BUILD SUCCESSFUL`; 136 tests, 0 failures, 0 errors, 0 skipped.

Additional checks:

- `git diff --check`: PASS.
- Production/test diff from `ccaab159` to verification branch tip: empty.
- Temporary `local.properties` contained only `MAPS_API_KEY=verification-placeholder` and was removed.

### Device-focused regression slices

All were run with `:app:connectedDebugAndroidTest` and the Android instrumentation runner class
argument on `emulator-5554`:

| Test slice | Result | Scope |
|---|---|---|
| `GutterBasicInfoUiTest` | PASS, 11/11 | Required photo modes and related form regression |
| `GutterImportExistingWaypointUiTest` | PASS, 1/1 | URL-only existing-photo import path |
| `StoreDitchResponseWaypointMapperInstrumentedTest` | PASS, 1/1 | Returned photo IDs mapped into draft slots |
| `GutterFormContractInstrumentedTest` | PASS, 2/2 | Form data boundary preservation |

Total device slice: 15/15 passed.

## Regression and limitations

- PASS: positive image ID remains successful.
- PASS: missing, zero, and negative image IDs become an explicit repository error.
- PASS: special mode and normal mode photo-slot rules remain intact.
- PASS: unchanged URL-only imported photos remain displayable without forced upload.
- NOT VERIFIED: controlled authenticated `nodeImage` failure response, retry UI after that response,
  subsequent `storeDitch`, and final server-side photo record for the reported incident.
- NOT VERIFIED: repository CI workflow/result; no CI workflow is present in the fixed branch.

## Issues

- `ISS-002`: original incident is not correlated to a live request/response/server record.
- `ISS-004`: backend correlation and CI evidence remain unavailable; device evidence is now available
  and passed.

## Failure classification

`environment` / `unknown` for the unavailable external backend and CI evidence. No implementation
failure was observed in the executed local or device slices.

## Final result

NOT VERIFIED

Local implementation and test-machine regression evidence pass. Release remains blocked by AC-004
and the missing CI/backend evidence.
