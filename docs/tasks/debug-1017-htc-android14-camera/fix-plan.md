# Fix Plan

## Status

Pending device trace. This is a candidate minimum scope, not an implementation approval.

## Candidate minimum changes after confirmation

1. If the stack is post-shutter, wrap the complete album-copy operation including `ContentResolver.insert()`, log the exception class/cause, and preserve the form photo even when system-album copy fails.
2. If the stack is CameraX initialization, wrap the `ProcessCameraProvider` future callback, log the full cause chain, and convert initialization failure into the existing camera-canceled result/toast path.
3. Add a lifecycle guard before using the overlay binding or `viewLifecycleOwner` from delayed camera callbacks.
4. Preserve the existing `Preview` + `ImageCapture` behavior; only change lens selection, flash, or CameraX version if the HTC trace isolates that compatibility boundary.
5. Fix the post-processing bitmap lifecycle defect by reading dimensions before `recycle()` and avoid full-resolution transformation when the EXIF/color settings require no conversion.
6. If the HTC trace proves memory pressure, use bounds-aware sampling or remove the in-capture full-resolution transform; validate image orientation and upload behavior separately.

## Verification gate

- Do not implement until the failing acceptance check and exception boundary are identified.
- After implementation, run a debug build, targeted camera/post-processing tests, `git diff --check`, and the affected HTC Android 14 capture flow.
- Any unavailable physical-device result must remain `NOT VERIFIED` and cannot be reported as pass.
