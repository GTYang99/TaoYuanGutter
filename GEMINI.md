# 專案說明
這是 Android Kotlin 專案，功能是 GutterForm 表單。

## 主要檔案
- ActivityGutterForm.kt：管理主要 Activity 和 Fragment 切換
- FragmentGutterBasicInfo.kt：基本資料表單
- MapFragment.kt：地圖標記

## 目前問題
- fabSubmit 點擊流程已修正，確保基本資料與照片資料皆填寫後才提交。
- 基本資料驗證邏輯已補齊 (`GutterBasicInfoFragment.validateRequiredFields`)。
- 照片驗證邏輯已補齊 (`FragmentGutterPhotos.validateAllPhotos`)，確保三張照片皆已拍攝。
- `FragmentGutterPhotos` 已實作，支援照片選擇與顯示。

## 修改內容
- **`ActivityGutterForm.kt`**:
    - 修改 `saveAndClose()` 函數，現在會先驗證 `GutterBasicInfoFragment` 的必填欄位，然後再驗證 `FragmentGutterPhotos` 的所有照片。
    - 確保 `buildAndFinishWithResult()` 函數能從基本資料 Fragment 和照片 Fragment 收集資料，並將其包含在結果 Intent 中。
    - 更新 `fillDataExtras` 以處理照片 URI 作為字符串。
- **`GutterPhotosFragment.kt`**:
    - 將照片儲存方式從 `File` 更改為 `Uri`，以更好地與 `ActivityResultContracts.TakePicture` 兼容。
    - 移除 `pendingFile` 變數。
    - 替換舊的 `cameraLauncher` 為 `takePictureLauncher` (使用 `ActivityResultContracts.TakePicture`)。
    - 修改 `cameraPermissionLauncher` 的回調邏輯，在授予權限後直接啟動 `takePictureLauncher`。
    - 移除 `openLandscapeCamera` 和 `createImageFile` 函數，因為它們已被新的 URI 創建和啟動流程取代。
    - 更新 `onViewCreated` 和 `onSaveInstanceState` 以使用 `Uri.parse()` 和 `Uri.toString()` 來儲存和恢復照片 URI。
    - 更新 `setEditable` 函數中的可見性檢查，以檢查 `photoUriSlotX` 是否為空。
    - 更新 `validateAllPhotos` 以檢查 `photoUriSlotX` 是否為空。
    - 更新 `getPhotoPaths` 以返回 `photoUriSlotX?.toString()`。
    - 移除 `getPhotoUris` 函數，因為 `getPhotoPaths` 現在返回 URI 的字符串表示。
    - 新增 `createPhotoUriForSlot` 函數，用於為每個照片槽創建持久性 URI。
- **`GutterFormPagerAdapter.kt`**:
    - 更新以正確取得 `GutterBasicInfoFragment` 和 `FragmentGutterPhotos` 的實例。
- **`PendingDraftsBottomSheet.kt`**:
    - 調整 BottomSheet 的最大高度為螢幕的 80%，確保其顯示在鏡頭下方。
    - 修改草稿點擊行為：現在單擊項目會直接恢復草稿到 `AddGutterBottomSheet`。
    - 調整 `showDraftActionDialog` 為僅處理刪除草稿的邏輯，現在透過長按項目觸發。
- **`PendingDraftAdapter.kt`**:
    - 新增 `onItemLongClick` 監聽器，支援長按項目以觸發刪除草稿功能。

## 本次結論
- 針對 `fabSubmit` 按鈕的觸發行為，已確認其驗證邏輯（需填寫基本資料與照片）及提示訊息（Toast）已正確實作於 `GutterFormActivity.kt` 的 `saveAndClose()` 函數中。
- 在 `GutterFormActivity.kt` 的 `buildAndFinishWithResult()` 函數中，`RESULT_LATITUDE` 和 `RESULT_LONGITUDE` 僅在 `basicData["coordY"]` (緯度) 和 `basicData["coordX"]` (經度) 欄位的值能成功透過 `toDoubleOrNull()` 解析為 Double 時才會被加入到回傳的 Intent 中。
- 若使用者輸入的座標值為空、非數字，或無法解析，`toDoubleOrNull()` 將回傳 `null`，導致 `RESULT_LATITUDE` 和 `RESULT_LONGITUDE` 遺失，進而影響地圖上的點位標記。
- 關於「編輯完點位後，在 `AddGutterBottomSheet` 中起點資料未正確更新」的問題，經過分析，問題點最可能出在 `MainActivity` (或實作 `LocationPickerHost` 的地方) 未能正確更新其維護的 `Waypoint` 列表，或是 `getInspectWaypoints()` 方法未能返回最新的 `Waypoint` 資料給 `AddGutterBottomSheet`。`AddGutterBottomSheet` 本身接收和顯示資料的邏輯是正確的。
- `PendingDraftsBottomSheet` 的最大高度已調整為螢幕的 80%，以優化使用者體驗，確保其位於手機鏡頭下方。
- 點擊 `PendingDraftsBottomSheet` 中的草稿項目現在會直接在 `AddGutterBottomSheet` 中恢復編輯，簡化了操作流程，不再經過中間的選擇對話框。
- 刪除草稿功能已從單擊動作移至長按動作，提供更直觀和安全的刪除方式。

## 注意事項
- minSdk 26
- 使用 ViewBinding

## 下次待處理事項
- **資料儲存與 API 整合**:
    - 將收集到的基本資料和照片 URI 實際儲存至 `GutterRepository` 或透過 API 傳送至後端。
- **UI/UX 優化**:
    - 進一步優化照片選擇介面。
    - 加入圖片刪除功能。
- **錯誤處理**:
    - 為照片選擇、儲存和權限問題增加更完善的錯誤處理機制。

## 建議的每日工作節奏
```
1. cd 專案目錄
2. gemini 啟動
3. 專注修一個功能
4. 確認沒問題 → 存檔（Ctrl+S in VS Code）
5. /quit 結束 session
6. 更新 GEMINI.md 記錄進度
7. 重複
```
