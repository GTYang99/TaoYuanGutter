# Implementation Plan

## Goal
- Keep the map's current viewport when the user switches to Dashboard and returns.

## Scope
- Update tab fragment retention in `MainShellActivity` and strengthen its existing navigation regression test.

## Affected Files
- `app/src/main/java/com/example/taoyuangutter/MainShellActivity.kt` — reuse fragments by stable tab tag and switch with show/hide.
- `app/src/androidTest/java/com/example/taoyuangutter/MainShellActivityTest.kt` — assert tab fragment identity survives both directions.

## Implementation Steps
1. Change `showTab()` to find/create each tab fragment once, hide other tabs, and show the selected fragment.
2. Set hidden fragments to `STARTED` and the selected fragment to `RESUMED`.
3. Update the focused test to verify the same map and dashboard instances are reused.

## Test Plan
- Build `:app:assembleDebug :app:assembleDebugAndroidTest` on the development host.
- Install and run only the focused shell and production-map instrumentation tests on the Android test emulator `emulator-5554`.
- Do not execute tests on the connected physical device.

### Physical Device Test Scope
- Requires physical device: No
- Device/environment: Android test emulator `emulator-5554` only
- In-scope Acceptance Criteria: AC-001, AC-002, AC-003
- Regression risk: hidden fragments must remain mutually exclusive and resume only when selected
- Full regression required: No
- Full regression trigger: 無
- Stop condition: focused shell test finishes; stop after its result

| AC | Environment | Steps | Expected | Evidence |
|---|---|---|---|---|
| AC-001 | `emulator-5554` | Launch the production map fragment, set a known GoogleMap center and zoom, switch Dashboard → Map, and read camera state | Center and zoom match the values set before switching | Production-map instrumentation result |
| AC-002 | `emulator-5554` | Switch Map ↔ Dashboard; compare parent and nested map fragment identity and visibility | Both original fragment instances are reused; only the selected tab is visible | Shell and production-map instrumentation results |
| AC-003 | `emulator-5554` | Launch the production map fragment and switch away/back | Initial map fragment is created once; tab return does not recreate it | Production-map instrumentation result and unchanged initialization source |

## Regression Plan
- Confirm selected tab fragment is visible and all other tab fragments are hidden.
- Confirm selected tab is RESUMED and inactive tab is not RESUMED.
- Assert the selected fragment is RESUMED and the hidden fragment is STARTED in the focused instrumentation test.
- Confirm existing map initialization code is untouched.

## Risks
- Fragment visibility or lifecycle caps may diverge from selected bottom navigation state.
- Keeping tab instances alive consumes the resources of both tabs while the shell remains open.

## Rollback Plan
- Revert the task commit.

## Current Behavior
- Every tab selection replaces the current fragment. Returning to Map creates a new `MapWorkspaceFragment`, whose map starts at the fixed Taoyuan fallback.

## Expected Behavior
- The same `MapWorkspaceFragment` and map view remain alive while Dashboard is shown, so returning restores the exact current center and zoom.

## Acceptance Criteria Traceability
| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | 1 | Focused test confirms retained map instance across tab switch |
| AC-002 | 1-2 | Focused test confirms fragment identity and active/hidden tab states |
| AC-003 | 1-2 | Focused test confirms initial Map tab and no recreation on return |

## Failure Behavior
- If fragment creation or lookup fails, keep the current selected fragment visible and do not alter map camera behavior.

## Security and Privacy
- 無

## Open Questions
- 無
