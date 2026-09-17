# Repository Analysis

## Task Classification

- Type: `feature`; this adds persisted point attributes and changes creation/import behavior.

## Current Behavior

- `GutterBasicInfoFragment` owns field order, mode-specific enablement, `collectData()` and draft notification. It has Cant Open and groups through Silt, but no connect-point/connect-pipe controls.
- `NodeDetails`, prefill paths, `StoreDitchNodeRequestMapper` and `StoreDitchNodeRequest` lack the new fields. The mapper always emits `XY_NUM`; submit validation requires and checks it for duplicates.
- `GutterInspectActivity` converts `NodeDetails` into editable waypoint `basicData` before it launches the edit form. It currently maps `IS_CANTOPEN` but has no keys for the two new attributes; without this handoff, an unchanged edit would fall back to false and overwrite existing server values.
- `GutterBasicInfoFragment.newInstance()` copies point data through an explicit argument whitelist, then `prefillData()` reads that same whitelist. New `basicData` keys are discarded unless both argument constants/mappings and prefill initialization are extended.
- The import sheet waits for current location and calls `getClosestNodeDetails(lng, lat, token)`, displays query coordinates, markers and a sheet-local location button.

## Expected Behavior

- Connect Point and Cant Open are mutually exclusive. Connect Pipe is an independent required no/yes state defaulting to no. Both persist through `Waypoint.basicData`, which existing draft serialization already retains.
- Create omits `XY_NUM` and consumes the server-generated response value; edit preserves but cannot alter its existing value.
- Import immediately loads recent stored points with no GPS, permission, map click, coordinate UI or map markers.

## Affected Modules and Dependencies

- `fragment_gutter_basic_info.xml`, `GutterBasicInfoFragment.kt`, `GutterInspectActivity.kt`: controls, mutual exclusion, mode policy, inspect-to-edit readback handoff, prefill and collection.
- `GutterApiModels.kt`, `StoreDitchNodeRequestMapper.kt`: DTOs, nullable/omitted create XY_NUM and new attribute mapping.
- `AddGutterBottomSheet.kt`: create/update response handling and system-name session update.
- `GutterApiService.kt`, `GutterRepository.kt`, `GutterFormActivity.kt`, `ImportExistingWaypointBottomSheet.kt`, `bottom_sheet_import_existing_waypoint.xml`: no-query import API and removal of location orchestration/UI.
- Existing focused tests: `StoreDitchNodeRequestMapperTest` and `GutterBasicInfoUiTest`; add import/API coverage.

## Risks

- JSON null serialization must be proven to omit, not serialize, create `XY_NUM`.
- Connect Point must remain limited to mutual exclusion with Cant Open; selecting it must preserve normal point detail values, validation requirements and all photo slots.
- New controls must be included in all reorder, virtual, view and import-lock lists.
- Inspect-to-edit must map both nullable read values into `basicData` before form construction; otherwise a no-op edit can write default false values back to the server.
- The `basicData` mapping must cross both `GutterInspectActivity` and the Fragment argument/prefill boundary; testing mapper output alone cannot prove UI or submission preservation.
- Removed location jobs/callbacks must not produce stale import-sheet updates.

## Assumptions

- The documented `XY_NUM` response can be associated with current waypoints by existing point role/order.
- `is_connect_point` and `is_connect_pipe` are Boolean on all edit/inspect read responses; missing means false.
- 虛擬點不適用 Cant Open、Connect Point、Connect Pipe；切換時三者清除且 payload 省略，與既有虛擬點保留 Pending Deploy 的行為一致。

## Potential Issues

- `ISS-001` is resolved. Test-discovered data loss, serialization or import lifecycle failures are implementation regressions and must enter Debug.
