# Knowledge Resolution

## Sources Reviewed

- User request: prepare the WMTS overlays for offline GPKG/GeoJSON and work from commit `203b3351c56902a9c612952d6412425ffddcb576`.
- User-provided requirement attachment; task identifier corrected to `FEAT-1006` per user instruction.
- Attachment folder inventory and file sizes: `20260824_水務局既有側溝更新v3_線轉面.gpkg` (19 MiB), `20261006_0601版可能側溝位置.geojson` (93 MiB), and `20261006_0824版既有側溝.geojson` (23 MiB).
- Repository `ai/architecture.md`, `ai/knowledge-resolution-rules.md`, `ai/planning-rules.md`, `ai/git-rules.md`, and the asset manifest template.
- Current map rendering at the requested revision: `MapOverlayController`, `MapPointPickerActivity`, `GutterFormActivity`, `Wmts3857TileProvider`, and layer strings.

## Resolved Decisions

| Decision | Source | Confidence | Affected AC |
|---|---|---:|---|
| For this exploratory task, use the two files explicitly linked by the requirement, pairing the legacy GPKG with the legacy layer and the possible-location GeoJSON with the possible layer; determine parser by actual file format, and defer definitive dataset mapping to a later task | User clarification and linked attachment names | High for this trial; provisional for later releases | AC-001 |
| Package both supplied files in the app installation | User clarification | High | AC-001 |
| Fill legacy polygons solid green and possible-location polygons solid red | User clarification | High | AC-002 |
| Apply both offline overlays in the main map, gutter form map, and point-picker map | User clarification | High | AC-003, AC-004 |
| The `roadServey` WMTS is the current source for the possible-gutter overlay | User clarification | High | AC-001, AC-003 |
| Label the two layer options with their trial source suffixes `.gpkg` and `.geojson`; no other UI changes | Current user clarification, overriding the source's general no-UI-change constraint for these option titles only | High | AC-006 |
| No APK/AAB size limit applies to this trial; use the existing qualitative no-crash/no-ANR/no-UI-blocking criteria and record observations | Current user clarification | High | AC-001, AC-004 |
| New worktree is based at the requested commit and uses a dedicated task branch | User request and repository Git rules | High | N/A |

## Repository Evidence

- At the requested revision, `MapOverlayController` loads the legacy overlay from `BackgroundWmtsLayer.LEGACY_DITCH`; the `showPossible` toggle currently controls `BackgroundWmtsLayer.ROAD_SURVEY`.
- `GutterFormActivity` and `MapPointPickerActivity` contain separate WMTS overlay loading paths, so limiting the change to the main map would leave other map contexts unchanged.
- `docs/assets/` contains only an asset-manifest template; there is no approved asset manifest specifying these datasets or their distribution policy.

## Unresolved Conflicts

- The source requirement's format labels are reversed relative to the linked files' actual formats; for this trial the user clarified to use those exact linked files, label the option titles with actual suffixes, and defer definitive data mapping until after testing.
- A third `20261006_0824版既有側溝.geojson` exists in the attachment folder, but the user specified using the two explicitly linked files for this trial.

## Safe Planning Assumptions

- The present file-to-layer association is only an experiment and must not be represented as the final dataset mapping for later releases.
- Use each file's actual format for parsing despite the reversed format labels in the original source.
- Existing layer controls stay unchanged; `roadServey` is the current possible-gutter online source to be replaced.

## Questions Requiring Approval

- The definitive dataset-to-layer mapping remains a documented follow-up question; it does not block this exploratory plan.

## Resolution Status

- Resolved for exploratory Planning. The final dataset-to-layer mapping remains out of scope and must be confirmed before a later production data refresh.
