# Knowledge Resolution

## Sources Reviewed

- Current user request: confirmed GPKG sources and requested edits in the `feat/FEAT-1006-wmts-offline-data` worktree.
- User-provided requirement file `/Users/a10362/Desktop/markdown file/TY/ty_feat_1007.md`, including its 1008 and 1014 amendment sections. The referenced `ty_feat_1004.md` path is absent; the existing 1007 file contains the latest decision and linked GPKG.
- The two linked/confirmed files in `/Users/a10362/Desktop/markdown file/TY/`.
- Existing FEAT-1006 task artifacts and current branch implementation/evidence.
- Repository `AGENTS.md`, `ai/knowledge-resolution-rules.md`, and `ai/planning-rules.md`.

## Resolved Decisions

| Decision | Source | Confidence | Affected AC |
|---|---|---:|---|
| Outline width changes continuously from 2.5 px at zoom <=16 to 1.5 px at zoom >=20; reference zooms and endpoint widths remain tunable | Updated FEAT-1006 requirement in the task history | High | AC-007 |
| App/runtime checks run only on Android emulator; physical-phone checks are out of scope | User-approved test-environment constraint recorded in updated requirement | High | AC-001–AC-007 |
| Task ID and task-artifact date use `FEAT-1006`; the original attachment filename remains unchanged as source provenance | User correction | High | All task artifacts |
| Use `20260824_水務局既有側溝更新v3_線轉面.gpkg` for the Water Department legacy overlay | Attachment's later 1008 decision and user's confirmation | High for this trial | AC-001, AC-002 |
| Use `20260601_水務局未調查側溝_v5.gpkg` for the possible-gutter overlay | Attachment's later 1008 decision and user's confirmation | High for this trial | AC-001, AC-002 |
| Include the three confirmed GPKGs in the app installation; remove the former GeoJSON from this trial's app assets | 1008 and 1014 source decisions; current request | High | AC-001 |
| Keep the `roadServey` semantic mapping as “可能側溝位置” for the layer being replaced | Earlier explicit user clarification | High | AC-001, AC-003 |
| Replace the Taoyuan administrative-area `regions` WMTS with the supplied `桃園市行政區.gpkg`; do not request the regions feature layer from the API | 1014 amendment in `ty_feat_1007.md` | High | AC-001, AC-008 |
| Remove file-extension suffixes from the two thematic layer titles; preserve other labels and UI | 1014 amendment in `ty_feat_1007.md` | High | AC-006 |
| Preserve the existing WMTS visual appearance for the Taoyuan administrative-area layer when switching its data source to GPKG | User clarification in this conversation: “沿用現有 WMTS 外觀” | High | AC-008 |
| Keep solid green/red fill, outline, all four hosts, and qualitative no-crash/no-ANR/no-UI-blocking criteria with observations recorded | Earlier explicit user decisions; latest request changes the data source only | High | AC-002–AC-007 |

## Source Evidence

- Legacy GPKG: `line_buffer_polygon`, EPSG:3826, Polygon, 36,970 features, 19,615,744 bytes, SHA-256 `4b0df3e90dab556b3de0bd859511dee5a3af5d171255e4157e32b8d3f6e1ecaf`.
- Possible-gutter GPKG: `refactored`, EPSG:3826, MultiPolygon, 61,435 features, 60,661,760 bytes, SHA-256 `de68967c0fd607b1921a8bd3856d2a6d70e754d2bbb966beca89c82f6cc16177`.
- Taoyuan administrative-area GPKG: table `桃園市行政區`, geometry `MULTIPOLYGON`, SRS id 100000 (`GCS_TWD97[2020]`), 13 features, 1,081,344 bytes, SHA-256 `aeed88ae15bc53956bbea3e7ebd9a3faf1bf29fc45a6bfaa4188b5333458c1bc`.
- The third file uses a geographic TWD97[2020] CRS; the current offline store is specialized for EPSG:3826. Planning must account for the CRS and MultiPolygon rather than assuming existing store compatibility.
- The requirement file contains older mixed-format wording and 1008 GPKG decisions; its later 1014 decision adds the administrative-area GPKG. Later dated decisions supersede older source-format wording.

## Unresolved Conflicts

- 無。The administrative-area style is confirmed: preserve the current WMTS appearance.

## Safe Planning Assumptions

- Preserve previous rendering decisions and scope except where the confirmed source format or the 1014 title decision explicitly changes them.
- Existing tests, release APK measurements, and execution report refer to the prior GeoJSON/GPKG pair; retain them as historical evidence only and repeat applicable validation against the new source.
- Keep task artifacts and branch consistently named `FEAT-1006`; source dataset filenames retain their original data dates. The local branch is `feat/feat-1006-wmts-offline-data`.

## Questions Requiring Approval

- 無。

## Resolution Status

- The new visual work item has explicit endpoint widths, reference zooms, and interpolation behavior. The test environment is explicitly limited to Android emulator. These decisions amend the prior fixed-width/physical-device plan.

- Source, host, title, and visual-preservation decisions are resolved for the amended exploratory Planning scope. Future source refreshes remain outside this task.
