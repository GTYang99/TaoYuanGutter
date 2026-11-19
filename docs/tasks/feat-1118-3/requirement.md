# Requirement

## Background
- 本任務取消 Android client 的「銜接點」功能與 `IS_TIEINPOINT` 的使用者可見及送出行為。
- 舊版 API、Intent、草稿與本地資料可能仍含有 `IS_TIEINPOINT`，更新後必須相容且不可造成 App 啟動或草稿列表閃退。

## Goal
- 新增、編輯、檢視與點位下拉流程不再顯示或操作「銜接點」。
- 所有 `storeDitch` request JSON 完全不包含 `IS_TIEINPOINT`，舊資料則靜默按一般點處理。

## Functional Requirements
- 移除新增／編輯表單中的「銜接點」欄位、控制項、預設值、互斥與狀態互動。
- 移除檢視頁及點位下拉名稱中的「（銜接點）」標記。
- `storeDitch` 對一般點、無法開蓋點、虛擬點及舊銜接點資料，序列化 JSON 均不得包含 `IS_TIEINPOINT`。
- 舊 response 或 draft 含 `IS_TIEINPOINT` 時，由 Gson／解析層相容讀取並忽略；執行時視為一般點，不套用銜接點的明細、照片或互斥規則。
- 舊「銜接點」資料若缺少一般點必填欄位，依一般點驗證阻止送出，允許使用者補齊後重新上傳。
- 舊資料同時含銜接點與虛擬點時，以虛擬點規則優先。
- 舊 Intent／Bundle、草稿與表單結果中的 `IS_TIEINPOINT` 可相容讀取，但不可重新產生或保存。
- 已上傳且被標記為唯讀的舊銜接點草稿可例外解鎖，補齊一般點必填欄位後重新上傳；其他已上傳唯讀草稿仍不可修改。
- 升級後若 draft JSON 部分損壞，保留可解析的 waypoint、照片及其他欄位，清除損壞欄位並讓使用者重新填寫；無法解析的 waypoint 略過，不得影響 App 啟動、草稿列表或其他可用資料。

## Non-functional Requirements
- 從 commit `78d6df31353184218e21928a321ae6febf81da13` 的已安裝版本更新至本版本時，必須保留 App 資料且不得閃退。
- 不新增權限、憑證、個資欄位或後端 API。
- 不修改本任務以外的未提交變更。
- API 舊欄位、legacy draft cleanup 與相容性測試可保留相關字串，但執行時不得顯示或送出。

## Acceptance Criteria
- AC-001：新增、編輯表單不顯示「銜接點」控制項、文案或相關狀態互動。
- AC-002：檢視頁與點位下拉名稱不顯示「（銜接點）」標記；含舊 `IS_TIEINPOINT` 的資料也不顯示該標記。
- AC-003：一般點、無法開蓋點、虛擬點及含 legacy `IS_TIEINPOINT` 輸入建立的 `storeDitch` request JSON 均不包含 `IS_TIEINPOINT`。
- AC-004：舊 response、Intent／Bundle 與 draft 含 `IS_TIEINPOINT` 時可正常載入、不閃退，並按一般點規則處理；虛擬點仍按虛擬點規則處理。
- AC-005：舊銜接點資料缺少一般點必填欄位時，送出被阻止；補齊後可修改並重新上傳。
- AC-006：已上傳唯讀的舊銜接點草稿可依例外規則解鎖後修改與重新上傳；非銜接點唯讀草稿仍不可修改。
- AC-007：從 commit `78d6df31` 安裝版本更新至本版本，App 啟動、草稿列表、草稿恢復、新增、編輯與檢視流程均不閃退，且可用資料保留。
- AC-008：部分損壞 draft 可保留可解析資料；不可解析 waypoint 被略過並可重新建立，不影響其他草稿。
- AC-009：相關 JVM、instrumentation、升級驗證、Debug build 與 source scan 通過；測試不得把銜接點當作執行中的 UI 或 request 行為依賴。

## Constraints
- 實作 branch 為 `codex/feat/feat-1118-3`，基於 `8d484d5a6d31924d099e793476f898ed99594aa8`。
- 不建立新的 worktree。
- 本任務只處理 Android client；不要求後端刪除資料或修改資料庫。
- `IS_CANTOPEN`、照片、草稿、一般點必填及虛擬點既有規則需維持，僅移除銜接點造成的特殊行為。

## Open Questions
- 無
