# Verification Report

## Revision Under Test
- Commit: `c6cb4bf` (`docs(refactor-0918): record AC-004 emulator attempt`)
- Branch: `codex/refactor-wms-layer-link-0918`
- `cf63365..c6cb4bf` contains task-evidence-only commits; no `app/src/main` or `app/src/test` source changes.
- Source worktree during verification: tracked source clean; unrelated untracked `.worktrees/` preserved.

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
| AC-004 | NOT VERIFIED | Limited Android 14 emulator attempt: with the three WMTS overlays toggled off/on while an unreachable global proxy blocked network, the map UI remained alive and controls remained visible after a pan; however, all network traffic was blocked, so this does not prove WMTS-only failure isolation. |

## Test and CI Review
- Focused test command: `./gradlew testDebugUnitTest --tests com.example.taoyuangutter.map.Wmts3857TileProviderTest --console=plain`
- Result: PASS on source-equivalent revision `c6cb4bf` — ran with Android Studio OpenJDK 25; Gradle completed the focused WMTS test successfully. The existing ignored local `MAPS_API_KEY` placeholder was used only for manifest configuration.
- Debug build: Existing evidence PASS on source-equivalent revision `5fad9e2`; not rerun in this limited verification scope.
- Verification scope: limited to the WMTS contract test and source-scope review; no full regression suite or new emulator exploration was run.
- CI: NOT VERIFIED — no CI result was supplied.
- Emulator startup: PASS — fixed revision `5fad9e2` passed the focused JVM test and debug APK build, was installed on Android 14 `sdk_gphone64_arm64`, and launched `LoginActivity`.
- Emulator AC-004 attempt: PARTIAL EVIDENCE — `emulator-5554`, Android 14/API 34, package `com.example.taoyuangutter`; an existing authenticated session entered `MainShellActivity`, the map loaded, and the three affected overlay controls were toggled off/on. A temporary unreachable proxy (`10.0.2.2:59999`) produced a visible layer-load-failure state while the map and controls remained available. The proxy was removed afterward and verified as `null`.
- Emulator WMTS-only failure: NOT VERIFIED — the available safe failure method blocked all app network traffic; no isolated WMTS endpoint outage or approved server-side failure control was available.

## Regression Review
- `git diff --check` passed with no whitespace errors.
- Static source review confirmed excluded WMS overlays were not changed; runtime regression evidence is unavailable.

## Final Result
- Result: NOT VERIFIED
- Category: environment
- Failed acceptance criteria: none observed
- Not verified acceptance criteria: AC-004
- Required next action: provide a simulator test account and a local authorized Google Maps API key, then run the plan's emulator WMTS failure-path check; CI must also report build and test results before release.
