# Issue Log

## ISS-DBG-1017-001

- task_id: `debug-1017-htc-android14-camera`
- phase: `implementation`
- category: `unknown` (provisional; likely implementation/OEM Camera2 or MediaProvider boundary)
- priority: `P1`
- title: HTC Android 14 photo capture may terminate the app
- status: `fixed_pending_verification`
- impact: The required photo capture flow may be blocked on the affected device.
- repro_steps:
  - Open the gutter form on an HTC Android 14 phone.
  - Enter the in-app camera from a photo slot.
  - Capture a photo; exact failing tap/phase is still unknown.
- expected: The photo is captured or a recoverable camera error is shown.
- actual: User reports an app crash while taking a photo.
- evidence:
  - Before `27e24a0`, `CameraOverlayFragment.startCamera()` called `cameraProviderFuture.get()` without handling initialization failure.
  - Before `27e24a0`, `ContentResolver.insert()` was outside `PhotoAlbumStore`'s local `try/catch` and was called from an uncaught lifecycle coroutine.
  - CameraX dependency is pinned to 1.4.1.
  - The active flow performs custom camera selection and full-resolution post-processing.
- `27e24a0` guards both P1 boundaries, preserves the form photo on post-capture failure, and adds permanent instrumentation coverage.
- API 34 AVD smoke evidence reached CameraX camera id 10 and attached Preview/ImageCapture without an app crash; no-camera and denied-camera exercises also kept the app process alive.
- API 34 instrumentation now passes the permanent `PhotoAlbumStoreInstrumentedTest`, which injects a throwing `insert()` and verifies a recoverable `null` result.
- API 34 instrumentation now passes the permanent `CameraProviderInitializationUiTest`, which injects a real CameraX provider initialization failure and verifies the form Activity remains alive.
  - No HTC logcat, crash report, or connected HTC device is available in this turn.
- Exact phase is decisive: preview/open points to CameraX future/binding; after shutter points to MediaStore/OEM provider or post-processing.
- next_action: `verification`
- owner: `developer`
