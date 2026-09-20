# Verification Report

## Revision and worktree

- Commit under test: `2306d0d05a12996f6409fbb8dda307634dc846a3`
- Branch: `fix/debug-0919-2-照片上傳流程`
- Production revision remained unchanged during verification.
- The pre-existing untracked `.worktrees/` directory was not modified or
  included.
- Verification artifacts were updated after evidence collection; those local
  document changes are not part of the production revision under test.

## Verification scope

Focused verification was limited to the two approved debug acceptance
criteria and their direct regression risks:

- photo metadata merge and waypoint reversal;
- replacement metadata retention and submit-time upload gating;
- retained photo IDs in `storeDitch` mapping;
- one focused Android device case for upload-complete row behavior;
- one focused Android device case for completed form exit behavior.

Full regression, CI, camera/network reproduction, and unrestricted device
testing were not run.

## Infrastructure investigation

The infrastructure check was run against the fixed revision above.

Environment evidence:

- Android device was discoverable as Sony XQ-AU52 / Android 12 with serial
  `adb-QV710EDR3A-hF5XZF._adb-tls-connect._tcp`.
- The debug APK and the two focused instrumentation cases had already run
  successfully on that device, so SDK, installation, ADB transport, and basic
  device execution were available.
- The debug build already enables OkHttp BODY logging. The application also
  emits filtered tags for `PhotoUpload` and `StoreDitch`, including request and
  response information; no production logging change was needed.
- Before the live flow, a filtered read of the most recent 500 device log
  lines using `OkHttp`, `PhotoUpload`, and `StoreDitch` returned no entries.
- After the live flow, the same filtered logging produced request and response
  evidence without recording credentials or tokens.

The controlled live flow was available for one new-photo branch, but the
backend returned HTTP 404 for `storeDitch`. No credentials were stored and no
additional data-mutating request was started after that response.

Classification: **environment**. The repository, debug APK, device, and
focused tests are runnable; the remaining missing evidence is a controlled
reversal flow, an existing-photo replacement fixture, and successful backend
persistence after `storeDitch`.

## Live login and fixture attempt

- The debug APK was installed from the fixed revision and launched on the
  Sony XQ-AU52.
- Login succeeded and authenticated map `scopeSearch` requests returned HTTP
  200. Credentials and tokens are intentionally not recorded here.
- The available pending-draft entries initially displayed the app's
  `資料加載不完整` warning because their existing photo data could not be
  loaded. No existing draft was submitted in that state.
- An editable waypoint then accepted one newly captured test photo. The user
  manually completed the form and pressed `更新側溝`.
- Live evidence: one `nodeImage` request and one HTTP 200 response returned
  `img_id=51863`; one `storeDitch` request contained `img_ids=[51863]` for
  that photo and there was no second `nodeImage` request.
- The `storeDitch` response was HTTP 404, so backend persistence and the exact
  existing-photo replacement scenario remain unverified.

The live backend is reachable, and the new-photo request boundary is now
observed. The remaining safe test scope is the real reversal-submit flow and
an existing-photo replacement fixture.

## Implementation review

The committed implementation matches the focused plan:

- `PhotoResultMetadataMerger` retains current-form `img_id` and upload state
  while clearing stale metadata for a changed URI and all metadata for a
  deletion.
- `GutterFormActivity` waits for an active single-photo upload before
  publishing the form result and refreshes the draft before dispatch.
- `reverseWaypoints()` continues to move complete `Waypoint` objects and only
  renumbers display fields.
- The shared `PhotoUploadSlotState.isAlreadyUploaded()` gate and API endpoint
  contracts are unchanged.

## Acceptance criteria

### AC-001 — order reversal preserves photo ownership and avoids duplicate upload

**NOT VERIFIED** for the complete end-to-end criterion.

Supporting evidence:

- `PhotoResultMetadataMergerTest#reversingWaypointsKeepsPhotoIdWithItsWaypoint`
  passed.
- `PhotoUploadManagerPendingPhotosTest#successfulPhotosRemainOutOfPendingUploadsAfterWaypointReversal`
  passed and recorded zero `uploadNodeImage` calls through a repository proxy.
- The focused unit suite passed with 27 tests, 0 failures, and 0 errors.
- `Debug0919WaypointAdapterUiTest#listShowsNoDataForBlankAndPartialRowsButFilledForUploadCompleteRow`
  passed 1/1 on Sony XQ-AU52 / Android 12.
- Live new-photo branch on Sony XQ-AU52: one successful `nodeImage` request,
  followed by one `storeDitch` request with the returned photo ID and no
  second `nodeImage` request.

Missing evidence: a real reversal-and-submit runtime trace proving the actual
application flow emits zero duplicate `nodeImage` requests.

### AC-002 — replacement `img_id` reaches `storeDitch` without re-upload

**NOT VERIFIED** for the complete end-to-end criterion.

Supporting evidence:

- `PhotoResultMetadataMergerTest#replacementKeepsNewIdAndDoesNotKeepOldId`
  passed.
- `PhotoUploadManagerPendingPhotosTest#mergedSuccessfulReplacementDoesNotInvokeUploadAgain`
  passed and recorded zero `uploadNodeImage` calls through a repository proxy.
- `StoreDitchNodeRequestMapperTest` passed 6/6, including retained photo IDs in
  the request payload.
- `StoreDitchResponseWaypointMapperTest` passed 2/2.
- `GutterFormExitUiTest#completedVirtualFormLeavesWithoutWarning` passed 1/1
  on Sony XQ-AU52 / Android 12.
- Live update request carried the returned `img_id=51863` in `img_ids`, with
  one `nodeImage` request total and no duplicate upload.

Limitation: the live `storeDitch` response was HTTP 404 and the captured flow
was a new-photo branch, not a confirmed existing-photo replacement. Backend
persistence and the exact replacement scenario remain **NOT VERIFIED**.

## Validation evidence

### Focused unit tests

Command:

```text
./gradlew :app:testDebugUnitTest \
  --tests com.example.taoyuangutter.gutter.PhotoResultMetadataMergerTest \
  --tests com.example.taoyuangutter.gutter.PhotoUploadManagerPendingPhotosTest \
  --tests com.example.taoyuangutter.gutter.PhotoUploadCandidateResolverTest \
  --tests com.example.taoyuangutter.api.StoreDitchNodeRequestMapperTest \
  --tests com.example.taoyuangutter.gutter.StoreDitchResponseWaypointMapperTest \
  --tests com.example.taoyuangutter.gutter.GutterFormExitRulesTest \
  --rerun-tasks --no-daemon --console=plain
```

Result: **PASS** — 27 tests, 0 failures, 0 errors.

### Debug build

Command: `./gradlew :app:assembleDebug --no-daemon --console=plain`

Result: **PASS** — `BUILD SUCCESSFUL`.

### Focused physical-device cases

- Device: Sony XQ-AU52, Android 12
- Serial: `adb-QV710EDR3A-hF5XZF._adb-tls-connect._tcp`
- Package: `com.example.taoyuangutter`
- `Debug0919WaypointAdapterUiTest#listShowsNoDataForBlankAndPartialRowsButFilledForUploadCompleteRow`: **PASS**, 1/1
- `GutterFormExitUiTest#completedVirtualFormLeavesWithoutWarning`: **PASS**, 1/1
- The device was initially asleep for the first case; it was woken and the
  same case completed successfully. No test failure or retry was recorded.

### Not verified

- Full regression suite: **NOT VERIFIED**
- CI build/test result: **NOT VERIFIED**; no CI run was started.
- Real camera/network duplicate-request trace: **NOT VERIFIED**
- Complete end-to-end AC-001 runtime reversal flow: **NOT VERIFIED**
- Existing-photo replacement persistence after `storeDitch`: **NOT VERIFIED**

## Regression review

Focused regression coverage passed for:

- new photo upload result retaining `img_id` and success state;
- replacement clearing the old ID while retaining the new ID;
- deletion clearing former server metadata;
- same-photo metadata preservation;
- upload-candidate gating;
- reversal association and zero repository upload calls;
- `storeDitch` photo-ID request mapping and response-order mapping.

No production behavior outside the approved debug scope was changed or
explored. Full regression remains unverified.

## Final result

`NOT VERIFIED`

Category: `environment` — the required real runtime/network request trace is
not available within the focused verification scope. The focused code,
unit-test, build, and device evidence is positive, but it cannot prove the
complete end-to-end duplicate-request behavior.

Next action: `infrastructure` for a controlled request-counting environment;
do not advance to Release until AC-001 and AC-002 receive complete evidence.
