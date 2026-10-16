# Implementation Plan

## Goal
- Use the user's current location for the initial mini-map viewport of an empty new/add form.

## Scope
- Mark only fresh, online, non-restored add-form launches as eligible for initial location centering.
- Reuse the existing form permission, fused-location, failure prompt, and fallback flow.
- Keep offline/restored drafts, saved coordinates, and coordinate persistence unchanged.

## Affected Files
- AddGutterBottomSheet.kt — determine fresh add-sheet eligibility.
- MainActivity.kt and MapWorkspaceFragment.kt — pass eligibility through the form launch; the fragment is the normal host.
- GutterFormNavigator.kt and GutterFormActivity.kt — carry and consume the launch flag.
- EditMapLocationPolicy.kt — allow the explicitly eligible new/add form while keeping current guards.
- EditMapLocationPolicyTest.kt — assert new-add eligibility and existing exclusions.
- EditMapInitialLocationInstrumentedTest.kt — replace the obsolete no-recenter expectation and verify an emulator GPS fix plus camera-only use.
- docs/tasks/debug1016-1/* — keep requirement, analysis, state, and execution evidence current.

## Implementation Steps
- Add a read-only predicate on AddGutterBottomSheet for a fresh online add form that is not editing, restored, or submitted read-only.
- Pass that predicate through both existing host launch paths and GutterFormNavigator into an optional GutterFormActivity intent extra.
- Extend EditMapLocationPolicy to accept the explicit new/add flag; preserve view, offline, submitted-read-only, and saved-coordinate exclusions.
- Update the policy and instrumentation coverage so a fresh new point centers on an injected emulator location without modifying form or waypoint coordinates, while an unmarked form keeps the fallback.

## Test Plan

- Build the app and instrumentation APK.
- Run the focused EditMapInitialLocationInstrumentedTest cases on emulator-5554 only, using an explicit adb serial and manual instrumentation invocation. Do not invoke a Gradle connected-device task that could include physical devices.
- Target the emulator GPS centering case and the unmarked/fallback regression case. Also run the saved-coordinate precedence case. Capture exact commands and results in execution-report.md.
- Do not run physical-device tests.

### Physical Device Test Scope
- Requires physical device: No
- Device/environment: Android emulator emulator-5554 (sdk_gphone16k_arm64); physical device validation is excluded by user instruction.
- In-scope Acceptance Criteria: AC-001, AC-002, AC-004, AC-005
- Regression risk: Saved coordinates, unmarked/restored forms, failure fallback, and coordinate non-persistence.
- Full regression required: No
- Full regression trigger: 無
- Stop condition: Stop after focused emulator cases finish or produce enough failure evidence to classify the issue.

| AC | Environment | Steps | Expected | Evidence |
|---|---|---|---|---|
| AC-001, AC-002 | Android emulator emulator-5554 | Grant app location permissions, inject an emulator GPS fix with `adb -s emulator-5554 emu geo fix 121.5 25.03`, open a fresh new/add form with missing coordinates | Camera centers on the fix; NODE_X/NODE_Y and session waypoint coordinates remain empty | Instrumentation assertions |
| AC-003 | Source and focused emulator coverage | Open with saved coordinates | Saved target remains; location flow stays inactive | Existing saved-coordinate instrumentation assertion |
| AC-004 | Android emulator emulator-5554 | Open with the new/add marker absent | Form remains at fallback and does not run location flow | Instrumentation regression assertion |
| AC-005 | Android emulator emulator-5554 | Run existing no-fix case | Unavailable prompt appears; fallback remains usable | Existing instrumentation assertions |

## Regression Plan
- Preserve existing edit-mode, saved-coordinate, manual-pan, permission-denial, and unavailable-location behavior.
- Preserve offline and restored-draft behavior by setting the new/add intent flag only for a fresh add sheet and retaining policy exclusions.
- Verify the current-location camera target does not mutate form coordinates or draft waypoint coordinates.

## Risks
- The new permission request could be unexpected on the fresh add path; the user explicitly requested current location, and the established form flow already handles denial.
- A test command that omits the emulator serial could touch a physical device; use only adb -s emulator-5554 and run instrumentation directly on that serial.

## Rollback Plan
- Revert the focused task commit; the intent flag defaults to false, so existing form behavior can be restored without migration.

## Current Behavior
- Missing-coordinate new/add forms stay at LatLng(24.9929, 121.3011) because the location policy is edit-only.

## Expected Behavior
- Eligible fresh online add forms use a usable fused current-location fix for the map camera only; failure keeps the established fallback and prompt.

## Acceptance Criteria Traceability
| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | Explicit launch flag and policy extension | Emulator fused-mock-location instrumentation |
| AC-002 | Keep the fix camera-only | Emulator assertions for currentFormData and sessionWaypoints |
| AC-003 | Retain saved-coordinate branch and policy guard | Existing saved-coordinate instrumentation assertion |
| AC-004 | Mark only fresh add forms and preserve exclusions | Policy unit assertions plus emulator unmarked-form regression |
| AC-005 | Reuse the existing location failure flow | Existing no-fix emulator instrumentation assertion |

## Failure Behavior
- Reuse the existing permission retry, location-unavailable message, and Taoyuan fallback. Dismissing the message continues editing as before.

## Security and Privacy
- Use foreground location permission already declared by the app. The acquired location is not written to form or draft data.

## Open Questions
- None.
