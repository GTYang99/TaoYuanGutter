# Implementation Plan

## Goal
- Keep valid saved waypoint centers and make blank gutter maps and explicit locate actions use the latest usable device location without mutating coordinate data.

## Scope
- Update blank-form location eligibility/presentation, zero/invalid viewport-coordinate handling, the shared controller's explicit recenter behavior, and host form handoff when the map target is absent.

## Affected Files
- `app/src/main/java/com/example/taoyuangutter/gutter/EditMapLocationPolicy.kt` — define editable blank-form location eligibility.
- `app/src/main/java/com/example/taoyuangutter/map/MapCoordinatePolicy.kt` — shared validation for map viewport and location-fix coordinates.
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt` — preserve valid saved centers and run camera-only location for any editable blank form.
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormNavigator.kt` — preserve and validate the host location handoff for synchronous blank-form camera seeding.
- `app/src/main/java/com/example/taoyuangutter/map/MyLocationController.kt` and `LocationFixQualityPolicy.kt` — allow explicit recenter actions to prefer a newer usable fix and retain cache as fallback.
- `app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt` and `app/src/main/java/com/example/taoyuangutter/MainActivity.kt` — use fresh-preferred location on host map location flows and avoid `(0,0)` handoff targets.
- `app/src/main/java/com/example/taoyuangutter/gutter/MapPointPickerActivity.kt` — use fresh-preferred location for the blank-map and explicit locate flows.
- Focused Kotlin unit tests and `EditMapInitialLocationInstrumentedTest.kt` — cover policy boundaries, saved-coordinate precedence, offline/restored blank-form camera-only behavior, fallback, and picker-compatible location policy.
- `docs/tasks/debug-1019-map-user-location/` — requirement, plan review, execution, verification, and state evidence.

## Implementation Steps
- Add a testable shared coordinate-validity rule that rejects missing, non-finite, out-of-range, and `(0,0)` map targets while preserving stored values.
- Change initial form-map eligibility so any editable, writable blank form can attempt current location, including offline and restored drafts; use a bounded high-accuracy fused request so GPS-only/offline environments can satisfy it; keep saved-point, view, and submitted read-only guards.
- For a blank map, center synchronously on a fresh usable host fix when supplied. Otherwise keep the map surface hidden during location resolution, then reveal it only after applying a usable result or after selecting the existing fallback on failure; saved points remain immediately visible.
- Add an opt-in fresh-preference mode to `MyLocationController`, with cached-location fallback when the fresh result is unavailable or invalid; enable it for host and picker locate flows.
- Make host map targets nullable and skip camera movement when both the waypoint and map target are absent.
- Extend unit and emulator instrumentation coverage, then run the focused checks and affected debug build.
- Record results and remaining environment/CI limitations in the execution report and state.

## Test Plan
- Run `:app:testDebugUnitTest` with filters for `EditMapLocationPolicyTest`, `MapCoordinatePolicyTest`, and `LocationFixQualityPolicyTest`.
- Build `:app:assembleDebug` and `:app:assembleDebugAndroidTest`.
- Run focused `EditMapInitialLocationInstrumentedTest` cases using the configured emulator serial, never an unfiltered connected-device invocation.
- Add emulator assertions for pre-result map visibility, successful-result reveal, and failure/fallback reveal; retain saved-coordinate and manual-pan protection coverage.

### Physical Device Test Scope
- Requires physical device: No.
- Device/environment: Android emulator `emulator-5554` if available.
- In-scope Acceptance Criteria: AC-001, AC-002, AC-004.
- Regression risk: camera initialization, permission/fallback behavior, and accidental coordinate persistence.
- Full regression required: No.
- Full regression trigger: 無.
- Stop condition: all specified form-map cases complete, or the emulator/test environment prevents execution.

| AC | Environment | Steps | Expected | Evidence |
|---|---|---|---|---|
| AC-001 | JVM test + emulator | Open a form with valid saved coordinates | Saved coordinate remains the camera target; initial location flow is inactive | JUnit and camera target assertion |
| AC-002 | Emulator | Open an editable blank form in normal, offline, and restored-draft modes with a fused test fix | Camera reaches the fix; `NODE_X`, `NODE_Y`, and waypoint snapshot coordinates remain unchanged | Instrumentation assertions |
| AC-003 | JVM test | Supply a newer fix with less than 10 m accuracy improvement and a usable cache | Fresh fix is preferred; stale or invalid fix falls back to cache | Policy/controller regression tests |
| AC-004 | Emulator | Open a blank form without a usable fix | Existing fallback and unavailable prompt remain | Instrumentation assertion |
| AC-005 | JVM test or focused callback test | Open a waypoint with no coordinate while map target is absent | No `(0,0)` camera target is issued | Nullable target policy/callback assertion |
| AC-006 | Code/test review | Tap Locate without Confirm, then inspect picker result path | Locate changes viewport only; coordinate is returned only on Confirm | Instrumentation/source review |
| AC-007 | Emulator | Open a blank form without a host fix and pause at permission choice; then grant/deny; also open with a host fix | Map is hidden while unresolved, reveals at current fix on success, reveals Taoyuan only after failure, and opens directly on a usable host fix | Instrumentation visibility and camera assertions |

## Regression Plan
- Confirm valid saved waypoint coordinates still prevent automatic location.
- Confirm manual pan remains protected from delayed form-location callbacks.
- Confirm failed location and permission denial retain the fallback/prompt.
- Confirm a blank form does not expose the fallback before the location flow resolves; confirm a usable host fix is used as its synchronous initial center.
- Confirm point-picker Locate does not return a result until Confirm.
- Confirm location recentering does not update form/session coordinate fields.

## Risks
- Existing offline/restored blank forms will now enter the foreground permission flow.
- High-accuracy acquisition uses more power than balanced acquisition, but the form requests it only for blank maps and keeps the existing bounded retry.
- Fresh-preferred recentering may wait for a new provider response; the accepted cached fix is used if the request fails or produces no newer usable result.
- Treating `(0,0)` as invalid affects only viewport selection; existing form data remains unchanged.
- When no usable host fix exists, the blank map is briefly hidden during initial location resolution. The existing fallback is still shown after the unavailable decision so the user can continue manually.

## Rollback Plan
- Revert the isolated task commit to restore the prior eligibility and cached-fix rules.

## Current Behavior
- Editable blank form location is gated by edit/fresh-add flags and excludes offline forms; `(0,0)` snapshots are treated as saved coordinates.
- The shared locate controller can retain a cached center when a newer result misses the 10 m accuracy-improvement threshold.
- Host handoff code substitutes `(0,0)` when it cannot read the map camera target.

## Expected Behavior
- Valid saved coordinates remain centered. Editable blank maps, including offline/restored maps, request a camera-only location fix.
- Explicit locate actions prefer a newer usable fix and fall back to the cached fix only when no usable fresh result is available.
- Missing host camera targets do not create a `(0,0)` camera move, and no camera-only location flow writes coordinate fields.
- AC-007 / Step 2 — a no-host-fix launch must keep the map hidden until success/fallback; a host-fix launch must use that fix before first display.

## Acceptance Criteria Traceability
| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | Steps 1–2 | Unit and emulator saved-coordinate tests |
| AC-002 | Steps 1–2 | Offline/restored emulator instrumentation and coordinate assertions |
| AC-003 | Step 3 | Location-quality policy unit tests |
| AC-004 | Steps 2–3 | Fallback/permission emulator instrumentation |
| AC-005 | Step 4 | Nullable host target test/source review |
| AC-006 | Steps 3 and 5 | Picker flow instrumentation/source review |

## Failure Behavior
- Preserve existing Taoyuan fallback and unavailable prompt when permission or a usable current fix is unavailable. Recenter buttons may use a still-usable cached fix if a newer fix is unavailable.
- During a blank form's unresolved initial location request, keep its map surface hidden; reveal it at the usable fix on success or at the Taoyuan fallback before showing the existing unavailable prompt.

## Security and Privacy
- Reuses foreground location permissions and the existing fused-location provider. Camera-only requests do not persist location data.

## Open Questions
- None.
