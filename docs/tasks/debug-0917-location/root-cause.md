# Root Cause Investigation

## Status

- Evidence sufficient to begin a focused implementation plan is not yet complete.

## Static Evidence

- `MyLocationController.enableMyLocationAndMove()` requests only `Priority.PRIORITY_HIGH_ACCURACY` through `getCurrentLocation`, so first centering waits for a new fix even when Play Services has a recent position.
- The point picker takes the opposite approach: it uses only `lastLocation`, so it cannot refine an older position.
- The form import fallback filters out locations worse than 30 m and waits up to 25 seconds, which favors accuracy over responsiveness.

## Required Runtime Evidence

- Capture recent-location timestamp/accuracy and time to first center on an Android 9+ device.
- Capture time and accuracy of the follow-up high-accuracy result.
- Confirm callback cancellation when the relevant screen closes.

## Provisional Classification

- Category: implementation_regression / inconsistent location policy.
- Priority: P2 until device evidence shows a core workflow block.
