# Repository Analysis

## Current Behavior

- AddGutterBottomSheet 新增模式的送出 listener 在非同步驗證前，將 btnSubmitGutter 設為 disabled 並將文字設為「上傳中…」。
- 必要欄位或照片驗證失敗時，validateWaypointPhotosAndFieldsOrAlert 顯示「資料/照片不足」Dialog 並回傳 false；listener 隨即呼叫 updateSubmitButtonState() 後退出 coroutine。
- updateSubmitButtonState() 在新增模式會立即 return，因此這條失敗路徑不會恢復按鈕狀態。Dialog 的「確定」按鈕沒有額外狀態回呼。

## Expected Behavior

- 保留目前缺資料/照片提示內容。
- 失敗路徑離開 coroutine 前，恢復新增模式的送出按鈕文字與 enabled 狀態；關閉 Dialog 後使用者可以修正表單並再次送出。

## Affected Modules

- app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt：送出 listener、送出按鈕狀態更新、必要資料/照片驗證 Dialog。
- app/src/main/res/layout/bottom_sheet_add_gutter.xml：提供按鈕初始文字及 btnSubmitGutter 元件；未發現 XML 是狀態卡住的原因。

## Dependencies

- GutterCompletionPolicy：決定必要欄位及照片槽位。
- PhotoUploadValidator：判斷照片來源是否可供上傳。
- lifecycleScope coroutine：送出前同步草稿與執行必要資料/照片驗證。

## Risks

- 修正需只清除本次送出期間套用的 loading 狀態，避免覆蓋編輯模式的按鈕規則或其他 loading 狀態。
- 驗證必須仍發生在照片補傳與 storeDitch API 呼叫之前。

## Unknowns

- 尚未取得實機重現錄影或 logcat；根因由目前分支的同步程式路徑確認，尚未做裝置操作驗證。

