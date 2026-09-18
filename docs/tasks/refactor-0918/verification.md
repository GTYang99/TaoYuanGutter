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
| AC-001 | NOT VERIFIED | Static review matches the required WMTS parameters, but the focused JVM test could not start because Java Runtime is missing. |
| AC-002 | NOT VERIFIED | Source explicitly sets `STYLE` to an empty value, but the request was not compiled or executed. |
| AC-003 | NOT VERIFIED | Source scope confirms all three entry points use the same provider and excluded layers remain unchanged; no Android build or physical-device result is available. |
| AC-004 | NOT VERIFIED | Source retains `null` tile URL degradation, but no compile or physical-device failure-path evidence is available. |

## Test and CI Review
- Focused test command: `./gradlew testDebugUnitTest --tests com.example.taoyuangutter.map.Wmts3857TileProviderTest --console=plain`
- Result: NOT VERIFIED — host reports `Unable to locate a Java Runtime` before Gradle test execution.
- Debug build, CI, and physical-device tests: NOT VERIFIED — same missing Java Runtime and no supplied device/test environment.

## Regression Review
- `git diff --check` passed with no whitespace errors.
- Static source review confirmed excluded WMS overlays were not changed; runtime regression evidence is unavailable.

## Final Result
- Result: NOT VERIFIED
- Category: environment
- Failed acceptance criteria: none observed
- Not verified acceptance criteria: AC-001, AC-002, AC-003, AC-004
- Required next action: provide a Java-configured Android build environment and an Android 9+ test device; rerun the focused test, assemble debug APK, and perform the plan's limited three-entry-point device checks.
