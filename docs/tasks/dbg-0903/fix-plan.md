# Fix Plan

Date: 2026-09-03
Task: DBG-0903
Phase: debug

## Goal

修正登入後主地圖第一次不自動載入側溝，以及 `rvWaypoints` 點選無法穩定跳轉到 `GutterFormActivity` 的問題。

## Scope

- 只改登入後主地圖載入時機、底部列表點擊路由與必要的驗證測試。
- 不改需求內容，不調整側溝資料結構，不做無關重構。

## Affected Files

- `app/src/main/java/com/example/taoyuangutter/MainActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormNavigator.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`
- `app/src/main/res/layout/bottom_sheet_add_gutter.xml`
- `app/src/main/res/layout/activity_gutter_form.xml`

## Implementation Steps

1. 先在主地圖登入後流程補齊一次明確的 scope reload 觸發點，讓定位完成後不必等使用者手動拖動地圖。
2. 檢查 `MainActivity` 與 `MapWorkspaceFragment` 的 reload 條件是否被 camera idle、busy 狀態或遮罩狀態擋住，必要時把首次載入與手動拖曳載入拆開。
3. 把 `rvWaypoints` 的點擊處理改成列表項目本身的明確 click callback，避免只靠 `GestureDetector` 攔截整個 RecyclerView。
4. 確認每個 waypoint 點擊後都穩定進到宿主回呼，再導向 `GutterFormActivity`。
5. 補上實機驗證重點，確認兩個問題都不再需要額外操作才會出現預期畫面。

## Test Plan

- 登入後觀察主地圖是否在首次定位完成後自動載入側溝，不需要再拖動地圖。
- 實機點選 `AddGutterBottomSheet` 的 `rvWaypoints` 每一列，確認都會打開對應的表單頁。
- 重新進出側溝流程一次，確認沒有影響原本的新增 / 編輯 / 檢視切換。

## Regression Plan

- 確認手動拖曳地圖時，原本的 viewport reload 仍然正常。
- 確認底部列表的拖曳排序、刪除按鈕與虛擬點操作沒有被新的點擊處理干擾。
- 確認 `GutterFormActivity` 的既有資料回填與返回流程沒有改壞。

## Risks

- 若首次載入與手動拖曳共用同一條 reload 路徑，容易產生重複 API 呼叫。
- 點擊事件改成 item click 後，可能需要保留拖曳把手的獨立觸控區，避免與排序功能衝突。
- 若主地圖目前的 busy 狀態判斷過嚴，首次定位後的 reload 仍可能被擋住。

## Rollback Plan

- 若驗證失敗，先回退點擊路由與首次 reload 的變更，只保留既有載入流程。

## Current Behavior

- 登入後地圖先顯示預設鏡頭，再跳到使用者位置。
- 側溝清單不會在那一刻穩定載入，常要等使用者再移動地圖才會觸發。
- `rvWaypoints` 在實機點選時沒有穩定導到 `GutterFormActivity`。

## Expected Behavior

- 登入完成並定位後，主地圖應自動載入目前視野內的側溝。
- 使用者不需要額外拖動畫面，側溝就會出現。
- 點選 `rvWaypoints` 任一列都能穩定進入表單頁。

## Open Questions

- 目前首次定位後，真正擋住 reload 的條件是 camera idle、busy 狀態，還是 location callback 的順序。
- `rvWaypoints` 的 tap 被吃掉，是 RecyclerView 事件分派問題，還是 item 內部子元件攔截了點擊。
