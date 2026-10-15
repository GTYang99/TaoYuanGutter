# Implementation Plan

## Goal

修正側溝編輯 sheet 開啟時上方可見地圖無法拖動平移的問題。

## Scope

- 重現並追蹤編輯頁地圖區的 DOWN/MOVE/UP/CANCEL 事件路由與座標。
- 依證據修正 `AddGutterBottomSheet` 與主地圖間最小必要的觸控轉送。
- 保留 sheet 表單、waypoint 編輯及既有地圖手勢行為。

## Affected Files

- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`：觸控區域辨識與事件轉送。
- `app/src/main/res/layout/bottom_sheet_add_gutter.xml`：若重現證據顯示觸控佔位來自表單根布局，才調整相關觸控區域。
- `app/src/test/` 或 `app/src/androidTest/`：依可測性加入觸控路由回歸檢查。

## Implementation Steps

1. 在編輯模式重現 AC-001，記錄 Dialog、sheet 容器、地圖可見區的螢幕邊界與觸控事件接收者。
2. 根據證據找出事件被攔截或座標錯置的原因，更新根因紀錄與最小修正範圍。
3. 修正觸控 hit test／轉送；地圖區事件以不修改原事件的複本轉成 Activity decor 座標後，完整送到主地圖，表單區事件仍留在 sheet。
4. 加入或更新可行的回歸檢查，並驗證 AC-001、AC-002。

## Test Plan

- 精準驗證編輯頁上方地圖拖曳平移，並確認 sheet 持續顯示。
- 在標題、類型選擇、waypoint 清單及底部操作區操作，確認仍由 sheet 回應。
- 驗證不同觸控起點的取消／結束事件不會污染下一次手勢。

### Physical Device Test Scope

- Requires physical device: Yes
- Device/environment: Android 裝置上的側溝編輯流程；具體裝置待執行階段指定
- In-scope Acceptance Criteria: AC-001、AC-002
- Regression risk: 地圖拖曳與 sheet 控制項之間的觸控分流
- Full regression required: No
- Full regression trigger: 無
- Stop condition: AC-001、AC-002 指定操作完成，或取得足夠失敗事件證據

| AC | Environment | Steps | Expected | Evidence |
|---|---|---|---|---|
| AC-001 | Physical device | 開啟側溝編輯頁，在上方可見地圖區拖動 | 地圖鏡頭平移且 sheet 保持開啟 | 操作結果；失敗時保留畫面與事件追蹤 |
| AC-002 | Physical device | 操作表單欄位、waypoint 清單及底部按鈕 | 對應控制項正常回應，地圖手勢不誤觸發 | 操作結果；失敗時保留畫面與事件追蹤 |

## Regression Plan

- 確認 sheet 表單內點擊及 waypoint 清單操作不會被送到地圖。
- 確認地圖區觸控結束或取消後，後續表單觸控仍正常。
- 確認既有新增 waypoint、編輯 waypoint 流程不受影響。

## Risks

- 事件轉送若使用錯誤座標框架，地圖可能收到偏移或不完整的手勢。
- BottomSheetDialog 視窗尺寸與 Android 系統事件處理可能影響重現結果。

## Rollback Plan

若回歸檢查失敗，回退本 task 的觸控路由變更並保留根因及重現證據供重新規劃。

## Current Behavior

編輯頁保留主地圖並在其上顯示 `AddGutterBottomSheet`；程式以 Dialog 的 Window.Callback 依 sheet 容器範圍選擇事件交給 Activity 或 Dialog。使用者回報可見地圖無法拖動，觸控失效的實際攔截位置尚待執行時確認。

## Expected Behavior

地圖區的完整拖曳事件由主地圖處理，sheet 表單區事件由 sheet 處理，兩種操作彼此不干擾。

## Acceptance Criteria Traceability

| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | 1、2、3 | 實機在編輯頁上方地圖拖動，確認鏡頭平移且 sheet 保持開啟 |
| AC-002 | 3、4 | 實機操作表單、waypoint 清單和底部按鈕，確認正常回應 |

## Failure Behavior

- 地圖觸控路由不可用時，不得造成 sheet 關閉、表單誤操作或殘留手勢狀態；需記錄失敗的事件序列與螢幕邊界。

## Security and Privacy

無

## Open Questions

- 根因待執行時事件追蹤確認；若實機暫不可用，使用可重現的 instrumentation／事件測試作替代證據並將未驗證項標示清楚。
