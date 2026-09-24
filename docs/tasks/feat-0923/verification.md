# Verification

## Revision Under Test

- Task: `feat-0923`
- Implementation revision: `452915a` (`fix(feat-0923): enforce submitted draft read-only state`)
- Verification workspace: isolated clean worktree at the committed revision
- Package: `com.example.taoyuangutter`
- Verification-only setup: an untracked `local.properties` containing only `MAPS_API_KEY=verification-placeholder`; no production credential or source change.

## Root Cause and Analysis

The previous verification record targeted the pre-feature baseline `692991c` and did not cover the submitted-draft read-only/re-upload implementation. During debug, fixed-revision testing of `9524e03` reproduced an AC-006 implementation failure: the basic-info fragment was created after the Activity's one-time lock attempt and `etRemarks` remained enabled. The fix was committed as `452915a`.

Developer validation on the fix revision now passes the targeted submitted read-only test and the full 52-test connected suite. This document records that evidence; it does not replace independent Verification or CI.

The earlier verification attempt did not reach a product-behavior PASS because the required runtime evidence was initially unavailable; that was an environment limitation, not the later implementation failure reproduced during debug.

1. The first exact-revision build stopped at `processDebugMainManifest` because the isolated worktree did not contain the gitignored `local.properties` required to substitute `<MAPS_API_KEY>`.
2. After adding a verification-only placeholder, the baseline revision completed its developer checks: 121 JVM tests passed, the Debug APK built, and Android test sources compiled.
3. Device discovery then failed before any test could start: `adb devices` could not start the daemon and reported `could not install *smartsocket* listener: Operation not permitted`.
4. After host-level ADB access was enabled, `emulator-5554` (`Medium_Phone(AVD)`, Android 14) was discovered. The fixed implementation revision `452915a` passed the targeted read-only test and `:app:connectedDebugAndroidTest` passed 52 tests with 0 failures, 0 errors, and 0 skipped. No CI result is available, and the connected suite does not exercise every AC-001, AC-002, AC-004, or AC-007 interruption/cleanup scenario.

The original environment blocker was correctly classified and remediated through `infrastructure`. The AC-006 implementation regression was subsequently fixed and developer-validated on `452915a`; the remaining acceptance criteria are `NOT VERIFIED`, not `PASS`, until their targeted runtime scenarios and the CI gate are covered.

## Acceptance Criteria

| AC | Result | Evidence and limitation |
|---|---|---|
| AC-001 | NOT VERIFIED | Source review and boundary unit tests cover marker-before-request; the connected suite does not exercise API failure/timeout/restart persistence. |
| AC-002 | NOT VERIFIED | Gson legacy default and boundary tests pass; the connected suite does not exercise pre-request interruption and restart persistence. |
| AC-003 | PASS | `PendingDraftAdapterUiTest.submittedAndUnsubmittedTagsUseRequiredStylesAndCallbacks` passed on `emulator-5554`. |
| AC-004 | NOT VERIFIED | `PendingDraftAdapterUiTest.existingGutterDraftHidesSubmissionTag` passed, but restore/delete/success-clear runtime regression was not covered by the connected suite. |
| AC-005 | PASS | `GutterDraftDatabaseMigrationTest.migration3To4AddsFalseSubmissionDefaultAndPreservesLegacyRow` passed on `emulator-5554`. |
| AC-006 | PASS (developer validation) | `SubmittedDraftReadOnlyUiTest.submittedDraftDisablesFormControlsAfterPagerCreation` passed on `452915a`; it asserts overlay/back affordance, submit hidden, virtual toggle, remarks, location, and photo controls disabled. Independent Verification should confirm the full acceptance-criteria scope. |
| AC-007 | NOT VERIFIED | The re-upload path and success/failure/timeout/interruption cleanup behavior still lack targeted runtime evidence. |

## Executed Checks

| Check | Result | Evidence |
|---|---|---|
| Exact revision worktree status | PASS | Fix commit `452915a` is committed; unrelated pre-existing working-tree changes remain uncommitted and were not staged. |
| Exact revision without local build setting | NOT VERIFIED | Manifest merge stopped because `<MAPS_API_KEY>` had no substitution. Classified as environment and resolved for subsequent checks with a placeholder. |
| `./gradlew :app:testDebugUnitTest` | PASS | 121 tests, 0 failures, 0 errors. |
| `./gradlew :app:assembleDebug` | PASS | Debug APK assembled from the committed revision. |
| `./gradlew :app:compileDebugAndroidTestKotlin` | PASS | Migration and pending-list UI test sources compiled. |
| `git diff --check` | PASS | No whitespace errors in the implementation revision. |
| `adb devices -l` with host socket access | PASS | `emulator-5554`, `Medium_Phone(AVD)`, Android 14. |
| Targeted submitted read-only test | PASS | 1 test, 0 failures on `emulator-5554` / Android 14. |
| `./gradlew :app:connectedDebugAndroidTest` | PASS | 52 tests, 0 failures, 0 errors, 0 skipped on the fixed revision. |
| CI build/test result | NOT VERIFIED | No CI result artifact or workflow result was available. |

## Regression Review

- `GutterRepository.storeDitch` payload construction and API contract are unchanged.
- Existing add/edit call sites now share the submission marker boundary; direct inspect-edit without a pending draft id remains a no-op.
- Room migration, legacy JSON defaulting, auto-save preservation, tag policy, click, and long-click paths are represented in the committed test sources.
- Runtime interruption and cleanup behavior for AC-001, AC-002, and AC-004 remains unverified; the connected device evidence covers the migration and pending-list UI checks listed above.

## Final Result

- Result: `PENDING INDEPENDENT VERIFICATION`
- Category: `implementation fixed; evidence incomplete`
- Failed acceptance criteria: none on the current developer-validated revision; AC-001, AC-002, AC-004, and AC-007 remain `NOT VERIFIED` for the missing targeted scenarios.
- Next action: `verification` — independently verify the committed revision, collect AC-007 retry/cleanup evidence, and obtain a CI result.
