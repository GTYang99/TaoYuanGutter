# Knowledge Resolution

## Sources Reviewed

- Current user decisions in this conversation.
- `docs/tasks/feat-0917/requirement.md` and `docs/tasks/feat-0917/analysis.md`.
- Historical implementation at `e32432d1af90bb838dd1ea735b412ba6daac253e` and the 2026-09-17 change `e56f5e3d19a709617592950cc90fc97cdf07b58b`.
- The 0917 requirement references `/Users/a10362/Desktop/markdown file/ty_feat_0917.md`, but that attachment is not present at the referenced path in this environment; used the checked-in task requirement and implementation history.
- Current `ImportExistingWaypointBottomSheet`, `GutterFormActivity`, `GutterApiService`, and `GutterRepository`.
- `ai/architecture.md`, `ai/planning-rules.md`, and the task templates.

## Resolved Decisions

| Decision | Source | Confidence | Affected AC |
|---|---|---:|---|
| Three tabs are ordered recent access, nearby, search; recent access is the default. | User decision | High | AC-001 |
| Recent access is the existing recent-saved feature; preserve its behavior. | User decision | High | AC-002 |
| Nearby restores GPS and the top-right location button; do not show coordinate text. | User decision | High | AC-003 |
| Each tab re-queries on return; search waits when no query was submitted and reruns the previous submitted XY_NUM otherwise. | User decision | High | AC-002–AC-004 |
| Nearby result scope/order and refresh UI follow the historical implementation. | User decision and historical source | High | AC-003, AC-005 |
| All tabs share candidate markers, selected-point highlighting, and import action. | User decision | High | AC-005 |
| Keep the existing sheet visual style. | User decision | High | AC-001 |
| Recent-saved lookup and nearby lookup use the same route, with no query for recent-saved and `lng`/`lat` for nearby. | User decision, consistent with historical implementation | High | AC-002–AC-003 |

## Repository Evidence

- Current sheet has two tabs: `最近存檔點位` and `搜尋點位`; the recent-saved request is no-query.
- Historical nearby lookup used the same `closestNodeDetails` route with `lng` and `lat`, and returned `NodeDetailsResponse`.
- `GutterFormActivity` still contains the legacy location permission, cached/current GPS, location marker/camera, and candidate-marker callbacks, but the current sheet no longer invokes the nearby flow on tab entry.
- The current search page only sends XY_NUM after IME search. Tab rendering does not currently re-run search requests.
- No import-specific tests were found in the current checkout; the planned task should add focused coverage.

## Resolved Conflicts

- The 0917 requirement intentionally removed the nearby GPS flow and specifies only no-query recent-saved lookup. The current task restores the prior nearby behavior as an explicit user decision while preserving the recent-saved behavior.
- The 0917 and historical nearby contracts use the same route differently. The user confirmed the backend supports both query modes, resolving the integration question.

## Safe Planning Assumptions

- Reuse the existing adapter, bottom-sheet controls, map callbacks, and location lifecycle machinery where their ownership and behavior match.
- Keep the HTTP request/response contract behind the existing service/repository boundary; restore the historical `lng`/`lat` request for nearby while preserving the no-query recent-saved request.
- Do not add client-side radius filtering, result sorting, or list limits; preserve the historical server response order.

## Remaining Questions

- 無。
