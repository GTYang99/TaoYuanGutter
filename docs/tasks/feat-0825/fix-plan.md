# Fix Plan

## Issue
- `ISS-002`

## Goal
- Ensure the first automatic location recenter loads the scope layer for the user's final viewport, even if the user does not manually move the map.

## Scope
- `MainActivity`
- A small tracker for location recenter reload ownership
- Unit tests for the tracker

## Steps
- Add a focused tracker that records whether a location move should trigger a reload after location update.
- In `onMapReady()`, mark the initial location move as reload-owned before requesting location.
- In the manual my-location path, reuse the same tracker.
- In permission denial, cancel pending location reload ownership.
- When location is updated, set `pendingLocationRecenterReload` once so the next camera idle uses the existing `requestForceScopeReload("location recenter")` path.
- Validate with `testDebugUnitTest`.
