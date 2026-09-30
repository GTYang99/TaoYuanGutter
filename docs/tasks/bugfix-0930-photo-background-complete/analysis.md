# Repository Analysis

## Current Behavior
- `GutterBasicInfoFragment` 與 `GutterPhotosFragment` 在相機結果回來時呼叫 `setPhotoLoading(true)`；`GutterFormActivity` 顯示可點擊、覆蓋全畫面的 `photoLoadingOverlay`。
- `fabSubmit` 經 `dispatchResultAfterPendingPhotoUploads()` 等待 coordinator，最多 30 秒；只有等到 success 才回傳表單結果。
- `PhotoSlotUploadCoordinator` 使用 process-scoped IO coroutine，不依附 Activity；Activity `onDestroy` 只解除 listener。新增流程送出前會等待 pending upload；編輯流程目前遇到進行中照片先提示稍後重試，不會送出。
- 背景 coordinator 與送出前同步上傳目前都可能把 `success=true` 但缺少有效 `img_id` 的回應標成 success；mapper 最後只把可解析 ID 放入 `img_ids`。

## Expected Behavior
- 拍照預覽只顯示槽位內載入狀態，不阻擋整個表單；表單完成只等待本機草稿同步，不等待網路。
- coordinator 在表單離開後繼續工作；新增與編輯的正式 `storeDitch` 都由父層上傳 gate 等待或阻擋。
- 新拍／替換照片必須取得正整數 ID 才算上傳成功；其他結果留在 failed/retryable，不可靜默省略 ID 後送出。

## Affected Modules
- `gutter/GutterFormActivity.kt`：移除表單結果等待網路，保留草稿同步與防重複 dispatch。
- `gutter/GutterBasicInfoFragment.kt`、`gutter/GutterPhotosFragment.kt`、`gutter/PhotoLoadingHost.kt`、`res/layout/activity_gutter_form.xml`：移除全頁照片載入觸控遮罩，保留槽位內 progress。
- `common/PhotoSlotUploadCoordinator.kt`、`common/PhotoUploadSlotState.kt`、`gutter/AddGutterBottomSheet.kt`：背景及送出前新照片上傳需有效正整數 ID。
- `app/src/test/...`：補 ID 邊界 unit test，保留匯入 URL-only 相容測試。

## Dependencies
- `GutterSessionRepository` 持久化照片 URI、upload state 與 image ID。
- `PhotoSlotUploadCoordinator` 維持 application/process scope，與 Activity listener 分離。
- `AddGutterBottomSheet.ensureWaypointPhotosUploadedBeforeSubmit()` 是正式 `storeDitch` 前的上傳 gate。
- `StoreDitchNodeRequestMapper` 只序列化照片 image IDs，不會從本機 URI 推導伺服器 ID。

## Risks
- 移除全頁遮罩後，相機返回期間的視覺回饋只剩照片槽位載入指示；必須確認槽位 progress 不被移除。
- 表單結果可能帶 `uploading` 狀態；父層必須繼續以 session draft/coordinator 結果為準，不能在該時點呼叫 `storeDitch`。新增與編輯均使用同一等待 gate。
- URL-only 匯入照片具有舊相容語意，不可用全域 `isAlreadyUploaded` 改寫來解決新照片 ID 問題。
- 本機 unit/build 不驗證實機觸控或 app process 被系統終止時的續傳；後者明確不在本次需求內。

## Unknowns
- 無影響本次實作的需求未知項；目前沒有實機/CI 證據，實作後按 task validation 記錄限制。
