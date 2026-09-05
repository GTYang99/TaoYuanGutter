# Plan Review

Task: FEAT-0905
Reviewer: Plan Critic Agent
Review Iteration: 1
Review Date: 2026-09-05

## Summary

Review Result: **APPROVED**

本計畫可進入實作，未發現阻擋問題。需求、AC、資料傳遞與修改範圍一致；測試涵蓋回應解析、Intent JSON 往返、真實 Activity 入口、重建、顯示條件及長文版面。以下兩項為非阻擋的測試實作建議。

本次以目前工作區任務文件為準（含欄位固定存在、可為空字串的補充），並採納 analysis.md / issue-log.md 已記錄的需求決策。未重新查證後端正式回應，亦未把相容性案例當成正式 API 契約。

## Review Checklist

| Item | Result | Notes |
| --- | --- | --- |
| Requirement understood | PASS | 顯示 API 回傳原因，位於基本資料座標編號上方；無新增 query。 |
| Acceptance Criteria complete | PASS | AC-001～004 涵蓋位置、樣式、完整文字與條件顯示；狀態及空白決策已記錄。 |
| Repository analysis complete | PASS | 核對模型、Service、Repository、InspectFlowCoordinator、Activity、Pager、Fragment 與 XML。 |
| Architecture impact reasonable | PASS | 沿用現有 Gson、Bundle 與 ViewBinding；無須新增 ViewModel、儲存層或套件。 |
| Affected modules identified | PASS | 模型、基本資料 Fragment、版面、資源與測試清單正確。 |
| Dependencies identified | PASS | 既有 JUnit、AndroidJUnit4、ActivityScenario 可沿用；不需後端或 Room 修改。 |
| Risks evaluated | PASS | 已處理欄位相容性、JSON/Bundle 遺漏、畫面重建、長文與大字體風險。 |
| Test Plan complete | PASS | TEST-001～005 對應所有 AC，環境缺失不可宣告通過；實作建議見 Findings。 |
| Regression Plan complete | PASS | 包含座標、虛擬點、其他欄位、分頁、照片與編輯入口。 |
| Open Questions documented | PASS | 已記錄補充決策；未指定尺寸可在需求限制內選擇。 |
| Implementation steps actionable | PASS | 欄位型別、Bundle keys、顯示判斷、插入位置與驗證命令皆明確。 |
| Scope appropriate | PASS | 僅唯讀顯示，不延伸修改座標排版、地圖、上傳或草稿。 |
| Task size appropriate | PASS | 單一 UI 功能，修改集中，無須拆分。 |
| Rollback strategy | PASS | 可回復本功能提交，無資料遷移或後端副作用。 |

## Findings

### Finding 1

Severity: Suggestion

Category: Test Reliability

Description: plan.md TEST-003 採用 MainShellActivityTest 的 ActivityScenario 範例是可行的，但該既有範例的 withCurrentActivity 在 Activity 不存在時直接 return，waitForFragment 最終也只在 Activity 非 null 時斷言。若照抄這些輔助函式，新測試可能在未找到畫面的情況下結束而未失敗。這是參考測試的限制，尚非本功能實作缺陷。

Recommendation: 新測試使用 scenario.onActivity 或明確要求 Activity、Fragment、View 存在；等待逾時應失敗，且重建後重新取得 View 並執行內容與可見性斷言。無須重構既有 MainShell 測試。

Planning Response: 不要求修訂計畫；供 Developer 落實既有測試要求。

Status: Open (non-blocking)

### Finding 2

Severity: Suggestion

Category: Test Isolation

Description: GutterInspectActivity 將 offscreenPageLimit 設為 1，開啟基本資料時亦可能建立照片分頁。照片分頁會解析 nodes 並使用預載照片資料，因此 TEST-003/004 的 fixture 需要維持模型必要資料有效，避免無關照片資料干擾原因區塊測試。

Recommendation: 原因區塊測試使用有效的 nodes 空陣列與預設空預載資料，或使用完整的本地點位 fixture；座標與照片回歸另使用代表性資料。不要直接以需求範例的遠端照片作為 UI 測試成功的必要條件。

Planning Response: 不要求修訂計畫；供 Developer 選擇穩定 fixture。

Status: Open (non-blocking)

## Blocking Issues

無。

## Improvement Suggestions

- Finding 1：測試找不到 Activity / Fragment / View 或等待逾時時，必須明確失敗。
- Finding 2：原因區塊測試使用有效、無遠端照片依賴的 fixture。

## Decision

**APPROVED** — 可依目前 plan.md 進入 Implementation，兩項建議不構成重新送審條件。

## Next Action

Implementation。仍須遵守專案既有分支、開發驗證、提交、CI 與 Verification 關卡。

## state.yaml Update

```yaml
phase: plan_review
status: approved
next_action: implementation
review:
  result: APPROVED
  iteration: 1
```

## Review Notes

- 原始碼基準 HEAD：`6f6902b050ea5ca7cc4affb4d05783f5b08e819b`。
- 任務目錄目前尚未納入 Git；審查對象是工作區文件。
- plan.md SHA-256：`9a01dae39143a62b77d6889f38d6f1b0dd80f29e2168152945f3f7be655206e6`。
- 已閱讀 AGENTS.md、Plan Critic/Planning 規則、架構、Issue Management、計畫與審查模板，並核對 Developer / Verification 關卡。
- 已檢視需求附圖及既有 colors.xml、strings.xml、Gradle 依賴、DashboardModelsTest、MainShellActivityTest。
- DitchDetails 目前無 revokeComment；新增尾端可空欄位可沿用 Activity 的 Gson 往返，但 Fragment 仍需明確新增 Bundle 傳遞，計畫已包含。
- 本次為計畫審查，未修改需求、計畫或 production code，未執行 build/test，未宣告 CI 或功能驗證通過。

## Definition of Done

- [x] All checklist items reviewed
- [x] Findings documented
- [x] Blocking Issues identified (none)
- [x] Improvement Suggestions separated
- [x] Decision recorded
- [x] state.yaml updated
