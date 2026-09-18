# Root Cause Investigation

## Status

- Root cause identified; minimum fix scope is ready for implementation planning.

## Static Evidence

- `MyLocationController.enableMyLocationAndMove()` requests only `Priority.PRIORITY_HIGH_ACCURACY` through `getCurrentLocation`, so first centering waits for a new fix even when Play Services has a recent position.
- The point picker takes the opposite approach: it uses only `lastLocation`, so it cannot refine an older position.
- The form import fallback filters out locations worse than 30 m and waits up to 25 seconds, which favors accuracy over responsiveness.

## Physical Device Evidence

- Device: Sony XQ-AU52, Android 12, serial `adb-QV710EDR3A-hF5XZF._adb-tls-connect._tcp`.
- Installed package: `com.example.taoyuangutter`, version `1.0.0` (version code 1). APK source revision is not available from the installed package and is therefore not asserted.
- Before the interaction, the Fused provider already held a usable cached location with 16.627 m horizontal accuracy.
- At `2026-09-17 23:50:33.507 +0800`, the main-map current-location control was tapped. `dumpsys location` recorded the app's high-accuracy request at `23:50:33.652`, 145 ms later.
- At `23:50:39.855`, the Fused Location service logged a new delivery event; the subsequent location snapshot showed a fused/network location with 21.038 m horizontal accuracy. This is approximately 6.35 seconds after the tap.
- Exact camera-animation completion time is NOT VERIFIED because the current app has no location/camera timing instrumentation. The delay to a new Fused result is directly observed.

## Root Cause

- The main-map flow ignores an already available cached Fused position and waits for `getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY)` before invoking its camera callback. The measured 6.35-second wait despite a cached 16.627 m location demonstrates the responsiveness gap.
- Other map flows use incompatible strategies (cached-only or strict-accuracy wait), so the user cannot receive a consistent immediate-center then correction experience.

## Classification

- Category: implementation_regression / inconsistent location policy.
- Priority: P2. A usable positioning workflow exists, but its response time is degraded.
