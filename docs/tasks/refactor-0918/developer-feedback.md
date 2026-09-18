# Developer Feedback for NOT VERIFIED

## Scope
- Verification result is `NOT VERIFIED` only for AC-004: isolated WMTS tile failure while the rest of the map remains usable.
- AC-001, AC-002, and AC-003 have passing evidence and do not require rework from this finding.

## Required Development Follow-up

### 1. Add a safe WMTS endpoint injection seam
- `Wmts3857RequestBuilder` currently fixes the production endpoint in `BASE_URL`.
- Add an optional, test-only or constructor-injected base URL while keeping the production default unchanged.
- This will allow the emulator to target a local mock endpoint that returns WMTS errors without blocking unrelated app traffic.

### 2. Add an isolated failure test
- Make the mock endpoint fail only the three WMTS GetTile requests (`roadServey`, `legacyDitch`, `regions`).
- Verify on the Android emulator that the base map, controls, and unrelated WMS overlays remain usable when those requests fail.
- Capture the request/log evidence and map-control result for AC-004.

### 3. Preserve the current contract
- Keep `STYLE=` empty, `WebMercatorQuad`, and the existing production endpoint as defaults.
- Do not change `map_ditch_nodes_labels`, `map_no_ditch_points`, or `deleted_area` as part of the verification follow-up.

## Suggested Acceptance Evidence
- Emulator: Android 14 `sdk_gphone64_arm64`.
- Test server: local mock reachable from the emulator at `10.0.2.2`.
- Expected result: only the three WMTS overlays fail to render; the map remains interactive and unrelated overlays remain available.
