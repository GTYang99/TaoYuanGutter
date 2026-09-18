# Developer Feedback for NOT VERIFIED

## Scope
- Verification result is `NOT VERIFIED` only for AC-004: isolated WMTS tile failure while the rest of the map remains usable.
- AC-001, AC-002, and AC-003 have passing evidence and do not require rework from this finding.

## Interpretation
- `NOT VERIFIED` is an evidence gap, not proof of an implementation defect. Do not enter Debug or change production behavior based on this result alone.

## Required Verification Follow-up

### 1. Provide an isolated failure environment
- Prefer a local mock endpoint, server-side failure switch, or test proxy that fails only the three WMTS GetTile requests.
- Re-run AC-004 on the Android emulator and record that the map, controls, and unrelated WMS overlays remain usable.

## Optional Development Follow-up

### 1. Add a safe WMTS endpoint injection seam if test infrastructure cannot be provided
- `Wmts3857RequestBuilder` currently fixes the production endpoint in `BASE_URL`.
- Add an optional, test-only or constructor-injected base URL while keeping the production default unchanged.
- This is a separate testability change requiring plan approval; it is not a fix implied by the current `NOT VERIFIED` result.

### 3. Preserve the current contract
- Keep `STYLE=` empty, `WebMercatorQuad`, and the existing production endpoint as defaults.
- Do not change `map_ditch_nodes_labels`, `map_no_ditch_points`, or `deleted_area` as part of the verification follow-up.

## Suggested Acceptance Evidence
- Emulator: Android 14 `sdk_gphone64_arm64`.
- Test server: local mock reachable from the emulator at `10.0.2.2`.
- Expected result: only the three WMTS overlays fail to render; the map remains interactive and unrelated overlays remain available.
