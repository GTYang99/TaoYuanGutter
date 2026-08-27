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

## Implementation Steps
1. 先定義 dashboard API 的 response model，讓「調查長度」與「調查進度」可以用穩定資料結構解析。
2. 在 repository 新增 dashboard 讀取方法，支援日期區間與月份範圍兩種互斥參數組合，並沿用現有 token 與 401 處理。
3. 建立新的主導覽容器，並把 `LoginActivity` / `AuthNavigator` 的登入成功導向改成 `MainShellActivity`，使其成為唯一入口。
4. 把既有 map 工作區抽成可重用的 `MapWorkspaceFragment`，將現有地圖、控制器、bottom sheet、overlay 與 FAB 都留在 map tab 內。
5. 定義 active tab layout state，明確規定 dashboard active 時隱藏 map 專用浮動控制項、底部面板與地圖 overlay；map active 時恢復原本地圖工作區的顯示與 inset，tabbar 本體維持常駐。
6. 建立儀表板畫面與 view model，完成標題、兩個主要區塊、動態組別切換與 API 狀態管理，並讓組別清單直接由 response keys 動態推導。
7. 實作「設定」複選清單與「篩選」彈窗，確保日期區間與月份範圍只能擇一送出，且每次開啟都回到空狀態，不保留上次選擇。
8. 實作圓餅圖元件與卡片/清單版面，讓調查進度、狀態與組別公里數依 API 回傳即時更新，並套用固定色碼（待匯入座標 `#FFC300`、待繪製 `#1962FF`、待修正 `#FF58E0`、已完成 `#000000`）。
9. 補齊字串、圖示、背景與版面資源，並調整不同螢幕寬度下的排版與 tabbar safe area。
10. 新增單元測試與 instrumentation 測試，覆蓋 API mapping、互斥篩選、tab 切換、active tab 可見性、登入導向與主要 UI 狀態。

## Test Plan
- 新增/更新 repository 與 model 的 unit tests，驗證 dashboard JSON mapping 與 query parameter 組裝。
- 新增 dashboard state 的 unit tests，驗證設定複選、全部組別單選與日期/月篩選互斥。
- 補一組 instrumentation tests，驗證登入後會進入 shell、預設是 map tab、dashboard tab 可切換、map controls 會隨 tab 顯示/隱藏、篩選彈窗可開啟、圓餅圖與主要卡片可顯示。
- 跑既有的主要單元測試，確認登入、地圖與草稿流程沒有被新導覽影響，且 401 仍能回到登入邏輯。

## Regression Plan
- 確認登入後一定先進 shell 並預設停在 map tab，原本的地圖工作區仍可正常進入。
- 確認切到 dashboard 時，既有地圖按鈕、底部面板、overlay 與 camera / bottom sheet 不會殘留在前景。
- 確認切回 map tab 時，原本地圖按鈕、底部面板與相機流程恢復正常。
- 確認 401 仍會回到登入邏輯，不會讓 dashboard 卡在空白或錯誤頁。
- 確認既有 API、草稿、編輯與檢視流程不受新增 tabbar 影響。

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
- `MapWorkspaceFragment` 與 `DashboardFragment` 是否需要共用同一個 activity-scoped ViewModel 來保存 tab 切換期間的 UI 狀態，或各自維持獨立 state 即可。
