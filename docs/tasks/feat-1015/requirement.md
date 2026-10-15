# Requirement

## Goal

- Restore the existing nearby-waypoint import flow and present recent saved points, nearby points, and point search as three tabs in the existing import sheet.

## Functional Requirements

### Import tabs

- Keep the existing bottom-sheet visual style and layout. Add three tabs in this order: `最近存取節點`, `附近點位`, `搜尋點位`; default to the first tab.
- `最近存取節點` is an alias for the stable `最近存檔點位` feature. Preserve its existing API behavior and results without changing that feature.
- Re-query a tab whenever the user returns to it. The recent-saved tab keeps its existing no-query request. Nearby uses the legacy GPS-based flow. Search re-runs the last submitted XY_NUM query; if no search has been submitted, keep the existing prompt and wait for input.

### Nearby points

- Restore the pre-2026-09-17 GPS query flow and the header's current-location button.
- Do not display the `查詢位置：經緯度` hint.
- Follow the legacy location flow, including its current-location marker/camera behavior, permission and location handling, and nearby-result handling.
- Follow legacy result behavior: retain existing rows under the loading overlay while refreshing, and consume the server's returned result order without client-side radius filtering or count limits.

### Search and import flow

- Preserve the existing XY_NUM search input and request behavior.
- All three tabs use the existing candidate-marker display, selected-waypoint highlight, and bottom `匯入` flow.
- Switching tabs clears the previous selection and map candidates as before. A successful response repopulates candidates for the active tab.

## Acceptance Criteria

- AC-001: The import sheet retains its existing visual style and displays the three tabs in the specified order, with `最近存取節點` selected by default.
- AC-002: The recent-saved tab retains the existing `GET /api/v1/node/closestNodeDetails` no-query behavior and data handling. It is re-queried when re-entered.
- AC-003: Nearby points use the legacy GPS query flow and header location button, do not show query coordinates, and re-query on tab entry and location-button tap. Legacy location marker/camera, permission/error, loading, and result-order behavior are preserved.
- AC-004: Search preserves the existing XY_NUM search. Returning to the tab re-runs the last submitted query; without a submitted query, the existing hint remains and no request is sent.
- AC-005: Each tab's candidate results use the common map markers, selected-point highlight, and import action. Tab changes clear the previous selection and candidate markers; refresh retains the previous list beneath the loading overlay until a response replaces it.

## Constraints and Sources

- Current user decisions in this conversation define labels, order, default tab, refresh behavior, map integration, and legacy nearby behavior.
- `docs/tasks/feat-0917/requirement.md` defines the recent-saved feature as a no-query `GET /v1/node/closestNodeDetails` returning a `NodeDetails` list.
- The pre-0917 implementation (parent `e32432d1af90bb838dd1ea735b412ba6daac253e` of the 2026-09-17 UI/API change) used `GET /api/v1/node/closestNodeDetails?lng=...&lat=...`, Bearer authorization, and a `NodeDetailsResponse` for nearby points.
- The user confirmed that the same `closestNodeDetails` route supports both query modes: no query for recent-saved points and `lng`/`lat` for nearby points. Nearby keeps the legacy Bearer authorization and `NodeDetailsResponse` handling.

## Open Questions

- 無。
