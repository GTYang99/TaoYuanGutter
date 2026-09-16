# Plan Review

Task: feat-0915-1  
Reviewer: Plan Critic Agent  
Review Iteration: 2  
Review Date: 2026-09-15

---

# Summary

## Review Result

- [x] APPROVED
- [ ] REQUEST_CHANGES
- [ ] BLOCKED

## Summary

計畫已完整對應五個預設備註膠囊、中文逗號附加、完整項目去重、自由文字編輯，以及既有 `NODE_NOTE`、草稿、重建與提交流程。受影響的 layout、Fragment、字串資源與 UI 測試檔案均已確認存在，現有 repository flow 也支持不新增 API 或資料欄位。本次沒有發現阻塞實作的需求、架構或驗證缺口。

實作前仍須先隔離目前工作樹中其他任務的未提交變更，尤其是與本任務預計會修改的 `strings.xml`。

---

# Review Checklist

| Item | Result | Evidence |
|---|---|---|
| Requirement understood | PASS | 五個固定文案、附加規則、去重、自由輸入與適用範圍均明確。 |
| Acceptance Criteria complete | PASS | AC-001～AC-004 覆蓋 UI、資料合併、`NODE_NOTE` 流程及特殊模式回歸。 |
| Repository analysis complete | PASS | 已確認 `etRemarks`、draft watcher、`collectData()`、動態重排與模式控制。 |
| Architecture impact reasonable | PASS | 維持 Kotlin、ViewBinding、Material Components、既有 Fragment 與單一 `NODE_NOTE` 流程。 |
| Affected modules identified | PASS | Layout、Fragment、strings 與 `GutterBasicInfoUiTest` 已列明。 |
| Dependencies identified | PASS | Generated binding、Material 元件、表單 contract、draft flow、重排流程與 API mapper 均已核對。 |
| Risks evaluated | PASS | 已涵蓋逗號分段去重、狀態同步、watcher、動態重排與未提交字串變更。 |
| Test Plan complete | PASS | 涵蓋五項顯示、附加、分隔、去重、手動編輯、draft／重建及特殊模式。 |
| Regression Plan complete | PASS | 已保護預填、自由輸入、資料收集、草稿、重建、提交與相鄰表單狀態。 |
| Open Questions documented | PASS | 需求與產品規則已確認，無未決問題。 |
| Implementation steps actionable | PASS | UI、click 行為、狀態同步、資料流與驗證順序清楚。 |
| Scope appropriate | PASS | 限定側溝表單備註區，不包含無側溝點位面板或 API schema。 |
| Task size appropriate | PASS | 變更集中於單一表單區塊與其測試。 |
| Rollback strategy | PASS | 以本任務單一實作 commit 回復即可移除新增行為。 |

---

# Findings

## Finding 1

Severity: Suggestion  
Category: Working-tree isolation

Description:

目前 `feat/側溝清單` 分支存在其他任務的未提交 production 與文件修改，且 `app/src/main/res/values/strings.xml` 與本任務預計變更檔案重疊。

Recommendation:

實作前先在不覆寫既有修改的前提下保存或隔離工作樹，並確認本任務只提交自己的檔案變更。

Status: Resolved as an implementation prerequisite

## Finding 2

Severity: Suggestion  
Category: UI integration and regression coverage

Description:

新增的膠囊容器若未與 `tvRemarksTitle`、`tilRemarks` 一起納入 `reorderEditableSections()`、`setVirtualMode()` 與 `setEditable()`，可能造成排序分離或在不可編輯模式仍可操作。

Recommendation:

實作時使用明確 view ID，並在 UI 測試中直接驗證膠囊容器／每個膠囊於檢視、匯入鎖定與虛擬點模式的 visibility 與 enabled 狀態。

Status: Resolved in plan steps 3 and 5

## Finding 3

Severity: Suggestion  
Category: Data merge rule

Description:

備註可能同時包含自由文字與預設內容；以子字串判斷會把「花圃旁有車擋」誤認為已選項目。

Recommendation:

依中文逗號分段、trim 後，以完整項目比對；重複點擊時保持原文字不變，並在測試中覆蓋自由文字包含預設詞的情境。

Status: Resolved in plan step 2 and test plan

---

# Blocking Issues

None.

---

# Decision

## APPROVED

Implementation may begin after the existing worktree changes are safely isolated.

---

# Next Action

- [ ] Planning
- [x] Implementation
- [ ] Requirement Clarification
- [ ] Human Review

---

# Review Notes

- Repository verification confirmed `GutterBasicInfoFragment` currently collects `etRemarks` directly as `NODE_NOTE` and the existing text watcher forwards changes to the draft flow.
- `GutterFormContract` and `StoreDitchNodeRequestMapper` already preserve the same field boundary; the plan correctly avoids changes to those files.
- Current working-tree changes are outside the task's intended production files except the shared `strings.xml`; isolation remains necessary before implementation.

---

# Definition of Done

- [x] All checklist items reviewed
- [x] Findings documented
- [x] Blocking Issues identified (if any)
- [x] Improvement Suggestions separated
- [x] Decision recorded
- [x] state.yaml updated
