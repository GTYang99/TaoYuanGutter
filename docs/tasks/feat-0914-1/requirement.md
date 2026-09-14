# Requirement

## Background

- 儀表板顯示內容 UI 調整。
- API 依登入帳號及完整當日時間，取得預設的今日里程與累積里程。

## Goal

- 將既有儀表板調整為「今日里程＋累積里程」及「調查進度搜尋」兩個區塊，並在進入頁面與搜尋時透過 `/v1/dashboard/getDashboard` 取得資料。

## Functional Requirements

- 進入儀表板頁面即呼叫 API：今日里程使用當日日期範圍；累積里程使用完整時間範圍。
- 今日里程顯示登入帳號當日實作的里程；累積里程顯示登入帳號的累積里程。
- 登入成功時保存 APP username；從 API `調查長度` 中所有組別的帳號動態 key 比對此 username，取相符 key 的數值為帳號里程。例如 username `10362` 在 `D組` 下對應 `2.34`，即顯示 `2.34 km`。
- 調查進度搜尋位於里程卡下方，篩選條件只保留日期區間（日）。
- 搜尋結果顯示在日期區間與操作按鈕下方。
- API 為 `/v1/dashboard/getDashboard`，使用 `date[start_date]` 與 `date[end_date]` 參數。

## UI Reference

- Figma：`https://www.figma.com/design/IfmNbZKhr4wojZ2bF5rYHG/%E6%A1%83%E5%9C%92%E5%B8%82%E9%81%93%E8%B7%AF%E5%81%B4%E6%BA%9D%E8%B3%87%E6%96%99%E5%B9%B3%E5%8F%B0_mobile--Copy-?node-id=2374-25616`
- 初始狀態：兩張並列的淡紫色里程卡、日期起訖選擇、搜尋／清除按鈕，以及「暫無資料」。
- 搜尋結果狀態：查詢結果以淺灰底、灰框、圓角卡顯示數值與「公里」。

## API Response Evidence

- `調查長度` 的 `全部`、各組別及組內帳號／`總長` 均為動態 key。
- `調查進度` 的資料結構仍由 API 回傳，但本需求未明定其是否需要顯示於新 UI。

## Acceptance Criteria

- AC-001：儀表板進入頁面即以當日日期範圍與完整時間範圍取得 dashboard 資料，並顯示今日里程與累積里程卡。
- AC-002：今日里程與累積里程均對應登入帳號；今日里程只計當日實作，累積里程為累積值。
- AC-003：畫面只有日期區間（日）作為調查進度搜尋條件，且搜尋結果位於條件下方。
- AC-004：使用者可選擇開始／結束日期、搜尋及清除；無結果時顯示「暫無資料」，有結果時依 Figma 顯示公里數值卡。
- AC-005：新 UI 符合 Figma 的手機版結構與主要視覺規格：402px 參考寬度、20px 外側邊距、8px 卡片圓角、淡紫色里程卡、白底灰框搜尋卡及底部雙 tab。
- AC-006：既有登入逾期（401）處理與 map／dashboard tab 切換不回歸。

## Constraints

- 使用現有 Android Kotlin、ViewBinding、Retrofit／Gson 與 MVVM 相鄰慣例；不引入 Tailwind 或 React。
- Figma 回傳的遠端 SVG 僅作為設計參考；既有 app icon 若已符合，不需新增或改寫資產。

## Open Question

無。
