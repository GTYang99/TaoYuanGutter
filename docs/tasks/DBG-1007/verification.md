# Independent Verification

## Revision and Worktree

- Task: `DBG-1007`
- Branch: `codex/DBG-1007-minimap-location`
- Commit under test: `9e35233f4b94a67344f61ca88fe7f4c49b7eff42`
- Worktree at verification start: clean (`git status --porcelain=v1` returned no paths).
- Production implementation is unchanged since `5114269122aa762c28f0093904c145e03ce3bd72`; commits after it add task evidence and emulator test harness/coverage. `git diff 5114269..HEAD -- app/src/main` is empty.
- Physical device: not used, per user direction.

## Requirement and Plan Review

The approved behavior is limited to normal existing-point edits with missing coordinates. Saved-coordinate edits retain their saved map target. A successful location fix moves only the form map camera; it must not update waypoint/form/draft coordinates or show a My Location marker. Permission denial and unavailable location preserve the Taoyuan fallback, and manual map movement must win over a late callback.

Implementation review of `GutterFormActivity.onMapReady()` and its location callbacks found the edit-only gate, initial fallback, fine-or-coarse permission check, one retry after recoverable denial, the 25-second per-attempt timeout, two maximum acquisition attempts, Settings return behavior, cancellation in `onDestroy()`, and camera-only callback. The policy unit tests cover edit eligibility, either permission, retry count, and manual-pan suppression. No implementation deviation was found. Offline/draft paths are gated out by `isOfflineMode` and `submittedDraftReadOnly`; new-point mode is gated out by `isEditMode`.

## Acceptance Criteria

| AC | Result | Verification evidence |
|---|---|---|
| AC-001 | PASS | Independently ran `missingCoordinateEditWithFusedMockLocationCentersCameraOnly` on `emulator-5554` using the selected test APK framework GPS provider and fix `25.030000,121.500000`; instrumentation returned `OK (1 test)`. Test asserts camera target within 100 m. Existing detailed record: `emulator-results/AC-001-AC-003-fused-mock.md` and its concise logcat. |
| AC-002 | PASS | Independently revoked fine and coarse permission and reran `savedCoordinateEditWithLocationDeniedDoesNotRequestPermission` on `emulator-5554`; `OK (1 test)`. It asserted both permissions denied, saved camera target retained, location flow inactive, and no unavailable prompt. |
| AC-003 | PASS | Same independent AC-001 run asserted `currentLat/currentLng` remain zero, `NODE_X`/`NODE_Y` remain blank, and session waypoint latitude/longitude remain null. |
| AC-004 | PASS | Reviewed `emulator-results/AC-004.xml` (one passing denial/retry case) and the test procedure in `execution-report.md`: first native denial offered one retry; second denial showed the unavailable prompt without another loop. Production callback tracks `editMapLocationPermissionRetryUsed`. |
| AC-005 | PASS | `emulator-results/AC-011-denied.xml` records the passing denied-return case; `execution-report.md` records that the unavailable prompt exposed Settings and Continue, Settings opened, and the form remained at fallback without permission. AC-004 evidence covers Continue dismissal. |
| AC-006 | PASS | After a normal emulator reboot, `dumpsys location` showed `last location=null` for fused, GPS, and network providers. On `emulator-5554` (Android 14 / API 34), reran `missingCoordinateEditWithoutUsableFixKeepsFallbackAndShowsUnavailablePrompt`; instrumentation XML reports `tests=1`, `failures=0`, testcase time `54.298s`. The test asserted the location flow finished, the unavailable prompt appeared, the camera stayed at the Taoyuan fallback, and waypoint/form coordinates stayed empty. Two 25-second windows plus startup/UI time match the observed duration; implementation review confirms attempt 1 starts exactly one retry and the second failure shows the prompt (`editMapLocationAttempt < editMapLocationMaxAttempts`, with max 2). Procedure and precondition: `emulator-results/AC-006-independent-environment.md`; XML: `emulator-results/AC-006-independent.xml`; source: `GutterFormActivity.kt`. |
| AC-007 | PASS | `execution-report.md` records the emulator case dismissing the unavailable prompt, confirming the Taoyuan fallback, swiping the visible map, and observing the camera target change. Independent AC-009 run also confirmed the visible form map responds to a gesture. |
| AC-008 | PASS | Independently ran `newPointDoesNotRecenterOnDeviceLocation` on `emulator-5554`; `OK (1 test)`. Source review confirms initial location acquisition is gated off for `isEditMode == false`, offline mode, and submitted-draft read-only mode; no draft initialization code was changed. |
| AC-009 | PASS | Independently ran `manualPanBeforeFusedMockCallbackKeepsManualCameraTarget` on `emulator-5554`; `OK (1 test)`. It panned before injecting a distinct fix, then asserted the camera remained at the manual target. Existing record: `emulator-results/AC-009-delayed-fused-callback.xml` and logcat. |
| AC-010 | PASS | Independently ran `coarseOnlyPermissionStartsEditLocationWithoutPermissionPrompt` on `emulator-5554`; `OK (1 test)`. Fine remained denied, coarse was granted, a location attempt started, and the permission prompt was absent. |
| AC-011 | PASS | Reviewed `emulator-results/AC-011-granted.xml` and `AC-011-denied.xml`: returning from Settings with permission started one attempt; returning denied stopped the flow and kept the fallback. The instrumented test assertions cover the Settings-return flag and attempt count. |

## Tests and Build

- Targeted policy unit test: `./gradlew --no-daemon :app:testDebugUnitTest --tests 'com.example.taoyuangutter.gutter.EditMapLocationPolicyTest' --rerun-tasks` — **PASS**, 32 tasks executed; Gradle reported `BUILD SUCCESSFUL`.
- App and instrumentation APKs: `./gradlew --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest` — **PASS**, `BUILD SUCCESSFUL` (tasks were up-to-date after the forced compile/test run).
- CI-workflow-equivalent local validation — **PASS**: ran the three workflow unit-test filters (`PhotoUploadSlotStateTest`, `PhotoUploadCandidateResolverTest`, `StoreDitchNodeRequestMapperTest`) and `:app:assembleDebug`; Gradle reported `BUILD SUCCESSFUL`.
- Independent emulator cases on the exact commit: AC-001/003, AC-002, AC-006, AC-008 new-point, AC-009, and AC-010 — all **PASS** as detailed above. AC-006's clean no-fix procedure, XML, and filtered logcat are preserved under `emulator-results/`.
- `git diff --check` — **PASS**.
- One attempted AC-002 invocation initially stopped at its precondition because permissions were still granted from the prior test; after explicitly revoking both permissions, the same case passed. This was test setup, not an app assertion failure.
- An initial independent no-fix AC-006 run did not observe the prompt because GMS Fused retained the previous mock fix after test-provider removal. A normal emulator reboot cleared the cached fix (`dumpsys location` reported null for fused/GPS/network); the rerun passed in 54.298 seconds with the unavailable prompt and fallback assertions.

## CI

- CI result: **NOT VERIFIED**. `gh` is not installed and the branch has not been pushed, so no external GitHub Actions result exists. No external CI was triggered.
- `.github/workflows/android-ci.yml` runs three JVM test filters (`PhotoUploadSlotStateTest`, `PhotoUploadCandidateResolverTest`, and `StoreDitchNodeRequestMapperTest`) plus `:app:assembleDebug`. Those exact commands passed locally. The workflow does not run `EditMapLocationPolicyTest` or emulator instrumentation, which were run locally as separate checks.

## Regression Review

- Saved coordinate path: independently passed AC-002 with both location permissions denied.
- New-point path: independently passed AC-008's new-point case.
- Offline and submitted-draft paths: implementation gate excludes them; no production changes were made to draft persistence or initialization.
- Coordinate persistence/privacy: independently asserted in AC-003; no My Location layer is enabled by this form flow.
- Manual map control: independently passed AC-009; AC-007's post-dialog manual move is supported by the developer run record.
- Permission retry and Settings return: emulator evidence for AC-004, AC-005, and both AC-011 outcomes reviewed.
- Missing-fix timeout and bounded reacquisition: AC-006 passed after clearing the emulator's stale in-memory fix with a normal reboot; no emulator data wipe was performed.
- Broader CI/regression suite: CI status unavailable; no full regression run was in the approved plan.

## Issues and Limitations

- `ISS-DBG-1007-LOC-004` — environment/P2: resolved by rebooting the emulator without wiping data; the clean no-fix AC-006 rerun passed.
- External CI run status is unavailable; `gh` is absent and no push or external job was authorized.
- No physical-device test was run, as explicitly requested.

## Final Result

**NOT VERIFIED**

All acceptance criteria passed on the implementation revision. The local build, targeted unit tests, emulator cases, and exact local CI-workflow commands passed. External CI status remains unavailable because the branch has not been pushed and the GitHub CLI is absent. Do not advance to Release until external CI evidence is available; keep `next_action: verification`.
