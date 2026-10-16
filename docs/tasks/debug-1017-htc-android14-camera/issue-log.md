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

## ISS-DBG-1017-002

- task_id: `debug-1017-htc-android14-camera`
- phase: `verification`
- category: `environment`
- priority: `P1`
- title: Verification cannot classify the HTC incident or close CI evidence
- status: `open`
- impact: Release and final root-cause attribution remain blocked; this is not evidence of a new production regression.
- repro_steps:
  - Run independent verification against committed revision `b92c122`.
  - Evaluate AC-DBG-001 and AC-DBG-002 using the available evidence.
  - Check the CI status/workflow result for the tested commit.
- expected: HTC incident metadata/trace and a CI result are available, or the criteria are explicitly dispositioned according to the task gate.
- actual: AC-DBG-001 and AC-DBG-002 are `NOT VERIFIED`; no CI status/workflow run is published. A read-only GitHub remote query failed with `Could not resolve hostname` for `github.com`.
- evidence:
  - API 34 targeted fault-injection tests and normal capture smoke passed.
  - No HTC model, build, incident app version, exact failing phase, logcat/tombstone, or crash report was supplied.
  - `.github/workflows/android-ci.yml` exists, but local build/test output is not CI evidence.
- classification: `environment` / evidence availability, not `implementation_regression`.
- next_action: `verification`
- owner: `verification`
