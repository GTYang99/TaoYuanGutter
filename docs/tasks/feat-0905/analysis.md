# Repository Analysis

## Current Behavior
- 任務類型：feature，編號 FEAT-0905；新增側溝唯讀檢視的退回原因。
- `api/GutterApiModels.kt` 的 `DitchDetails` 尚無 `revokeComment`；Gson 不會將此未知欄位保留到模型。
- `GutterInspectActivity.newIntent()` 將模型序列化為 JSON，Activity 解析後由 InspectPagerAdapter 傳給 `GutterInspectBasicFragment.newInstance()`；Fragment 以 Bundle 保存欄位，bindFields() 顯示。
- `fragment_inspect_basic.xml` 使用 NestedScrollView 與垂直 LinearLayout，第一項為側溝座標編號，目前無退回原因區塊。

## Expected Behavior
- 只在 `SPI_STATE=2` 且 API data.revokeComment 非空白時，在側溝座標編號上方顯示唯讀多行退回原因，底色 #FCF1F0、紅色外框、紅色粗體標題「退回原因」。
- API 會固定帶 `revokeComment` 欄位，但值可能為空字串；當原因為空字串、純空白或 `SPI_STATE` 不等於 `2` 時，不顯示整個紅框區塊。
- 截圖只作本次區塊的視覺參考；不延伸修改既有座標排版或其他欄位。

## Affected Modules
- `app/src/main/java/com/example/taoyuangutter/api/GutterApiModels.kt`：資料欄位。
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterInspectBasicFragment.kt`：Bundle 傳遞與畫面綁定。
- `app/src/main/res/layout/fragment_inspect_basic.xml`、colors.xml、strings.xml 與新增背景 drawable：區塊樣式。
- API 模型單元測試與 Android UI 測試；Activity、Repository、Service 為追蹤與回歸檢查點，預期不用修改。

## Dependencies
- 現有 Retrofit/Gson、ViewBinding、Fragment arguments、NestedScrollView；無新增套件、Room migration 或 API 呼叫需求。
- 已閱讀原始需求與附圖；requirement.md 原文複製，附圖同名保存以維持相對連結。
- 現有 `DashboardModelsTest` 示範 Gson/JUnit；`MainShellActivityTest` 示範 ActivityScenario 與 UI 執行方式；未發現退回原因專屬測試。
- 專案規範以 Android 9+ 為目標；目前 minSdk=24，本次不調整版本設定。

## Risks
- 新欄位正式契約會固定存在；模型仍建議可為 null，避免舊資料、測試資料或異常回應造成不相容。
- 文字必須經 Intent JSON 與 Fragment Bundle 完整傳遞，重新建立畫面後不可遺失或殘留前一筆資料。
- 長文與大字體可能增加高度；應自然換行並隨整頁捲動，避免固定高度或省略文字。

## Unknowns
- 無 open question。使用者已於 2026-09-05 補充：空白不顯示紅框範圍、只在 `SPI_STATE=2` 顯示、revokeComment 是回傳結果，且欄位固定存在但值可能為空字串。
- 紅色精確色碼、圓角及間距未指定，規劃採附圖近似值及既有 Android 尺度，非新增驗收要求。
