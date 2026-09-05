# Implementation Plan

## Goal
- 在檢視側溝的基本資料分頁加入 API 回傳的「退回原因」區塊。

## Scope
- FEAT-0905 / feature；僅擴充回應模型、唯讀顯示與必要測試。
- requirement.md 保留使用者原文；以下 AC 為需求追溯編號，使用者於 2026-09-05 補充空值、狀態與 API 契約決策：revokeComment 欄位固定存在，但值可能為空字串。
- 本階段只做 Planning；進入實作前完成 Plan Review。

## Affected Files
- `app/src/main/java/com/example/taoyuangutter/api/GutterApiModels.kt`：DitchDetails 新增可空欄位。
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterInspectBasicFragment.kt`：傳遞與綁定原因。
- `app/src/main/res/layout/fragment_inspect_basic.xml`：於座標標題之前加入容器與兩個 TextView。
- `app/src/main/res/drawable/bg_inspect_revoke_comment.xml`（新增）：背景、紅框及圓角。
- `app/src/main/res/values/colors.xml`、`app/src/main/res/values/strings.xml`：具名色彩與標題。
- `app/src/test/java/com/example/taoyuangutter/api/DitchDetailsRevokeCommentTest.kt`（新增）：回應解析及 JSON 往返。
- `app/src/androidTest/java/com/example/taoyuangutter/GutterInspectRevokeCommentTest.kt`（新增）：真實檢視 Activity 的傳遞、顯示與重建。

## Implementation Steps
1. Plan Review 確認本計畫可實作；不改寫原始 requirement.md。
2. 在 DitchDetails 建構子尾端新增 `@SerializedName("revokeComment") val revokeComment: String? = null`，保留既有 API 路徑、查詢與 Repository 行為。
3. 新增背景與字串資源；底色固定 #FCF1F0，紅框及標題紅色可先採 #FF666B、1dp 外框、4dp 圓角，視覺檢查時對照附圖調整。
4. 在基本資料頁垂直容器最前方新增原因容器，內距建議 16dp、下方間距 16dp；標題粗體、內容沿用 15sp 正文尺度並設 wrap_content，自然多行且不設 maxLines 或 ellipsize。
5. 新增 ARG_SPI_STATE 與 ARG_REVOKE_COMMENT，newInstance() 寫入 ditch?.spiState 與 ditch?.revokeComment；bindFields() 只在 `SPI_STATE == "2"` 且 revokeComment 非空白時顯示原因容器，否則整個紅框區塊設為 GONE，不保留空白區域，也不以現有通用 get() 將空值強制轉成「—」。
6. 驗證 Activity 的 JSON 往返與 Pager 傳遞自動保留新欄位；只在證據顯示必要時修改傳遞流程，不加入額外網路請求。
7. 加入下列解析與畫面測試，執行建置、單元測試與 Android 測試，保存結果及人工畫面證據，再依開發流程提交與交付 Verification。

## Test Plan
- AC-001：基本資料頁中，原因容器位於側溝座標編號標題上方。
- AC-002：底色 #FCF1F0、紅框、紅色粗體標題「退回原因」符合原始需求。
- AC-003：顯示 data.revokeComment 的完整文字，支援多行、中文、標點及原有換行。
- AC-004：只有 `SPI_STATE=2` 且 data.revokeComment 非空白時才顯示原因容器；空字串、純空白或非 `SPI_STATE=2` 時，不顯示整個紅框區塊。
- TEST-001（JUnit，AC-003）：解析代表性 DitchDetailsResponse，核對原因與既有 ditchId、xyNum、nodes；覆蓋正常文字與空字串，並保留缺欄位、null、純空白的相容性測試，確認模型可安全解析。
- TEST-002（JUnit，AC-003）：DitchDetails 經 Gson 序列化與還原後原因一致，覆蓋中文、換行及長文字，對應 Activity Intent 使用方式。
- TEST-003（Android，AC-001/002/003/004）：用 `SPI_STATE=2` 且有原因的 fixture 經 GutterInspectActivity.newIntent() 開啟檢視，確認標題、正文與區塊位置，重建 Activity 後保持內容；採既有 AndroidJUnit4/ActivityScenario，不新增 FragmentScenario 依賴。
- TEST-004（Android，AC-004）：驗證空字串、純空白、`SPI_STATE=1`、`SPI_STATE=3`、缺少 SPI_STATE 及未知狀態均不顯示原因容器；可另以缺欄位或 null fixture 做相容性保護。連續開啟有原因與無原因側溝，確認不殘留舊文字或空白紅框。
- TEST-005（人工，AC-001/002/003/004）：Android 9+ 裝置或模擬器以附圖內容、長文、窄螢幕及放大字體檢查配色、換行、不裁切、整頁捲動，以及不符合條件時座標區塊回到頁首。
- 開發驗證命令：`./gradlew :app:assembleDebug :app:testDebugUnitTest`；具可用 Android 裝置時執行 `./gradlew :app:connectedDebugAndroidTest`。
- Planning 未執行 build/test；設備或環境缺失必須記錄為 environment blocker，不得記為測試通過。

## Regression Plan
- 檢查側溝座標起點／節點／終點、虛擬點顏色及其他基本資料仍正確。
- 檢查基本資料與點位資料分頁切換、照片檢視、可編輯與不可編輯入口保持原行為。
- 檢查 revokeComment 為空字串時仍可開啟頁面且不顯示紅框，長文後下方欄位仍可捲動到達；缺欄位舊資料只作相容性檢查。
- 執行既有單元與 Android 測試；不調整地圖、上傳、草稿或編輯資料格式。

## Risks
- JSON 與 Bundle 傳遞若漏加欄位，API 有值但頁面空白；由往返及真實入口測試攔截。
- 附圖未提供精確排版尺寸，建議尺寸屬實作選擇；以原文明確配色與可讀性為準。

## Rollback Plan
- 若新增顯示造成回歸，回復本 feature 的模型、UI、資源與測試提交，不修改其他功能或後端資料。

## Current Behavior
- DitchDetails 無 revokeComment；基本資料分頁直接從側溝座標編號開始，未顯示原因。

## Expected Behavior
- AC-001/002/003/004 成立：當 `SPI_STATE=2` 且 API data.revokeComment 非空白時，原因以紅框多行區塊出現在座標編號上方，標題為紅色粗體「退回原因」，底色 #FCF1F0。
- 當原因為空字串、純空白或 `SPI_STATE` 不等於 `2` 時，不顯示整個紅框區塊；缺欄位或 null 只作相容性保護，不視為正式 API 情境。

## Open Questions
- 無。使用者已於 2026-09-05 補充：空白不顯示紅框範圍、只在 `SPI_STATE=2` 顯示、revokeComment 是回傳結果，且欄位固定存在但值可能為空字串。
