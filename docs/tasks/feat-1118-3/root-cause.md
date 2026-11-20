# Root Cause Analysis

## Scope and Evidence

- Task: `feat-1118-3`
- Verification revision: `108a6dc2bd441a87cc053d130c058f2b9eac5141`
- Verification report: `docs/tasks/feat-1118-3/verification.md`
- Issue: `ISS-feat-1118-3-001`
- No production code was changed during this analysis.

## Classification

The verification failure is an `implementation_regression`, with stale test expectations as a secondary test-suite alignment problem. The approved knowledge-resolution decision explicitly says that the historical `feat-1118` removal of `IS_CONNECTING` is not applicable to this task. Therefore, removing the general-point connection-pipe path in this branch is outside the approved scope.

## Root Causes

### R1 — Existing tests still assert removed tie-in behavior

The production change correctly removed `IS_TIEINPOINT` from request serialization, detail/photo exemption, and inspection behavior. The following tests were not migrated and still assert the old behavior:

- `StoreDitchNodeRequestMapperTest` (three failing assertions)
- `GutterCompletionPolicyTest.connectPointUsesCantOpenDetailExemption`
- `InspectionPresentationTest.tieInInspectionHidesTheSameDetailFieldsAsCantOpen`

These tests must be rewritten to assert that legacy tie-in input is ignored and processed as a normal point. They must not be fixed by restoring tie-in behavior.

### R2 — `IS_CONNECTING` was removed together with `IS_TIEINPOINT`

The approved task removes only tie-in behavior. The implementation also removed `IS_CONNECTING` from `NodeDetails`, `StoreDitchNodeRequest`, the request mapper, form Intent/result contracts, and form/inspection paths. The baseline at `8d484d5a` shows that a normal non-virtual, non-`IS_CANTOPEN` point sent and restored this value.

This is an implementation regression unless a new product decision explicitly expands the task scope. The fix must restore the existing `IS_CONNECTING` path while keeping `IS_TIEINPOINT` absent from runtime UI, local write-back, and request JSON.

### R3 — Submitted legacy tie-in drafts have no unlock exception

`SubmittedDraftResumePolicy.isReadOnly()` only checks the submission marker and valid `SPI_NUM`. `GutterSessionFlowCoordinator` therefore passes `submittedDraftReadOnly = true` for all submitted drafts without `SPI_NUM`, and `GutterFormActivity`/`AddGutterBottomSheet` block edits. There is no check for a submitted legacy tie-in waypoint, so AC-006 is not implemented.

### R4 — Draft normalization does not remove legacy markers at the save boundary

`GutterDraftCoordinator.autoSaveSessionDraft()` snapshots `wp.basicData` directly. `GutterSessionRepository.toEntity()` serializes the snapshot directly. The current `IS_TIEINPOINT` handling only excludes the key when deciding whether a draft is effectively empty; it does not prevent the key from being persisted again. This violates the requirement that legacy keys may be read but must not be regenerated or saved.

### R5 — Corrupt draft parsing is all-or-nothing

`GutterSessionRepository.entityToDraft()` returns `null` when the complete waypoint JSON fails to parse, and `parseLegacyDrafts()` returns an empty list when the complete legacy JSON fails. This drops an entire draft or migration batch instead of retaining valid waypoints and skipping only malformed waypoints, leaving AC-008 unimplemented.

### R6 — Instrumentation tests still depend on deleted symbols and old behavior

`GutterBasicInfoUiTest` still references `R.id.cbConnectPoint`, and `GutterFormContractInstrumentedTest` references the removed `EXTRA_DATA_IS_CONNECTING` contract. Other instrumentation cases still expect tie-in detail exemptions. This prevents reliable instrumentation compilation/runtime evidence and is not covered by the previous JVM-only failure report.

## Acceptance-Criteria Mapping

| Root cause | Affected AC | Evidence |
|---|---|---|
| R1 | AC-003, AC-005, AC-009 | 5 targeted JVM failures in `verification.md` |
| R2 | AC-003, AC-004, AC-009 | Baseline/current diff removes `IS_CONNECTING` paths; knowledge-resolution.md excludes historical connecting-pipe removal |
| R3 | AC-006 | `SubmittedDraftResumePolicy.kt` has no legacy tie-in exception |
| R4 | AC-004, AC-007, AC-008 | draft snapshot and repository serialization preserve raw `basicData` |
| R5 | AC-007, AC-008 | `entityToDraft()` and `parseLegacyDrafts()` are whole-payload failure paths |
| R6 | AC-001, AC-002, AC-004, AC-009 | deleted resource/contract references remain in `app/src/androidTest` |

## Regression Risk

The highest risk is restoring `IS_CONNECTING` too broadly and accidentally restoring tie-in exemptions. The implementation must distinguish:

- `IS_TIEINPOINT`: legacy read compatibility only; never displayed, written back, or serialized.
- `IS_CONNECTING`: preserve existing normal-point behavior, subject to the existing virtual/`IS_CANTOPEN` rules.

## Additional Confirmation

- Re-ran the targeted JVM command with the existing worktree: 27 tests completed, exactly the same 5 failures were reproduced at the five stale tie-in assertions listed in R1.
- `./gradlew compileDebugKotlin` completed successfully.
- Baseline/current diff confirms that `IS_CONNECTING` was removed from the request model, mapper, response model, and form contract, while the approved knowledge resolution excludes that historical removal.
- Static inspection confirms that AC-006 has no legacy tie-in predicate in `SubmittedDraftResumePolicy`, AC-008 has whole-payload failure returns in both draft parsing paths, and draft serialization still receives raw waypoint `basicData`.
- These findings raise confidence in the non-device conclusions to approximately 95%. Instrumentation compilation/runtime and baseline upgrade remain explicitly unverified.
