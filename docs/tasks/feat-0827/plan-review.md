# Plan Review

## Decision
- `REQUEST_CHANGES`

## Review Summary
- 這份 `plan.md` 已經比前一版成熟，主導覽 shell、map tab、dashboard tab 的方向也比較清楚。
- 目前還不建議直接進 implementation 的原因，不是功能方向錯，而是幾個關鍵決策還沒有寫到可以直接開工的程度。
- 只要把下面列出的補強項目補齊，這份 plan 就有機會通過 review。

## What Must Be Added

### 1. 主導覽容器的責任邊界要寫死
- 要明確寫出登入後的唯一入口是什麼。
- 要明確寫出 `MainShellActivity`、`MainActivity`、`MapWorkspaceFragment` 各自的角色。
- 要明確寫出 `LoginActivity` / `AuthNavigator` 導向哪個 activity。
- 要避免同一份 plan 同時像是「新 shell 重構」又像是「在現有 main activity 上加 tab」。

### 2. 遷移路線要補完整
- 要寫清楚現有地圖工作區哪些東西保留、哪些東西搬移、哪些東西包進 fragment。
- 要寫清楚 `MainActivity` 是否只保留 map tab 的工作區邏輯。
- 要寫清楚哪些 controller、launcher、bottom sheet、overlay 仍然掛在 map tab。
- 要寫清楚 implementation 的順序，不能只列檔名，否則開工時會有多種拆法。

### 3. Tab 切換時的 UI 共存規則要具體
- 要寫清楚 dashboard active 時，哪些 map FAB、底部面板、工具列要隱藏或停用。
- 要寫清楚 map active 時，tabbar 與內容區怎麼避開既有控制項。
- 要寫清楚 bottom inset、z-order、safe area、FAB 位置怎麼處理。
- 這一段如果不補，review 會把它視為高風險 regression。

### 4. Dashboard API 契約要可落地
- 要寫清楚 `date` 與 `monthRange` 兩種參數的送出規則。
- 要寫清楚這兩種篩選方式如何互斥，UI 層與 repository 層各自負責什麼。
- 要寫清楚動態中文 key 的 mapping 策略。
- 要寫清楚 0 值、空值、未知組別、缺少某個子欄位時的顯示規則。

### 5. 測試案例要具體到情境
- 要寫清楚登入後是否正確進入 shell。
- 要寫清楚 map tab 與 dashboard tab 切換時，原本地圖控制項是否維持正確狀態。
- 要寫清楚設定複選與篩選互斥的測試。
- 要寫清楚 401 是否仍回登入。
- 要寫清楚旋轉或重回頁面時，tab 與篩選狀態是否正確。

### 6. Open Questions 不能維持空白
- 如果已經決定好了，就把決策直接寫進 plan 內文。
- 如果還有未定案的地方，至少列出 1 到 2 個問題。
- 不要讓 `Open Questions` 維持 `無`，因為這會讓 review 以為沒有遺留決策，但其實還有。

## Blocking Issues
- 目前沒有看到要求外部決策才能繼續的 blocker。
- 這份 plan 的問題屬於「補齊後即可進 implementation」，不是「無法判斷怎麼做」。

## Recommendation
- 先補齊上面 6 項，再重新送 review。
- 補完後，這份 plan 會比較像一份可以直接交給 developer 的 implementation plan，而不是還在挑架構路線的草案。

## Overall Assessment
- `REQUEST_CHANGES`
- 目前 plan 接近可做，但還差幾個關鍵決策與落地細節。
- 補完後再進 implementation，風險會低很多。
