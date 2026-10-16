# Root Cause Investigation

## Status

- The exact HTC incident is still not confirmed because the HTC model, firmware/security patch, failing phase, and crash stack are unavailable.
- Two code-level uncaught boundaries were identified and fixed in `27e24a0`:
  - CameraX provider initialization: `cameraProviderFuture.get()` could escape the main-executor callback.
  - Post-capture album copy: Android 10+ `ContentResolver.insert()` was outside the local `try/catch`.
- Both boundaries were fault-injected on the API 34 emulator after the fix. The tests passed and the app did not terminate.
- Classification remains `unknown` for the HTC attribution; the implementation mechanisms are classified and addressed.

## Strongest candidates and evidence

### A. Post-capture MediaStore/OEM provider exception

The active flow is `GutterPhotosFragment` → `GutterFormActivity` → `CameraOverlayFragment`. After `ImageCapture.onImageSaved()`, the app normalizes the JPEG and calls `PhotoAlbumStore.copyToSystemAlbum()`. The Android 10+ path writes to `VOLUME_EXTERNAL_PRIMARY` with `RELATIVE_PATH` and `IS_PENDING`.

Before `27e24a0`, `ContentResolver.insert()` executed before `PhotoAlbumStore`'s `try/catch`, so an OEM `MediaProvider` exception could escape the lifecycle coroutine and terminate the app. The fix moves `insert()` into the guarded transaction, logs the exception, best-effort deletes a partially created URI, and returns `null`. The capture callback catches post-processing failures and preserves the form photo/result.

Permanent test evidence on `emulator-5556` / API 34:

- `PhotoAlbumStoreInstrumentedTest.insertFailureReturnsNullWithoutThrowing` wraps a provider whose `insert()` throws `IllegalStateException`.
- Result: `OK (1 test)`. The exception is converted to a recoverable `null` result; no test process crash occurred.
- A prior normal AOSP API 34 control copied a real JPEG through MediaStore and published it successfully. This lowers the probability of a universal Android 14 storage failure, but does not model HTC MediaProvider behavior.

### B. CameraX provider initialization exception

Before `27e24a0`, `CameraOverlayFragment.startCamera()` registered a listener and called `cameraProviderFuture.get()` without handling a failed future. CameraX 1.4.1 can surface `ExecutionException` whose cause chain includes `InitializationException` and `CameraUnavailableException`.

The fix catches both synchronous provider-request failures and asynchronous future failures, logs the full throwable, and uses the existing canceled camera result/toast path. Use-case construction and `bindToLifecycle()` are also inside the recoverable initialization boundary; the existing `Preview` + `ImageCapture` behavior is unchanged.

Permanent test evidence on `emulator-5556` / API 34:

- `CameraProviderInitializationUiTest.providerInitializationFailureDoesNotTerminateFormActivity` configures the real CameraX provider with a failing `CameraFactory.Provider` and `RetryPolicy.NEVER`.
- Result: `OK (1 test)`. The form Activity remained `RESUMED`, and post-test logcat contained no `FATAL EXCEPTION`.

## Other possible errors and confidence

| Candidate | When it fits | Confidence it is the reported HTC cause | Confidence in the stated mechanism |
|---|---|---:|---:|
| MediaStore/OEM provider exception at `insert()` | App disappears immediately after shutter/photo file creation | ~70–80% if the crash is post-shutter; lower without phase evidence | ≥95% when `insert()` throws; reproduced by fault injection |
| CameraX provider initialization / Camera2-HAL failure | App disappears before preview or shutter | ~65% if the crash is before preview; HTC attribution remains conditional | ≥95% when provider initialization fails; reproduced with real CameraX fault injection |
| CameraX use-case binding/resolution incompatibility | Preview never becomes usable, often with `IllegalArgumentException` or `CameraUnavailableException` | ~35% pending HTC stack | ≥90% that the new outer binding guard prevents a Java exception from escaping |
| Post-capture lifecycle race | Overlay is dismissed or Activity is recreated while capture callback completes | ~30% pending timing evidence | ≥90% that the new view-owner/binding guards cover the identified Java race |
| High-resolution bitmap memory pressure / OOM | Large HTC camera output, especially a high-megapixel model | ~25% conditional on HTC model and an OOM/LMK trace | ~90% that this remains a real secondary risk; not changed in this P1 scope |
| `ImageCaptureException` from camera service contention | Shutter reports failure but app stays open | ~20% as a process-crash cause | ≥90% that the existing error callback handles ordinary Java callback failure |
| Permission or Android 14 scoped-storage denial | Permission prompt/denial or album copy failure | ~15% as a universal cause | ≥90% that normal AOSP API 34 path is allowed; OEM provider exception remains possible |
| Direct conflict with HTC stock Camera app | Only if an external camera intent or resource contention is involved | ~10% | ~90% that this app uses an in-process CameraX overlay, not a stock-camera intent |

These percentages are conditional engineering confidence, not measured incident probabilities. The missing HTC model, build, reproduction phase, and stack trace prevent a definitive attribution.

## Why this is not yet a proven HTC-specific root cause

- The available AVD is `DBG1017_API34_Camera`, Android 14 API 34, model `sdk_gphone64_arm64`, not HTC hardware.
- Its normal CameraX capture and AOSP MediaStore copy passed, and injected failures now recover without process termination.
- No HTC `AndroidRuntime`, CameraX/Camera2, `cameraserver`, MediaProvider, tombstone, or Play crash trace is available.
- Public testing services available in this investigation do not provide an HTC/U24 Android 14 Camera2/HAL equivalent.

## Residual risks outside this P1 fix

- The normalizer may decode a full-resolution JPEG and allocate another bitmap; an HTC high-resolution output could still trigger native memory pressure.
- Lens metadata probing and filtered multi-camera selection add OEM-specific surface area.
- The existing 4:3 aspect-ratio configuration remains unchanged; no HTC-specific target-resolution change is justified without a device trace.

## Required evidence to close HTC attribution

- HTC model, Android build/security patch, app version/commit.
- Exact phase: camera open, preview, shutter, post-capture return, or album copy.
- Bounded `adb logcat -v threadtime` from just before the tap through process death, including `AndroidRuntime`, `CameraX`, `Camera2`, `cameraserver`, `MediaProvider`, `CameraOverlay`, and `ActivityTaskManager`.
- If no device can be connected, a tombstone or Play crash report containing the exception class and top app frame.
