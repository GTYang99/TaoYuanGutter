# Requirement

## Background
- App 目前已登入 API 依賴本機儲存的 Bearer token。
- 已知後端在 token 未提供、失效或過期時會回傳 HTTP 401，body 常見為 `success=false`、`message=尚未登入`、`errors=null`。
- 目前只有部分畫面會在收到 401 後清除本機登入狀態並導回登入頁，其他流程仍可能只顯示一般錯誤。

## Goal
- 當任何已登入 API 收到 token 過期或尚未登入回應時，App 要強制登出並回到登入頁。
- 強制登出流程需清除本機 token 與使用者資訊，避免使用者停留在已登入畫面繼續操作。
- 若 token 過期時使用者正在編輯側溝資料，App 必須先把目前資料保存為草稿，再提示使用者即將登出。

## Functional Requirements
- 已登入 API 回傳 HTTP 401 時，統一判定為 auth expired。
- auth expired 發生後，App 必須清除 `auth_token`、`user_name`、`user_company`、`group_id`。
- auth expired 發生後，App 必須顯示「登入狀態已失效，請重新登入」Dialog，提示使用者將要登出。
- 使用者確認 Dialog 後，App 必須以清空 task stack 的方式導回 `LoginActivity`。
- 若目前有編輯中的側溝/點位/照片資料，顯示 Dialog 前必須先保存或同步到既有草稿資料庫。
- 若目前沒有可保存的編輯內容，仍需顯示 Dialog 並完成強制登出。
- 同一次錯誤事件不得重複導頁或重複顯示多個登入失效提示。
- 手動登出 API 回傳 401 時仍視為登出成功，清除本機登入狀態並回登入頁。
- 登入 API 本身的 401 代表帳密或登入失敗，不得觸發強制登出。

## Non-functional Requirements
- 強制登出判斷需集中化，避免每個畫面自行比對 `401`。
- 不修改後端 API contract。
- 不降低既有 API 錯誤訊息與表單驗證處理。

## Acceptance Criteria
- AC-001：Dashboard API 回傳 401 時，App 清除本機登入狀態並導回 `LoginActivity`。
- AC-002：Map scope search、ditch details、node details、closest node details、store ditch、delete ditch、update state、store no ditch 等已登入 API 回傳 401 時，App 清除本機登入狀態並導回 `LoginActivity`。
- AC-003：照片上傳或側溝資料上傳流程遇到 401 時，不只顯示上傳失敗訊息，還必須觸發強制登出。
- AC-004：強制登出前必須顯示 Dialog，文案包含「登入狀態已失效，請重新登入」，並提示使用者將要登出。
- AC-005：使用者確認 Dialog 後，App 清除本機登入狀態並回到 `LoginActivity`。
- AC-006：若 401 發生時正在新增、編輯、檢視轉編輯、離線表單、照片拍攝或照片上傳流程，App 必須先把目前可保存資料寫入草稿。
- AC-007：草稿保存成功後，待上傳草稿列表可以看到剛才編輯中的資料，且照片本機路徑/待上傳狀態不被清掉。
- AC-008：若草稿保存失敗或沒有可保存內容，仍需讓使用者完成強制登出；保存失敗需有可追蹤 log 或錯誤訊息。
- AC-009：手動點擊登出且後端回傳 401 時，App 仍完成本機登出並回到登入頁，不需要另存草稿。
- AC-010：登入畫面呼叫 login API 回傳 401 時，只顯示登入失敗，不觸發額外清除或重複導頁。
- AC-011：同一畫面或同一批並行 API 中多個 401 只觸發一次強制登出 Dialog 與一次導頁。
- AC-012：非 401 錯誤，例如 404、409、422、500 或網路連線失敗，維持原本錯誤顯示與流程，不強制登出。
- AC-013：新增或更新單元測試，驗證 auth expired 判定、非 401 排除、手動登出 401、login 401 差異與草稿保存觸發。
- AC-014：新增或更新 UI/instrumentation 測試，驗證已登入編輯畫面遇到 401 後會先存草稿、顯示 Dialog，確認後回到登入頁。

## Constraints
- Planning 階段不得修改 production code。
- 不變更既有 API endpoint 與 request/response model 欄位。
- 不略過既有測試。

## Open Questions
無
