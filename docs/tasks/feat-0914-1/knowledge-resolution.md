# Knowledge Resolution

## Sources Reviewed

| Source | Evidence | Authority / confidence |
|---|---|---|
| 使用者指定附檔 `ty_feat_0914-1.md` | 功能、API、回應範例與 Figma URL | 高 |
| Figma node `2374:25617` | 初始狀態：兩張里程卡、日期起訖、搜尋／清除與「暫無資料」 | 高 |
| Figma node `2374:25667` | 搜尋後狀態：已填日期與 20.5 公里結果卡 | 高 |
| `DashboardFragment`／`activity_dashboard.xml` | 現行完整儀表板含群組、進度圖表、篩選 bottom sheet | 高；現況 |
| `DashboardModels.kt`、`GutterApiService.kt`、`GutterRepository.kt` | 已有 endpoint、日期與月份篩選模型及動態群組／帳號資料解析 | 高；現況 |
| `LoginActivity.kt` | 僅保存 token、姓名、公司與 group id；未保存登入 username | 高；現況 |
| 使用者補充 API 規則，2026-09-14 | 保存 APP username，與所有組別內的帳號動態 key 比對後取得里程；`D組.10362=2.34` 的顯示值為 2.34 km | 高；明確產品決策 |

## Resolved Decisions

| Decision | Rationale | Affected AC |
|---|---|---|
| 重用既有 dashboard endpoint、Retrofit service、repository 與 Gson 動態 key deserializer | endpoint 與 `date[start_date]`／`date[end_date]` 已存在且符合需求所列 API。 | AC-001、AC-003、AC-004 |
| 新畫面以 `DashboardFragment`、ViewBinding 與 XML 資源取代現行完整 dashboard 版面，不建立新 tab 或新 Activity | Figma 是既有 dashboard tab 的手機畫面；主導航 shell 已經存在。 | AC-005、AC-006 |
| 移除月份篩選、組別／進度選擇、圖表與明細呈現，僅保留日期搜尋與公里結果 | 使用者需求明定搜尋選項僅剩日期區間，Figma 只呈現該流程。 | AC-003、AC-004 |
| 保留目前 401 導回登入與既有 bottom tab 的 host 行為 | 需求未要求改變驗證或導航，且此為既有已驗證行為。 | AC-006 |
| 登入成功時保存 username，並以 username 比對所有非「全部」調查長度群組中的 `accounts` key | 使用者已明確指定 username 對比規則；既有 `DashboardLengthGroupDeserializer` 已把各帳號保存為 `accounts`。 | AC-002 |
| 預設載入進行兩個查詢：今日為當日 start/end date，累積為未帶日期的完整時間範圍 | 附檔背景明定「當日與完整時間」取得預設內容；既有 Retrofit endpoint 接受 null 日期參數。 | AC-001、AC-002 |
| 日期搜尋的結果為登入帳號在使用者指定起訖日的里程 | 搜尋 UI 僅保留日期區間，且使用者指定同一帳號比對規則。 | AC-003、AC-004 |

## Unresolved Conflict

無。使用者已指定以保存的 username 與 API 組別內帳號 key 比對，禁止以組別或全部的 `總長` 代替。

## Assumptions Safe for Planning

- 完整時間範圍以未帶日期參數請求；若後端拒絕空日期，實作須依 API 錯誤顯示錯誤狀態而不可偽造累積值。
- 進入頁面的當日 API 查詢可組裝當日的開始／結束日期，格式須沿用既有 API 的日期格式。
- 日期搜尋會以使用者選擇的起訖日重新呼叫相同 endpoint。
- 空結果與成功結果可依 Figma 分別顯示「暫無資料」或公里數值卡。

## Questions Requiring Approval

無。
