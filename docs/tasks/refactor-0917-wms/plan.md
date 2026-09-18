# Implementation Plan

## Goal

- Make every `roadServey` WMS GetMap request use the required empty `STYLES` parameter without changing other overlays.

## Scope

- Change only the three `roadServey` provider constructions and add focused request-URL coverage; do not introduce WMS caching.

## Affected Files

- `app/src/main/java/com/example/taoyuangutter/map/MapOverlayController.kt` — main-map provider configuration.
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt` — form-map provider configuration.
- `app/src/main/java/com/example/taoyuangutter/gutter/MapPointPickerActivity.kt` — point-picker provider configuration.
- `app/src/main/java/com/example/taoyuangutter/map/Wms3857RequestBuilder.kt` — pure-Kotlin EPSG:3857 request serialization extracted from the Android-facing provider.
- `app/src/main/java/com/example/taoyuangutter/map/Wms3857TileProvider.kt` — delegate request generation to the extracted builder without altering tile behavior.
- `app/src/test/java/com/example/taoyuangutter/map/Wms3857RequestBuilderTest.kt` — verify empty styles are encoded as `STYLES=`.

## Implementation Steps

1. Change the `roadServey` `styles` argument to an empty string in `MapOverlayController`.
2. Make the identical scoped change in `GutterFormActivity` and `MapPointPickerActivity`.
3. Extract the existing EPSG:3857 URL construction into a pure request builder, following the existing EPSG:3826 builder pattern; keep `Wms3857TileProvider` as the Google Maps adapter.
4. Add a JVM unit test that parses the builder URL and asserts that `STYLES` is present with an empty value while layer and other request fields remain intact.
5. Run targeted unit tests and inspect the diff to confirm no non-`roadServey` style changed.

## Test Plan

- Run the new `Wms3857RequestBuilderTest`.
- Run existing `MapOverlayControllerStateTest` and `Wms3826RequestBuilderTest`.
- Build the debug app.

### Physical Device Test Scope

- Requires physical device: Yes
- Device/environment: Android 9+ device or emulator with authenticated map access and network capture available.
- In-scope Acceptance Criteria: AC-001, AC-002, AC-003.
- Regression risk: plan-survey layer rendering or another WMS layer style changes unintentionally.
- Full regression required: No
- Full regression trigger: A failure in an unrelated WMS overlay or map entry-point initialization.
- Stop condition: Requests are observed for all three entry points and other WMS overlays remain visible with their existing style.

| AC | Environment | Steps | Expected | Evidence |
|---|---|---|---|---|
| AC-001 | Physical device | Open main map with plan-survey layer enabled | `roadServey` request has `STYLES=` and map renders | Captured request and result |
| AC-002 | Physical device | Open gutter form map and standalone point picker | Both `roadServey` requests have `STYLES=` | Captured requests and result |
| AC-003 | Physical device | Toggle other WMS overlays | Existing named-style overlays still render | Result; screenshot on failure |

## Regression Plan

- Confirm plan-survey overlay visibility toggling still adds/removes its overlay.
- Confirm labels, legacy ditch, region, deleted-area, and no-ditch overlays retain their current styles and behavior.
- Confirm map-form and point-picker startup does not fail.

## Risks

- `STYLES=` must be emitted rather than omitted; the provider behavior is covered directly by a unit test.
- Backend rendering cannot be proven without an authenticated network-enabled device session.

## Rollback Plan

- Revert this single refactor commit to restore the named `roadServey` style.

## Current Behavior

- All three `roadServey` call sites specify `TY_RSGDBIP_道路調查`; the provider serializes it into every GetMap URL.

## Expected Behavior

- All three call sites supply an empty style and the provider emits `STYLES=`; other layer configurations are unchanged.

## Acceptance Criteria Traceability

| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | 1, 3, 4 | Unit URL assertion and main-map device request capture |
| AC-002 | 2, 3, 4 | Unit URL assertion and form/picker device request captures |
| AC-003 | 5 | Diff review, state tests, and overlay device smoke |

## Failure Behavior

- If a WMS request fails or returns no tile, retain existing Google Maps tile-provider behavior; this task adds no retry or cache policy.

## Security and Privacy

- No permission, credential, or user-data change.

## Open Questions

- None.
