# Plan Review

Task: bugfix-0930-photo-background-complete
Reviewer: Plan Critic Agent
Review Iteration: 1
Review Date: 2026-09-30

# Summary

## Review Result

- [x] APPROVED
- [ ] REQUEST_CHANGES
- [ ] BLOCKED

## Summary

範圍是表單層完成返回與上傳結果有效性；計畫保留父層正式送出 gate，涵蓋新增與編輯流程，並採 focused JVM tests/debug build。可進入實作。

# Review Checklist

| Item | Result | Notes |
|------|--------|------|
| Requirement understood | PASS | 表單立即返回與最終 server commit 分開處理 |
| Acceptance Criteria complete | PASS | 遮罩、draft、coordinator、ID 與匯入相容均有 AC |
| Repository analysis complete | PASS | 已追蹤 Activity、Fragments、coordinator、父層 submit gate 與 mapper |
| Architecture impact reasonable | PASS | 沿用 process coordinator、Room draft 與既有 submit gate |
| Affected modules identified | PASS | 僅照片表單及其 upload submission path |
| Dependencies identified | PASS | draft repository、coordinator、image ID mapper |
| Risks evaluated | PASS | imported URL semantics 與 process death 明列 |
| Test Plan complete | PASS | targeted JVM tests、debug build、靜態 gate review |
| Regression Plan complete | PASS | 保留 slot indicators 及 URL-only import tests |
| Open Questions documented | PASS | 無外部決策依賴 |
| Implementation steps actionable | PASS | 4 個有順序的修改步驟 |
| Task size appropriate | PASS | 小型 UX/state bugfix，未引入 WorkManager 或 schema change |
| Rollback strategy (if applicable) | PASS | 單一 task commit 可回退，無 migration |

# Findings

## Finding 1

Severity:
- [x] Major
- [ ] Critical
- [ ] Minor
- [ ] Suggestion

Category: Data integrity

Description: 移除表單等待不能連帶移除正式 `storeDitch` 前的等待，否則 request 可能在背景 upload 尚無 image ID 時送出。

Recommendation: 保留 AddGutterBottomSheet gate，新增與編輯流程皆須等待 coordinator 或同步補傳；缺少有效 ID 時不得以 success 通過。

Planning Response:
plan.md 明確保留同一正式送出 gate，並將 coordinator 與直接上傳的 success 條件限定為正整數 ID。

Status:
- [x] Resolved
- [ ] Open

## Finding 2

Severity:
- [ ] Critical
- [ ] Major
- [ ] Minor
- [x] Suggestion

Category: Compatibility

Description: 全域改寫 `isAlreadyUploaded` 會改變既有 URL-only imported photo 語意。

Recommendation: 只在新上傳回應處套用有效 ID gate，並執行既有 import 相容測試。

Planning Response:
計畫不改 imported-photo predicate，明列既有 resolver test 作回歸檢查。

Status:
- [x] Resolved
- [ ] Open

# Blocking Issues

None.

# Improvement Suggestions

- 實作時保留照片槽位原有預覽及上傳 progress indicators。

# Decision

## APPROVED

Implementation may begin.

# Next Action

- [x] Implementation
- [ ] Planning
- [ ] Requirement Clarification
- [ ] Human Review

# state.yaml Update

```yaml
phase: plan_review
status: approved
next_action: implementation
```

# Review Notes

無實機交互證據是已知 validation 限制，不阻擋此小型 implementation；不得在最終報告中宣稱 UI runtime 已驗證。
