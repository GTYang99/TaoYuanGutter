# Repository Analysis

## Current Behavior

- Main map location is handled by `MyLocationController`, which calls only high-accuracy `getCurrentLocation`; it does not first use `lastLocation`.
- `MapPointPickerActivity` uses `lastLocation` and does not request a follow-up accurate fix.
- `GutterFormActivity` import reuses a host location when available; otherwise it waits for an accuracy threshold of 30 m for up to 25 seconds.

## Expected Behavior

- Each entry point should provide fast feedback from a recent location and then optionally correct from a better current result under one consistent lifecycle-safe policy.

## Affected Modules

- `map/MyLocationController.kt`
- `map/MapWorkspaceFragment.kt`
- `gutter/MapPointPickerActivity.kt`
- `gutter/GutterFormActivity.kt`
- legacy `MainActivity.kt`, if it remains a supported direct entry point

## Dependencies

- Google Play Services Fused Location Provider, runtime location permission, Google Maps camera APIs, and existing form import callbacks.

## Risks

- Recent locations can be stale or low accuracy; correction criteria must prevent disruptive camera jumps.
- A lifecycle-unaware callback can update a closed map or form.
- Continuous requests would increase battery use and exceed the approved scope.

## Unknowns

- Runtime measurements across devices are still required to establish an acceptable first-center time and correction threshold.
