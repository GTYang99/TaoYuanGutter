# Implementation Plan

## Goal
- 在側溝表單的備註欄提供五個預設內容膠囊，並讓其結果安全地沿用既有 `NODE_NOTE` 資料流程。

## Scope
- 僅調整 `GutterBasicInfoFragment` 的備註區塊、必要字串與對應 UI 測試；不改動 API、草稿 schema、無側溝點位面板或其他表單欄位。

## Affected Files
- `app/src/main/res/layout/fragment_gutter_basic_info.xml`：新增備註預設內容的膠囊容器與可識別 view ID。
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt`：綁定五個膠囊、以核准規則更新 `etRemarks`，並將狀態整合至可編輯／虛擬／匯入流程與表單重排。
- `app/src/main/res/values/strings.xml`：新增五項預設文字與必要無障礙字串。
- `app/src/androidTest/java/com/example/taoyuangutter/GutterBasicInfoUiTest.kt`：新增 UI／流程回歸測試。

## Implementation Steps
1. 在備註輸入框附近新增五個 Material 風格膠囊，使用字串資源提供花圃、焊接、車擋、水泥封邊與螺絲固定，並設置清楚的 content description。
2. 在 `GutterBasicInfoFragment` 集中設定膠囊 click listener，僅在表單可編輯時更新 `etRemarks`：以中文逗號「，」附加預設內容，並按逗號分段比對完整文字，同一項目只加入一次。
3. 將膠囊容器納入 `reorderEditableSections()`、`setEditable()`、`setVirtualMode()` 與匯入鎖定的可見／啟用狀態，使它與既有備註欄完全一致。
4. 驗證既有 `etRemarks` watcher 會在膠囊更新後通知草稿變更；不新增平行資料欄位，並以表單重建／`collectData()` 確認保留 `NODE_NOTE`。
5. 擴充 instrumentation test，覆蓋五項顯示、每個膠囊的附加結果、中文逗號分隔、不重複加入、手動補充、草稿／重建資料，以及檢視、匯入鎖定與虛擬點限制。
6. 執行 targeted Android instrumentation test、相關 unit test 與 app build，記錄實際命令、結果及任何無法驗證的項目。

## Test Plan
- `GutterBasicInfoUiTest`：確認五個膠囊在一般可編輯表單顯示且文字正確；逐一點擊並驗證 `etRemarks` 結果符合核准規則。
- `GutterBasicInfoUiTest`：在已有自由文字與多個預設內容後操作膠囊，驗證使用中文逗號附加、可繼續編輯，以及重複點擊不會加入第二次。
- Fragment／activity 測試：由 `basicData["NODE_NOTE"]` 預填、操作膠囊、活動重建後驗證 `collectData()["NODE_NOTE"]` 與草稿通知結果。
- 回歸測試：檢視模式與匯入鎖定不可透過膠囊變更資料；虛擬點模式不顯示備註標題、輸入框或膠囊。
- 執行 `./gradlew test`、相關 connected Android test 及 `./gradlew assembleDebug`；若裝置或環境不可用，標示 `NOT VERIFIED`。

## Regression Plan
- 確認既有備註的預填、自由輸入、`NODE_NOTE` 收集、草稿保存、重建與 API 提交流程不變。
- 確認 `reorderEditableSections()` 維持備註區在既有欄位排序中的位置。
- 確認虛擬點、檢視模式、匯入鎖定及「無法開蓋」等相鄰表單狀態不受影響。
- 確認不改變 `GutterFormContract`、Room draft schema、`StoreDitchNodeRequestMapper` 或 API payload。

## Risks
- 動態重排 layout 時若遺漏膠囊容器，可能造成它與備註輸入框分離或在不同模式顯示不一致。
- 現有工作樹含未提交的 `strings.xml` 變更；實作時需以目前內容為基礎，避免覆寫他人修改。

## Rollback Plan
- 回復此任務的單一實作 commit，即可移除膠囊 UI 與行為而保留既有 `NODE_NOTE` 資料。

## Current Behavior 
- 表單備註僅有一個可多行手動輸入的 `etRemarks`。
- 備註會由 `NODE_NOTE` 預填並由 `collectData()` 原樣收集，已有草稿 watcher。

## Expected Behavior
- 可編輯表單在備註欄提供五個預設內容膠囊。
- 使用者可用核准的填入規則快速加入內容，之後仍可自由編輯；資料仍只走 `NODE_NOTE`。

## Acceptance Criteria Traceability
| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | 1、3 | `GutterBasicInfoUiTest` 顯示／文案檢查 |
| AC-002 | 2、5 | 每項膠囊、中文逗號、自由文字與不重複加入 UI 測試 |
| AC-003 | 2、4、5 | `collectData()`、活動重建與草稿通知流程測試 |
| AC-004 | 3、5 | 檢視、匯入鎖定與虛擬點回歸測試 |

## Failure Behavior
- 本功能不發出獨立網路請求；若表單不可編輯，膠囊不得變更 `etRemarks`。
- 重複點擊已存在的預設內容時不變更文字；不應顯示錯誤或發出獨立網路請求。

## Security and Privacy
- 不新增權限、持久化欄位或網路資料；預設文字與使用者備註繼續依既有 app 私有草稿及提交流程處理。

## Open Questions
- 無。
