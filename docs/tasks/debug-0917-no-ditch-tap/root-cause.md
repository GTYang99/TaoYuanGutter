# Root Cause Investigation

## Status

- Root cause is not yet identified; no production-code change is authorized.

## Static Evidence

- No-ditch selection is intentionally limited to the first map tap by `noDitchPickedLatLng == null`; reset restores selection.
- The no-ditch panel is visible while map selection is expected and applies dynamic bottom insets.
- Main-button enablement does not explicitly disable the map view, so static inspection alone cannot prove a button-state cause.

## Required Runtime Evidence

- Reproduce on an affected device with model, Android version, resolution, navigation mode, and display scaling recorded.
- Capture the view bounds of the visible panel and map plus the tap coordinate.
- Compare a tap in the unobscured map region before entry, after entry, after reset, and after exit.

## Provisional Classification

- Category: unknown.
- Priority: P2 until reproduction shows the reporting workflow is broadly blocked.
