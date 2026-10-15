# Requirement

## Background
- 使用折疊手機時，主畫面點選「無側溝點位」後輸入回報文字，輸入面板會往畫面上方移動過多。
- 使用者提供的 debug note 將此 inline 底部面板稱為 dialog；實際 UI 是 `activity_main.xml` 內的 `noDitchPanel`。

## Goal
- 修正無側溝回報文字輸入時的鍵盤／視窗位移，使輸入內容與操作按鈕保持可用。

## Functional Requirements
- 點選 `btnReportNoDitch`、選取點位並聚焦備註欄位時，無側溝回報面板應維持在可視區域內。
- 鍵盤顯示與收起時，面板不得因重複套用 IME inset 而被推離預期位置。
- 修正不得改變既有的點位選取、重設、備註輸入及送出流程。

## Non-functional Requirements
- 不修改 API、資料格式、權限或無側溝回報的產品流程。
- 只調整與無側溝輸入面板位置及其驗證相關的檔案。

## Acceptance Criteria
- AC-001：在可用的緊湊／折疊式視窗配置中，進入無側溝回報、選取點位並顯示鍵盤後，面板不會被推到畫面上方；備註欄位與操作按鈕仍可見或可透過面板內捲動操作。
- AC-002：鍵盤收起後，面板回到原本靠近底部導覽／系統區域的相對位置，不殘留 IME 位移。
- AC-003：既有無側溝流程的點位選取、重設、備註送出與離開功能維持可用。

## Constraints
- 維持目前 inline `CardView` 面板，不改成新的 Dialog 或 BottomSheet 元件。
- 正常登入流程為 `LoginActivity` → `MainShellActivity` → `MapWorkspaceFragment`，離線相容路徑仍可能使用 `MainActivity`；兩者共用無側溝面板控制器。
- 目前沒有可供本工作使用的折疊裝置，因此折疊姿態下的實際座標證據必須標記為 `NOT VERIFIED`。

## Open Questions
- 無；平台在不同裝置上的 IME resize／pan 實際差異列為驗證限制，不作為未記錄的需求假設。
