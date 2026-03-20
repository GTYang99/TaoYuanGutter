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
- **`FragmentGutterPhotos.kt`**:
    - 實作了照片選擇與顯示功能，最多可新增三張照片。
    - 新增 `validateAllPhotos()` 方法，用於檢查三個照片欄位是否皆已填寫。
    - 新增 `getPhotoPaths()` 方法，用於回傳所選照片的 URI。
    - 調整了 UI 元素的顯示邏輯，以支援編輯模式和檢視模式下的照片操作。
- **`GutterFormPagerAdapter.kt`**:
    - 更新以正確取得 `GutterBasicInfoFragment` 和 `FragmentGutterPhotos` 的實例。

## 本次結論
- 針對 `fabSubmit` 按鈕的觸發行為，已確認其驗證邏輯（需填寫基本資料與照片）及提示訊息（Toast）已正確實作於 `GutterFormActivity.kt` 的 `saveAndClose()` 函數中。
- 在 `GutterFormActivity.kt` 的 `buildAndFinishWithResult()` 函數中，`RESULT_LATITUDE` 和 `RESULT_LONGITUDE` 僅在 `basicData["coordY"]` (緯度) 和 `basicData["coordX"]` (經度) 欄位的值能成功透過 `toDoubleOrNull()` 解析為 Double 時才會被加入到回傳的 Intent 中。
- 若使用者輸入的座標值為空、非數字，或無法解析，`toDoubleOrNull()` 將回傳 `null`，導致 `RESULT_LATITUDE` 和 `RESULT_LONGITUDE` 遺失，進而影響地圖上的點位標記。
- 關於「編輯完點位後，在 `AddGutterBottomSheet` 中起點資料未正確更新」的問題，經過分析，問題點最可能出在 `MainActivity` (或實作 `LocationPickerHost` 的地方) 未能正確更新其維護的 `Waypoint` 列表，或是 `getInspectWaypoints()` 方法未能返回最新的 `Waypoint` 資料給 `AddGutterBottomSheet`。`AddGutterBottomSheet` 本身接收和顯示資料的邏輯是正確的。

## 注意事項
- minSdk 26
- 使用 ViewBinding

## 下次待處理事項
- **照片管理與儲存**:
    - 進一步完善 `FragmentGutterPhotos` 中的照片處理邏輯，例如：
        - 考慮加入相機直接拍攝功能。
        - 實作圖片壓縮或大小限制。
        - 確保照片 URI 的長期有效性，特別是在不同情境下（如背景儲存、重啟 App）。
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
