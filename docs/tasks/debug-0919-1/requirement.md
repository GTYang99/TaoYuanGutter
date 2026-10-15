# Requirement

## Background

表單與既有點位匯入流程目前存在四項 UI／資料狀態不一致：銜接點未套用無法開蓋規則、新增側溝空白項目被誤標為已填寫，以及匯入既有點位後仍可能修改定位或虛擬點狀態。

## Goal

修正上述四項流程，使畫面狀態、欄位可編輯性與既有資料狀態符合產品規則，並保留匯入點位的原始資料。

## Functional Requirements

- 在 `GutterBasicInfoFragment` 中，測量狀態選擇「銜接點」時，應比照「無法開蓋」處理不需填寫的選項：深度照片、頂寬度照片、溝蓋板厚度、深度、頂寬、材質、破損、懸掛、淤積、接管；後續審核流程也保持一致。
- 在 `AddGutterBottomSheet` 的新增側溝列表中，只有所有上傳條件都符合時才顯示「已填寫資料」；照片必須符合既有可上傳判定，不能只因欄位有值、座標已預填或存在預設值就判定完成。
- 在 `ImportExistingWaypointBottomSheet` 匯入既有點位並返回表單後，`GutterBasicInfoFragment` 的 `btnPickLocation` 不得讓使用者編輯定位。
- 在匯入既有點位並返回表單後，`GutterFormActivity` 的 `cbIsVirtual` 不得讓使用者切換；必須保留匯入點位原本的虛擬點狀態與其他欄位數值。

## Non-functional Requirements

- 不改變無關表單、照片、定位與匯入流程。
- 修正應涵蓋檢視模式、進入編輯模式及 Activity 狀態恢復等相關 UI 狀態切換。

## Acceptance Criteria

- AC-001：選取「銜接點」後，深度照片、頂寬度照片、溝蓋板厚度、深度、頂寬、材質、破損、懸掛、淤積、接管呈現與「無法開蓋」相同的免填／禁用狀態，且送出或審核判定不要求這些欄位。
- AC-002：新增側溝項目只有在所有既有上傳條件均符合時顯示「已填寫資料」；任一必要欄位或必要照片不符合上傳條件時顯示「暫無資料」。
- AC-003：匯入既有點位返回表單後，`btnPickLocation` 顯示不可編輯且點擊不會啟動定位編輯流程；進入編輯模式後仍維持不可編輯。
- AC-004：匯入既有點位返回表單後，`cbIsVirtual` 保留 API 回傳狀態但不可操作；進入編輯模式或 Activity 狀態恢復後仍不可切換，其他匯入欄位數值不得因該控制項狀態更新而被清除。

## Constraints

- 工作 branch 固定為 `fix/debug-0919-1-UI流程`。
- 不清理或改動其他 linked worktree。
- 必須先完成 root cause 與 minimum fix scope，再進入 implementation。

## Open Questions

- 無。共同免填欄位與 AC-002 的上傳條件已由需求決定；實作需沿用現有送出前驗證規則，不另行發明判定。
