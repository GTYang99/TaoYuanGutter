# Upload Failure Classification Table

本表是這次實作對照表，目的只有兩個：

1. 使用者在失敗 Alert 看到固定的錯誤分類與參考代碼
2. 開發者可用這張表把畫面上的分類，對照回目前程式中的詳細原因與位置

## 使用者分類對照

| 使用者看到的錯誤分類 | 參考代碼 | 使用者文案方向 | 內部詳細原因對照 | 目前程式位置 |
| --- | --- | --- | --- | --- |
| 網路連線失敗 | `NETWORK_ERROR` | 目前網路不穩或無法連線，請確認訊號後再試。 | `UnknownHostException`、`ConnectException`、`SocketException`、一般 `網路連線失敗` 訊息 | `AddGutterBottomSheet.showStoreDitchFailureDialog()`、`UploadFailureClassifier.forStoreDitchError()` |
| 側溝資料上傳失敗 | `STORE_DITCH_FAILED` | 側溝資料未成功送出，請稍後再試。 | `storeDitch()` 回傳 `ApiResult.Error`，含後端 `message` | `AddGutterBottomSheet.performEditSubmit()`、`submitNewGutterRequest()` |
| 側溝資料上傳失敗 | `STORE_DITCH_AUTH_FAILED` | 登入狀態已失效，請重新登入後再試。 | `storeDitch()` HTTP `401` | 同上 |
| 側溝資料上傳失敗 | `STORE_DITCH_VALIDATION_FAILED` | 側溝資料未成功送出，請檢查填寫內容後再試。 | `storeDitch()` HTTP `422` 或後端欄位驗證失敗訊息 | 同上 |
| 照片處理失敗 | `PHOTO_PROCESS_FAILED` | 照片檔案無法讀取或處理，請重新拍攝或重新選取照片。 | `uploadNodeImage()` 回傳 `無法處理圖片檔案`、壓縮/解碼失敗 | `GutterRepository.uploadNodeImage()`、`PhotoUploadManager.uploadWaypointPhotos()` |
| 照片上傳失敗 | `PHOTO_UPLOAD_FAILED` | 照片未全部上傳成功，請重新嘗試或存入草稿稍後再傳。 | 單張照片 API 錯誤、HTTP `401/422/500`、retry 3 次後仍失敗 | `PhotoUploadManager.uploadWaypointPhotos()`、`MainActivity.finalizePhotoUploadFlow()` |
| 網路連線失敗 | `PHOTO_NETWORK_ERROR` | 照片上傳時網路不穩或無法連線，請確認訊號後再試。 | 照片上傳過程中的 socket / host / timeout 類訊息 | `PhotoUploadManager.uploadWaypointPhotos()`、`UploadFailureClassifier.forPhotoBatchFailures()` |
| 伺服器回應逾時 | `SERVER_TIMEOUT` | 已送出上傳請求，但等待伺服器回應逾時。 | 最後一張照片 `withTimeoutOrNull(60000)` 逾時；可能後端已收到但前端未等到回應 | `PhotoUploadManager.LAST_PHOTO_WAIT_TIMEOUT_MS`、`MainActivity.showPhotoUploadTimeoutAlert()` |
| 未知錯誤 | `UNKNOWN_ERROR` | 發生未預期錯誤，請重試；若持續發生請回報管理人員。 | 未分類 Exception、非預期流程中斷 | `MainActivity.finalizePhotoUploadFlow()` catch、`AddGutterBottomSheet` catch |

## 這次實作的顯示原則

- Alert 只顯示：
  - 失敗類型
  - 人話說明
  - 參考代碼
  - 最多 1 到 3 行失敗摘要
- 不直接顯示 raw logcat 或 stack trace
- 不改原本上傳與草稿保存流程，只掛在現有失敗出口補顯示

## 這次實作的主要接點

1. `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`
   - `storeDitch` 失敗 Alert 顯示分類
2. `app/src/main/java/com/example/taoyuangutter/gutter/PhotoUploadManager.kt`
   - 蒐集單張照片失敗摘要
3. `app/src/main/java/com/example/taoyuangutter/MainActivity.kt`
   - 照片整批失敗與 timeout Alert 顯示分類
4. `app/src/main/java/com/example/taoyuangutter/common/UploadFailureClassifier.kt`
   - 使用者顯示分類與參考代碼集中管理
