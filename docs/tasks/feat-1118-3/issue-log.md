# Issue Log

## ISS-feat-1118-3-001

- task_id: `feat-1118-3`
- phase: verification
- category: implementation_regression
- priority: P1
- title: Targeted JVM tests still assert removed tie-in behavior
- status: open
- impact: Verification and the test gate cannot pass.
- evidence:
  - `StoreDitchNodeRequestMapperTest` has three assertions requiring `IS_TIEINPOINT` in JSON.
  - `GutterCompletionPolicyTest.connectPointUsesCantOpenDetailExemption` requires tie-in exemptions.
  - `InspectionPresentationTest.tieInInspectionHidesTheSameDetailFieldsAsCantOpen` requires tie-in inspection behavior.
  - Command result: 27 tests completed, 5 failed.
- next_action: debug
- owner: developer

## ISS-feat-1118-3-002

- task_id: `feat-1118-3`
- phase: debug
- category: implementation_regression
- priority: P1
- title: General connection-pipe compatibility was removed with tie-in behavior
- status: open
- impact: Normal-point form, inspect, and storeDitch compatibility may regress beyond the approved tie-in removal.
- evidence:
  - `knowledge-resolution.md` explicitly excludes the historical `feat-1118` connecting-pipe removal from this task.
  - Baseline `8d484d5a` contains `IS_CONNECTING` in `NodeDetails`, request model, mapper, and form contract.
  - Current production code no longer contains those paths.
- next_action: implementation_debug
- owner: developer

## ISS-feat-1118-3-003

- task_id: `feat-1118-3`
- phase: debug
- category: implementation_regression
- priority: P1
- title: Legacy submitted and damaged drafts do not meet compatibility policy
- status: open
- impact: AC-006 and AC-008 are not implemented; submitted legacy tie-in drafts cannot be repaired and partially corrupt drafts may be discarded wholesale.
- evidence:
  - `SubmittedDraftResumePolicy.isReadOnly()` has no legacy tie-in exception.
  - `GutterSessionRepository.entityToDraft()` returns null for a malformed waypoint payload.
  - `parseLegacyDrafts()` returns an empty list for a malformed legacy batch.
  - Draft serialization still receives raw waypoint `basicData`.
- next_action: implementation_debug
- owner: developer
