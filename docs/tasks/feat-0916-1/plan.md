# Implementation Plan

## Goal
- 以唯一、可點選的主地圖 `btnMeasureDistance` 取代面板代理入口；目標面板開啟時只顯示該按鈕，並維持兩種來源的量測回復行為。

## Scope
- 調整主地圖控制項位置／visibility、Dialog 外部觸控轉送、主按鈕來源路由與既有測距整合；不變更測距計算、草稿、上傳或側溝資料。

## Affected Files
- `app/src/main/res/layout/activity_main.xml`：把 `btnMeasureDistance` 移至右側控制列最上方。
- `app/src/main/java/com/example/taoyuangutter/main/MainBlockingUiController.kt`：建立正常、面板測距唯一可見、blocking、測距中的明確控制項 policy。
- `app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt`：從主按鈕解析清單／可編輯表單來源，套用／解除 policy 並保留既有量測回復。
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterListBottomSheet.kt`：移除代理 callback，加入外部觸控轉送，保留 `hideForMeasure()`／`showAfterMeasure()`。
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`：移除代理 callback，沿用外部觸控轉送及 `hideSelf()`／`showSelf()`。
- `app/src/main/res/layout/bottom_sheet_add_gutter_list.xml`、`app/src/main/res/layout/bottom_sheet_add_gutter.xml`：移除代理測距 view。

## Implementation Steps
- 1. 移除兩個面板的代理測距 UI、binding callback 與 host callback；保留可逆收起／回復 API。
- 2. 將既有 `btnMeasureDistance` 移至右側控制列第一個位置，位於登出區下方及兩種面板上緣以外，保留既有 id、圖示與 click listener。
- 3. 在 `MainBlockingUiController` 建立集中、可重算的控制項 policy：正常時恢復全部；目標面板正常顯示時隱藏其他控制項、只顯示／啟用測距；blocking 優先並禁用測距。
- 4. 在 `MapWorkspaceFragment` 的清單／表單顯示、收起、dismiss、量測與 blocking 狀態切換時套用 policy；確保 inspect-only flow 不套用此規則。
- 5. 為清單面板加入與編輯面板相同的外部觸控轉送：僅將 ACTION_DOWN 在 sheet content 外的完整手勢轉送 `requireActivity()`。
- 6. 主按鈕點擊：量測中則退出；否則依可見來源（先清單、後可編輯 `activeSheet`）呼叫其可逆收起 API 再進入來源專屬量測。沒有目標面板時保留主地圖原有量測。
- 7. 保留退出順序：清理測距、重裝正常 map-click listener、清單依最新 `showPlan` 回復 scope、編輯保留工作圖層、最後回復同一面板；Back 走同一路徑。
- 8. 增加控制項 policy、無代理按鈕、主按鈕位置、觸控轉送與兩來源量測的測試；執行 build、unit、instrumentation 與已登入裝置 smoke。

## Test Plan
- Layout/UI：兩面板不含代理測距 id；`btnMeasureDistance` 是右側第一個控制項。
- 兩面板：只有 `btnMeasureDistance` visible/enabled，其餘主地圖控制項 GONE；loading 時測距不開放。
- 主按鈕觸控轉送不觸發 cancel、dismiss、close confirmation 或草稿清理。
- 清單／編輯的量測、重設、關閉與 Back，驗證 listener、`showPlan`、工作圖層和面板回復。
- `:app:testDebugUnitTest`、`:app:assembleDebug`、聚焦 instrumentation、已登入實體裝置 smoke。

## Regression Plan
- 多草稿清單、新增／一般編輯、inspect-only、無側溝、upload blocking、登入／登出與主地圖非面板控制項恢復。

## Risks
- 控制項 visibility 必須集中管理，避免 lifecycle 或 loading 過程遺留錯誤狀態。
- 外部觸控轉送不能影響 sheet 內容操作。
- 先前已提交的代理按鈕實作與本計畫衝突，必須移除而非並存。

## Rollback Plan
- 此需求修訂以獨立提交實作；回退後恢復前一代理入口行為。

## Acceptance Criteria Traceability
| AC | Steps | Validation |
|---|---|---|
| AC-001 | 1-4 | Layout/UI 與裝置確認唯一可見主按鈕。 |
| AC-002 | 2, 5, 6 | 兩面板主按鈕觸控轉送與無 cancel/dismiss 測試。 |
| AC-003 | 3-7 | 清單量測、scope 隱藏／偏好回復、Back smoke。 |
| AC-004 | 3-7 | 編輯量測、工作圖層保留與 Back smoke。 |
| AC-005 | 3, 4, 7, 8 | 回歸、listener、草稿、blocking 與控制項恢復測試。 |

## Failure Behavior
- 地圖未初始化、來源不可用或 blocking overlay 顯示時，不收起面板、不啟動測距。
- Fragment 狀態已保存時不進行新 transaction，保留現有 UI／資料交由狀態重建。

## Security and Privacy
- 不新增網路請求、權限或個資處理。

## Open Questions
- 無。
