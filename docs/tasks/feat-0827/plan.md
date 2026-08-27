# Implementation Plan

## Goal
- 新增一個主導覽容器，內含 `map` 與 `material-symbols:dashboard-rounded` 兩個 tab，預設進入原本主地圖，並串接 `/v1/dashboard/getDashboard` 完成儀表板、圓餅圖、設定複選與日期/月篩選功能。

## Scope
- 只處理登入後的主導覽容器、兩個 tab 的切換、儀表板畫面、dashboard API 串接、必要資源與測試。
- 不調整與儀表板無關的地圖業務邏輯、草稿流程、相機流程或側溝編輯流程，但需要讓 map tab 與 dashboard tab 的顯示狀態互斥。

## Entry Flow
- `LoginActivity` 維持既有登入流程，但登入成功後由 `AuthNavigator` 導向 `MainShellActivity`，不再直接進入 `MainActivity`。
- `MainShellActivity` 是登入後的唯一主入口，也是唯一的 tab host。
- `MainActivity` 不再作為 app 的第一層入口，只保留 map tab 的既有工作區邏輯來源，供 `MapWorkspaceFragment` 重用。
- `MapWorkspaceFragment` 承載原本地圖畫面與 map 專用控制項，`DashboardFragment` 承載儀表板畫面。
- 初始化時 shell 預設選中 map tab，讓使用者一登入就看到原本的主地圖。

## Affected Files
- `app/src/main/java/com/example/taoyuangutter/MainShellActivity.kt` - 新增主導覽容器，負責 tabbar、初始登入導向與 map/dashboard 切換。
- `app/src/main/java/com/example/taoyuangutter/MainActivity.kt` - 逐步退回成 map tab 的既有工作區來源，供新 shell 抽離使用。
- `app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt` - 新增 map tab 承載容器，重用現有地圖工作區與控制項。
- `app/src/main/java/com/example/taoyuangutter/dashboard/DashboardFragment.kt` - 儀表板 tab 的主畫面。
- `app/src/main/java/com/example/taoyuangutter/login/LoginActivity.kt` - 登入成功後導向新的主導覽容器。
- `app/src/main/java/com/example/taoyuangutter/login/AuthNavigator.kt` - 新增 shell 導向路徑。
- `app/src/main/res/layout/activity_main_shell.xml` - 主導覽容器 layout，包含 tabbar 與內容區。
- `app/src/main/res/layout/activity_main.xml` - 現有 map layout 轉為 map tab 內容來源，配合 fragment 容器重用。
- `app/src/main/java/com/example/taoyuangutter/api/GutterApiService.kt` - 新增 dashboard endpoint。
- `app/src/main/java/com/example/taoyuangutter/api/GutterApiModels.kt` - 新增 dashboard response model。
- `app/src/main/java/com/example/taoyuangutter/api/GutterRepository.kt` - 新增 dashboard 查詢方法與參數組裝。
- `app/src/main/java/com/example/taoyuangutter/dashboard/*` - 新增儀表板畫面、狀態管理、列表/卡片與圓餅圖元件。
- `app/src/main/res/layout/activity_dashboard.xml`、`app/src/main/res/layout/*dashboard*` - 新增儀表板主畫面與篩選/設定彈窗。
- `app/src/main/res/menu/*` - 新增 tabbar menu 資源。
- `app/src/main/res/values/strings.xml` - 新增儀表板文案、按鈕字串與錯誤提示。
- `app/src/main/res/drawable/*` - 新增 dashboard icon 與必要的背景/狀態圖示。
- `app/src/main/AndroidManifest.xml` - 註冊新的主導覽容器與儀表板畫面。
- `app/src/test/java/com/example/taoyuangutter/api/*`、`app/src/test/java/com/example/taoyuangutter/dashboard/*` - 單元測試。
- `app/src/androidTest/java/com/example/taoyuangutter/*` - tab 切換、篩選與圓餅圖 UI 驗證。

## Implementation Checklist

### Blocking fix order
- [ ] Restore the map tab so it preserves the original `MainActivity.kt` behavior instead of opening a separate under-wired map surface.
- [ ] Reconcile `MapWorkspaceFragment` with the existing map lifecycle, controller, launchers, overlays, bottom sheets, and map-only controls.
- [ ] Confirm dashboard and map tab visibility rules are truly mutually exclusive, including returning to map after dashboard navigation.
- [ ] Fix dashboard visual conformance issues called out by verification: total card gradient, group card styling/layout, filter sheet layout, and the `其他追蹤項目` heading.
- [ ] Make the dashboard filter sheet match the approved interaction model: bottom sheet presentation, centered title, close button, and empty initial state.
- [ ] Repair the connected instrumentation path so `connectedDebugAndroidTest` passes on the target device configuration.

### Implementation steps
1. Normalize the map host first, because AC-001 / AC-013 / AC-014 are the core blocker cluster and the current fragment-based map surface is incomplete.
2. Align the dashboard shell visuals with the approved requirement and Figma reference, keeping the existing data-binding logic intact where possible.
3. Rework the filter UI into the required bottom sheet flow, with mutually exclusive date/month behavior and reset-on-open behavior.
4. Tighten the tab switch state handling so dashboard active state hides map-only controls and map active state restores them without residual overlays.
5. Update or add tests only after the UI/state behavior is stable, then rerun unit and connected instrumentation verification.

## Test Plan
- 先用 unit tests 驗證 dashboard state、query 組裝、百分比/資料格式與篩選互斥邏輯。
- 再用 instrumentation tests 驗證登入後預設進入 map tab、切換到 dashboard 後 map-only controls 消失、切回 map 後原控制項恢復。
- 最後跑 `testDebugUnitTest` 與 `connectedDebugAndroidTest`，確認驗證環境中沒有 ActivityScenario / developer option 的額外阻礙。

## Regression Plan
- 確認登入後的第一個可見工作區仍是原本地圖工作流，而不是一個新的、簡化過的 map surface。
- 確認 dashboard tab 不會把 map-only bottom sheet、FAB、overlay 或相機控制帶到前景。
- 確認切回 map tab 時，原本地圖流程可完整回復，不留下殘影或半初始化狀態。
- 確認 401 邏輯仍導回登入，不會因 shell/tab host 改動而改變 auth 行為。
- 確認既有草稿、編輯與檢視流程不受主導覽容器影響。

## Risks
- 主導覽結構可能比預期更大，因為目前 app 沒有既成 tabbar 框架。
- 地圖工作區如果抽離邊界抓得太粗，可能影響原本 `MainActivity` 內的 launcher、controller 與 bottom sheet 生命週期。
- Dashboard API 的資料 key 是動態且中英混合，容易在 mapping 與 null 處理上出錯。
- 圓餅圖與卡片內容需要跟 API 狀態同步，若 state 管理不清楚，容易出現 UI 與資料不一致。
- 新增 tabbar 後可能碰到 system inset、底部導航與現有浮動按鈕的疊位問題。
- 若 map 與 dashboard 的顯示規則沒有完全互斥，可能造成 tab 切換時出現重疊控制項或空白畫面。

## Rollback Plan
- 若驗證失敗，移除 dashboard tab 與相關新畫面、回復原本登入後直接進入地圖的流程，保留既有 API 與地圖功能不變。

## Current Behavior
- 登入後主要入口只有地圖工作區，沒有儀表板 tabbar 或 dashboard 畫面。
- API 層尚未支援 dashboard endpoint 與其 response model。
- UI 資源中也沒有對應的儀表板版面、篩選彈窗或圓餅圖元件。

## Expected Behavior
- 登入後先進入主導覽容器，預設顯示 map tab，且可在 map / dashboard 兩個 tab 間切換。
- 切換到 dashboard 時會看到完整的區塊、按鈕與圓餅圖，且 map 專用控制項不會出現在前景。
- 使用者可以用「設定」切換組別顯示，用「篩選」選日期或月份，且兩種篩選方式不能同時生效。
- API 回傳後，調查長度、調查進度、組別明細與圓餅圖都會同步更新。

## Open Questions
- `MapWorkspaceFragment` 是否應該直接承接原本 `MainActivity` 的 map 工作流，還是改成更薄的容器後再逐步抽出共享狀態。
- `DashboardFragment` 的 visual polish 是否要一次對齊 Figma，或先以 blocker 修正為主、細節再分批處理。
