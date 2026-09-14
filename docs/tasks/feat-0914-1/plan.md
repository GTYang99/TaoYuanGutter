# Implementation Plan

## Goal

- 依 Figma 將儀表板改為登入帳號的今日／累積里程與日期區間搜尋，且只顯示該帳號的里程。

## Scope

- 只修改登入 username 保存、dashboard 個人里程解析、dashboard UI／狀態、必要資源與測試；不變更地圖、側溝編輯、後端 API 或既有導航結構。

## Affected Files

- `login/LoginActivity.kt`、`login/AuthNavigator.kt`：新增 username preference getter、保存及清除。
- `api/DashboardModels.kt`：提供從 `surveyLength` 動態帳號 key 解析單一 username 里程的純函式／模型輔助。
- `dashboard/DashboardViewModel.kt`：建立今日、累積與搜尋的 query／UI state；以 token 和 username 載入資料。
- `dashboard/DashboardFragment.kt`：頁內日期選擇、搜尋、清除、loading／錯誤／空結果與 Figma 版面渲染。
- `res/layout/activity_dashboard.xml`、`res/drawable/*dashboard*`、`res/values/strings.xml`、`res/values/colors.xml`：新版面與視覺資源。
- `api/DashboardModelsTest.kt`、`dashboard/DashboardViewModelTest.kt`：帳號動態 key、query 與 UI state 測試。

## Implementation Steps

1. 在登入成功後保存原始 username，提供只讀 getter，並在登出／401 清除流程一併移除，避免跨帳號沿用資料。
2. 在 dashboard model 層新增帳號里程 resolver：略過 `全部`，遍歷每個 group 的 `accounts`，比對 username 並回傳相符值；未命中時回傳明確空結果，絕不使用 group／全部 `總長`。
3. 重整 `DashboardUiState`，分別表示今日、累積及搜尋里程、loading 與錯誤；預設以當日 `start_date=end_date` 與完整時間（無日期）兩個 query 載入，將回應交由 resolver。
4. 將 dashboard layout 改為 Figma 的標題、兩張並列里程卡、搜尋卡、兩個日期欄位、搜尋／清除按鈕與查詢結果區；保留既有 bottom tab，移除舊群組、進度、圓餅圖與月份篩選的可見 UI。
5. 在 `DashboardFragment` 使用頁內日期選擇器，驗證起訖日期完整且起日不晚於迄日；搜尋後以日期 query 讀取並顯示帳號里程，清除後重設欄位與結果空狀態。
6. 保留 token 缺失與 401 的既有登入導向、顯示 API／網路錯誤和 loading，並確認底部 dashboard tab 不影響 map 專用控制項。
7. 補齊 resolver、當日／累積 query、搜尋／清除、缺少帳號與日期驗證的測試，再執行 Gradle 單元測試與 build。

## Test Plan

- `DashboardModelsTest`：解析 username `10362` 得到 `D組.10362=2.34`；未命中與只有 `全部` 時不可回退為總長。
- `DashboardViewModelTest`：今日 query 使用同一天起訖、累積 query 無日期、搜尋 query 僅含日期、清除回到空搜尋狀態。
- Dashboard fragment／instrumentation：登入後切到 dashboard，確認雙卡、日期欄位、搜尋／清除、空結果與成功結果可見；驗證起訖日輸入與 401 導回登入。
- 執行相關 unit tests 與 app build；可用裝置時做 Figma 初始／有結果狀態的實機 smoke test。

## Regression Plan

- 確認 map/dashboard tab 切換仍互斥，回到 map 時既有 map 控制項恢復。
- 確認 username 清除後不會顯示前一帳號的里程。
- 確認 endpoint 的既有日期參數編碼與 Bearer token 未改變，並保留 401 handler。
- 確認其他 dashboard response 群組／進度的 Gson 解析不受新增 resolver 影響。

## Risks

- 後端對完整時間空日期 query 的接受度，以及 username 未在資料內時的空狀態，需以整合驗證證實。
- API 動態 key 格式改變會直接影響帳號比對；需保留 null-safe 解析與測試範例。

## Rollback Plan

- 若新 dashboard 造成回歸，回復本任務 commit 即可還原舊 dashboard、登入偏好設定與 API 使用方式。

## Current Behavior

- 現有 dashboard 顯示全部／組別長度、調查進度與多模式 filter，沒有登入帳號的今日／累積里程。

## Expected Behavior

- Dashboard 顯示登入帳號的今日及累積里程，並允許以日期區間查詢同一帳號的里程。

## Acceptance Criteria Traceability

| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | 3 | ViewModel query test、API smoke test |
| AC-002 | 1、2、3 | resolver unit test、登入後 dashboard smoke test |
| AC-003 | 4、5 | fragment/instrumentation test |
| AC-004 | 4、5 | fragment/instrumentation test、empty/result UI smoke test |
| AC-005 | 4 | Figma visual smoke test |
| AC-006 | 1、6 | 401 regression test、map/dashboard tab instrumentation |

## Failure Behavior

- token 缺失或 401：沿用既有清除登入資訊並導回登入頁；網路／API 錯誤：顯示錯誤，保留已成功的其他區塊資料；username 未命中：里程和查詢結果顯示空資料，不以總長替代；日期無效：禁止搜尋並提示使用者。

## Security and Privacy

- username 與既有 token 同存於 app 私有 SharedPreferences；不得記錄 token、password 或 username 到 log。登出／401 時一併清除 username。

## Open Questions

- 無。
