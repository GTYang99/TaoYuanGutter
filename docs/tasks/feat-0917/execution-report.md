# Implementation Execution Report

## Implemented

- Updated nodeDetails/storeDitch contract to canonical `IS_TIEINPOINT` and `IS_CONNECTING`.
- Non-virtual storeDitch nodes now include Boolean `IS_CANTOPEN`, `IS_TIEINPOINT`, and `IS_CONNECTING`, including `false`.
- Virtual nodes omit `IS_CANTOPEN`, `IS_TIEINPOINT`, and `IS_CONNECTING` from form/result snapshots and request JSON.
- Added `nodeDetails` String `"0"`/`"1"` parsing helpers and Cant Open priority normalization.
- Rewired form/import/draft/edit handoff to canonical keys; legacy active request paths no longer emit lowercase connect keys.
- Implemented Cant Open / 銜接點 mutual exclusion with disabled/gray counterpart behavior.
- Changed UI copy to `連結管`, placed it after `淤積程度`, defaulting to `無`.
- Blocked inspect-to-edit when nodeDetails preload fails; photo-only issues may still continue with warning.
- Added label/status display for `(銜接點)` and `(待架站)` with pending-deploy last.
- Removed `(銜接點)` from the editable add-gutter waypoint list; inspection spinner labels remain independent.
- Reordered inspection details to material, structural damage, hanging pipeline, silt, connecting pipe, then remarks.
- Preserved create XY_NUM omission and edit XY_NUM preservation.
- Kept recent saved waypoint import no-query `closestNodeDetails` behavior.

## Validation

| Check | Result |
|---|---|
| `:app:compileDebugKotlin` | PASS |
| `:app:testDebugUnitTest --tests StoreDitchNodeRequestMapperTest --tests StoreDitchResponseParsingTest` | PASS, 9 tests |
| `:app:testDebugUnitTest` | PASS, 86 tests |
| `:app:assembleDebug` | PASS |
| `:app:compileDebugAndroidTestKotlin` | PASS |
| `:app:connectedDebugAndroidTest` | PASS, 36 tests on `adb-QV710EDR3A-hF5XZF (2)._adb-tls-connect._tcp` |
| `InspectionPresentationTest` + `:app:compileDebugKotlin` | PASS |

## Notes

- Authenticated API request/response capture was not run separately beyond automated/device tests.
- Untracked `.worktrees/` remains untouched.
- No physical-device test was run for these two display fixes per user direction.

## ISS-013 Negative Presence Wording Fix

- Changed only the inspection display mapping for `附掛或過路管線` and `連結管`: present is `有`, absent is `無`.
- Left `溝體結構受損` on its existing `是／否` wording.

| Check | Result |
|---|---|
| `:app:testDebugUnitTest --tests com.example.taoyuangutter.gutter.InspectionPresentationTest` | PASS, 3 tests |
| `:app:compileDebugKotlin` | PASS |

- Physical-device testing was not run, per user direction.
- Implementation commit: `b2ba985` (`fix(feat-0917): use absence wording in inspection`).

## ISS-014 Rollback

- Reverted the earlier ISS-014 implementation in `14f6c78`. It only moved when import success state was written and did not eliminate the mismatch between progress counting and actual upload eligibility.
- No validation result is claimed for the withdrawn implementation. ISS-014 is reopened for the revised, single-candidate-source fix plan.
