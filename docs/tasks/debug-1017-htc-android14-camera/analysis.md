# Repository Analysis

## Current behavior

- The current photo flow enters `GutterPhotosFragment`, creates a temporary JPEG under the app-specific external Pictures directory, and opens `CameraOverlayFragment` inside `GutterFormActivity`; the current branch does not call `LandscapeCameraActivity` from this flow.
- `CameraOverlayFragment` uses CameraX 1.4.1 (`Preview` + `ImageCapture`), Camera2 interop for lens metadata, rear-camera candidate filtering, flash state, zoom, orientation rotation, and asynchronous post-capture JPEG normalization followed by a MediaStore album copy.
- CameraX provider initialization is asynchronous. The callback calls `cameraProviderFuture.get()` outside the `try/catch` that surrounds use-case binding. A provider initialization failure can therefore escape as an uncaught callback exception.
- After a successful `ImageCapture` callback, the current branch performs full-resolution JPEG normalization and then copies the file to the system album from a `lifecycleScope.launch` coroutine. The Android 10+ `MediaStore` path calls `ContentResolver.insert()` before its `try/catch`; an OEM MediaProvider exception at that line propagates out of the coroutine.
- Controlled API 34 fault injection confirmed the boundary: a temporary `ContentResolver.wrap()` provider that throws from `insert()` caused `PhotoAlbumStore.copyToSystemAlbum()` to throw, while a real AOSP API 34 JPEG copy passed. The temporary test was removed after execution, so this is evidence rather than a production change.
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
  - after shutter success: the app's new MediaStore album-copy path and HTC's MediaProvider/Gallery implementation; `ContentResolver.insert()` is not guarded.
- There is no evidence that the app launches or conflicts directly with the HTC stock camera app. “Software conflict” currently means an in-process CameraX/Camera2 or MediaStore/OEM-provider compatibility boundary, not an app-to-app camera intent conflict.
- Android 14 storage/privacy behavior is not the leading cause in the observed source: capture writes to an app-specific external-files directory, and Android 10+ album copy uses MediaStore with `IS_PENDING`; `WRITE_EXTERNAL_STORAGE` is only requested below Android 10.
- The app's `Preview` + `ImageCapture` pair is ordinary, but its extra multi-camera metadata probing and filtered lens selector increase OEM-specific surface area. A HTC log is required to distinguish this from a generic camera-service contention or HAL error.

The strongest current conclusion is conditional: if the user-visible crash occurs after the shutter has produced the photo file, the unguarded MediaStore insert is a code-level root cause with at least 95% confidence as a crash mechanism. The successful AOSP CameraX capture makes a universal Android 14 CameraX capture defect unlikely. The exact HTC attribution remains below 95% until a device log or a reliable phase report is available.

If the handset is HTC U24 pro, its official Android 14 / 50 MP multi-camera specification makes the current no-target-resolution plus full-resolution normalization path a secondary memory-risk hypothesis. That hypothesis needs an OOM/LMK trace or a very large `CameraOverlay` JPEG dimension log before it can outrank the unguarded MediaStore exception.

## Secondary risks

- The post-capture path decodes the complete JPEG and may create another full-size bitmap for EXIF rotation or brightness. HTC output resolution and memory pressure are unknown.
- The current revision's `normalizeCapturedPhotoOrientation()` logs `output.width` and `output.height` after `output.recycle()`. This is a real correctness defect, but it is inside `runCatching`; it is not sufficient evidence by itself for a process crash.
- `PhotoAlbumStore.copyToMediaStore()` catches failures after `insert()` but not the `insert()` call itself. Scoped storage makes a normal API 34 permission denial less likely, but OEM provider behavior can still throw at this boundary; the exact exception must be captured.
- CameraX initialization and binding are performed while the overlay view lifecycle is active, but the future callback has no destroyed-view guard. A delayed provider completion after overlay dismissal is another unverified lifecycle risk.

## Risks

- Changing CameraX versions or removing OEM-specific features without a trace could hide the actual failure or regress lens/flash behavior.
- Replacing the custom camera with an external camera intent would change product behavior and is out of scope for root-cause analysis.
- A full device regression is required after any camera lifecycle or post-processing fix.

## Unknowns

- HTC model, firmware/security patch, app build/version, and exact reproduction step.
- Whether the crash occurs at provider initialization, use-case binding, shutter capture, bitmap post-processing, MediaStore copy, or result delivery.
- Whether another app, privacy switch, DND policy, or HTC camera service is holding the camera resource.
- Whether the reported crash started after the album-copy feature in `fd81f7d6` and occurs after the image file has already been written.
