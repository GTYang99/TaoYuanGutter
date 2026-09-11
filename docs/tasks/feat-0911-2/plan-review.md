# Plan Review

## Decision
APPROVED

## Review Summary
- 本輪重新審查已涵蓋 requirement、analysis、更新後的 plan、state 與既有主地圖實作。
- 前次的兩項 Major 已解決：計畫明定 EPSG:3826 的 GRS80/TWD97 TM2 zone 121 參數、Web Mercator 四角逆算與投影後取全域 bounds；同時將 Android／Google Maps 相依自 local JUnit 範圍分離。
- 固定向量 `(54849, 28085, 16)` 的四角投影與全域 bounds 已獨立重算，符合計畫所列 `279756.220,2754335.172,280312.364,2754888.262`（公尺）。
- scope 維持在主地圖，並完整涵蓋 `MainActivity`、`MapWorkspaceFragment`、`OverlayState`、BottomSheet 及既有互動的回歸邊界。

## Checklist
| Item | Result | Evidence |
|---|---|---|
| Requirements understood | Pass | `requirement.md` 功能需求、限制與 AC-001 至 AC-005 |
| Acceptance criteria complete | Pass | `plan.md` 的 traceability table 覆蓋全部 AC |
| Repository and architecture analysis | Pass | `analysis.md` 識別現有 EPSG:3857 provider、雙 host、ViewModel state 與 Google Maps 相依 |
| Affected modules and dependencies | Pass | `plan.md` 分別列出 pure request builder、tile wrapper、controller、UI、兩個 host 與 tests |
| Risks evaluated | Pass | CRS／投影、服務可用性、z-index、雙 host 漏接與 runtime 限制均已列出 |
| Test plan complete | Pass | pure local JUnit、固定獨立 BBOX 向量、build，以及逐項實機 smoke test 與 `NOT VERIFIED` 規則 |
| Regression plan complete | Pass | 既有 WMS、no-ditch interaction、polyline、measure labels、basemap 與非主地圖範圍均已涵蓋 |
| Open questions documented | Pass | 無阻擋問題；已記錄 Google Maps runtime 驗證限制 |
| Implementation steps actionable | Pass | 已指定 projection constants、四角取界、encoding、state default、UI contract 與 host updates |
| Scope and task size | Pass | 限定單一主地圖功能，未擴及 picker 或表單地圖 |
| Rollback strategy | Pass | 可回退本任務單一 commit 以還原既有五個覆蓋圖層 |

## Findings

### Finding 1
Severity: Major

Category: Implementation Plan

Description:
前輪計畫未明定 EPSG:3826 BBOX 的 projection 參數、四角 bounds 與可重現預期值。

Planning Response:
已於 `plan.md` Steps 1–3 與 Test Plan 明定 GRS80/TWD97 TM2 zone 121、四個 tile 角點、min/max 取界、UTF-8 encoding，以及固定 BBOX vector。此向量已在本輪獨立重算確認。

Status: Resolved

### Finding 2
Severity: Major

Category: Test Plan

Description:
前輪未區分可在純 JUnit 執行的邏輯與需要 Android／Google Maps runtime 的覆蓋圖層行為。

Planning Response:
已將 `Wms3826RequestBuilder`／projection 設計為 pure Kotlin local JUnit 範圍；TileOverlay、sheet callback、remove/re-add 與 recreation 已明確列為逐項實機 smoke test，且無法執行時必須記為 `NOT VERIFIED`。

Status: Resolved

### Finding 3
Severity: Suggestion

Category: Regression Plan

Description:
前輪未把關閉圖層後重新開啟 sheet 的 initial dispatch 行為列為明確測試。

Planning Response:
已於 Step 5、Test Plan 和 AC-004 traceability 納入關閉後重開 sheet 仍維持未勾選且不得重新加入 overlay 的 smoke case。

Status: Resolved

## Open Questions
無。

## Implementation Readiness
- Ready to begin implementation under the approved scope.
