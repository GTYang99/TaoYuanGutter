# Plan Review

Task: feat-0911-1
Reviewer: Plan Critic Agent
Review Iteration: 2
Review Date: 2026-09-11

---

# Summary

## Review Result

- [x] APPROVED
- [ ] REQUEST_CHANGES
- [ ] BLOCKED

## Summary

第 1 輪指出的兩個 Major finding 已由計畫修訂解決：request mapping 已有明確的純 Kotlin mapper 與 JSON assertion 測試；照片候選判定已集中到共用 resolver，並明確涵蓋兩個 host、一般 edit、draft-resume、三個 slot 與受控 smoke。重新核對後，計畫具備可執行的 production seam、測試入口、回歸矩陣與驗證證據要求，可進入 implementation。

# Review Checklist

| Item | Result | Notes |
|------|--------|------|
| Requirement understood | PASS | 已區分既有點位 metadata 省略、既有照片顯示／不重傳、新照片替換與新增行為。 |
| Acceptance Criteria complete | PASS | AC-001～AC-005 可追溯，未發現需求缺漏。 |
| Repository analysis complete | PASS | 已找到 request builder、preload、upload manager、兩個 host 與草稿相關流程。 |
| Architecture impact reasonable | PASS | 變更集中於 gutter request mapping 與照片上傳判定，符合既有分層。 |
| Affected modules identified | PASS | 已明確列出 request mapper、photo candidate resolver、兩個 host、upload manager 與測試檔案。 |
| Dependencies identified | PASS | 已指出 Gson null 欄位、photo metadata state 與 form contract。 |
| Risks evaluated | PASS | 已涵蓋 null serialization、重傳、兩個 host 及特殊模式風險。 |
| Test Plan complete | PASS | 已定義 mapper JSON assertion、resolver 分支測試、manager slot/category 驗證與兩個 host smoke。 |
| Regression Plan complete | PASS | 已具體列出一般 edit、draft-resume、三 slot、特殊模式、metadata 保留與兩個 host 回歸。 |
| Open Questions documented | PASS | 目前無需外部產品決策；待補的是實作計畫細節。 |
| Implementation steps actionable | PASS | Step 1～7 已指定 production seam、呼叫端替換方式、測試案例與 smoke evidence。 |
| Task size appropriate | PASS | 範圍可控，但需要把共用照片判定責任整理清楚。 |
| Rollback strategy (if applicable) | PASS | 單一 task commit 可回退，且未涉及資料 migration。 |

# Findings

## Finding 1

Severity:
- [ ] Critical
- [x] Major
- [ ] Minor
- [ ] Suggestion

Category:

Testability / Affected Files

Description:

`buildStoreDitchRequest()` 位於 `AddGutterBottomSheet.kt` 且為 private，現有 `app/src/test` 沒有可直接建立該 UI 元件的測試基礎。計畫 Step 4 只說「加入可測試的最小入口」及「視測試可見性最小化調整」，沒有指定要抽出的純 mapping helper、其 production 檔案、呼叫關係與測試檔案，因此 AC-001 與 AC-004 尚未形成可執行方案。

Recommendation:

在 `plan.md` 明確指定 request mapping 的責任位置與測試入口：例如將不依賴 Android UI 的 node/request mapping 抽成 `api` 或 `common` 下的純 Kotlin helper，讓 `AddGutterBottomSheet` 只負責傳入模式與 waypoint；或明確說明採用何種可測試的 visibility／factory。測試必須以實際 Gson 序列化後的 JSON key 缺席驗證既有 node，並驗證新增 node 的 metadata 仍存在；同時列出對應 production/test 檔案。

Planning Response:
已修訂。新增 `app/src/main/java/com/example/taoyuangutter/api/StoreDitchNodeRequestMapper.kt` 作為純 Kotlin 單節點 mapper，明確以 `requestNodeId != null` 分流；`AddGutterBottomSheet` 只組合 request，不清除 waypoint metadata。新增 `StoreDitchNodeRequestMapperTest`，直接以 Gson 序列化 JSON assertion 驗證既有 node 的 `captured_at`／`img_ids` key 缺席，以及新增 node metadata 保留。`GutterApiModels.kt` 僅沿用既有 nullable 欄位，不改 API contract。

Status:
- [ ] Open
- [x] Resolved

---

## Finding 2

Severity:
- [ ] Critical
- [x] Major
- [ ] Minor
- [ ] Suggestion

Category:

Test Plan / Regression Plan

Description:

照片是否上傳不是由 `PhotoUploadManager` 的 `originalWaypoints` 參數決定；目前它主要依 `photo*ImgId` 判斷，而 edit-diff 清空未變更照片的邏輯分別存在 `MainActivity.kt` 與 `MapWorkspaceFragment.kt`。兩個 host 都有實作，且 `currentSessionResumedFromDraft` 會繞過 edit-diff、採全量草稿上傳。計畫 Step 5～6 與 AC-002、AC-003、AC-005 沒有明確說明要抽共用純邏輯，或逐一測試兩個 host 及草稿分支；因此「零個 candidate」「只上傳替換 slot」目前無法保證覆蓋實際執行路徑。

Recommendation:

在 `plan.md` 明確指定照片變更判定的唯一測試／實作責任，避免只寫「`PhotoUploadManager` 或 helper」。至少列出：未修改既有照片、替換 slot 1／2／3、既有 metadata 保留但 upload-copy 清空、一般 edit 與 resumed draft 各自的預期，以及 MainActivity／MapWorkspaceFragment 兩個呼叫端的回歸驗證。若採共用 helper，需列出 production 檔案與移除／替代兩份重複邏輯的步驟；若不抽取，需列出兩個 host 的具體測試或 smoke evidence。

Planning Response:
已修訂。新增 `app/src/main/java/com/example/taoyuangutter/gutter/PhotoUploadCandidateResolver.kt`，集中兩個 host 的 snapshot-diff／upload-copy 判定；`MainActivity` 與 `MapWorkspaceFragment` 改為呼叫同一 resolver，`PhotoUploadManager` 僅消費候選並維持 slot、imgId、virtual、cant-open 規則。計畫明確列出一般 edit 與 `currentSessionResumedFromDraft` 的差異，並以 `PhotoUploadCandidateResolverTest` 覆蓋未修改照片、slot 1／2／3 替換、metadata 保留與 draft-resume；兩個 host 各自以 MockWebServer／repository fake smoke 驗證 `nodeImage` 次數與 category。若環境不可用，按 AC 逐項記為 `NOT VERIFIED`。

Status:
- [ ] Open
- [x] Resolved

---

# Blocking Issues

無。兩項 blocking finding 已由明確 production seam、測試檔案與 host／session 分支矩陣補齊。

# Improvement Suggestions

- 在 plan 的 affected files 中明確標示 `GutterApiModels.kt` 是否只驗證既有 nullable DTO，避免 implementation 階段誤改 API model。
- 測試與 smoke evidence 可補充 request body 的觀測方式（例如 MockWebServer／repository fake／受控 log），並避免以 `toString()` 物件輸出代替 JSON assertion。
- 目前工作樹另有未納入本任務的 `GutterApiService.kt` base URL 修改；implementation 前應保持該既有變更不被 task commit 混入或覆寫。

# Decision

## APPROVED

Implementation may begin.

# Next Action

- [ ] Planning
- [x] Implementation
- [ ] Requirement Clarification
- [ ] Human Review

# state.yaml Update

```yaml
phase: plan_review
status: approved
next_action: implementation
review:
  result: APPROVED
  iteration: 2
```

# Review Notes

此 review 僅審查 planning artifacts；未修改 production code 或 `requirement.md`。工作樹在 review 前已有 `GutterApiService.kt` 未提交修改，與本 task plan 無直接關聯，需在後續 commit isolation 時保護。

# Definition of Done

- [x] All checklist items reviewed
- [x] Findings documented
- [x] Blocking Issues identified
- [x] Improvement Suggestions separated
- [x] Decision recorded
- [x] state.yaml updated

---

# Iteration 2 Review Conclusion

第 1 輪的兩項 Major finding 均已 resolved；本輪 checklist 全數通過，決策為 `APPROVED`。可依修訂後的 `plan.md` 進入 implementation。
