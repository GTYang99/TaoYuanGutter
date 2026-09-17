# Plan Review

## Review Scope

- Reviewed `requirement.md`, `analysis.md`, `plan.md`, `state.yaml`, repository architecture and relevant existing API/UI/test paths.

## Checklist

| Item | Result |
|---|---|
| Requirements and acceptance criteria | Complete; all four ACs are concrete and include virtual-node behavior. |
| Repository analysis and affected modules | Complete; inspect-to-edit now covers the response, waypoint, Fragment argument, prefill and collection boundaries. |
| Dependencies and risks | Complete; serialization, response mapping, state policy and location lifecycle are covered. |
| Test and regression plans | Complete; mapper, UI, inspect-to-edit, mode, import and device coverage now address each AC and the Connect Point/Cant Open regression boundary. |
| Scope and rollback | Appropriate; no backend, Room schema, photo protocol or general map-location behavior is expanded. |
| Open questions | None. |

## Findings

### Finding 1

Severity: Major

Category: API Serialization

Description: The approved virtual-node rule requires omission of `IS_CANTOPEN`, but the current request DTO makes it non-null. Stating only that virtual payload omits the key was not sufficiently actionable.

Recommendation: Explicitly make `IS_CANTOPEN` serializable as an omitted nullable field for virtual nodes and add a JSON assertion.

Planning Response: Added to implementation step 1, step 2 and mapper JSON test plan.

Status: Resolved

### Finding 2

Severity: Major

Category: Response Mapping

Description: The plan required system `XY_NUM` response handling but did not state the existing `DitchXyNum` shape used for deterministic mapping.

Recommendation: Map `start`, ordered `nodes`, and `end` to start, intermediary and end waypoints explicitly.

Planning Response: Added to implementation step 2.

Status: Resolved

### Finding 3

Severity: Suggestion

Category: Observability

Description: Device validation should retain sanitized request/response evidence only and must not log credentials.

Recommendation: Use existing diagnostic logging with Authorization redacted.

Planning Response: Covered by existing app logging practice; implementation must not add token logging.

Status: Accepted

### Finding 4

Severity: Major

Category: Repository Analysis / Edit Readback

Description: The plan does not identify `GutterInspectActivity.kt` as an affected file or give it an implementation step. The existing inspect-to-edit flow loads `NodeDetails` and constructs each editable waypoint's `basicData` in this activity, including the current `IS_CANTOPEN` mapping. Adding the two response DTO fields and form controls alone will not preserve `is_connect_point` and `is_connect_pipe` when a user opens an existing point from inspect mode and then edits it. The form would instead receive absent keys and submit their default values, violating AC-001 and AC-002.

Evidence: `GutterInspectActivity.kt` builds the edit `basicData` map at lines 447-483; `GutterBasicInfoFragment` consumes that map to initialise and collect form state. `plan.md` lists `GutterBasicInfoFragment.kt`, `GutterFormActivity.kt`, and `AddGutterBottomSheet.kt`, but not `GutterInspectActivity.kt`.

Recommendation: Add `GutterInspectActivity.kt` to affected files and an explicit step that maps both nullable response Booleans to the form's persisted `basicData` keys with `false` fallback. Extend the test plan with an inspect-to-edit prefill assertion that confirms both values survive an unchanged edit submission.

Planning Response: Added to the revised repository analysis, affected files, implementation step 3, and inspect-to-edit test plan.

Status: Resolved

### Finding 5

Severity: Major

Category: Form Data Boundary

Description: The revised plan correctly adds `GutterInspectActivity` as the inspect-to-edit handoff owner, but it does not cover the next boundary: `GutterBasicInfoFragment.newInstance()` copies a whitelist of `basicData` keys into Fragment arguments, and `prefillData()` reads that same explicit argument set. Merely writing `is_connect_point` and `is_connect_pipe` into `basicData` will therefore not make the controls display their values or reliably retain them through `collectData()`. The existing no-op edit test can miss this defect if it tests the mapper directly rather than the Fragment construction path.

Evidence: `GutterBasicInfoFragment.kt` constructs its arguments at lines 210-270 and reads its prefill values at lines 596-619; neither current list has either new key. `GutterInspectActivity.kt` writes the editable waypoint `basicData` at lines 447-483. The plan's step 3 names the handoff but does not require extending the Fragment argument constants, `newInstance()` mapping, or `prefillData()` initialization.

Recommendation: Amend step 3 or 5 to explicitly add both persisted keys to `GutterBasicInfoFragment`'s argument contract and prefill path, including false defaults. Make the inspect-to-edit regression test launch the form through this Fragment construction path, assert the rendered controls, then assert an unchanged submission retains the original 0/1 values.

Planning Response: Added to implementation step 3 and the end-to-end inspect-to-edit test scenario.

Status: Resolved

### Finding 6

Severity: Major

Category: Requirement Scope / Regression Plan

Description: Implementation step 4 says Connect Point should apply the same detailed-field and photo restrictions as Cant Open. The approved requirement only states that Connect Point and Cant Open are mutually exclusive; it does not say Connect Point clears, hides, disables, skips validation for, or suppresses uploads of any other fields or photos. Making Connect Point Cant Open-equivalent would discard or block otherwise valid point data and expands product behavior beyond the approved intent.

Evidence: `requirement.md` defines only ordering and mutual exclusion for Connect Point, while the current form's Cant Open path clears detailed fields and excludes photo slots 2 and 3. `plan.md` step 4 explicitly proposes applying those restrictions to Connect Point without a corresponding requirement or resolved decision.

Recommendation: Restrict Connect Point handling to mutual exclusion with Cant Open. Preserve the normal non-Cant-Open detail-field validation, values and photo upload behavior when Connect Point is selected. Replace the current test expectation with regression coverage proving that selecting Connect Point does not clear or suppress normal details/photos, while selecting Cant Open retains its existing restrictions.

Planning Response: Revised implementation step 4 to limit Connect Point to mutual exclusion, preserving normal point fields, validation and photo slots; UI, mode, regression and device tests now cover that boundary.

Status: Resolved

## Decision

**APPROVED**

The revised plan is actionable, preserves approved product scope, identifies the complete readback and form-data path, and provides sufficient acceptance-criteria, regression and validation coverage to begin implementation.
