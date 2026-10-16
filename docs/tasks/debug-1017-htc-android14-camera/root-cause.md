# Root Cause Investigation

## Status

- Root cause is not confirmed for the specific HTC device because the model, firmware, failing phase, and crash stack are still unavailable. No production-code change is authorized in this investigation turn.
- The strongest code-level failure mechanism is now proven: an exception from the post-capture MediaStore `insert()` can escape the current photo coroutine. Confidence in that mechanism is `>=95%`; confidence that it is the reported HTC crash remains below 95% until the failing phase or HTC stack is obtained.
- Provisional category: `unknown` with two implementation/OEM compatibility hypotheses. The post-capture MediaStore boundary is the leading hypothesis if the crash happens after the shutter callback; CameraX provider initialization remains the leading hypothesis if the crash happens while opening preview.
- Priority: P1 because the affected device's required photo workflow may be blocked.

## Strongest candidates

### A. Post-capture MediaStore/OEM provider exception — leading for a shutter-time crash

The current branch contains `fd81f7d6` (`feat: 拍照相簿 a copy 功能`), which added a system-album copy immediately after `ImageCapture.onImageSaved()`. `CameraOverlayFragment.capturePhoto()` launches a lifecycle coroutine and calls `PhotoAlbumStore.copyToSystemAlbum()`. On Android 10+, `copyToMediaStore()` calls `ContentResolver.insert()` at `PhotoAlbumStore.kt:56` before entering its `try/catch`. An HTC Android 14 MediaProvider implementation that throws from `insert()` can therefore escape the coroutine and terminate the app. This maps closely to a report phrased as “拍照後閃退” and is a concrete regression boundary introduced by the album-copy feature.

This is not yet proven: Android 14 scoped storage normally permits this MediaStore insertion, and no HTC stack trace shows the exception class.

#### Controlled fault-injection proof

- A temporary API 34 instrumentation test replaced the app context's resolver with `ContentResolver.wrap()` around a provider whose `insert()` throws `IllegalStateException`.
- The test passed with the expected assertion: `PhotoAlbumStore.copyToSystemAlbum()` propagated the injected exception instead of returning `null`.
- A second test copied a real 2x2 JPEG through the stock AOSP API 34 MediaProvider and passed; logcat showed the pending file being opened and published in `Pictures/TaoYuanGutterDebug`.
- A separate temporary test ran a real CameraX `ImageCapture.takePicture()` on emulator camera id 10, produced a readable JPEG, then copied it through the same `PhotoAlbumStore`; it returned `OK (1 test)` in 18.44s. AOSP logcat showed the file publish and an asynchronous quota warning, but no app `FATAL EXCEPTION`.
- The temporary test source and dependency were deleted afterward; no temporary source or build-configuration diff remains. This proves the app's exception boundary, not that HTC actually throws the same exception.

### B. CameraX provider initialization exception — leading for an open/preview crash

The most likely failure boundary is CameraX provider initialization on the HTC Android 14 camera stack. `CameraOverlayFragment.startCamera()` registers a listener and calls `cameraProviderFuture.get()` at `CameraOverlayFragment.kt:455-460` without a catch. CameraX documents that `ProcessCameraProvider.getInstance()` may fail with `InitializationException`, whose cause may be `CameraUnavailableException`; `CameraUnavailableException` includes camera-in-use, camera-service/device error, and max-camera-resource cases.

If the HTC Camera2 service/HAL reports one of those failures, the exception is thrown from the main-executor callback and can terminate the app before the existing binding `try/catch` at `CameraOverlayFragment.kt:490-554` is reached. This explains a device-specific crash and is compatible with a software interaction between the app's CameraX layer and the HTC OEM camera implementation, but it is not yet proof of a HTC-specific defect.

## Supporting source evidence

- Active entry point: `GutterPhotosFragment.launchCameraOverlay()` → `GutterFormActivity.showCameraOverlay()` → `CameraOverlayFragment.startCamera()`.
- `cameraProviderFuture.get()` is outside the `try/catch` in `startCamera()`.
- Existing catch handles only failures after a provider has been returned, including `bindToLifecycle()`; it cannot handle a failed provider future.
- The app uses CameraX 1.4.1, target SDK 36, and Camera2 interop/lens filtering.
- The app's CameraX callback is only partially guarded: the binding `try/catch` begins after the provider future has already been obtained and after lens-candidate construction.
- The album-copy feature was added on 2026-06-25 and is present in the investigated revision; this is a stronger temporal/code-path lead for a crash that occurs after a photo file is saved.
- The post-capture asynchronous normalization path was introduced by `6f9bd132` on 2026-04-23, and the system-album copy was added by `fd81f7d6` on 2026-06-25. The latter added the unguarded `insert()` boundary; no later all-branch fix for `PhotoAlbumStore` was found.
- The camera history includes `uiFix/模擬器拍照閃退` and later photo-normalization diagnostics, showing that camera stability has already required device-oriented fixes.

## Alternative hypotheses

1. **Capture/HAL error:** `ImageCapture.takePicture()` reports an `ImageCaptureException`; the current callback displays a toast, so a pure callback error should not crash unless a framework/vendor native failure also occurs. Requires logcat.
2. **Post-processing memory pressure:** the full-size JPEG is decoded and potentially copied/rotated. An HTC high-resolution output could trigger memory pressure. The function catches Kotlin `Throwable`, so a normal Java/Kotlin decode failure should be logged and continue; native process death or an uncaught lifecycle exception remains possible.
3. **Post-processing bitmap lifecycle defect:** the current revision reads dimensions after recycling the bitmap. This is a concrete bug, but the access is within `runCatching`, so it currently looks more likely to cause a warning and skipped normalization than the reported process crash.
4. **Post-capture view lifecycle race:** `onImageSaved()` accesses `viewLifecycleOwner` and later calls `requireContext()` without a guard. If the overlay is dismissed while a slow vendor capture or file operation is completing, either access can throw outside a local handler.
5. **Camera resource contention:** the stock camera, another app, or a stale CameraX session may hold the resource. This would map to `CameraUnavailableException.CAMERA_IN_USE` or `CAMERA_ERROR`; no device trace is available.
6. **Android 14 permission/storage conflict:** source review does not support a normal permission denial as the leading generic cause, because the capture file is app-specific and MediaStore insertion is the Android 10+ path. However, an OEM MediaProvider exception from the unguarded `insert()` remains a concrete post-capture candidate.

### Conditional high-resolution risk

- If the affected phone is HTC U24 pro, HTC's official specification lists Android 14, a 50 MP main camera, a 50 MP telephoto camera, and an 8 MP wide/depth camera. CameraX documentation states that an `ImageCapture` with only an aspect-ratio preference can select the highest available resolution; this code sets 4:3 but no target resolution.
- The post-capture normalizer decodes the whole JPEG and may allocate a second full-size bitmap for rotation or brightness correction. For a 50 MP JPEG, one ARGB_8888 bitmap is roughly 200 MB before decoder/native overhead, and rotation can temporarily require another allocation. This is a credible OEM-specific memory-pressure risk if the HTC output is high resolution.
- It is not currently the leading proven crash mechanism: the normalizer is inside `runCatching`, the API 34 AVD actual capture passed, and an HTC OOM/LMK trace is unavailable. Promote this hypothesis only if the device evidence contains `OutOfMemoryError`, low-memory kill, or a `CameraOverlay` log showing very large dimensions immediately before death.

## Non-HTC Android 14 evidence

- A dedicated API 34 AVD (`DBG1017_API34_Camera`, SDK 34 / Android 14) was created and the debug APK was installed.
- A direct in-process camera-overlay instrumentation smoke run completed once with `tests=1, failures=0`; logcat showed CameraX binding `Preview` and `ImageCapture` to emulator camera id 10, including an `ImageCapture` pipeline at 1280x960 and both use cases attached. No `FATAL EXCEPTION` or app process death was observed.
- A separate no-camera/permission-denied exercise produced CameraX `CameraIdListIncorrectException` / `No available camera can be found` warnings while `GutterFormActivity` remained the foreground activity; it did not produce an app crash.
- These results lower the probability of a universal Android 14 crash and support an OEM-specific boundary, but they do not reproduce HTC HAL or HTC MediaProvider behavior. A later attempt to force the capture callback was `NOT VERIFIED` because the instrumentation harness became `STARTED`/lost overlay focus before its own assertion; that test failure is not product crash evidence.

## Confidence update

- `>=95%`: if an OEM `MediaProvider.insert()` exception occurs during the current post-capture path, the current implementation can let it escape because the call is outside the local `try/catch` and the caller coroutine has no local exception handler. This was demonstrated by controlled API 34 fault injection.
- `~90%`: there is no direct app-to-app conflict with the HTC stock camera; the active flow is an in-process CameraX overlay rather than a stock-camera intent.
- `~80%`: an OEM-specific CameraX/Camera2/HAL or MediaProvider compatibility boundary is involved, based on the clean AOSP API 34 result and the device-specific report. This is not `>=95%` because the HTC phase and stack are missing.
- The actual CameraX capture path now has scoped AOSP evidence, so a universal `ImageCapture`/Android 14 failure is less likely; the remaining uncertainty is specifically OEM behavior and the unknown user-visible phase.
- Public sources did not yield a model-specific HTC CameraX/MediaProvider crash report. The U24 pro high-resolution branch is therefore conditional on the model, not an attribution.
- Firebase Test Lab's current official device catalog has no HTC or U24 entry, so a cloud Android 14 device cannot provide equivalent HTC Camera2/HAL or MediaProvider evidence.
- `~70–80%`: the exact reported HTC crash is the post-capture MediaStore boundary if the user sees the crash immediately after the shutter/photo preview; it drops to `~65%` if the app disappears before preview is visible.

## Required proof to close debug

- HTC model and firmware/security patch.
- App version/commit installed on the device.
- Reproduction phase: camera open, shutter press, post-capture return, or album copy.
- `adb logcat -v threadtime` excerpt from just before the tap through process death, including `AndroidRuntime`, `CameraX`, `Camera2`, `cameraserver`, `CameraOverlay`, and `ActivityTaskManager`.
- If no device can be connected, an Android tombstone/Play crash report with the exception class and top app frame is the minimum substitute.

## Classification and route

- Current classification: `unknown` pending device evidence. The failure mechanism for the unguarded MediaStore `insert()` is proven under injection, but the exact HTC attribution remains unconfirmed.
- If the trace is `InitializationException` / `CameraUnavailableException` at `cameraProviderFuture.get()`, route to `implementation` → debug fix: catch and surface provider initialization failures, log the nested cause, and guard the view lifecycle.
- If the trace is a vendor `CameraAccessException`/HAL error during binding or capture, route to `implementation` with an HTC-specific compatibility test and minimal CameraX configuration change; do not blame the stock camera app without resource-contended evidence.
- If the trace is only environmental camera contention or missing permission, route to `infrastructure`/user-device setup rather than changing product code.
