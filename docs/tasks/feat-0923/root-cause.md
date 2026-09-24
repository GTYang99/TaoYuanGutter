# Root Cause Analysis

## Classification

- Task type remains `feature`.
- This document records the planning root cause identified by Plan Critic; it does not reclassify the task as `debug` and does not authorize production-code changes during planning.

## Problem

The plan could mark a draft as `hasSubmittedStoreDitch=true` from a UI Host callback before the actual `GutterRepository.storeDitch(...)` method was entered. That did not precisely implement the requirement that only a request that truly enters the `storeDitch` call is considered submitted.

## Evidence

- `AddGutterBottomSheet` invokes `onGutterSubmitted(...)` immediately before `submitNewGutterRequest(...)` in the add path.
- `AddGutterBottomSheet.performEditSubmit()` invokes `onGutterSubmitting()` before calling `repository.storeDitch(...)` in the edit path.
- The current `GutterRepository.storeDitch(...)` method is the shared owner of the actual API call, but the original plan did not include it as an affected file or define a callback at its method-entry boundary.
- Draft persistence is owned by `GutterDraftCoordinator`／`GutterSessionRepository`; the original plan did not define the behavior when the Host has no current draft id or when the expected Room row is missing.
- `GutterDraftDatabase` uses `exportSchema = false`, and current test dependencies do not provide a Room migration helper; the original plan named a migration test without specifying a reproducible fixture.
- Existing tests cover Gson draft serialization but do not bind `PendingDraftAdapter` or assert the pending-list tag layout.

## Causal Chain

1. The submission marker was assigned to a UI callback rather than the shared repository call boundary.
2. Add and edit flows therefore had two semantically similar but independently ordered paths.
3. A process stop between the Host callback and `GutterRepository.storeDitch` entry could make an unentered request appear submitted.
4. Without a defined missing-draft policy, the marker could also be silently lost or create an untracked state.
5. Without explicit migration and adapter test seams, the plan could not produce deterministic evidence for AC-003 to AC-005.

## Planning Correction

- Define `GutterRepository.storeDitch(...)` method entry as the single shared submission boundary for both add and edit calls.
- Invoke a `suspend onRequestEntered` callback at the beginning of that repository method, before the Retrofit service call; the callback resolves the current draft id and persists the marker.
- For an existing add/resumed draft id, ensure the Room row exists before marking. For direct inspect-edit with no pending draft id, keep the existing API flow and do not create a tagged pending draft; there is no list item to classify.
- Preserve `hasSubmittedStoreDitch=true` in every later auto-save and retry path.
- Add a repository ordering test with a fake API, an explicit Room 3→4 instrumentation migration fixture, and a pending-adapter instrumentation test with exact tag assertions.

## Residual Limitation

No persistence-plus-network operation can make a local database write and a remote HTTP request physically atomic. The corrected contract defines the observable boundary as entry into `GutterRepository.storeDitch`; the marker is persisted synchronously at that method boundary before the Retrofit request is issued, and all remaining outcomes (success, error, timeout, cancellation after entry) retain the submitted state.

## Scope Update for Re-Planning

The requirement was subsequently clarified: a general draft with `hasSubmittedStoreDitch=true` may be resumed for inspection, but its saved content must be read-only. The user may only return, delete it from the pending-list long-press flow, or submit the entire original draft again through the existing upload flow. `SPI_NUM` inspect/edit items remain outside this policy, and legacy drafts default to unsubmitted/editable.

This adds a second boundary beyond the submission marker: the submitted read-only state must propagate from the pending-draft resume path through `GutterSessionFlowCoordinator`／`GutterFormNavigator` into both `AddGutterBottomSheet` and `GutterFormActivity`. The lock must cover all editable controls and the existing overlay visual, while leaving only the outer return and full re-upload actions available. The re-plan adds explicit propagation, UI-lock, overlay, retry, success-cleanup, and interruption evidence before implementation resumes.

## Debug Finding: Verification Revision and Evidence Gap

### Classification

- Verification finding: evidence/revision mismatch, not an implementation regression.
- Related issue: `ISS-feat-0923-004`.
- Affected acceptance criteria: AC-006 and AC-007 are `NOT VERIFIED`; no implementation `FAIL` is established.

### Evidence

- `docs/tasks/feat-0923/verification.md` records `692991ca361b7fec07e117fde01a3659e337a4b5` as the revision under test.
- `692991c` is the baseline before the submitted-draft read-only and re-upload implementation in `9524e03`.
- The connected test evidence listed in that report covers the submitted/unsubmitted tag UI and Room migration, but contains no execution evidence for the new read-only overlay, Activity result return, Room reread, or re-upload retry behavior.
- The current implementation and task state identify `9524e03` as the implementation revision, so the existing verification result cannot be used to pass or fail AC-006/AC-007.

### Root Cause

Verification was recorded against the prior baseline revision and was not rerun after the new implementation commit. Because the targeted instrumentation cases described in the approved plan were not present in the verification evidence, the feedback is an evidence gap rather than proof of a product defect.

### Minimum Resolution

1. Keep production code unchanged until the implementation revision is independently tested.
2. Run verification against fixed commit `9524e03` (or a new commit only after any test-only changes are committed).
3. Add or run targeted evidence for AC-006 and AC-007: read-only controls/overlay, Activity recreation and return, Room-authoritative snapshot re-read, success cleanup, and failure/timeout/interruption retry preservation.
4. If those tests show a behavior contradiction, create a new implementation-regression issue with the concrete trace and re-enter debug; otherwise update verification with PASS/NOT VERIFIED per criterion.

## Debug Evidence Update: Fixed-Revision Baseline

- An isolated checkout was created at `9524e03752f286bd19949cb5a10a7cbf504d8f40` (`9524e03`); no production or test changes were added to that checkout.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest` passed on the fixed revision.
- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:connectedDebugAndroidTest` passed on the fixed revision using the Android 14 `Medium_Phone(AVD)` emulator: 51 tests, 0 failures, 0 errors, 0 skipped.
- The generated fixed-revision report still contains only the existing baseline suites. It has no `SubmittedDraftReadOnlyUiTest`, `SubmittedDraftRetryFlowTest`, or equivalent AC-006/AC-007 runtime case.
- Static control-flow review confirms the intended guards and Room reread paths exist in `AddGutterBottomSheet`, `GutterFormActivity`, `GutterFormNavigator`, and both Host implementations, but static review cannot prove touch blocking, lifecycle race behavior, request completeness, or success/failure cleanup.

### Confidence

- Root-cause classification (`verification_revision_and_evidence_gap`): **99%**. The old report revision, implementation commit, clean fixed-revision checkout, and test inventory are independently consistent.
- Fixed-revision build and existing-regression baseline: **98%** for the covered 51 cases.
- AC-006/AC-007 product behavior: **not above 95%**; it remains `NOT VERIFIED` because the required targeted runtime evidence is absent.
- Release readiness: **not ready** until targeted AC-006/AC-007 verification and CI evidence are recorded.

## Debug Finding: Submitted Read-Only Lock Misses Pager-Created Fragment

### Classification

- Verification category: `implementation_regression`.
- Related issue: `ISS-feat-0923-005`.
- Failed acceptance criterion: AC-006.
- The earlier revision/evidence mismatch remains valid as historical context, but a fixed-revision runtime repro now establishes a concrete implementation defect.

### Reproduction Evidence

- Fixed revision: `9524e03`.
- Device: Android 14 `Medium_Phone(AVD)` (`emulator-5554`).
- Targeted test: `SubmittedDraftReadOnlyUiTest.submittedDraftDisablesFormControlsAfterPagerCreation`.
- Result: `1` test, `1` failure, `0` errors, `0` skipped.
- Failure: `GutterFormActivity`'s `etRemarks` remained `enabled=true`, while AC-006 requires the submitted draft's editable controls to be locked.

### Causal Chain

1. `GutterFormActivity.onCreate` calls `setupViewPager(...)` and then `applySubmittedDraftReadOnlyUi()`.
2. `applySubmittedDraftReadOnlyUi()` attempts `pagerAdapter.getBasicInfoFragment()?.setEditable(false)`.
3. At that point `ViewPager2` may not have created the basic-info child fragment, so the lookup returns `null` and no control lock is applied.
4. `GutterBasicInfoFragment` is later created with its normal editable default, leaving controls enabled beneath the overlay.

### Minimum Fix Scope

- Propagate the submitted read-only flag through `GutterFormPagerAdapter` into `GutterBasicInfoFragment` creation.
- Make the fragment initialize itself non-editable whenever that flag is present, independent of pager timing or recreation.
- Retain the Activity-level lock as a defensive re-application after pager synchronization.
- Add the targeted instrumentation test to the task's committed test scope and cover the key controls required by AC-006.
- Do not change Room authority, submission-marker semantics, retry preservation, or unrelated working-tree changes.

### Confidence

- Root-cause identification: **98%**; the failure is reproduced on the fixed implementation revision and matches the observed lifecycle ordering.
- Proposed lifecycle fix: **98% after targeted runtime validation**; the new test passes after the read-only flag is propagated into fragment creation and the Activity post-lock is retained.
- AC-007: **not yet verified**; this defect does not by itself establish the re-upload flow result.

### Fix Validation

- Fix commit: `452915a` (`fix(feat-0923): enforce submitted draft read-only state`).
- Targeted instrumentation test: PASS on Android 14 `Medium_Phone(AVD)`.
- Full connected suite: PASS, 52 tests, 0 failures, 0 errors, 0 skipped.
- JVM unit tests: PASS, 124 tests.
- Independent verification and CI remain separate gates; this evidence closes the implementation-debug loop but does not mark Release ready.
