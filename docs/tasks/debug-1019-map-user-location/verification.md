# Developer Audit Findings — Not Independent Verification

## Revision and Status

- Audited revision: `6468e8903ba2985a3a85c5732e235e55783380f8`
- Worktree: clean at the start of this audit.
- Overall result: `NOT VERIFIED`.
- This is a developer coverage audit used to route a newly found failure to Debug. It is not an independent Verification and cannot advance the task to Release.

## Acceptance Criteria

| AC | Result | Evidence / reason |
|---|---|---|
| AC-001 | PASS | `savedCoordinateEditKeepsSavedCameraTarget()` passed on Android 14 API 34 and asserts the saved waypoint remains the camera target without activating automatic location. |
| AC-002 | PASS | Android 14 API 34 instrumentation passed for ordinary blank edit, offline blank, restored blank, and fresh-add flows; assertions confirm camera-only movement and unchanged form/session coordinates. |
| AC-003 | NOT VERIFIED | The location-policy unit tests pass, but host-map and point-picker Locate have no end-to-end runtime capture in this task. |
| AC-004 | FAIL | `NearbyLocationFixPolicy.shouldAccept()` checks only timestamp and accuracy. A fresh accurate `(0,0)` provider fix passes and is sent to `showImportMyLocationMarker()`, which animates the form map to that coordinate (`GutterFormActivity.kt`, `NearbyLocationFixPolicy.kt`). Tracked as `ISS-DBG-1019-007` and routed to Debug. |
| AC-005 | PASS | Source review confirms both host callbacks keep an absent map target nullable and omit the camera movement. |
| AC-006 | PASS | Source review confirms point-picker Locate changes the viewport while Confirm is the result-return path. |

## CI and Remaining Evidence

- Android CI Run #20 for commit `6468e89` is queued. Run #19 for prior commit `673671a` remains in progress, but its job has no step results after runner startup. Neither is a passing CI result for the audited revision.
- Independent Verification of the final implementation revision is still required.
- AC-003 host and picker end-to-end behavior remains unverified.

## Route

- `ISS-DBG-1019-007` is an `implementation_regression`, priority P2. The code change must follow Debug evidence and then receive fresh Developer Validation and independent Verification on the committed revision.

## Developer Follow-up After Debug Fix

- `NearbyLocationFixPolicy` now validates latitude and longitude with `MapCoordinatePolicy.isUsableCoordinate()` before accepting a fresh result.
- `NearbyLocationFixPolicyTest` passes all 3 tests, including the fresh accurate `(0,0)` regression case; `assembleDebug` and `assembleDebugAndroidTest` also pass.
- This resolves the source-level defect in `ISS-DBG-1019-007`, but does not convert the finding into an independent Verification result. Re-evaluate all acceptance criteria on the new committed revision.
