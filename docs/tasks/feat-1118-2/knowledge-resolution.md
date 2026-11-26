# Knowledge Resolution

## Sources Reviewed

- Original requirement `/Users/a10362/Desktop/markdown file/TY/ty_feat_1118-2.md`.
- Possible-gutter district files under `/Users/a10362/Desktop/markdown file/TY/未調查側溝行政區細分13區`.
- Existing-gutter district files under `/Users/a10362/Desktop/markdown file/TY/既有資料`.
- Current task decisions OQ-001 through OQ-079 and the clarification that source consistency validation is host-side, not phone-side.
- Current repository implementation, tests, task artifacts, `AGENTS.md`, knowledge-resolution rules, planning rules, and architecture.

## Resolved Decisions

| Decision | Evidence | Confidence | Affected AC |
|---|---|---:|---|
| Package 3 full GPKGs plus 13 possible-gutter and 13 existing-gutter district GPKGs; total 29 | User clarification OQ-051/OQ-052 | High | AC-001, AC-004 |
| Full mode uses only the original 3 full GPKGs | User OQ-063 | High | AC-004 |
| Partial mode uses matching district files for possible and existing-gutter layers | User OQ-061/OQ-062 | High | AC-004, AC-005 |
| Region layer stays on full GPKG and filters `TOWNNAME` in partial mode | User OQ-017/OQ-026/OQ-074 | High | AC-004, AC-005 |
| Possible district files use exact filenames such as `桃園區.gpkg`; existing files use `TOWN_桃園區.gpkg` | User OQ-053/OQ-064 and host-side inspection | High | AC-001, AC-004 |
| Possible files are `MULTIPOLYGON` EPSG:3826; existing district files are `POLYGON` EPSG:3826 | Host-side SQLite metadata inspection | High | AC-001, AC-010 |
| Full/district sources cannot be displayed simultaneously; selection changes use atomic latest-result switching | User OQ-019/OQ-020/OQ-028/OQ-029/OQ-067/OQ-068 | High | AC-006, AC-008 |
| District files are preferred; per-district fallback to full-file attribute filtering is allowed and must not crash | User OQ-068/OQ-071/OQ-072 | High | AC-007 |
| Full mode is also selected when all 13 districts are individually checked; all-select is a synchronized tri-state control | User OQ-018/OQ-046/OQ-057 | High | AC-002–AC-004 |
| Only enabled layers load; unused providers/cache/indexes are released; source cache is bounded and versioned | User OQ-035/OQ-039/OQ-040/OQ-049 | High | AC-008 |
| Cross-restart persistence keeps district choices and existing layer toggles; derived mode is recalculated | User OQ-023/OQ-038 | High | AC-009 |
| Host/CI performs GPKG consistency and manifest validation; emulator performs runtime loading/rendering/UX validation; physical phone is out of scope | User clarification after OQ-073 and project rules | High | AC-001, AC-011 |

## Source Evidence

- All 13 possible-gutter files in the supplied directory use table `refactored`, geometry `MULTIPOLYGON`, and SRS 3826.
- All 13 existing-gutter files in the supplied directory use a district-specific table, geometry `POLYGON`, and SRS 3826.
- Exact hashes, sizes, feature counts, extents, and full-vs-district consistency must be generated in the host-side asset manifest before implementation validation.

## Resolved Source-Data Decision

- The supplied possible-gutter district files are accepted as the authoritative newer dataset for partial selection. They are not exact partitions by the full source's `Town` attribute, but their 13-file geometry union exactly matches all 61,435 full-file geometries. Metadata, schema, CRS, and existing-gutter union pass. The per-district label differences remain manifest diagnostics and do not block implementation.

## Safe Planning Assumptions

- The original three full assets remain packaged for all-select mode and fallback.
- Partial mode may load multiple district sources concurrently through a bounded executor, but only the latest completed generation is committed to the map.
- Existing renderer behavior, colors, z-index, geometry support, and unrelated layer controls remain unchanged.

## Decision Applied

- User approved trusting the supplied source files and closing ISS-003. Partial selection uses district files as authoritative; validation gates on metadata and union consistency.

## Resolution Status

- Resolved. Proceed to verification with the accepted source-authority decision.
