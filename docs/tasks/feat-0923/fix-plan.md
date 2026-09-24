# Debug Fix Plan

## Task

- Task: `feat-0923`
- Related issues: `ISS-feat-0923-004`, `ISS-feat-0923-005`
- Classification: implementation regression after fixed-revision reproduction

## Root Cause

The existing verification report tests baseline revision `692991c`, while the submitted-draft read-only and re-upload behavior was implemented in `9524e03`. A fixed-revision targeted test then reproduced an AC-006 failure: the pager-created basic-info fragment remained editable because the Activity attempted to lock it before the fragment existed.

## Minimum Fix Scope

The minimum authorized implementation and validation scope is:

- propagate submitted read-only state into pager-created basic-info fragments;
- retain a post-pager Activity lock as a defensive fallback;
- add committed targeted instrumentation coverage for the submitted read-only overlay and representative editable controls;
- execute the targeted instrumentation test on the fix commit;
- verify Room reread after Activity recreation and before re-upload;
- verify successful draft deletion/cleanup;
- verify failed, timed-out, and interrupted re-upload keeps the draft marker and allows retry;
- update `execution-report.md`, `verification.md`, and `state.yaml` with criterion-level evidence.

## Evidence Collected

- Fixed revision `9524e03` was tested in an isolated checkout with the Android Studio bundled JBR.
- JVM unit tests passed.
- Connected Android tests passed: 51/51, with 0 failures, 0 errors, and 0 skipped on Android 14 `Medium_Phone(AVD)`.
- The suite does not contain the targeted AC-006/AC-007 cases, so this result validates the build and existing regression surface only; it does not close the evidence gap.
- The fixed-revision targeted read-only test failed on `etRemarks.isEnabled == true`, proving an implementation regression in the pager-fragment lifecycle.
- Fix commit `452915a` propagated the read-only flag into fragment creation and retained the Activity post-lock fallback.
- The targeted read-only test passed after the fix; the full connected suite passed 52/52 and JVM unit tests passed 124/124.

## Stop Conditions

- Re-implementation is authorized only after this root cause and minimum fix scope are recorded.
- After the fix, a passing targeted test is required before routing to verification completion.

## Implementation Result

- Status: completed in commit `452915a`.
- Targeted AC-006 regression test: PASS.
- Developer validation: PASS for JVM and connected Android suites.
- Remaining: independent Verification evidence for AC-007 and CI result.
