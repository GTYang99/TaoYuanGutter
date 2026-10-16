# Issue Log

## ISS-DBG-1017-001

- task_id: `debug-1017-htc-android14-camera`
- phase: `debug`
- category: `unknown` (provisional; likely implementation/OEM Camera2 boundary)
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
  - No HTC logcat, crash report, or connected HTC device is available in this turn.
- next_action: `debug`
- owner: `developer`
