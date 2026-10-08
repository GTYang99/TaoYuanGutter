# Requirement

## Source

- Task ID: `FEAT-1006` (per the user correction).
- User-provided requirement attachment from `/Users/a10362/Desktop/markdown file/TY/`; task artifacts use `FEAT-1006`.
- The attachment's later `1008 決議調整工項` section and the user's confirmation establish the two GPKG sources below. This decision supersedes the attachment's earlier mixed-format source pair.

## Background

- The legacy Water Department and possible-gutter map overlays currently load feature data from WMTS/API sources, producing unnecessary network traffic.
- This is an exploratory offline-data change. Long-term source versions and mapping can be revisited after this test.

## Goal

- Package the two confirmed GPKG files with the app and use them as local sources for the Water Department legacy and possible-gutter overlays in every existing map host.

## Functional Requirements

- Use `20260824_水務局既有側溝更新v3_線轉面.gpkg` for the Water Department legacy overlay. Its GeoPackage table is `line_buffer_polygon` (EPSG:3826, Polygon, 36,970 features).
- Use `20260601_水務局未調查側溝_v5.gpkg` for the possible-gutter overlay. Its GeoPackage table is `refactored` (EPSG:3826, MultiPolygon, 61,435 features). `roadServey` is the online source previously used for the “可能側溝位置” layer.
- Package both confirmed GPKG files with the app installation and render their features locally without requesting corresponding WMTS feature data.
- Render legacy polygons with opaque `#156D1D` fill and possible-gutter polygons with opaque `#FA0000` fill. Preserve polygon holes and MultiPolygon members.
- In the layer-option titles, identify both source files as GPKG: `水務局舊資料（.gpkg）` and `可能側溝位置（.gpkg）`. Make no other UI changes.
- Draw a same-color outline around both offline polygon layers with zoom-dependent, adjustable width. At broad scale (zoom 16 and below), use a slightly thicker `2.5` tile pixels; at close scale (zoom 20 and above), use `1.5` tile pixels so the line does not dominate. Interpolate continuously between those zooms. Expose the reference zooms and endpoint widths as tunable style parameters.
- Apply the offline overlays in the `MainActivity` and `MapWorkspaceFragment` main-map hosts, the gutter form map, and the point-picker map.
- Parse and render without blocking the UI thread. Avoid crashes and ANRs; release or bound provider-owned overlay and cache resources when disabled or when a map is destroyed.

## Non-functional Requirements

- After installation, both feature sources are available offline. Other base-map and unrelated overlay network behavior stays unchanged.
- Record packaged asset sizes and observed app responsiveness. There is no APK/AAB size limit for this trial.
- Treat this as a trial using the two specified GPKGs; it does not settle future source versions or permanent layer mapping.

## Acceptance Criteria

- AC-001: The APK includes both specified GPKGs with source bytes verified against their recorded hashes. Both overlays load local features after installation without requesting `legacyDitch` or `roadServey` feature data.
- AC-002: Legacy polygons render opaque `#156D1D`; possible-gutter polygons render opaque `#FA0000`. EPSG:3826 geometries align with the base map, including polygon holes and MultiPolygon members.
- AC-003: Both overlays work in `MainActivity`, `MapWorkspaceFragment`, the gutter form map, and the point-picker map. Existing layer controls show and hide the corresponding overlays.
- AC-004: With either or both overlays enabled, pan and zoom through the supplied data in all four map hosts without a crash, ANR, or UI-blocking parse/render operation. Record observed responsiveness and load behavior.
- AC-005: Disabling a layer removes its rendered content and releases or bounds its provider-owned cache/resources. Re-enabling restores one correct local overlay without duplicates.
- AC-006: The layer options display exactly `水務局舊資料（.gpkg）` and `可能側溝位置（.gpkg）`; other UI labels and layout remain unchanged.
- AC-007: Both polygon layers display a same-color outline whose width changes continuously with zoom: `2.5` tile pixels at zoom 16 and below, interpolated between zoom 16 and 20, and `1.5` tile pixels at zoom 20 and above. Reference zooms and endpoint widths are adjustable style parameters. The outline remains visible at broad scale without appearing oversized at close scale; fills and holes remain intact.

## Constraints

- Use only the two GPKG sources identified above for this trial; do not use the previously planned GeoJSON as the possible-gutter source.
- Change only the two layer-option titles; do not change other UI labels or layout.
- Preserve thematic colors, map controls, and unrelated map layers.
- Do not claim this test establishes permanent dataset-to-layer mapping.

## Open Questions

- 無。兩個 GPKG 來源及其本次工項對應已由使用者確認；後續正式資料版本的選擇屬於未來資料更新範圍。
