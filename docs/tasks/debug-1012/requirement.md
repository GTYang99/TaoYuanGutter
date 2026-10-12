# Requirement

## Background
- 編輯既有側溝時，storeDitch 回傳 HTTP 403 會出現「重傳／存入草稿」對話框。
- 靜態追蹤發現編輯送出失敗路徑未關閉主畫面的 blocking upload spinner。

## Goal
- 編輯請求失敗後，停止上傳 spinner，並保留既有的重傳與存入草稿操作。

## Functional Requirements
- storeDitch 編輯請求收到失敗結果或一般例外時，通知 host 結束提交 loading。
- 使用者選擇「重傳」時，重新提交流程須再次開啟 loading。
- 使用者選擇「存入草稿」時，既有對話框與 host 收尾流程照常執行。

## Non-functional Requirements
- 不改變 HTTP 403 分類、API 契約、請求資料或既有 host 的草稿處理策略。
- 不增加執行階段依賴。

## Acceptance Criteria
- AC-001：編輯模式收到 storeDitch HTTP 403 後，blocking upload overlay 與 spinner 停止；「上傳失敗」對話框仍可操作。
- AC-002：在 AC-001 的失敗對話框選「存入草稿」後，sheet 可依既有 host 行為收尾，且 spinner 不會持續顯示。
- AC-003：選「重傳」會再次進入編輯提交並開啟 loading；第二次請求失敗後 loading 仍會停止。
- AC-004：非取消類一般例外進入既有失敗對話框前，blocking loading 已結束。

## Constraints
- 修正限於已定位的編輯 storeDitch 失敗生命週期及其回歸測試。
- 驗證使用 Android 模擬器 Medium_Phone，不使用實機。
- Draft persistence contract for a single-gutter host is not expanded in this task; the existing host handling remains as-is.

## Open Questions
- 是否要求單側溝編輯的「存入草稿」在目前沒有 matching draft 時一律建立持久化草稿，需另立需求確認；本次 AC 只檢查 spinner 與既有收尾流程。
