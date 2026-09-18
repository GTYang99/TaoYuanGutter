# Repository Analysis

## Current Behavior

- `Wms3857TileProvider` always serializes the supplied `styles` value as the WMS `STYLES` query parameter.
- `MapOverlayController`, `GutterFormActivity`, and `MapPointPickerActivity` each create a `roadServey` provider with `styles = "TY_RSGDBIP_道路調查"`.
- Existing tests cover the EPSG:3826 request builder but do not assert the EPSG:3857 `roadServey` URL. The current EPSG:3857 URL construction uses Android `Uri`, which is not suitable for an ordinary JVM unit test.

## Expected Behavior

- Each `roadServey` provider supplies an empty style so generated GetMap URLs retain `STYLES=` without selecting the named style.
- Other WMS providers continue to supply their existing style names.

## Affected Modules

- `map/MapOverlayController.kt`: main-map plan-survey overlay.
- `gutter/GutterFormActivity.kt`: form-map plan-survey overlay.
- `gutter/MapPointPickerActivity.kt`: standalone point-picker plan-survey overlay.
- `map/Wms3857TileProvider.kt`: delegate URL construction to a testable request builder.
- `map/Wms3857RequestBuilder.kt`: new pure-Kotlin request builder and focused URL serialization coverage.

## Dependencies

- Google Maps `UrlTileProvider` requests the URL constructed by `Wms3857TileProvider`.
- GeoServer accepts an empty `STYLES` parameter for the `roadServey` layer.
- The existing `Wms3826RequestBuilder` establishes the local pattern for a pure request builder with JVM unit tests.

## Risks

- Changing a shared provider default would alter unrelated WMS layers; the approved scope instead changes only the three `roadServey` call sites.
- An omitted parameter differs from `STYLES=`; the provider must continue serializing the empty value.
- The three entry points use separate overlay setup code, so all must be updated together.

## Unknowns

- None. The supplied requirement explicitly specifies `STYLES=`.
