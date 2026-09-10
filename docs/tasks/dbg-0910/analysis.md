# Repository Analysis

## Task Classification

- `debug`：本次輸入同時包含三個症狀，且匯入照片問題尚缺重現證據；先完成 root-cause investigation，再進行最小修正。

## Current Behavior

- `AddGutterBottomSheet.setupBottomSheetBehavior()` 將 sheet 高度與 `peekHeight` 設為螢幕高度的 1/2。
- `validateWaypointPhotosAndFieldsOrAlert()` 以 `MAT_TYP`、`IS_BROKEN`、`IS_HANGING`、`IS_SILT` 等 raw key 組成缺少欄位提示，未經完整中文顯示映射。
- `GutterFormActivity.handleImportedNodeDetails()` 先回填欄位，再非同步下載照片；下載後才回填 Fragment photo slots 與 Activity `currentFormData`。目前「無法開蓋」首開不能確認、返回後正常的實際競態尚待重現。

## Expected Behavior

- 上傳前審核的四個指定欄位使用中文名稱提示，且不改變內部 field key 或 request payload。
- Add gutter sheet 可視高度約為螢幕高度 60%，地圖可視區與底部操作仍正確。
- 匯入「無法開蓋」點位後，slot 1／fileCategory 1 的概況照片完成本機化、顯示並可直接確認；一般匯入仍保留三張照片與 metadata。

## Affected Modules

- `gutter/AddGutterBottomSheet.kt`：validation label mapping、sheet height。
- `gutter/GutterFormActivity.kt`：import download sequencing、authoritative photo state、confirmation readiness。
- `gutter/GutterBasicInfoFragment.kt`：photo rendering、cant-open validation and locked-state behavior。
- `gutter/GutterFormContract.kt`：只在證據證明 Intent/result 傳遞遺漏時調整；key contract 維持不變。
- `common/UploadFailureClassifier.kt`：僅在 review 顯示流程證實需要共用中文分類時考慮，預設不納入。
- `app/src/androidTest`、`app/src/test`：新增失敗前測試與 regression coverage。

## Dependencies

- `NodeDetails.nodeImg.fileCategory`、remote URL、image ID。
- `GutterRepository.downloadImageToLocalContentUri()` 與 `PhotoUploadValidator`。
- `prefillDataFromImport()`、`prefillPhotos()`、`updateCurrentFormPhotos()`、`currentFormSnapshot()`。
- `IS_CANTOPEN` 的 slot 1 必填／slot 2、3 非必填規則。
- BottomSheet behavior callback 與 `onSheetViewportInsetChanged()` 地圖 viewport 更新。

## Risks

- 非同步下載完成前後的 lifecycle／binding 狀態造成照片只更新其中一個 owner。
- 修正 import state 時誤清掉既有 image ID、capturedAt 或 upload state。
- 60% 高度可能影響 RecyclerView 滾動、底部按鈕、地圖觸控路由或 insets。
- 目前 source 與 `feat-0910-1` artifact 的 cant-open restore 行為不一致；本 task 依核准行為「取消後不回填」保護，不能以本次 debug 偷渡另一種產品決策。

## Unknowns

- 首開失敗是否是 `prefillPhotos()` 未執行、Activity state 被後續同步覆蓋、照片 URI 不可讀，或確認按鈕在下載完成前被判定為 disabled。
- 中文提示的正式文案，以及「一張照片」是否固定為 slot 1/fileCategory 1。
