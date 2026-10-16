# Root Cause Investigation

## Status

- Root cause is not confirmed. No production-code change is authorized in this investigation turn.
- Provisional category: `unknown` with a strong `implementation` / `OEM Camera2 compatibility` hypothesis.
- Priority: P1 because the affected device's required photo workflow may be blocked.

## Strongest candidate

The most likely failure boundary is CameraX provider initialization on the HTC Android 14 camera stack. `CameraOverlayFragment.startCamera()` registers a listener and calls `cameraProviderFuture.get()` at `CameraOverlayFragment.kt:455-460` without a catch. CameraX documents that `ProcessCameraProvider.getInstance()` may fail with `InitializationException`, whose cause may be `CameraUnavailableException`; `CameraUnavailableException` includes camera-in-use, camera-service/device error, and max-camera-resource cases.

If the HTC Camera2 service/HAL reports one of those failures, the exception is thrown from the main-executor callback and can terminate the app before the existing binding `try/catch` at `CameraOverlayFragment.kt:490-554` is reached. This explains a device-specific crash and is compatible with a software interaction between the app's CameraX layer and the HTC OEM camera implementation, but it is not yet proof of a HTC-specific defect.

## Supporting source evidence

- Active entry point: `GutterPhotosFragment.launchCameraOverlay()` → `GutterFormActivity.showCameraOverlay()` → `CameraOverlayFragment.startCamera()`.
- `cameraProviderFuture.get()` is outside the `try/catch` in `startCamera()`.
- Existing catch handles only failures after a provider has been returned, including `bindToLifecycle()`; it cannot handle a failed provider future.
- The app uses CameraX 1.4.1, target SDK 36, and Camera2 interop/lens filtering.
- The camera history includes `uiFix/模擬器拍照閃退` and later photo-normalization diagnostics, showing that camera stability has already required device-oriented fixes.

## Alternative hypotheses

1. **Capture/HAL error:** `ImageCapture.takePicture()` reports an `ImageCaptureException`; the current callback displays a toast, so a pure callback error should not crash unless a framework/vendor native failure also occurs. Requires logcat.
2. **Post-processing memory pressure:** the full-size JPEG is decoded and potentially copied/rotated. An HTC high-resolution output could trigger memory pressure. The function catches Kotlin `Throwable`, so a normal Java/Kotlin decode failure should be logged and continue; native process death or an uncaught lifecycle exception remains possible.
3. **Post-processing bitmap lifecycle defect:** the current revision reads dimensions after recycling the bitmap. This is a concrete bug, but the access is within `runCatching`, so it currently looks more likely to cause a warning and skipped normalization than the reported process crash.
4. **Camera resource contention:** the stock camera, another app, or a stale CameraX session may hold the resource. This would map to `CameraUnavailableException.CAMERA_IN_USE` or `CAMERA_ERROR`; no device trace is available.
5. **Android 14 permission/storage conflict:** source review does not support this as the leading hypothesis. The capture file is app-specific and MediaStore insertion is the Android 10+ path; exact permission state still needs to be recorded.

## Required proof to close debug

- HTC model and firmware/security patch.
- App version/commit installed on the device.
- Reproduction phase: camera open, shutter press, post-capture return, or album copy.
- `adb logcat -v threadtime` excerpt from just before the tap through process death, including `AndroidRuntime`, `CameraX`, `Camera2`, `cameraserver`, `CameraOverlay`, and `ActivityTaskManager`.
- If no device can be connected, an Android tombstone/Play crash report with the exception class and top app frame is the minimum substitute.

## Classification and route

- Current classification: `unknown` pending device evidence.
- If the trace is `InitializationException` / `CameraUnavailableException` at `cameraProviderFuture.get()`, route to `implementation` → debug fix: catch and surface provider initialization failures, log the nested cause, and guard the view lifecycle.
- If the trace is a vendor `CameraAccessException`/HAL error during binding or capture, route to `implementation` with an HTC-specific compatibility test and minimal CameraX configuration change; do not blame the stock camera app without resource-contended evidence.
- If the trace is only environmental camera contention or missing permission, route to `infrastructure`/user-device setup rather than changing product code.
