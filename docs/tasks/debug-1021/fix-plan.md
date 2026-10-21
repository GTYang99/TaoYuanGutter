# Fix Plan

## Issue

ISS-DBG-1021-001 — 新增模式的必要資料/照片驗證失敗後，送出按鈕停留在 disabled「上傳中…」。

## Minimum Fix Scope

- 僅調整 AddGutterBottomSheet 的送出失敗清理路徑。
- 在 validateWaypointPhotosAndFieldsOrAlert 回傳 false 時，呼叫既有 setSubmitLoading(false) 統一清除提交狀態並復原新增按鈕；不可依賴僅處理編輯模式的 updateSubmitButtonState()。
- 在 Dialog 顯示期間或其關閉時完成狀態清理，使按「確定」、返回鍵或其他允許的關閉方式都不會留下 loading 狀態。
- 保留現有必填欄位、照片規則、Dialog 文案及編輯模式行為。
- 不修改資料/API 合約，不調整照片補傳或 storeDitch 的順序。

## Regression Risk

應確認新增模式重試可用，且編輯模式仍依原有變更狀態決定更新按鈕。驗證失敗仍須在照片補傳及 storeDitch 呼叫之前結束。

## Validation Plan for Implementation

- 對新增模式缺少必要欄位及缺少必要照片分別驗證 Dialog 出現，關閉後按鈕恢復 enabled 且標籤為「新增側溝」。
- 確認驗證失敗未呼叫照片補傳及 storeDitch。
- 執行對應 Android 單元/儀器測試及受影響模組 build；若裝置不可用，將該項標記 NOT VERIFIED。

