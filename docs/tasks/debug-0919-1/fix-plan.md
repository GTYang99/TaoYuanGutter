# Minimum Fix Plan

## Scope

只修正 AC-001～AC-004 所需的表單驗證、列表完成判定與匯入鎖定狀態；不改動無關照片上傳、定位查詢或 API contract。

## Planned Changes

1. 抽出或集中「銜接點／無法開蓋共同免填」的狀態判定，讓 `GutterBasicInfoFragment`、`AddGutterBottomSheet` 的欄位驗證與照片驗證使用同一規則。
2. 將 AC-001 指定的十項欄位納入銜接點免填範圍，並同步 UI disabled／required indicator 與送出前驗證。
3. 讓 `WaypointAdapter` 的「已填寫資料」只在與送出相同的必要欄位及 `PhotoUploadValidator.isUsableForUpload()` 條件全部成立時顯示；保留虛擬點與無法開蓋的既有例外規則。
4. 將 `btnPickLocation` 與 `cbIsVirtual` 的 enabled 狀態統一納入 `isImportLocked`、`isViewMode`、`isFormEditable` 判定，並在 click listener 增加防線。
5. 覆蓋 Activity 進入編輯模式及 `onSaveInstanceState` 恢復路徑，確保匯入點位狀態不被重新啟用或清除。

## Validation Plan

- AC-001：銜接點與無法開蓋各自驗證欄位、照片、送出前檢查及 required indicator。
- AC-002：空白／預填座標／部分填寫／所有欄位及照片可上傳四種列表狀態。
- AC-003：匯入後直接返回、進入編輯、重建 Activity 三種狀態確認定位不可用。
- AC-004：匯入虛擬點與非虛擬點，確認 checked 狀態保留、checkbox 不可用、欄位不被清除。
- 執行受影響模組的 unit／build 檢查；physical device evidence 於 verification 階段記錄。

## Implementation Result

已依本計畫完成 production implementation；實際變更與驗證限制記錄於 `execution-report.md`。
