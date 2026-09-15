# Repository Analysis

## Current Behavior
- `GutterBasicInfoFragment` 以 `etRemarks` 顯示並編輯備註；初次載入會從 `NODE_NOTE` 預填，`collectData()` 將文字原樣回收為 `NODE_NOTE`。
- `setupDraftWatchers()` 已監聽 `etRemarks` 的文字變更，因此以程式設定文字也會走既有草稿變更通知。
- `setEditable()` 對 `etRemarks` 與 `tilRemarks` 套用檢視／匯入鎖定的可編輯狀態；虛擬點模式目前隱藏整個備註區塊。

## Expected Behavior
- 在可填寫的備註欄附近呈現五個可操作的預設內容膠囊。
- 膠囊操作應更新同一個 `etRemarks`，讓既有草稿、重建與提交管線自然保留結果。
- 在備註本來不可見或不可編輯的模式，膠囊必須有相同的可見性與可操作限制。

## Affected Modules
- `app/src/main/res/layout/fragment_gutter_basic_info.xml`：在 `tilRemarks` 前後增加膠囊容器與五個預設項目。
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt`：設定點擊行為、依可編輯／虛擬／匯入狀態同步膠囊狀態，並避免破壞既有草稿通知。
- `app/src/main/res/values/strings.xml`：新增可本地化的膠囊文案及必要的無障礙描述。
- `app/src/androidTest/java/com/example/taoyuangutter/GutterBasicInfoUiTest.kt`：覆蓋膠囊呈現、填入及特殊模式的回歸行為。

## Dependencies
- `FragmentGutterBasicInfoBinding` 會由 layout 的 view ID 自動產生 binding；新增 view 後需採用穩定且語意明確的 ID。
- Material Components 已是 app dependency；可重用 Material 的按鈕／選取樣式，不需新增第三方元件。
- `GutterFormContract`、`GutterFormActivity`、`GutterSessionRepository` 和 `GutterRepository` 已依 `NODE_NOTE` 傳遞資料，預期不需修改。
- `reorderEditableSections()` 會將 `tvRemarksTitle` 與 `tilRemarks` 移到可編輯表單的排序位置；新增的膠囊容器必須納入此排序，避免顯示位置被移動流程破壞。

## Risks
- 預設內容須以中文逗號「，」附加，且同一預設內容只能出現一次；去重需按逗號分段後比對完整預設文字，避免以子字串判斷誤刪使用者自由輸入。
- 若膠囊未與 `setEditable()`、`setVirtualMode()` 和 `reorderEditableSections()` 同步，可能在檢視／鎖定狀態仍可寫入，或在虛擬點模式殘留顯示。
- 將程式化填入直接寫入 `EditText` 會觸發既有草稿 watcher；實作需確保一次操作不造成意外的重複字串或焦點／鍵盤回歸。
- `strings.xml` 已有其他未提交修改；本任務實作前必須以最小且不重疊的 patch 處理。

## Unknowns
- 無。
