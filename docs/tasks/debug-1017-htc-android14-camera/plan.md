# Debug Plan

## Scope

Investigate the HTC Android 14 photo-capture crash without changing production code.

## Evidence sequence

1. Trace the actual photo entry point and all camera implementations in the target revision.
2. Inspect CameraX version, Android target, manifest permissions/storage, orientation, lens selection, flash, and post-capture processing.
3. Compare camera-related history for prior crash fixes and regressions.
4. Obtain or request device-specific logcat and map the exception to the exact code boundary.
5. Classify the cause and document the smallest fix before re-entering implementation.

## Implementation gate

No production-code change is authorized until the crash stack trace or an equivalent device reproduction identifies one cause and maps it to an acceptance check.
