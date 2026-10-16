# Fix Plan

## Status

Approved for implementation by the user's request on 2026-10-16. Scope is limited to the two P1 failure boundaries reproduced on the API 34 emulator; HTC attribution remains unverified.

## Approved minimum changes

1. CameraX provider initialization: catch synchronous provider-request failures and asynchronous `ProcessCameraProvider` future failures, log the complete cause, and convert the failure into the existing camera-canceled result/toast path.
2. Camera binding: keep the existing `Preview` + `ImageCapture` configuration, but include candidate construction and lifecycle binding in the recoverable initialization boundary.
3. Post-capture processing: guard delayed callback access to the fragment view lifecycle and context; catch album-copy failures without losing the form photo.
4. MediaStore copy: include `ContentResolver.insert()` in the guarded transaction, log the exception, and best-effort delete any partially created item.
5. Do not change lens selection, flash, CameraX version, bitmap transformation, or memory behavior; those remain outside this P1 scope without HTC-specific evidence.

## Verification gate

- Run a debug build, targeted camera/post-processing tests, `git diff --check`, and the equivalent API 34 emulator capture flow.
- Emulator validation is explicitly the requested substitute for physical-device testing in this phase.
- Any unavailable physical-device result must remain `NOT VERIFIED` and cannot be reported as pass.
