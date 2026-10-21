# Analysis

## Task Classification
- Task ID: `fix-1021-map-dashboard-viewport`
- Type: `bugfix`
- Current branch: `codex/fix/map-dashboard-viewport`
- Scope: preserve the main map fragment and viewport during map/dashboard tab changes.

## Current Behavior
- `MainShellActivity.showTab()` creates a new fragment and calls `replace()` for every tab selection.
- Switching to Dashboard removes `MapWorkspaceFragment`; returning creates a new one.
- The new fragment's `onMapReady()` first moves its camera to `(24.9929, 121.3011)`, then starts an asynchronous location request.
- `MainViewModel` stores overlay state only. It does not store camera target or zoom.
- The location callback can later move the map to a usable fix; without a usable fix, the hardcoded Taoyuan center remains visible.

## Expected Behavior
- Tab visibility remains mutually exclusive, while returning to Map shows the same fragment and map instance.
- The user's current map viewport remains unchanged by Dashboard navigation.
- Initial map location and explicit Locate behavior remain as they are.

## Root Cause
- This is a source-confirmed implementation defect. `replace()` removes the map fragment and its map view; `showTab()` does not retain or restore its instance. Each return therefore runs `MapWorkspaceFragment.onMapReady()` again and applies the fixed fallback before location resolution.
- The existing shell test checks the fragment type after each switch but does not assert instance retention or map viewport preservation.

## Affected Modules
- `app/src/main/java/com/example/taoyuangutter/MainShellActivity.kt` — tab fragment creation, visibility, and lifecycle.
- `app/src/androidTest/java/com/example/taoyuangutter/MainShellActivityTest.kt` — map/dashboard switch regression coverage.

## Dependencies
- AndroidX FragmentManager transaction APIs and the existing `MainShellActivity` fragment test harness.

## Risks
- Retaining hidden fragments can keep state and child UI alive. The active tab must be the only RESUMED fragment, and visibility must remain exclusive.
- The change must work with FragmentManager state restoration after activity recreation.

## Validation Limit
- No runtime reproduction has been run yet for this task. Source paths prove the reset mechanism; the targeted emulator test will verify fragment retention on tab navigation.
