# Fix Plan

## Scope
- Implement the current user's approved behavior for a fresh, online, non-restored new/add form with missing waypoint coordinates.
- Keep camera centering separate from form and waypoint coordinate data.
- Validate only on Android emulators; do not use the connected physical phone.

## Minimum Fix Boundary
- Add an explicit new/add eligibility flag to the existing form launch.
- Reuse the existing fused-location permission/acquisition flow and current fallback behavior.
- Keep saved coordinates, view mode, offline mode, submitted read-only mode, and restored drafts outside the new/add path.
- Add focused policy and emulator instrumentation coverage only.

## Validation
- Build the app and androidTest APKs.
- Run the focused current-location and fallback instrumentation cases using emulator-5554 explicitly.
- Record build, test, and any environment limitations in execution-report.md.
