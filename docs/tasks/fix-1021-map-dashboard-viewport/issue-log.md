# Issue Log — fix-1021-map-dashboard-viewport

## ISS-FIX-1021-001 — Main map resets to Taoyuan after Dashboard navigation

- Task: `fix-1021-map-dashboard-viewport`
- Phase found: investigation
- Category: `implementation_regression`
- Priority: P2
- Status: resolved
- Impact: Switching tabs loses the current main map viewport and may leave the map at the fixed Taoyuan fallback.
- Expected: Returning from Dashboard preserves the existing map viewport.
- Actual: The shell replaces `MapWorkspaceFragment`; the new map unconditionally moves to `(24.9929, 121.3011)` before asynchronous location resolution.
- Evidence:
  - `app/src/main/java/com/example/taoyuangutter/MainShellActivity.kt`
  - `app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt:567`
  - `app/src/main/java/com/example/taoyuangutter/main/MainViewModel.kt`
  - `app/src/androidTest/java/com/example/taoyuangutter/MainShellActivityTest.kt`
- Resolution: retain the tagged map fragment and its map view while Dashboard is selected; the focused emulator instrumentation test passes.
- Next action: verification
