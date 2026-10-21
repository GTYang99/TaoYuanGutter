# Issue Log

## ISS-DBG-1021-001

- task_id: debug-1021
- phase: debug
- category: implementation_regression
- priority: P2
- title: 新增模式資料/照片不足 Dialog 關閉後送出按鈕未恢復
- status: open
- impact: 新增表單在一次必填驗證失敗後無法再透過送出按鈕重試；按鈕持續呈現 loading 狀態。
- repro_steps:
  - 開啟新增側溝表單。
  - 填入起點/終點及側溝類型，但保留必要欄位或照片不足。
  - 點擊「新增側溝」。
  - 在「資料/照片不足」Dialog 按「確定」。
- expected: Dialog 關閉後，按鈕顯示「新增側溝」且可點擊。
- actual: 按鈕仍顯示「上傳中…」且 disabled。
- evidence:
  - app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt:1100-1108
  - app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt:1893-1895
  - app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt:2010-2018
- next_action: implementation_debug

