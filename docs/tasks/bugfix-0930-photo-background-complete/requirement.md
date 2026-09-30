# Requirement

## Background
拍照後，`photoLoadingOverlay` 會覆蓋整個表單；「完成」流程也會等待單張照片上傳，可能讓使用者誤以為頁面卡住。背景上傳若回報成功但沒有有效 `img_id`，目前仍可能被視為完成。

## Goal
讓使用者在照片背景上傳期間仍可完成單點表單並返回上一頁；保留草稿與照片路徑，正式送出前仍確保新拍／替換照片具有有效伺服器 ID。

## Functional Requirements
- 拍照後不以全頁遮罩阻擋表單操作；單張預覽與上傳狀態仍在照片槽位顯示。
- 點選 `fabSubmit` 時先完成本機草稿同步，再回傳表單結果；不得等待背景網路上傳完成。
- 表單 Activity 結束不得取消已排入的背景照片工作；正式側溝送出流程須等待必要的進行中上傳。
- 新拍或替換照片的 API 回應只有在含正整數 `img_id` 時才能標記為上傳成功；缺少或無效 ID 時保留可重試狀態，禁止以缺少 ID 的成功結果送出側溝。
- 保留既有 server-backed 匯入照片的相容判定，不把本次修正擴大成匯入照片重傳重構。

## Non-functional Requirements
- 不新增外部依賴；保留使用者已拍攝照片與草稿。
- Validation 採小範圍 JVM unit tests 加 affected debug build；不要求全 App 回歸或實機測試。

## Acceptance Criteria
- AC-001：從基本資料或照片頁拍照後，不再出現會攔截整頁觸控的照片預覽遮罩；照片槽位自己的載入／上傳指示仍可用。
- AC-002：單張上傳仍在進行時點 `fabSubmit`，表單在草稿同步後回傳，不等待網路完成；結果保留照片 URI、session draft ID 與當下上傳狀態。
- AC-003：離開表單不取消 coordinator 工作；後續正式送出在必要上傳仍進行時等待，且上傳失敗／逾時不呼叫 `storeDitch`。
- AC-004：新上傳回應 ID 為 null、0 或負數時，槽位不得標記 success；正整數 ID 才能標記 success 並進入 `img_ids`。
- AC-005：既有 URL-only 匯入照片的現有相容測試仍通過。
- AC-006：上述狀態規則有 targeted unit-test evidence，Activity/Fragment/XML 變更可通過 `assembleDebug`。

## Constraints
- 僅處理表單拍照後的全頁遮罩、完成回傳等待與新照片上傳成功判定。
- 不改 API contract、資料庫 schema、相機拍攝流程或正式上傳順序；`storeDitch` 仍須在所需照片可用後才執行。
- 背景 coroutine 保證限於 app process 存活期間；本工項不承諾 process death 後系統層級續傳。

## Open Questions
無
