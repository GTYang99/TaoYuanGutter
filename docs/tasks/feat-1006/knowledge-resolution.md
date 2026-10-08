# Knowledge Resolution

## Sources Reviewed

- Current user request: confirmed GPKG sources and requested edits in the `feat/FEAT-1006-wmts-offline-data` worktree.
- User-provided requirement attachment in `/Users/a10362/Desktop/markdown file/TY/`, including its later `1008 決議調整工項` section.
- The two linked/confirmed files in `/Users/a10362/Desktop/markdown file/TY/`.
- Existing FEAT-1006 task artifacts and current branch implementation/evidence.
- Repository `AGENTS.md`, `ai/knowledge-resolution-rules.md`, and `ai/planning-rules.md`.

## Resolved Decisions

| Decision | Source | Confidence | Affected AC |
|---|---|---:|---|
| Task ID and task-artifact date use `FEAT-1006`; the original attachment filename remains unchanged as source provenance | User correction | High | All task artifacts |
| Use `20260824_水務局既有側溝更新v3_線轉面.gpkg` for the Water Department legacy overlay | Attachment's later 1008 decision and user's confirmation | High for this trial | AC-001, AC-002 |
| Use `20260601_水務局未調查側溝_v5.gpkg` for the possible-gutter overlay | Attachment's later 1008 decision and user's confirmation | High for this trial | AC-001, AC-002 |
| Include both GPKGs in the app installation; remove the former GeoJSON from this trial's app assets | Latest source decision; current request | High | AC-001 |
| Keep the `roadServey` semantic mapping as “可能側溝位置” for the layer being replaced | Earlier explicit user clarification | High | AC-001, AC-003 |
| Both option titles identify `.gpkg`; no other UI change | Latest GPKG decision combined with earlier UI constraint | High | AC-006 |
| Keep solid green/red fill, outline, all four hosts, and qualitative no-crash/no-ANR/no-UI-blocking criteria with observations recorded | Earlier explicit user decisions; latest request changes the data source only | High | AC-002–AC-007 |

## Source Evidence

- Legacy GPKG: `line_buffer_polygon`, EPSG:3826, Polygon, 36,970 features, 19,615,744 bytes, SHA-256 `4b0df3e90dab556b3de0bd859511dee5a3af5d171255e4157e32b8d3f6e1ecaf`.
- Possible-gutter GPKG: `refactored`, EPSG:3826, MultiPolygon, 61,435 features, 60,661,760 bytes, SHA-256 `de68967c0fd607b1921a8bd3856d2a6d70e754d2bbb966beca89c82f6cc16177`.
- Combined source size is 80,277,504 bytes (about 76.6 MiB).
- The original attachment contains older mixed-format wording and links; the later dated adjustment section names both GPKGs. The later explicit decision and the user's confirmation resolve that conflict for this trial.

## Unresolved Conflicts

- 無。The latest decision identifies both input files and their trial layer roles.

## Safe Planning Assumptions

- Preserve previous rendering decisions and scope except where the confirmed source format requires GPKG ingestion and `.gpkg` labeling.
- Existing tests, release APK measurements, and execution report refer to the prior GeoJSON/GPKG pair; retain them as historical evidence only and repeat applicable validation against the new source.
- Keep task artifacts and branch consistently named `FEAT-1006`; source dataset filenames retain their original data dates.

## Questions Requiring Approval

- 無。

## Resolution Status

- Resolved for the amended exploratory Planning scope. Future source refreshes remain outside this task.
