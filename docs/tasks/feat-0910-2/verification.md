# Verification Report

## Verification Basis

- Task: `feat-0910-2`
- Branch: `uiFix/填寫順序`
- Verification round: real-device regression verification on 2026-09-11
- Verified revision: `3b76650` (latest production changes through `64ac477`)
- Requirement, analysis, approved plan, plan review, verification rules, testing rules, latest state, and issue log were reviewed before this round.
- Verification made no production-code changes. This report, task state, and issue log are the only updated artifacts.

## Evidence Collected

| Check | Result | Evidence |
|---|---|---|
| Revision and whitespace | PASS | `git diff --check 49deb64..HEAD` completed without output. |
| Layout XML parse | PASS | Ruby REXML parsed the relevant layout resources successfully. |
| Debug unit suite | FAIL | `./gradlew testDebugUnitTest --no-daemon` completed 49 tests with 1 failure: `MainMapLoadIndicatorStateMachineTest.minimumZoomMatchesGutterLayerRequirement`. This pre-existing failure is outside the form UI acceptance criteria but keeps the suite red. |
| Full connected Android suite on XQ-AU52 | FAIL | `connectedDebugAndroidTest` built and installed, then executed 5 tests: 2 passed, 3 failed. The device disconnected during cleanup, so the scheduled 13-test run did not finish. |
| New-form UI path | FAIL | `newFormShowsRequiredOrderLabelsButtonsAndDefaults` timed out and raised `NoActivityResumedException` after 127.416 seconds. |
| Existing-data preservation path | FAIL | `existingBrokenAndSiltValuesArePreserved` timed out and raised `NoActivityResumedException` after 143.303 seconds. |
| Virtual-point initial state | PARTIAL PASS | The test confirmed virtual mode hides the switch bar, cannot-open control, and normal form fields; it also clicked virtual mode off. The device disconnected before restoration assertions could complete. |
| CI | NOT VERIFIED | No CI build/test evidence is available. |

## Acceptance Criteria

| AC | Result | Evidence |
|---|---|---|
| AC-001 | FAIL | The current new-form device test cannot remain in a resumed Activity to complete measurement-status assertions. Source/layout inspection is insufficient to override the runtime failure. |
| AC-002 | FAIL | The current new-form device test cannot complete the required order assertions because the Activity loses resumed state. |
| AC-003 | FAIL | The current new-form device test cannot complete broken/silt default assertions because the Activity loses resumed state. |
| AC-004 | FAIL | The existing-data preservation test now also loses resumed state before its checked-state assertions complete. Earlier passing evidence was from an older revision and does not verify the current revision. |
| AC-005 | FAIL | The current new-form device test cannot complete photo-button label assertions because the Activity loses resumed state. |
| AC-006 | FAIL | The core `GutterBasicInfo` regression suite has reproducible runtime failures. The virtual-point-off restoration subcase is additionally NOT VERIFIED because XQ-AU52 disconnected before its final assertions. |

## Failure Classification

- **Implementation regression:** The form Activity repeatedly fails to remain resumed for both the new-form and existing-data paths. This is a current-revision runtime failure, not a missing-evidence condition. It is tracked by `ISS-0910-2-09` and routes to Debug.
- **Environment limitation:** XQ-AU52 disconnected during the virtual-point test and test-run cleanup, leaving the remainder of the connected suite incomplete. This does not explain the two earlier `NoActivityResumedException` failures.
- **Unrelated validation failure:** `MainMapLoadIndicatorStateMachineTest.minimumZoomMatchesGutterLayerRequirement` remains red. It does not establish a form implementation defect, but the overall unit suite is not passing.

## Scope Review

- The implementation diff extends beyond the originally planned form ordering/default changes: it changes `GutterFormActivity`, virtual-point behavior, map startup timing, upload filtering, `AddGutterBottomSheet`, a map load-indicator threshold, and adds Android UI tests.
- The virtual-point and form-startup changes are related to defects found during this task, but this expanded scope requires Debug to establish the form lifecycle regression before Release can assess it. No production scope was modified during verification.

## Reproduction Evidence

- Device: Sony XQ-AU52, Android 12, transport `adb-QV710EDR3A-hF5XZF._adb-tls-connect._tcp`.
- Command: `JAVA_HOME=/Applications/Android Studio.app/Contents/jbr/Contents/Home ./gradlew connectedDebugAndroidTest --no-daemon`.
- Report: `app/build/outputs/androidTest-results/connected/debug/TEST-XQ-AU52 - 12.xml` records 5 executed tests, 2 passes, and 3 failures.
- For the two lifecycle failures, captured logcat shows `GutterFormActivity` is destroyed after the Espresso timeout and the framework raises `NoActivityResumedException`; no app `FATAL EXCEPTION` was recorded. This is a recurrence after the prior `f723ade` map-startup deferral and must be debugged rather than treated as a successful fix.

## Issues and Limitations

- Updated `ISS-0910-2-09` records the recurrence across both new and existing form paths.
- `ISS-0910-2-10` remains relevant for the device-disconnection portion of this run, but it is not the classification for the preceding lifecycle failures.
- The backend photo-upload SQL/API issue (`ISS-0910-2-03`) remains outside this client UI verification.
- No CI evidence is available.

## Final Result

**FAIL.** Current real-device automation reproduced a form-Activity lifecycle regression on the revision under review. Release is blocked. Resulting task state is `phase: verification`, `status: verification_failed`, `next_action: debug`.

## Revalidation after lifecycle isolation (2026-09-11)

- Device state was checked directly: XQ-AU52 was asleep during the recurring failures. `always_finish_activities=0`.
- After waking the device, `GutterBasicInfoUiTest#newFormShowsRequiredOrderLabelsButtonsAndDefaults` passed.
- After updating the stale single-page `switchPageBar` expectation, `GutterBasicInfoUiTest#turningVirtualPointOffRestoresNormalFormInteraction` passed.
- The complete `GutterBasicInfoUiTest` class remains subject to Android Test `EmptyActivity` cross-scenario task handoff; one run failed in the first test while the other two passed. This is recorded as environment/test-harness evidence, not an App crash.
- No production-code changes were made for the lifecycle symptom; the opaque-theme experiment was reverted.

## Revalidation after test-task isolation (2026-09-11)

- Added `Intent.FLAG_ACTIVITY_CLEAR_TASK` to `GutterBasicInfoUiTest.launchForm()` so each `ActivityScenario` starts in an isolated task. This changes test setup only and does not alter application behavior.
- `GutterBasicInfoUiTest`: **PASS**, 3/3 on Sony XQ-AU52 / Android 12. Evidence: `TEST-XQ-AU52 - 12.xml`, `failures="0"`.
- `GutterCantOpenUiTest`: **PASS**, 4/4 on Sony XQ-AU52 / Android 12. Evidence: `TEST-XQ-AU52 - 12.xml`, `failures="0"`.
- Targeted unit tests: **PASS** — `GutterSessionDraftTest` and `UploadFailureClassifierTest` completed successfully.
- The prior `NoActivityResumedException` did not recur in these isolated runs. This supports the classified cause as test/device task-lifecycle interference rather than a newly reproduced application crash.
- `testDebugUnitTest`, CI, and the full connected Android suite remain **NOT VERIFIED** in this revalidation round.
