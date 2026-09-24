# Issue Log

## ISS-feat-0923-001

- Task: `feat-0923`
- Phase: verification
- Category: `environment`
- Priority: P1
- Title: Android runtime verification unavailable
- Status: resolved
- Impact: Initial device-dependent verification was blocked; the blocked check has now been rerun.
- Evidence: Restricted execution reproduced `could not install *smartsocket* listener: Operation not permitted`; host-permitted execution discovered `emulator-5554` (`Medium_Phone(AVD)`, Android 14), and the fixed revision's `:app:connectedDebugAndroidTest` passed 52 tests with 0 failures, 0 errors, and 0 skipped.
- Root cause: restricted host socket permission, not an application failure.
- Next action: verification — assess the connected-test evidence and remaining acceptance-criteria coverage.

## ISS-feat-0923-002

- Task: `feat-0923`
- Phase: verification
- Category: `environment`
- Priority: P2
- Title: Verification worktree initially lacked Maps build setting
- Status: resolved for local build checks; keep as environment prerequisite
- Impact: exact-revision build initially stopped before compilation at manifest merge.
- Evidence: missing `<MAPS_API_KEY>` substitution; a verification-only placeholder allowed the same revision to pass build, JVM tests, and Android test compilation.
- Next action: ensure verification environments provide a non-production placeholder or approved local Maps setting without committing credentials.

## ISS-feat-0923-003

- Task: `feat-0923`
- Phase: verification
- Category: `environment`
- Priority: P2
- Title: CI result unavailable
- Status: open
- Impact: CI gate cannot be marked PASS and Release cannot advance.
- Evidence: no CI workflow result or CI artifact is present for fixed implementation revision `452915ad22d135b87ef1705bc11808dbeccaee28`.
- Next action: infrastructure — provide or run CI for the fixed revision.

## ISS-feat-0923-004

- Task: `feat-0923`
- Phase: debug
- Category: `verification_failure`
- Priority: P1
- Title: New submitted-draft flow has no verification evidence on its implementation revision
- Status: open
- Impact: AC-006 and AC-007 cannot be accepted or rejected; Release remains blocked.
- Evidence: `verification.md` tests `692991c`, while the submitted read-only/re-upload implementation is in `9524e03`; the recorded 51 connected tests do not cover the new flow.
- Root cause: verification was not rerun against the new implementation revision, so the current feedback is an evidence gap rather than a reproduced product failure.
- New evidence: fixed commit `9524e03` was tested in an isolated checkout; JVM tests passed and the Android 14 connected suite passed 51/51 with 0 failures, 0 errors, and 0 skipped. The report still has no targeted AC-006/AC-007 cases.
- Independent verification update: fixed commit `452915a` was tested in a clean isolated checkout; 124 JVM tests and 52 Android instrumentation tests passed with 0 failures, 0 errors, and 0 skipped. The committed suite still has no targeted AC-001/AC-002/AC-004/AC-005/AC-006-complete/AC-007 flow evidence, and no CI result is available.
- Next action: infrastructure/verification — provide targeted runtime evidence and CI for fixed commit `452915a`; only route back to implementation debug if a concrete behavior failure is reproduced. Existing baseline pass does not close this issue.

## ISS-feat-0923-005

- Task: `feat-0923`
- Phase: debug
- Category: `implementation_regression`
- Priority: P1
- Title: Submitted draft basic-info controls remain editable after pager creation
- Status: resolved
- Impact: AC-006 is not met; the submitted draft can expose editable controls beneath the read-only overlay.
- Evidence: On fixed revision `9524e03`, `SubmittedDraftReadOnlyUiTest.submittedDraftDisablesFormControlsAfterPagerCreation` failed on Android 14 `Medium_Phone(AVD)` because `etRemarks` remained enabled (`1` test, `1` failure, `0` errors, `0` skipped).
- Root cause: `applySubmittedDraftReadOnlyUi()` runs before `ViewPager2` creates the basic-info fragment, so `getBasicInfoFragment()` returns null; the later-created fragment defaults to editable.
- Route: implementation_debug — propagate the read-only state into fragment creation, add targeted regression coverage, then rerun developer validation.
- Resolution: fixed in `452915a`; targeted test and the full 52-test connected suite pass. Independent Verification remains pending.

## ISS-feat-0923-006

- Task: `feat-0923`
- Phase: debug
- Category: `implementation_regression`
- Priority: P1
- Title: Submitted re-upload can write mutable form state back through the auto-save callback
- Status: resolved
- Impact: AC-007 is not met; a failed or interrupted submitted re-upload may mutate the persisted draft source instead of preserving the Room-authoritative snapshot for retry.
- Evidence: On fixed revision `452915a`, `AddGutterBottomSheet.performSubmittedReupload()` restores the Room row into mutable `waypoints` and passes it into the retry flow (`AddGutterBottomSheet.kt:1111-1138`). `ensureWaypointPhotosUploadedBeforeSubmit()` invokes `onWaypointsChanged` during photo success/error and after projection (`AddGutterBottomSheet.kt:2151-2185`). `GutterSheetSessionBinder` unconditionally routes that callback to `onAutoSaveRequested` (`GutterSheetSessionBinder.kt:27-38`). This contradicts the approved no-write and immutable-snapshot contract in `analysis.md:43-55` and `plan.md:46-48`.
- Root cause: submitted retry does not use a separate immutable Room snapshot/projection boundary and does not suppress the existing mutable `onWaypointsChanged` auto-save path.
- Resolution: fixed in `ee55f08` by adding `SubmittedRetrySnapshot`, using transport-only retry projections, suppressing submitted mutable waypoint callbacks, and preserving retry request metadata without writing the submitted draft.
- Developer evidence: JVM 126/126 PASS; targeted submitted read-only instrumentation PASS; `MainShellActivityTest` class retry 12/12 PASS. Runtime AC-007 no-write/failure/timeout/interruption cases remain for independent Verification.
- Next action: verification — rerun AC-007 against committed revision `ee55f08` and record CI evidence separately.

## ISS-feat-0923-007

- Task: `feat-0923`
- Phase: implementation
- Category: `implementation_regression`
- Priority: P1
- Title: Pending draft item subtitle overlaps title
- Status: resolved
- Impact: Draft list rows can render the creation-time subtitle over the title text.
- Evidence: `item_pending_draft.xml` wrapped `tvPendingDraftTitle` in `layoutPendingDraftTitle` but kept `tvPendingDraftTime` constrained to the nested title view. `bottom_sheet_pending_drafts.xml` only hosts the RecyclerView; `PendingDraftAdapter` inflates the affected item layout.
- Root cause: the parent `ConstraintLayout` subtitle chain used a nested child instead of the direct title-row container as its vertical anchor.
- Resolution: fixed in `e595311`; the subtitle now anchors below `layoutPendingDraftTitle`, and the measured-layout regression test passes 3/3.
- Next action: verification — rerun the pending-draft UI checks against `e595311`.

## ISS-feat-0923-008

- Task: `feat-0923`
- Phase: plan_review
- Category: `planning_gap`
- Priority: P1
- Title: Existing-gutter tag and restore identity policies are split
- Status: resolved
- Impact: AC-003、AC-004、AC-008 were not safely implementable from the prior plan; the list could omit the required tag or the restored form could disagree with the list about read-only/editable mode.
- Evidence: Plan Critic Review Iteration 7 Finding 1–2; Iteration 8 planning revision in `analysis.md`／`plan.md`.
- Root cause: the scope change was described at the adapter level without naming the policy owners or defining one normalized valid-`SPI_NUM` predicate shared by tag selection, restore mode, title and delete identity.
- Resolution: `PendingDraftTagPolicy` is now the planned single owner of `hasValidSpiNum` and `EXISTING_GUTTER` precedence; restore, title and delete paths reuse it; JVM boundary and cross-policy tests are mapped to AC-003／AC-004／AC-008. The revised plan passed the fresh Iteration 8 review.
- Next action: verification — implementation is complete in `f8f40fe`; keep runtime evidence separate from the planning resolution.
