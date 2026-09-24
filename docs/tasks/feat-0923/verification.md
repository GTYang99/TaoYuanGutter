# Verification

## Inputs

- Requirement: `docs/tasks/feat-0923/requirement.md`
- Analysis and plan: `docs/tasks/feat-0923/analysis.md`, `plan.md`
- Plan review: `docs/tasks/feat-0923/plan-review.md`
- State: `docs/tasks/feat-0923/state.yaml`
- Git diff and committed revision: `452915ad22d135b87ef1705bc11808dbeccaee28`
- Verification and testing rules: `ai/verification-rules.md`, `ai/testing-rules.md`

## Revision Under Test

- Task: `feat-0923`
- Implementation revision: `452915ad22d135b87ef1705bc11808dbeccaee28` (`452915a`)
- Verification workspace: isolated clean worktree at `/Users/a10362/.codex/worktrees/feat-0923-verification-0002/TaoYuanGutter`
- Package: `com.example.taoyuangutter`
- Build variant: `debug`
- Verification-only setup: untracked `local.properties` containing only `MAPS_API_KEY=verification-placeholder`
- Worktree state before and after checks: clean; `HEAD` remained the fixed revision

### Physical Device Context

```yaml
revision: 452915ad22d135b87ef1705bc11808dbeccaee28
device:
  serial: emulator-5554
  model: Medium_Phone(AVD)
  android_version: Android 14
app:
  package: com.example.taoyuangutter
  build_variant: debug
```

## Executed Checks

| Check | Result | Evidence |
|---|---|---|
| Fixed revision and isolated worktree | PASS | `git rev-parse HEAD` matched `452915ad...`; no tracked or relevant untracked source/test changes before or after testing. |
| `./gradlew :app:testDebugUnitTest` | PASS | 38 suites, 124 tests, 0 failures, 0 errors. |
| `./gradlew :app:assembleDebug` | PASS | Debug APK assembled successfully from the fixed revision. |
| `./gradlew :app:compileDebugAndroidTestKotlin` | PASS | Android instrumentation sources compiled successfully. |
| `git diff --check 452915a^ 452915a` | PASS | No whitespace errors in the implementation commit. |
| `adb devices -l` | PASS | `emulator-5554` discovered as Android 14 `Medium_Phone(AVD)`. |
| `./gradlew :app:connectedDebugAndroidTest` | PASS | 52 tests, 0 failures, 0 errors, 0 skipped; report timestamp `2026-09-24T01:44:22Z`. |
| CI build/test result | NOT VERIFIED | No repository CI workflow or CI result artifact is available. |

## Acceptance Criteria

| AC | Steps / scope | Result | Actual result and evidence | Retry count |
|---|---|---|---|---:|
| AC-001 | Verify marker ordering and persistence after API outcomes/restart | NOT VERIFIED | `GutterRepositoryStoreDitchBoundaryTest` and source review prove the callback boundary precedes the request, but no committed runtime case covers API failure/timeout or app restart after entering `storeDitch`. | 0 |
| AC-002 | Verify pre-request interruption remains unsubmitted across restart/restore | NOT VERIFIED | Legacy JSON default and tag-policy unit tests pass, but no runtime case simulates interruption before repository entry and subsequent draft restore. | 0 |
| AC-003 | Verify submitted/unsubmitted tag text, position, size, and styles | PASS | `PendingDraftAdapterUiTest.submittedAndUnsubmittedTagsUseRequiredStylesAndCallbacks` passed; it asserts both texts, visibility, colors, outline drawable, padding, click, and long-click callbacks. | 0 |
| AC-004 | Verify existing `SPI_NUM` behavior and list restore/delete/success cleanup regression | NOT VERIFIED | `PendingDraftAdapterUiTest.existingGutterDraftHidesSubmissionTag` passed, but the full restore, long-press delete, and successful cleanup flow was not covered by a targeted committed runtime case. | 0 |
| AC-005 | Verify legacy migration, list loading, and restore | NOT VERIFIED | `GutterDraftDatabaseMigrationTest.migration3To4AddsFalseSubmissionDefaultAndPreservesLegacyRow` and legacy Gson default tests passed; list loading and restore of the migrated row were not independently exercised. | 0 |
| AC-006 | Verify every submitted-draft edit surface is locked while return and re-upload remain available | NOT VERIFIED | `SubmittedDraftReadOnlyUiTest.submittedDraftDisablesFormControlsAfterPagerCreation` passed for overlay, back, hidden inner submit, virtual toggle, remarks, location, and photo controls. It does not cover all required node, attachment, type, import, overlay-boundary, or outer re-upload interactions. | 0 |
| AC-007 | Verify full Room-sourced re-upload payload and success/failure/timeout/interruption retry behavior | FAIL | Static review of the fixed revision contradicts the approved source-of-truth/no-write contract: `performSubmittedReupload()` restores the Room row into mutable `AddGutterBottomSheet.waypoints` and passes it into the upload/request flow; `ensureWaypointPhotosUploadedBeforeSubmit()` invokes `onWaypointsChanged` on photo success/error, while `GutterSheetSessionBinder` routes that callback to auto-save. This can mutate the submitted draft during retry instead of preserving the Room snapshot. No runtime retry test exists. | 0 |

## Regression Review

- The implementation commit changes only `GutterBasicInfoFragment`, `GutterFormActivity`, `GutterFormPagerAdapter`, and adds `SubmittedDraftReadOnlyUiTest`; no API model or third-party dependency change is present in the tested revision.
- Existing form, inspection, import, exit, mapper, Room migration, pending-list tag, and submitted read-only tests passed in the 52-test connected suite.
- The previous pager-created-fragment regression is covered by `SubmittedDraftReadOnlyUiTest` and passed on the fixed revision.
- The submitted re-upload source/no-write contract is not met by source review: `AddGutterBottomSheet.kt:1111-1138`, `2034-2186`, and `GutterSheetSessionBinder.kt:27-38` use mutable form state and an auto-save callback during the submitted retry path, contrary to `analysis.md:45-55` and `plan.md:46-48`.
- Re-upload payload completeness, no-write lifecycle behavior, retry preservation, and success cleanup remain unverified.

## Issues

- `ISS-feat-0923-003`: CI result unavailable; remains open and blocks Release.
- `ISS-feat-0923-004`: targeted submitted-draft flow evidence is incomplete; the independent fixed-revision run confirms the baseline suite passes but does not close the missing AC-001/002/004/005/006/007 scenarios.
- `ISS-feat-0923-005`: submitted basic-info control lock regression is resolved by `452915a`; the targeted test and full connected suite passed.
- `ISS-feat-0923-006`: submitted re-upload invokes the mutable waypoint/update callback path; this is an implementation regression affecting AC-007 and requires Debug before re-implementation.

## Validation Limitations

- No CI build/test result is available.
- The committed test suite has no targeted runtime coverage for API failure/timeout/interruption persistence, Host/Activity recreation and Room reread, full no-write behavior, or re-upload success/failure/timeout/interruption cleanup.
- The available emulator completed the connected suite, but passing the existing suite does not prove the uncovered acceptance criteria.
- Source review found a concrete implementation deviation in the submitted re-upload no-write/source-of-truth contract; runtime retry evidence is still unavailable.

## Failure Classification

- `implementation` for AC-007 (`ISS-feat-0923-006`); the submitted retry path can invoke the existing auto-save callback from mutable form state.
- `environment` remains applicable to the separate unavailable CI gate.

## Next Action

- `debug`: investigate and fix the submitted re-upload immutable-snapshot/no-write violation, then rerun developer validation and Verification on a new committed revision.

## Final Result

FAIL

## Post-Debug Developer Handoff

- The AC-007 implementation regression was fixed in new committed revision `ee55f08`.
- Developer validation recorded 126/126 JVM tests, targeted submitted read-only instrumentation PASS, and `MainShellActivityTest` retry 12/12 PASS.
- The prior AC-007 FAIL remains historical evidence for revision `452915a`; it must not be reused as the result for `ee55f08`.
- Independent Verification is pending and must target `ee55f08`, including the submitted retry payload/no-write/failure/timeout/interruption cases.
- A subsequent layout fix was committed as `e595311`; all new Verification must target `e595311`.
- CI remains `NOT VERIFIED`; Release is still blocked.
