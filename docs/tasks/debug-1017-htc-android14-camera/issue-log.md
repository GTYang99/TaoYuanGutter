# Issue Log

## ISS-DBG-1017-001

- task_id: `debug-1017-htc-android14-camera`
- phase: `debug`
- category: `unknown` (provisional; likely implementation/OEM Camera2 or MediaProvider boundary)
- priority: `P1`
- title: HTC Android 14 photo capture may terminate the app
- status: `open`
- impact: The required photo capture flow may be blocked on the affected device.
- repro_steps:
  - Open the gutter form on an HTC Android 14 phone.
  - Enter the in-app camera from a photo slot.
  - Capture a photo; exact failing tap/phase is still unknown.
- expected: The photo is captured or a recoverable camera error is shown.
- actual: User reports an app crash while taking a photo.
- evidence:
  - `CameraOverlayFragment.startCamera()` calls `cameraProviderFuture.get()` without handling initialization failure.
  - CameraX dependency is pinned to 1.4.1.
  - The active flow performs custom camera selection and full-resolution post-processing.
- `fd81f7d6` added post-capture MediaStore album copying; `ContentResolver.insert()` is outside the local `try/catch`, and the call is made inside an uncaught lifecycle coroutine.
- API 34 AVD smoke evidence reached CameraX camera id 10 and attached Preview/ImageCapture without an app crash; no-camera and denied-camera exercises also kept the app process alive.
- A temporary API 34 instrumentation test passed for a normal AOSP MediaStore JPEG copy and passed its expected assertion that an injected `ContentResolver.insert()` exception escapes `PhotoAlbumStore.copyToSystemAlbum()`; the temporary test was removed after the run.
  - No HTC logcat, crash report, or connected HTC device is available in this turn.
- Exact phase is decisive: preview/open points to CameraX future/binding; after shutter points to MediaStore/OEM provider or post-processing.
- next_action: `debug`
- owner: `developer`
