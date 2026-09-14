# Repository Analysis

## Task Classification

- `feature`：改變儀表板預設資料載入、登入資訊保存與使用者可見 UI；需完整 Plan Review。

## Current Behavior

- `LoginActivity` 保存 token、名稱、公司與 group id，未保存原始 username。
- `DashboardFragment` 首次只用空 `DashboardQuery` 呼叫一次 API，並顯示全部／群組長度、進度、圓餅圖與兩種篩選。
- `DashboardResponseData.surveyLength` 已可解析組別內的帳號 key；`DashboardLengthGroup.accounts` 排除 `總長` 後保留其餘動態 key/value。
- `DashboardFilterBottomSheet` 支援日期與月份；此需求只保留日期搜尋且 Figma 直接在頁面中顯示起訖欄位與操作按鈕。

## Expected Behavior

- 登入成功後可取得保存的 username；dashboard 以該 username 在各組別帳號資料中取出里程，不顯示組別或全部總長作為個人值。
- Dashboard 初始時分別取當日與完整時間資料，呈現今日／累積兩張卡；日期搜尋只顯示該帳號的選定區間里程或空狀態。
- 版面符合 Figma 初始及有結果狀態，並維持既有 Dashboard tab、401 導回登入與 Map tab 行為。

## Affected Modules

- `app/src/main/java/com/example/taoyuangutter/login/LoginActivity.kt`、`AuthNavigator.kt`：保存、讀取及清除 username。
- `app/src/main/java/com/example/taoyuangutter/api/DashboardModels.kt`：新增可測試的帳號里程解析，遍歷非「全部」群組的 `accounts`。
- `app/src/main/java/com/example/taoyuangutter/dashboard/DashboardViewModel.kt`：管理今日、累積、日期搜尋三種帳號里程狀態，以及兩個預設 query。
- `app/src/main/java/com/example/taoyuangutter/dashboard/DashboardFragment.kt`：改為頁內日期選擇、搜尋／清除與新狀態渲染。
- `app/src/main/res/layout/activity_dashboard.xml`、`app/src/main/res/drawable/`、`app/src/main/res/values/strings.xml`、`colors.xml`：改為 Figma 對應的雙里程卡與搜尋卡樣式、文案、可見性與空／結果狀態。
- `app/src/test/java/com/example/taoyuangutter/api/DashboardModelsTest.kt`、`app/src/test/java/com/example/taoyuangutter/dashboard/DashboardViewModelTest.kt`：補 username 解析與 query／狀態測試。

## Dependencies

- Retrofit endpoint `api/v1/dashboard/getDashboard` 與既有 Bearer token／401 處理。
- `DashboardLengthGroupDeserializer` 對動態帳號 key 的現有解析。
- Android `DatePickerDialog`、ViewBinding、Material 元件及現有 tab shell。
- Figma nodes `2374:25617`（初始）與 `2374:25667`（有結果）提供版面與主要 token：外側 20px、8px 圓角、#F5F2FF／#562ECB 里程卡、白底灰框搜尋卡。

## Risks

- username 在回應中缺失時，誤用組別／全部總長會造成個人里程錯誤；必須顯示 0 或空資料並保留診斷狀態，不得 fallback 到 group total。
- 完整時間的空日期 request 是既有 endpoint 的可選參數慣例；若後端拒絕，累積卡必須顯示可辨識錯誤，而非誤標為 0。
- 取代現有 layout 時遺漏 401、loading 或 tab host 相容性，可能造成登入導向或底部導覽回歸。
- 日期格式或 start > end 未驗證，可能導致查詢失敗或不一致結果。

## Unknowns

- 無阻擋規劃的未知項目。完整時間的空日期 endpoint 行為需以整合／實機驗證記錄；若失敗應分類為 API／環境證據，不改變帳號取值規則。
