# Repository Analysis

## Current Behavior

- `GutterBasicInfoFragment` 的「銜接點」目前主要只與「無法開蓋」互斥，未完整套用深度照片、頂寬度照片、溝蓋板厚度、深度、頂寬、材質、破損、懸掛、淤積、接管的欄位禁用與必填狀態。
- `WaypointAdapter` 的 `hasFilledFormData` 將 `NODE_X`、`NODE_Y`、狀態欄位及其他預填值納入判定，且沒有重用送出前的完整上傳／欄位驗證，因此空白新增項目可能被顯示為「已填寫資料」。
- 匯入流程會設定 `isImportLocked`，但定位按鈕沒有在所有 import-lock／edit-mode 轉換路徑明確重新套用 disabled；`enterEditMode()` 目前也無條件重新啟用 `cbIsVirtual`。

## Expected Behavior

- 銜接點應使用與無法開蓋一致的免填、禁用及審核判定規則。
- 新增列表的完成標籤應反映使用者真正完成的表單資料，不應被系統預填座標或預設狀態誤導。
- 匯入既有點位的定位及虛擬點狀態在返回表單、進入編輯模式及狀態恢復後均維持鎖定。

## Affected Modules

- `app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/WaypointAdapter.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/ImportExistingWaypointBottomSheet.kt`

## Dependencies

- `isImportLocked`、`isFormEditable`、`isViewMode` 與 `isVirtualMode` 的 UI 狀態同步。
- `applyCantOpenUi`、`applyConnectionMutualExclusionUi`、`updateRequiredIndicators` 及表單資料收集／審核判定。
- `Waypoint` 的 `basicData`、座標預填流程與列表 adapter 的完成狀態顯示。
- 匯入 API 回傳的 `NodeDetails` 及 `syncImportedVirtualState`。

## Risks

- 若直接複製「無法開蓋」清除資料的行為，可能誤刪銜接點原本需要保留的欄位；須先確認審核規則與欄位清單。
- 若只修正畫面 enabled 狀態而未修正 Activity 的 edit-mode／state-restore 路徑，匯入後仍可能被重新啟用。
- 若修改列表完成判定過度縮小欄位集合，可能使真正已填寫的側溝錯誤顯示為空白。
- 目前根工作樹的 `.worktrees/` 是其他 branch 的 linked worktree，不能當作本 task 的未提交 production 變更處理。

## Unknowns

- 尚未有 `docs/tasks/debug-0919-1/` 的既有 verification failure、log 或 device reproduction evidence。
- 「銜接點」共同免填欄位與「已填寫資料」的上傳條件已由需求明確決定，但仍需以測試覆蓋確認所有狀態切換。
