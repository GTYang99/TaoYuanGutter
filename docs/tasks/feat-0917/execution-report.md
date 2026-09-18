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

## Notes

- Authenticated API request/response capture was not run separately beyond automated/device tests.
- Untracked `.worktrees/` remains untouched.
