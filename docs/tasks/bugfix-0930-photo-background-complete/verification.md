# Verification Report

## Verification Scope

- Fixed revision under test: `bc4f2ab4f0f0a40e2093aea6006bf8b396d67553` on `codex/bugfix-0930-photo-background-complete`.
- Worktree was clean when verification started. The fixed commit is the documentation-only handoff commit above implementation commit `d865b499ce867ebc4563df49a95e5ad584e6ff9d`.
- JVM tests and build ran from an exported snapshot of the fixed commit in `/private/tmp/tyg-verify-0930`; the task worktree remained unchanged.

## Acceptance Criteria

| AC | Result | Evidence / limitation |
|---|---|---|
| AC-001 | PASS | Source review found no `PhotoLoadingHost` or `photoLoadingOverlay` wiring. Slot-level `pbPhotoLoading1..3` indicators remain in both photo fragments/layouts. On device, the user captured three photos and returned to the point form; the photo slot and enabled `fabSubmit` were visible with no full-screen blocker. Completing the point form returned to the parent `新增側溝` list, where 起點／節點1／終點 showed `已填寫資料`. |
| AC-002 | PASS | On device, the slot 1 progress indicator was visible and `fabSubmit` was enabled. Tapping `完成` returned to the parent page while `photo=true,state=uploading`; the result contract and parent merge logs retained that state. The user reported the API test station was active. The app source still has a fixed Taipei API base URL, so the network route itself was not independently confirmed (ISS-005). |
| AC-003 | PASS | `PhotoSlotUploadCoordinator` uses a process-scoped `SupervisorJob + Dispatchers.IO` scope. Add/edit submission paths keep `ensureWaypointPhotosUploadedBeforeSubmit()` before `storeDitch`; source review confirms failed/no-result upload returns before submission. No backend submission was attempted on device. |
| AC-004 | PASS | `PhotoUploadSlotStateTest` covers null, zero, negative, and positive IDs. Only a positive ID maps to success; the coordinator and submit-time upload use the same outcome helper. |
| AC-005 | PASS | `PhotoUploadCandidateResolverTest` passed all 8 tests, including URL-only imported-photo compatibility cases. |
| AC-006 | PASS | The three planned test classes passed 20/20 tests with no failures or skips, and `:app:assembleDebug` succeeded. |

## Test Evidence

- Environment: Gradle 9.6.0, JDK 21.0.7, Android SDK platform 36.
- Focused tests: `:app:testDebugUnitTest --tests 'com.example.taoyuangutter.common.PhotoUploadSlotStateTest' --tests 'com.example.taoyuangutter.gutter.PhotoUploadCandidateResolverTest' --tests 'com.example.taoyuangutter.api.StoreDitchNodeRequestMapperTest'` — PASS, 20 tests, 0 failures, 0 errors, 0 skipped.
- Debug build: `:app:assembleDebug` — PASS (`BUILD SUCCESSFUL`).
- Diff whitespace check: `git diff --check ec818a6..HEAD` — PASS.
- Device: serial `adb-QV710EDR3A-hF5XZF._adb-tls-connect._tcp`, model `XQ-AU52`, Android 12 / API 31; package `com.example.taoyuangutter`.
- Device AC-002 run (2026-09-30, local device time): serial `adb-QV710EDR3A-hF5XZF._adb-tls-connect._tcp`; model `XQ-AU52`; Android 12 / API 31; package `com.example.taoyuangutter`. The form showed photo timestamp `2026-09-30 11:29:36`; `pbPhotoUploadStatus1` was present and `fabSubmit` was enabled/clickable. Tapped `完成` at `[540,2262]`. Logcat at 11:37:15 showed draft sync with slot 1 `photo=true,state=uploading`, then `GutterFormContract.putResultData` with photo present, `readResultData` returning `photo=true,state=uploading`, and `AddGutterBottomSheet.updateWaypointBasicData.merged` retaining `photo=true,state=uploading`. The parent `新增側溝` page was resumed; 起點 showed `已填寫資料`. Snapshot-to-parent merge took about 130 ms in the captured logs; no wait for upload completion occurred. Source review confirms `sessionDraftId` is synced before dispatch and the same ID is included in the result Intent. The parent final `新增側溝` action was not pressed.
- Network environment: operator reported the API test station was active and airplane mode with Wi-Fi enabled. The source build's `BackendEndpoints.ACTIVE_API_TAPIEI_URL` is statically `https://taipei.srgeo.com.tw/TY_RSGDBIP_BK/`; the actual routing destination was not independently visible in app logs. Only the background photo upload was initiated; no `storeDitch` request was invoked.
- JVM tests and build ran from the exported snapshot. No production source or test file changed during verification; verification artifacts were updated after the device result was captured.

## Regression Review

- Slot-level photo preview and upload indicators remain available after removing the full-screen touch blocker.
- Existing URL-only imported photos remain excluded from upload by the compatibility predicate; targeted tests pass.
- The pre-`storeDitch` upload gate remains in add and edit flows; missing/invalid IDs become failed/retryable state.
- No implementation deviation from the approved plan was found in the reviewed diff.
- Camera return, local draft synchronization, and result handoff during an actual slot-level `uploading` state were exercised on device. AC-002 passed. The numeric session draft ID was not emitted to logcat; preservation is supported by source tracing of the same ID through draft sync and result Intent construction.

## CI and Limitations

- CI build/test: `NOT VERIFIED`. `.github/workflows/android-ci.yml` defines the plan-scoped test/build job, but no remote run exists; the attempted branch push was rejected by automatic approval review pending explicit user authorization. This blocks Release.
- The device run relied on the operator's report that the API test station was active. The checked-in app source statically targets the Taipei host; verify the device's actual route before any further photo uploads.

## Issues

- `ISS-004`: CI evidence unavailable; category `environment`, priority `P2`, next action `infrastructure`.
- `ISS-005`: The device's effective API route is not independently confirmed as the test station; category `environment`, priority `P2`, next action `infrastructure`.

## Failure Classification

`environment` — CI evidence is unavailable, and the device's effective API route is not independently confirmed. No acceptance criterion failed; AC-002 passed on device.

## Next Action

`infrastructure` — provide CI build/test evidence and confirm the device's API test route before further uploads; then resume the remaining verification gates.

## Final Result

`NOT VERIFIED`
