# Verification Report

## Revision Under Test
- Commit: `cf63365` (`refactor(refactor-0918): use WMTS background layers`)
- Branch: `codex/refactor-wms-layer-link-0918`
- Source worktree before verification: clean at `cf63365`.

## Implementation Review
- `Wmts3857RequestBuilder` builds the specified GWC WMTS endpoint with `SERVICE=WMTS`, `VERSION=1.0.0`, `REQUEST=GetTile`, `TILEMATRIXSET=WebMercatorQuad`, and Google tile `zoom/y/x` mapped to `TILEMATRIX/TILEROW/TILECOL`.
- `BackgroundWmtsLayer` limits the new provider to the three approved layers and defines their required formats.
- All three approved layers use `Wmts3857TileProvider` in the main map, point picker, and form map. `map_ditch_nodes_labels`, `map_no_ditch_points`, and `deleted_area` retain their existing providers.
- The provider catches invalid URL/tile arguments and returns `null`, matching the specified tile-level failure degradation.

## Acceptance Criteria

| AC | Result | Evidence / limitation |
|---|---|---|
| AC-001 | PASS | `Wmts3857TileProviderTest` passed; it verifies all approved layers' WMTS endpoint parameters, formats, and z/y/x matrix mapping. |
| AC-002 | PASS | The same passing test verifies `STYLE` decodes to an empty value for every approved layer. |
| AC-003 | PASS | Static scope review confirms all three entry points use the same provider and excluded layers remain unchanged; `assembleDebug` compiled the affected Android code successfully. |
| AC-004 | NOT VERIFIED | Source retains `null` tile URL degradation, but no compile or physical-device failure-path evidence is available. |

## Test and CI Review
- Focused test command: `./gradlew testDebugUnitTest --tests com.example.taoyuangutter.map.Wmts3857TileProviderTest --console=plain`
- Result: PASS — ran with Android Studio OpenJDK 25 and a temporary ignored `MAPS_API_KEY=test` placeholder.
- Debug build: PASS — `./gradlew assembleDebug --console=plain` succeeded with the same temporary placeholder.
- CI: NOT VERIFIED — no CI result was supplied.
- Emulator test: NOT VERIFIED — Android 14 `sdk_gphone64_arm64` installed the debug APK and launched `LoginActivity`. The map flow requires a test account; `MainActivity` cannot be started directly because it is not exported. A functional Maps API key is also unavailable.

## Regression Review
- `git diff --check` passed with no whitespace errors.
- Static source review confirmed excluded WMS overlays were not changed; runtime regression evidence is unavailable.

## Final Result
- Result: NOT VERIFIED
- Category: environment
- Failed acceptance criteria: none observed
- Not verified acceptance criteria: AC-004
- Required next action: provide a simulator test account and a local authorized Google Maps API key, then run the plan's emulator WMTS failure-path check; CI must also report build and test results before release.
