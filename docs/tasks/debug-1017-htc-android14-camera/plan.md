# Debug Plan

## Scope

Investigate the HTC Android 14 photo-capture crash, then implement the two P1 fixes explicitly requested by the user on 2026-10-16. Validation is emulator-only; no physical HTC test is authorized in this task turn.

## Evidence sequence

1. Trace the actual photo entry point and all camera implementations in the target revision.
2. Inspect CameraX version, Android target, manifest permissions/storage, orientation, lens selection, flash, and post-capture processing.
3. Compare camera-related history for prior crash fixes and regressions.
4. Obtain or request device-specific logcat and map the exception to the exact code boundary.
5. Classify the cause and document the smallest fix before re-entering implementation.

## Implementation gate

The original investigation gate required an HTC stack trace or equivalent device reproduction before production changes. The user subsequently authorized an expedited implementation based on equivalent controlled API 34 fault injection for two uncaught boundaries: CameraX provider initialization and `MediaStore.insert()`. The implementation is limited to those P1 boundaries and must preserve the existing camera behavior.
