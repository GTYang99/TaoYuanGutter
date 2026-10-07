# Requirement

## Source

- User-provided requirement attachment; task identifier corrected to `FEAT-1006` per user instruction.
- The original source has reversed format labels relative to the linked files. For this exploratory task, the user's clarification authorizes the two linked files and their linked layer roles; definitive dataset mapping remains a follow-up.

## Background

- The two map overlays currently use online WMTS/API data and create excessive network traffic.
- Replace these sources with offline vector data while keeping their existing map appearance.

## Goal

- For this exploratory release, package the two files explicitly linked in the source requirement with the app and load them locally for the Water Department legacy and possible-gutter overlays across all map contexts.

## Functional Requirements

- Use the specifically linked `20260824_水務局既有側溝更新v3_線轉面.gpkg` for the Water Department legacy overlay and `20261006_0601版可能側溝位置.geojson` for the possible-gutter overlay for this trial. Determine parsing format from each file's actual format/extension; the source's format labels are reversed.
- Package both files with the app installation and render the data without requesting the corresponding online data layers.
- Draw the legacy data as solid green-filled polygons and possible-gutter data as solid red-filled polygons.
- In the layer-option titles, mark the current trial source format: `水務局舊資料（.gpkg）` and `可能側溝位置（.geojson）`; make no other UI changes.
- Apply the offline overlays to the main map, gutter form map, and point-picker map.
- Manage parsing, rendering, and map overlay resources so data loads off the UI thread and map pan/zoom remains usable.

## Non-functional Requirements

- Offline feature source after installation; smooth map interaction and bounded/released rendering resources.
- This is an exploratory implementation with the specified files and layer pairing; it does not establish the final authoritative dataset-to-layer mapping for later releases.

## Acceptance Criteria

- AC-001: The APK includes the two files explicitly linked above, and both overlays can read their local packaged data after installation without fetching their feature data from the network.
- AC-002: The legacy overlay displays opaque `#156D1D`-filled polygons and the possible-gutter overlay displays opaque `#FA0000`-filled polygons, geographically aligned with the existing base map and preserving polygon holes.
- AC-003: Both overlays work in the `MainActivity` and `MapWorkspaceFragment` main-map hosts, the gutter form map, and the point-picker map; existing layer controls continue to show and hide the corresponding overlays.
- AC-004: With each overlay enabled, a user can pan and zoom across the supplied data in all four map hosts without an app crash, ANR, or blocking the map UI while the data is parsed/rendered.
- AC-005: Turning either overlay off removes its rendered content and releases its provider-owned cache and overlay resources; turning it on again restores the correct local content without duplicate overlays.
- AC-006: The layer options display exactly `水務局舊資料（.gpkg）` and `可能側溝位置（.geojson）`; other UI labels and layout remain unchanged.

## Constraints

- Only change the two layer-option labels as described in the functional requirements; do not change other UI labels or layout.
- Preserve the green appearance for Water Department legacy data and red appearance for possible gutter locations.

## Open Questions

- OQ-001 (follow-up, non-blocking for this trial): After testing, confirm the definitive file-to-layer mapping and source versions for a production release. For this exploratory task, use the two linked files and their linked thematic roles, parsing each by its actual format.
