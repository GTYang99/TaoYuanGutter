# Verification Report

## Revision Under Test
- Commit: `9b6ff35` (`docs(refactor-0918): clarify unverified feedback routing`)
- Branch: `codex/refactor-wms-layer-link-0918`
- `cf63365..9b6ff35` contains task-evidence-only commits; no `app/src/main` or `app/src/test` source changes.
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
| AC-004 | NOT VERIFIED | Physical Sony XQ-AU52 attempt: after a temporary unreachable proxy blocked network, the three WMTS overlays were toggled off/on and the map was panned; the map and controls remained available and the screen showed `側溝圖層載入失敗（18.2級）`. Because all app traffic was blocked, this does not prove WMTS-only failure isolation. |

## Test and CI Review
- Focused test command: `./gradlew testDebugUnitTest --tests com.example.taoyuangutter.map.Wmts3857TileProviderTest --console=plain`
- Result: PASS on source-equivalent source revision — ran with Android Studio OpenJDK 25; Gradle completed the focused WMTS test successfully. The ignored local `MAPS_API_KEY=test` placeholder was used only for manifest configuration.
- Debug build: PASS on `9b6ff35` — `./gradlew assembleDebug --no-daemon --console=plain` completed with the temporary placeholder.
- Verification scope: limited to the WMTS contract test, source-scope review, and physical-device AC-004 attempt; no full regression suite was run.
- CI: NOT VERIFIED — no CI result was supplied.
- Physical-device AC-004 attempt: PARTIAL EVIDENCE — Sony XQ-AU52, Android 12/API 31, package `com.example.taoyuangutter`; the installed `9b6ff35` debug APK entered `MainShellActivity` through the existing authenticated session. The three affected overlay controls were toggled off/on, the map was panned, and the map controls remained visible while the layer-load-failure state appeared. Evidence: `evidence/ac004-physical-network-failure.png`.
- Physical WMTS-only failure: NOT VERIFIED — the safe available method blocked all app network traffic; no isolated WMTS endpoint outage or approved server-side failure control was available. Proxy settings were removed afterward and verified as `null`.
- Emulator: not used in this verification round.

## Regression Review
- `git diff --check` passed with no whitespace errors.
- Static source review confirmed excluded WMS overlays were not changed; runtime regression evidence is unavailable.

## Final Result
- Result: NOT VERIFIED
- Category: environment
- Failed acceptance criteria: none observed
- Not verified acceptance criteria: AC-004
- Required next action: provide an isolated WMTS-only failure control usable by the physical device, then repeat AC-004; CI must also report build and test results before release.
