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

## Follow-up Debug Fix Plan: Submitted Re-upload No-Write Contract

### Task

- Task: `feat-0923`
- Related issue: `ISS-feat-0923-006`
- Failed acceptance criterion: AC-007
- Classification: implementation regression reproduced by source audit on `452915a`

### Root Cause

The submitted retry path rereads the Room row but restores it into mutable `AddGutterBottomSheet.waypoints`. Photo upload preparation then invokes the shared `onWaypointsChanged` callback, and `GutterSheetSessionBinder` routes that callback to draft auto-save. The retry path therefore does not preserve the required immutable Room snapshot/no-write boundary.

### Minimum Authorized Scope

- Add a dedicated immutable submitted-retry snapshot/projection path sourced from the Room row.
- Keep request mapping and photo upload normalization transport-only; do not write the normalized result through the editable form callback.
- Guard or bypass submitted retry callbacks so `onWaypointsChanged` and session auto-save are not invoked by submitted retry progress or failure.
- Preserve success deletion and keep the draft row unchanged on upload failure, timeout, cancellation, or interruption.
- Add focused tests covering payload completeness, no Room write on retry failure, and retry from the unchanged submitted draft.

### Required Validation Before Re-verification

- Run targeted JVM/instrumentation tests for the submitted retry and no-write contract.
- Run the full relevant JVM and connected Android regression suites.
- Commit the implementation and tests, then rerun independent Verification against that exact commit.
- Obtain CI build/test evidence; local green tests do not close the CI gate.

### Stop Conditions

- Do not alter approved submission-marker or `SPI_NUM` behavior.
- Do not use the current uncommitted working-tree changes as the verification revision.
- Do not route back to Release until AC-007 passes and all remaining AC/CI evidence is resolved.

## Follow-up Implementation Result

- Fix commit: `ee55f08`.
- Implemented the Room snapshot/transport projection boundary and suppressed submitted retry auto-save callbacks.
- JVM unit tests: 126/126 PASS.
- Targeted submitted read-only instrumentation: PASS.
- `MainShellActivityTest` class retry: 12/12 PASS after one first-run focus timing failure in the full suite.
- Independent Verification must rerun against `ee55f08`; AC-007 is not marked PASS by developer validation alone.
