# Debug Fix Plan

## Task

- Task: `feat-0923`
- Related issue: `ISS-feat-0923-004`
- Classification: verification evidence/revision gap

## Root Cause

The existing verification report tests baseline revision `692991c`, while the submitted-draft read-only and re-upload behavior was implemented in `9524e03`. The report therefore cannot establish runtime behavior for AC-006 or AC-007.

## Minimum Fix Scope

No production-code fix is authorized by the current evidence. The next validation scope is:

- verify fixed implementation revision `9524e03`;
- execute targeted instrumentation coverage for the submitted read-only overlay and all editable controls;
- verify Room reread after Activity recreation and before re-upload;
- verify successful draft deletion/cleanup;
- verify failed, timed-out, and interrupted re-upload keeps the draft marker and allows retry;
- update `verification.md` and `state.yaml` with criterion-level evidence.

## Evidence Collected

- Fixed revision `9524e03` was tested in an isolated checkout with the Android Studio bundled JBR.
- JVM unit tests passed.
- Connected Android tests passed: 51/51, with 0 failures, 0 errors, and 0 skipped on Android 14 `Medium_Phone(AVD)`.
- The suite does not contain the targeted AC-006/AC-007 cases, so this result validates the build and existing regression surface only; it does not close the evidence gap.

## Stop Conditions

- If targeted verification passes, route to verification completion without re-implementation.
- If a concrete behavior failure is reproduced, classify it as `implementation_regression`, add the trace to `issue-log.md`, and create a revised fix plan before changing production code.
