# Issue Log

## ISS-0910-1-01

- Task: feat-0910-1
- Phase: plan_review
- Category: planning_gap
- Priority: P1
- Status: resolved
- Title: Session snapshot ownership and restoration contract are incomplete
- Impact: AC-005 cannot be safely implemented without risking loss or inconsistent restoration of photo and pending-capture state.
- Evidence: `plan-review.md`, Finding 1
- Next action: plan_review

## ISS-0910-1-02

- Task: feat-0910-1
- Phase: plan_review
- Category: planning_gap
- Priority: P1
- Status: resolved
- Title: AC-006 merge behavior is unspecified
- Impact: Restoring the snapshot may overwrite information changed after cant-open confirmation.
- Evidence: `plan-review.md`, Finding 2
- Next action: plan_review

## ISS-0910-1-03

- Task: feat-0910-1
- Phase: plan_review
- Category: planning_gap
- Priority: P1
- Status: resolved
- Title: Acceptance criteria lack implementation and validation traceability
- Impact: The task cannot demonstrate release-readiness against AC-001 through AC-009.
- Evidence: `plan-review.md`, Finding 3
- Next action: plan_review

## ISS-0910-1-04

- Task: feat-0910-1
- Phase: plan_review
- Category: planning_gap
- Priority: P1
- Status: resolved
- Title: Configuration-change retention conflicts with the selected snapshot owner
- Impact: The promised same-session restore after rotation cannot be guaranteed and may violate AC-005 or AC-007.
- Evidence: `plan-review.md`, Finding 4
- Next action: plan_review

## ISS-0910-1-05

- Task: feat-0910-1
- Phase: plan_review
- Category: planning_gap
- Priority: P1
- Status: resolved
- Title: Late camera-result token contract is outside the stated plan scope
- Impact: An obsolete capture result can still repopulate a cleared or replaced measurement-photo slot and trigger upload.
- Evidence: `plan-review.md`, Finding 5
- Next action: plan_review

## ISS-0910-1-06

- Task: feat-0910-1
- Phase: verification
- Category: environment
- Priority: P1
- Status: open
- Title: Connected suite has unrelated Activity recreation failure on XQ-AU52
- Impact: Full connected verification cannot be classified as PASS; the failure is in existing `GutterInspectRevokeCommentTest` and does not exercise the cant-open flow.
- Evidence: `app/build/outputs/androidTest-results/connected/debug/TEST-XQ-AU52 - 12.xml`
- Next action: verification

## ISS-0910-1-07

- Task: feat-0910-1
- Phase: verification
- Category: environment
- Priority: P1
- Status: open
- Title: Instrumentation package cleanup failed during targeted UI rerun
- Impact: The corrected `GutterCantOpenUiTest` could compile but could not produce a valid execution result.
- Evidence: Gradle connected test output: `DELETE_FAILED_INTERNAL_ERROR` for `com.example.taoyuangutter` and `.test` on emulator-5554 and XQ-AU52.
- Next action: verification

## ISS-0910-1-08

- Task: feat-0910-1
- Phase: verification
- Category: environment
- Priority: P1
- Status: open
- Title: Local CI-equivalent check fails on existing lint baseline
- Impact: CI gate cannot be marked PASS for this revision.
- Evidence: `./gradlew check assembleDebug --no-daemon`; lint reports 50 errors and 401 warnings, first at existing `GutterFormActivity.onBackPressed()`.
- Next action: verification

## ISS-0910-1-09

- Task: feat-0910-1
- Phase: plan_review
- Category: planning_gap
- Priority: P1
- Status: resolved
- Title: Revised plan removes required restore and session-isolation behavior
- Impact: The plan cannot satisfy AC-005, AC-006, or AC-007 and lowers approved product intent.
- Evidence: `plan.md`; `plan-review.md`, Finding 6
- Next action: plan_review

## ISS-0910-1-10

- Task: feat-0910-1
- Phase: plan_review
- Category: requirement_gap
- Priority: P1
- Status: resolved
- Title: Uncommitted base-URL change conflicts with feat-0910-1 scope
- Impact: The reviewer cannot safely preserve or remove `GutterApiService.kt` changes while the approved plan excludes API behavior changes.
- Evidence: `GutterApiService.kt` changes `DEMO_URL` to `BASE_URL`; `plan.md` step 8 requires unrelated URL changes to be removed.
- Next action: plan_review

## ISS-0910-1-11

- Task: feat-0910-1
- Phase: plan_review
- Category: planning_gap
- Priority: P2
- Status: resolved
- Title: Revised plan omits required failure, security, and rollback sections
- Impact: Risk and recovery behavior are not reviewable before implementation.
- Evidence: `plan.md`; `ai/templates/plan-template.md`
- Next action: plan_review

## ISS-0910-1-12

- Task: feat-0910-1
- Phase: plan_review
- Category: planning_gap
- Priority: P2
- Status: resolved
- Title: Plan still directs removal of a confirmed user-owned URL change
- Impact: Implementation scope and commit isolation remain ambiguous.
- Evidence: `plan.md` step 8; user confirmation that `GutterApiService.kt` is manual work outside feat-0910-1.
- Next action: implementation

## ISS-0910-1-13

- Task: feat-0910-1
- Phase: verification
- Category: implementation_regression
- Priority: P1
- Status: superseded
- Title: Cant-open dirty merge and stale camera-result protections are not wired into production mutations
- Impact: A measurement-photo capture started before confirming cant-open can complete afterward and repopulate a cleared slot. Fields or photo slots changed through production callbacks also have no dirty marker, so restore may overwrite a later value.
- Evidence: `GutterFormActivity.kt` exposes `markCantOpenFieldChanged`, `markCantOpenPhotoChanged`, and `invalidateCapture`, but repository search finds no production invocations. `GutterBasicInfoFragment.kt` accepts a camera result whenever its still-active token matches; confirming cant-open does not invalidate tokens for slots 2 or 3. The unit tests call marker methods directly, bypassing the missing production integration.
- Resolution: The user changed the approved product decision on 2026-09-10: cancelling cant-open does not restore cleared data. The snapshot/dirty-merge requirement is removed and the task returns to Planning for revised implementation and plan review.
- Next action: plan_review
