# Repository Analysis

## Verification failure re-analysis — 2026-10-16

### What actually failed

The independent verification did not observe a product failure. It returned `NOT VERIFIED` because two acceptance checks require incident-specific HTC evidence that is not present:

- AC-DBG-001 requires the HTC model, Android 14 build, installed app version, exact capture step, and `FATAL EXCEPTION` trace.
- AC-DBG-002 requires classification of the actual incident boundary. Emulator success and injected exceptions demonstrate possible mechanisms, but cannot identify which mechanism occurred on HTC.

AC-DBG-003 and AC-DBG-004 were dispositioned PASS: the minimum fix scope was documented before implementation, and the unavailable HTC retest was explicitly marked `NOT VERIFIED`.

### Evidence separation

| Evidence layer | Result | What it proves | What it cannot prove |
|---|---|---|---|
| Static source/diff review | PASS | The two P1 guards are present in the committed revision and stay within the approved scope | That HTC produced either exception |
| API 34 fault injection | PASS | The provider-future and `MediaStore.insert()` Java failure boundaries recover without app termination | HTC Camera2/HAL or MediaProvider behavior |
| API 34 normal capture smoke | PASS | Preview, shutter, JPEG normalization, MediaStore publication, and return to form work on AOSP emulator | HTC-specific reproduction |
| HTC incident artifact | Missing | Nothing about the actual reported crash | Incident boundary, confidence upgrade, or exact fix attribution |
| CI workflow result | Missing | Local build is not CI | CI gate status for `b92c122` |

The verification non-pass is therefore an evidence-availability problem, not evidence that the implementation contradicts the requirement. The local smoke flow also argues against classifying this as an `implementation_regression`.

The user confirmed during verification round 2 that the requested HTC model/build, app version, exact failure step, and crash trace are not currently available.

### CI-specific finding

The configured local remote is GitLab. A read-only query found no remote ref named codex/debug/debug-1017-htc-android14-camera, so the current task branch is local-only and no remote pipeline result for its current revision is available. The repository has a GitHub Actions workflow, but GitHub API lookups for the inferred project path returned 404; this checkout has no .gitlab-ci.yml. Local build and instrumentation results are not CI evidence. CI remains NOT VERIFIED until a matching remote pipeline result is available.

### Updated conclusion

The current evidence supports the following bounded conclusion: the implementation fixes are locally valid on API 34, but the reported HTC crash remains unclassified. Confidence is high in the recoverability of the two tested mechanisms, while confidence in exact HTC attribution remains conditional on the missing device phase and stack. The next useful evidence is the HTC incident artifact, not another equivalent AOSP pass.

## Current behavior

- The current photo flow enters `GutterPhotosFragment`, creates a temporary JPEG under the app-specific external Pictures directory, and opens `CameraOverlayFragment` inside `GutterFormActivity`; the current branch does not call `LandscapeCameraActivity` from this flow.
- `CameraOverlayFragment` uses CameraX 1.4.1 (`Preview` + `ImageCapture`), Camera2 interop for lens metadata, rear-camera candidate filtering, flash state, zoom, orientation rotation, and asynchronous post-capture JPEG normalization followed by a MediaStore album copy.
- CameraX provider initialization is asynchronous. Before `27e24a0`, the callback called `cameraProviderFuture.get()` outside the `try/catch` that surrounded use-case binding, so a provider initialization failure could escape as an uncaught callback exception. The fix now guards the future and the complete binding boundary.
- A temporary API 34 instrumentation test configured the real CameraX 1.4.1 provider with a failing `CameraFactory.Provider` and `RetryPolicy.NEVER`. The provider future failed with `InitializationException` caused by `CameraUnavailableException(CAMERA_ERROR)`; the same callback shape as `startCamera()` observed `ExecutionException`. The test passed with `OK (1 test)` and was removed after evidence collection.
- After a successful `ImageCapture` callback, the current branch performs full-resolution JPEG normalization and then copies the file to the system album from a `lifecycleScope.launch` coroutine. The Android 10+ `MediaStore` path calls `ContentResolver.insert()` before its `try/catch`; an OEM MediaProvider exception at that line propagates out of the coroutine.
- Controlled API 34 fault injection confirmed the boundary: a temporary `ContentResolver.wrap()` provider that threw from `insert()` caused the pre-fix `PhotoAlbumStore.copyToSystemAlbum()` to throw, while a real AOSP API 34 JPEG copy passed. The permanent regression test now verifies the fixed behavior returns `null` without throwing.
- A real API 34 CameraX `ImageCapture.takePicture()` test then produced a JPEG and completed the AOSP MediaStore copy. This separates the normal camera capture path from the injected OEM-provider failure path; the AOSP run emitted only an asynchronous quota warning and no app crash.

## Expected behavior

- A device/OEM camera-service failure should result in a logged, user-visible camera error and a safe cancellation, not process termination.
- A successful capture should return to the form after post-processing and optional album copy, even when the album copy is unavailable.

## Evidence reviewed

- `app/src/main/java/com/example/taoyuangutter/gutter/GutterPhotosFragment.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/CameraOverlayFragment.kt`
- `app/src/main/java/com/example/taoyuangutter/common/PhotoAlbumStore.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/xml/file_paths.xml`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- Camera-related history including `2ce44cf`, `6f9bd13`, `b0b604e`, and `45aadf1`.
- Regression history: `fd81f7d6` (`feat: 拍照相簿 a copy 功能`, 2026-06-25) introduced `PhotoAlbumStore` and the post-capture album-copy call. It is an ancestor of the investigated revision.
- History also shows `6f9bd132` introduced asynchronous post-capture normalization and `b0b604e1` later added full-resolution JPEG logging/processing; `45aadf1f` fixed only the recycled-bitmap logging defect on a separate hotfix branch. No later all-branch `PhotoAlbumStore` guard was found.

## Software-conflict assessment

- There is no evidence that the app launches or conflicts directly with the HTC stock camera app. The active path is an in-process CameraX overlay.
- There are two plausible compatibility boundaries, with different timing signatures:
  - before preview/shutter: CameraX 1.4.1, Android Camera2 service, and the HTC OEM camera HAL; the provider future is not guarded.
  - after shutter success: the app's new MediaStore album-copy path and HTC's MediaProvider/Gallery implementation; before `27e24a0`, `ContentResolver.insert()` was not guarded.
- There is no evidence that the app launches or conflicts directly with the HTC stock camera app. “Software conflict” currently means an in-process CameraX/Camera2 or MediaStore/OEM-provider compatibility boundary, not an app-to-app camera intent conflict.
- Android 14 storage/privacy behavior is not the leading cause in the observed source: capture writes to an app-specific external-files directory, and Android 10+ album copy uses MediaStore with `IS_PENDING`; `WRITE_EXTERNAL_STORAGE` is only requested below Android 10.
- The app's `Preview` + `ImageCapture` pair is ordinary, but its extra multi-camera metadata probing and filtered lens selector increase OEM-specific surface area. A HTC log is required to distinguish this from a generic camera-service contention or HAL error.

The strongest current conclusion is conditional: if the user-visible crash occurs after the shutter has produced the photo file, the pre-fix MediaStore insert was a code-level crash mechanism with at least 95% confidence; if the app disappears before preview, the pre-fix CameraX provider future was also a code-level crash mechanism with at least 95% confidence under provider initialization failure. Both boundaries are now guarded and have permanent API 34 fault-injection coverage. The successful AOSP CameraX capture makes a universal Android 14 CameraX capture defect unlikely. The exact HTC attribution remains below 95% until a device log or reliable phase report is available.

If the handset is HTC U24 pro, its official Android 14 / 50 MP multi-camera specification makes the current no-target-resolution plus full-resolution normalization path a secondary memory-risk hypothesis. That hypothesis needs an OOM/LMK trace or a very large `CameraOverlay` JPEG dimension log before it can outrank the unguarded MediaStore exception.

## Secondary risks

- The post-capture path decodes the complete JPEG and may create another full-size bitmap for EXIF rotation or brightness. HTC output resolution and memory pressure are unknown.
- The current revision's `normalizeCapturedPhotoOrientation()` logs `output.width` and `output.height` after `output.recycle()`. This is a real correctness defect, but it is inside `runCatching`; it is not sufficient evidence by itself for a process crash.
- Before `27e24a0`, `PhotoAlbumStore.copyToMediaStore()` caught failures after `insert()` but not the `insert()` call itself. The fixed code catches the full transaction and cleans up a partial item.
- Before `27e24a0`, CameraX initialization and binding were performed while the overlay view lifecycle was active, but delayed callbacks had no complete destroyed-view guard. The fixed callback checks context/view state before returning UI or form results.

## Risks

- Changing CameraX versions or removing OEM-specific features without a trace could hide the actual failure or regress lens/flash behavior.
- Replacing the custom camera with an external camera intent would change product behavior and is out of scope for root-cause analysis.
- A full device regression is required after any camera lifecycle or post-processing fix.

## Unknowns

- HTC model, firmware/security patch, app build/version, and exact reproduction step.
- Whether the crash occurs at provider initialization, use-case binding, shutter capture, bitmap post-processing, MediaStore copy, or result delivery.
- Whether another app, privacy switch, DND policy, or HTC camera service is holding the camera resource.
- Whether the reported crash started after the album-copy feature in `fd81f7d6` and occurs after the image file has already been written.
