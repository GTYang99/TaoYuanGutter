# Requirement

## Background

- 待上傳草稿列表目前沒有區分草稿是否曾經送出 `POST /v1/ditch/storeDitch`。
- 使用者需要從列表快速辨識：草稿是從未送出，或已送出但因失敗、逾時等原因仍留在本機。

## Goal

- 在 `PendingDraftsBottomSheet` 的草稿項目顯示「曾提交過上傳」或「未提交上傳」tag。
- 只有真正進入 `storeDitch` 呼叫的草稿才算「曾提交過上傳」；App 在呼叫前閃退或從未呼叫 API 的草稿算「未提交上傳」。
- 由檢視流程進入編輯的既有側溝草稿不顯示任何 tag。

## Functional Requirements

- 在草稿持久化資料保存是否曾提交 `storeDitch` 的狀態，且需涵蓋新增、重試、失敗、逾時及 App 重啟後恢復。
- 新增流程在呼叫 `storeDitch` 前持久化「已提交」狀態；編輯流程在呼叫帶 `SPI_NUM` 的 `storeDitch` 前也持久化相同狀態。
- `PendingDraftsBottomSheet` 的一般草稿依狀態顯示一個 tag：已提交顯示「曾提交過上傳」，未提交顯示「未提交上傳」。
- 「曾提交過上傳」沿用 `AddGutterBottomSheet` 的虛擬點 badge：實心主色背景、白色文字。
- 「未提交上傳」沿用相同尺寸與字體，但不填滿，僅顯示邊框與主色文字。
- 由檢視／編輯既有側溝產生、可由 `SPI_NUM` 識別的草稿不顯示任何 tag。
- 成功上傳後既有草稿刪除行為維持不變；tag 不得影響繼續編輯、刪除或重試流程。

## Non-functional Requirements

- 既有 Room 草稿資料必須透過 migration 相容；舊版草稿缺少新欄位時不得造成讀取或恢復失敗。
- 不新增第三方依賴，不改變 `storeDitch` API payload 或既有上傳錯誤處理。

## Acceptance Criteria

- AC-001：草稿在真正進入新增或編輯 `storeDitch` 呼叫前已保存提交狀態；即使之後 API 失敗、逾時或 App 在回呼前結束，重新開啟草稿列表仍顯示「曾提交過上傳」。
- AC-002：未進入 `storeDitch` 呼叫的草稿（包含呼叫前 App 閃退而留下的草稿）顯示「未提交上傳」；此狀態在重啟與恢復草稿後仍保留。
- AC-003：兩種 tag 的文字、位置、尺寸與樣式符合指定規則；已提交為實心主色／白字，未提交為透明底／邊框與主色字。
- AC-004：檢視／編輯既有側溝的草稿不顯示任何 tag，且原有草稿列表的點擊恢復、長按刪除與成功清除行為不回歸。
- AC-005：舊版 Room 草稿可完成 migration、列表載入與恢復；缺少提交狀態時依規劃的相容預設處理，不影響既有草稿資料。

## Constraints

- 僅處理待上傳草稿列表及其提交狀態來源；不修改 API 契約、後端資料或其他列表。
- 必須保留工作區既存的 `gradle/libs.versions.toml` 修改與 `.worktrees/` 未追蹤內容。

## Open Questions

- 舊版草稿沒有「是否已提交」證據；規劃暫採 migration 預設為未提交，並由既有側溝識別規則優先隱藏 tag，待 Plan Review 確認。
