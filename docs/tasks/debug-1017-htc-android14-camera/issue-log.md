# Issue Log

## ISS-DBG-1017-001

- task_id: debug-1017-htc-android14-camera
- phase: implementation
- category: unknown (provisional; likely implementation/OEM Camera2 or MediaProvider boundary)
- priority: P1
- title: HTC Android 14 photo capture may terminate the app
- status: fixed_pending_verification
- impact: The required photo capture flow may be blocked on the affected device.
- repro_steps:
  - Open the gutter form on an HTC Android 14 phone.
  - Enter the in-app camera from a photo slot.
  - Capture a photo; exact failing tap/phase is still unknown.
- expected: The photo is captured or a recoverable camera error is shown.
- actual: User reports an app crash while taking a photo.
- evidence:
  - Before 27e24a0, CameraOverlayFragment.startCamera() called cameraProviderFuture.get() without handling initialization failure.
  - Before 27e24a0, ContentResolver.insert() was outside PhotoAlbumStore's local try/catch and was called from an uncaught lifecycle coroutine.
  - CameraX dependency is pinned to 1.4.1.
  - The active flow performs custom camera selection and full-resolution post-processing.
- 27e24a0 guards both P1 boundaries, preserves the form photo on post-capture failure, and adds permanent instrumentation coverage.
- API 34 AVD smoke evidence reached CameraX camera id 10 and attached Preview/ImageCapture without an app crash; no-camera and denied-camera exercises also kept the app process alive.
- API 34 instrumentation passes PhotoAlbumStoreInstrumentedTest and CameraProviderInitializationUiTest.
- Round 2 at 190eabe9 repeated both instrumentation tests and the normal Android 14 capture-to-MediaStore flow successfully on emulator-5556.
- No HTC logcat, crash report, or connected HTC device is available.
- Exact phase is decisive: preview/open points to CameraX future/binding; after shutter points to MediaStore/OEM provider or post-processing.
- next_action: verification
- owner: developer

## ISS-DBG-1017-002

- task_id: debug-1017-htc-android14-camera
- phase: verification
- category: environment
- priority: P1
- title: Verification cannot classify the HTC incident or confirm CI evidence
- status: open
- impact: Release and final root-cause attribution remain blocked; this is not evidence of a new production regression.
- repro_steps:
  - Run independent verification against the current committed revision.
  - Evaluate AC-DBG-001 and AC-DBG-002 using available evidence.
  - Check the configured remote and CI result for the tested commit.
- expected: HTC incident metadata/trace and a CI result are available, or affected acceptance checks are explicitly dispositioned according to the task gate.
- actual: AC-DBG-001 and AC-DBG-002 remain NOT VERIFIED. The local task branch is not listed by the configured GitLab remote. The inferred GitHub repository/API lookup returns 404; no CI result is available for the tested revision.
- evidence:
  - Round 2 was run on commit 190eabe98a7dd341ab64492f37420687defbd4a9; app source has no diff from b92c122.
  - Debug build succeeded; both camera/MediaStore fault-injection tests passed; normal API 34 capture and album publication passed.
  - git ls-remote --heads gitlab-http codex/debug/debug-1017-htc-android14-camera returned no matching ref.
  - The configured project remote is GitLab; GitHub status/workflow lookups for the inferred project returned 404. There is no .gitlab-ci.yml in the repository.
  - User confirmed during round 2 that no HTC model/build/app version, exact failing phase, logcat/tombstone, or crash report is currently available.
- classification: environment / evidence availability, not implementation_regression.
- next_action: verification
- owner: verification
