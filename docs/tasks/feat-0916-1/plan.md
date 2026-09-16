# Implementation Plan

## Planning Baseline
- Branch: `feat/表單合併測距功能`
- Base revision: `ac1eacbc236385e131b523ac63418e278768ec20` (`ac1eacb`)

## Goal
- 在新增側溝清單與側溝編輯面板期間，安全地啟用既有主地圖測距模式，並依來源恢復正確面板與側溝工作圖層。

## Scope
- 僅處理測距入口的可見／可點、面板暫時收起與回復、工作圖層可見性，以及相應測試；不變更草稿、上傳、側溝資料或地圖測距演算法。

## Affected Files
- `app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt`：加入測距啟動來源與可逆回復編排。
- `app/src/main/java/com/example/taoyuangutter/main/MeasureModeUiController.kt`：若需要，擴充既有控制器以支援工作流回復時機。
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterListBottomSheet.kt`：依 OQ-001 決策提供清單的測距入口，以及新增 `hideForMeasure()`／`showAfterMeasure()` 的非 destructive 暫時收起／回復合約。
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`：依 OQ-001 決策提供編輯面板的測距入口及暫時收起／回復合約。
- `app/src/main/res/layout/bottom_sheet_add_gutter_list.xml`、`app/src/main/res/layout/bottom_sheet_add_gutter.xml`：新增可辨識、可測試的測距代理入口。
- `app/src/main/res/layout/activity_main.xml`：僅維持既有主地圖測距面板與控制項；本工項不以它承載 Dialog 上方的代理入口。
- `app/src/androidTest/java/com/example/taoyuangutter/MainShellActivityTest.kt` 及／或新增聚焦測試：覆蓋來源、回復與既有量測行為。

## Implementation Steps
- 1. 依已確認的 OQ-001 至 OQ-003，將面板代理入口、scope 圖層策略與返回鍵規則轉為可測試的行為。
- 2. 在 `MapWorkspaceFragment` 建立明確的測距啟動來源狀態，保存要回復的清單／編輯面板與目前工作側溝資料，不以 `dismiss()` 破壞面板生命週期。
- 3. 在兩個面板 layout 加入具 content description／id 的代理入口，並以各自的 host callback 請求測距；`AddGutterListBottomSheet` 新增 `hideForMeasure()`／`showAfterMeasure()`，以 editor sheet 現有 Dialog decor visibility 與 translation 動畫模式暫時隱藏／顯示，但絕不呼叫 cancel、dismiss、`confirmClose()` 或建立新的 list fragment；`AddGutterBottomSheet` 沿用既有 `hideSelf()`／`showSelf()`。
- 4. 由 `MapWorkspaceFragment` 集中執行進入順序：記錄來源與同一個 `addGutterListSheet`／`activeSheet` 實體 → 呼叫來源的可逆隱藏 API → 清單來源清除工作圖層並強制隱藏 scope 線段，或編輯來源保留工作圖層 → 啟動既有測距。若 FragmentManager 已保存狀態或來源實體已不再加入，停止回復、不做新 transaction，交由既有狀態重建流程處理。
- 5. 在 `MapWorkspaceFragment` 將正常 `setOnMapClickListener { handleMainMapTap(...) }` 抽為單一安裝函式；退出時先讓 `DistanceMeasureManager.exit()` 清除量測 listener／疊加，再重裝正常 map-click listener，最後才回復來源面板。既有 camera-move-started、marker、polyline 與 camera-idle listener 不由測距 manager 覆寫，維持原註冊。
- 6. 清單來源退出時依 `mapOverlayController.currentState().showPlan` 重算 scope 線段可見性，而非一律顯示；量測期間若 `onOverlayTogglesChanged` 更新偏好，僅更新 overlay state，仍強制 scope 線段隱藏至退出時才套用最新偏好。
- 7. 編輯來源退出時保留現有工作側溝線段與節點並回復相同面板；清單來源則保持工作圖層不顯示並回復相同清單，不遺失 waypoint 或草稿資料。
- 8. 以 `viewLifecycleOwner` 註冊 `OnBackPressedCallback`，僅在「由清單或編輯面板啟動且測距中」啟用；callback 必須呼叫同一退出／回復流程、消費 Back，並在回復完成後停用，避免觸發 bottom sheet cancel／dismiss 或 `MainShellActivity` 的預設返回行為。
- 9. 新增／調整可重複的測試，並執行編譯、單元測試與已登入裝置的手動地圖 smoke。

## Test Plan
- 針對來源狀態／圖層策略建立可單元測試的 host 邊界，驗證清單、編輯、退出與重複退出的轉換。
- 驗證 `DistanceMeasureManager.exit()` 後由 host 重裝正常 map-click listener，並以聚焦 UI／整合測試確認一般地圖點擊仍進入 `handleMainMapTap`。
- 驗證 scope 線段在清單量測中強制隱藏、原本 `showPlan=false` 時退出後維持隱藏，以及量測期間更新 `showPlan` 偏好後依最新值回復。
- 驗證兩來源的 Android Back 都由測距 callback 消費、關閉測距並回到同一面板，未出現清單關閉確認或編輯 sheet 的 `onDismiss` 清理。
- 為清單面板加入聚焦測試：`hideForMeasure()` 後 `showAfterMeasure()` 恢復同一 fragment／draft list，且未呼叫 `confirmClose()`、`onCancel()`、`onAddGutterListConfirmedClose()` 或任何 Fragment transaction。
- 執行 `:app:testDebugUnitTest`、`:app:assembleDebug` 與聚焦 `MainShellActivityTest`。
- 在已登入實體裝置逐一驗證清單與編輯兩條流程：啟動、設定起點、移動地圖、重設、關閉、回復面板與資料。

## Regression Plan
- 驗證多側溝清單的新增、選取、返回、關閉確認與草稿保存。
- 驗證單筆編輯時 waypoint、線段、節點圖示及地圖點擊／marker 選取在測距退出後仍正確。
- 驗證已隱藏的主地圖計畫圖層不會因清單量測退出而被錯誤顯示。
- 驗證主地圖既有測距、回報無側溝、圖層與其他 FAB 的互斥啟用狀態。

## Risks
- 面板代理入口與主 Activity 測距控制器需共用同一模式狀態，避免兩個入口顯示不同步。
- 以 dismiss 實現「向下收起」會誤觸既有 onDismiss 副作用；清單必須使用新增的 `hideForMeasure()`／`showAfterMeasure()`，編輯面板使用既有 `hideSelf()`／`showSelf()`。
- 工作圖層清理與測距疊加清理的順序錯誤會造成編輯圖層遺失或地圖 listener 未恢復。

## Rollback Plan
- 將本功能限制於獨立提交；若回復流程造成回歸，可回退該提交並恢復原本面板與測距彼此互斥的行為。

## Current Behavior
- 測距僅由 Activity 版面中的主地圖 FAB 啟動；兩個 BottomSheetDialogFragment 開啟時，該按鈕被 Dialog 視窗遮住。
- 現有測距退出會清除量測起點與虛線，但未保存或回復底部面板來源。

## Expected Behavior
- 兩個面板皆能觸發同一測距體驗，關閉後無縫回到啟動來源。
- 清單來源不顯示工作側溝，編輯來源保留工作側溝，且二者均不影響草稿與表單資料。

## Acceptance Criteria Traceability
| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | 1, 2, 3 | 已登入裝置確認兩面板中代理入口可見、可點；聚焦 UI 測試。 |
| AC-002 | 2, 3, 4, 5, 6, 8 | 清單流程手動 smoke：量測、重設、關閉與 Back；確認無工作圖層、全部 scope 側溝在量測中隱藏，並依最新 `showPlan` 偏好回復。 |
| AC-003 | 2, 3, 4, 5, 7, 8 | 編輯流程手動 smoke：量測、重設、關閉與 Back；確認線段／節點與資料保留。 |
| AC-004 | 5, 8, 9 | 單元／UI 測試與手動檢查，確認測距疊加清除、Back 消費及一般 map 點擊恢復。 |
| AC-005 | 6, 7, 9 | 多草稿、單筆編輯、圖層偏好與主地圖控制項回歸測試。 |

## Failure Behavior
- 地圖尚未初始化時保持既有按鈕無動作／不可用保護，不收起面板。
- 面板或 Fragment 狀態已保存時，不執行 Fragment transaction；保留目前 UI，避免資料或草稿被意外 dismiss。

## Security and Privacy
- 不新增網路請求、權限、個資收集或憑證存取；僅使用既有地圖座標的本地測距行為。

## Open Questions
- OQ-001：**已決定（2026-09-16）**：採面板內同功能代理入口。
- OQ-002：**已決定（2026-09-16）**：清單量測期間隱藏所有 scope 側溝圖層，退出後恢復。
- OQ-003：**已決定（2026-09-16）**：系統返回鍵先退出測距並還原原面板。
