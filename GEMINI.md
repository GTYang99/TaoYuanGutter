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
    - **新增 `EXTRA_IS_EDIT_MODE` 和 `EXTRA_DATA_XY_NUM` 常數，並在 `newIntent` 和 `newViewIntent` 中傳遞 `isEditMode` 旗標 (編輯模式下為 `true`)。**
    - **在 `onCreate` 中，獲取 `isEditMode` 旗標和 `xyNum` 資料。**
    - **調整 `enterEditMode()` 函數，明確設定 `isEditMode = true`。**
    - **修改 `buildEmptyData()` 函數，在編輯模式下將 `gutterId` 預設為空字串，以符合「不用顯示側溝編號欄位」的需求。**
    - **更新 `buildAndFinishWithResult`，在編輯模式下不回傳 `RESULT_DATA_GUTTER_ID`。**
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
    - **修改建構子，接收 `isEditMode` 參數並傳遞給 `GutterBasicInfoFragment.newInstance`。**
- **`GutterBasicInfoFragment.kt`**:
    - **新增 `ARG_IS_EDIT_MODE` 和 `ARG_DATA_XY_NUM` 常數。**
    - **更新 `newInstance` 函數，接受 `isEditMode` 和 `xyNum` 參數。**
    - **在 `onViewCreated` 中，如果 `isEditMode` 為 `true`，則隱藏 `tilGutterId` (側溝編號) 欄位。**
    - **在 `prefillData` 中，如果 `isEditMode` 為 `true` 且 `xyNum` 非空，則將 `xyNum` 代入 `etMeasureId` (測量座標編號)。**
    - **調整 `validateRequiredFields` 函數，在編輯模式下不驗證 `gutterId` (側溝編號) 欄位。**
- **`PendingDraftsBottomSheet.kt`**:
    - 調整 BottomSheet 的最大高度為螢幕的 80%，確保其顯示在鏡頭下方。
    - 修改草稿點擊行為：現在單擊項目會直接恢復草稿到 `AddGutterBottomSheet`。
    - 調整 `showDraftActionDialog` 為僅處理刪除草稿的邏輯，現在透過長按項目觸發。
- **`PendingDraftAdapter.kt`**:
    - 新增 `onItemLongClick` 監聽器，支援長按項目以觸發刪除草稿功能。
- **`GutterInspectActivity.kt`**:
    - 修正了從 `DitchDetails` 讀取的節點資料未能正確轉換為 `Waypoint` 物件並傳遞給 `AddGutterBottomSheet` 以便編輯的問題。
    - 現在，當點擊編輯按鈕時，`GutterInspectActivity` 會將 `DitchDetails.Node` 列表轉換為 `Waypoint` 列表，然後將此列表序列化為 JSON 字串，並連同 `spiNum` 一起透過 `setResult` 傳遞給呼叫者，確保 `AddGutterBottomSheet` 能夠正確載入編輯前的資料。

## 本次結論
- 針對 `fabSubmit` 按鈕的觸發行為，已確認其驗證邏輯（需填寫基本資料與照片）及提示訊息（Toast）已正確實作於 `GutterFormActivity.kt` 的 `saveAndClose()` 函數中。
- 在 `GutterFormActivity.kt` 的 `buildAndFinishWithResult()` 函數中，`RESULT_LATITUDE` 和 `RESULT_LONGITUDE` 僅在 `basicData["coordY"]` (緯度) 和 `basicData["coordX"]` (經度) 欄位的值能成功透過 `toDoubleOrNull()` 解析為 Double 時才會被加入到回傳的 Intent 中。
- 若使用者輸入的座標值為空、非數字，或無法解析，`toDoubleOrNull()` 將回傳 `null`，導致 `RESULT_LATITUDE` 和 `RESULT_LONGITUDE` 遺失，進而影響地圖上的點位標記。
- **解決編輯模式下點位資料顯示問題**: 透過以下修改，確保從檢視畫面編輯時，點位資料能正確載入：
    - **MainActivity**: 調整 `openInspectBottomSheet` 以傳遞 WGS84 座標至 `GutterInspectActivity`。
    - **MainActivity**: 優化 `inspectLauncher` 來處理 `EXTRA_RESULT_WAYPOINTS_JSON`，確保 `AddGutterBottomSheet.newInstanceForEdit` 被正確呼叫。
    - **MainActivity**: 重構 `openWaypointForEdit` 以優先從伺服器獲取最新的點位詳情 (`nodeDetails`)，即使地圖上的 `latLng` 已存在，確保表單能完整填入所有資料，包括照片。
    - **GutterInspectActivity**: 實作了 `ditchToWaypoints` 方法，將 `DitchDetails` 轉換為 `Waypoint` 列表，並包含 WGS84 座標，然後將其以 JSON 格式和 `spiNum` 一起傳回給 `MainActivity`。
- **編輯模式下的表單行為優化**：
    - **進入編輯點位時，`GutterBasicInfoFragment` 中的「側溝編號」欄位會被隱藏。**
    - **「測量座標編號」欄位會自動帶入 API 回傳的 `XY_NUM` 資料。**

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
