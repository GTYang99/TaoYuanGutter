# Plan Review

Task: `feat-0923`
Reviewer: Plan Critic Agent
Review Iteration: 7
Review Date: 2026-09-24

## Review Result

- [ ] APPROVED
- [x] REQUEST_CHANGES
- [ ] BLOCKED

## Review Scope

本輪針對新增功能重新審查：具有效 `SPI_NUM` 的既有側溝草稿，列表顯示唯一「既有側溝編輯中」tag，且恢復後維持可編輯／重新送出流程。上一輪 Review Iteration 6 的 APPROVED 結論保留為歷史紀錄，不視為本次新增範圍已核准實作。

已讀取並核對：

- `AGENTS.md`
- `ai/plan-critic-rules.md`
- `ai/issue-management.md`
- `docs/tasks/feat-0923/requirement.md`
- `docs/tasks/feat-0923/analysis.md`
- `docs/tasks/feat-0923/plan.md`
- `docs/tasks/feat-0923/state.yaml`
- 現行 `PendingDraftTagPolicy`、`PendingDraftAdapter`、`SubmittedDraftResumePolicy` 與相關測試

本輪未修改 production code。

## Checklist

| Check | Result | Evidence |
|---|---|---|
| Requirements fully understood | PASS | AC-001～AC-008 已明確區分一般草稿與有效 `SPI_NUM` 既有側溝草稿；三種 tag、唯一性、可編輯／重新送出與一般 submitted read-only 邊界均有描述。 |
| Acceptance Criteria complete | PASS | 新增的 AC-008 覆蓋未進入 API、失敗、逾時、中斷後保留內容／`SPI_NUM`／tag 並可繼續送出。 |
| Repository analysis complete | PASS | 已檢查列表 adapter、恢復 coordinator、form flow、Room source、提交 boundary 與既有測試；主要行為邊界有記錄。 |
| Affected modules correct | FAIL | plan 未把實際負責 tag 判定的 `PendingDraftTagPolicy.kt` 與恢復判定的 `SubmittedDraftResumePolicy.kt` 列為 affected files，也未明定兩者共用同一有效 `SPI_NUM` predicate。 |
| Dependencies identified | PASS | Room、draft repository/coordinator、兩個 Host、form flow、既有 tag drawable 與 instrumentation runtime 均已列出。 |
| Risks evaluated | PASS | 已記錄 valid `SPI_NUM` 優先、唯一 tag、誤套用一般 submitted read-only、長文案 layout 與既有重送流程回歸風險。 |
| Test Plan complete | FAIL | 有三種 tag 與 AC-008 flow 測試，但尚未要求覆蓋 valid predicate 的 whitespace／缺少 START／SPI_NUM 位於非 START waypoint 等邊界，也未固定 tag policy 與 read-only policy 必須一致。 |
| Regression Plan complete | PASS | 既有側溝失敗／重送／成功清除／長按刪除與一般 submitted read-only 回歸均已列出。 |
| Open Questions documented | PASS | 無待產品決策；新增行為與技術邊界已寫明。 |
| Implementation steps actionable | FAIL | Step 4 只描述「以有效 `SPI_NUM` 判定」，但沒有指定要修改現行 `PendingDraftTagPolicy`（目前對 valid `SPI_NUM` 回傳 `null`），也沒有指定 restore policy 的共用實作。 |
| Scope appropriate | PASS | 範圍仍限於草稿狀態、列表 tag、既有側溝恢復與既有重送流程，不改 API payload。 |
| Rollback strategy exists | PASS | rollback 限定於本次 submitted mode／tag policy 變更，保留既有 baseline 與無關工作區變更。 |

## Findings

### Finding 1

Severity: Major
Category: Affected Modules / State Policy Consistency
Status: Open

Description:

現行 `app/src/main/java/com/example/taoyuangutter/pending/PendingDraftTagPolicy.kt` 的 `PendingDraftTagKind` 只有 `SUBMITTED`／`UNSUBMITTED`，且對 START waypoint 有非空 `SPI_NUM` 時回傳 `null`。因此目前程式不可能產生「既有側溝編輯中」tag。plan 的 affected files 只列 `PendingDraftAdapter.kt`，沒有列出必須變更的 tag policy；同時 `SubmittedDraftResumePolicy.kt` 又自行解析 `SPI_NUM`，沒有真正與列表 tag 共用 predicate。若實作只改 adapter，tag 仍不會出現；若分別修改兩處，可能出現「列表顯示既有側溝 tag 但恢復進入唯讀」或反向不一致，直接影響 AC-003、AC-004、AC-008。

Recommendation:

修訂 `plan.md` 的 affected files、Step 4／5、traceability 與測試計畫：明確列出並修改 `PendingDraftTagPolicy.kt`（新增既有側溝 tag kind／唯一 tag policy），並以同一個可測試的 `hasValidSpiNum`／identity predicate 同時供列表 tag 與 `SubmittedDraftResumePolicy` 使用。明定有效 `SPI_NUM` 時 `kindFor` 必須回傳 existing-gutter kind，且 read-only policy 必須對同一 draft 回傳 false；新增 policy-level assertion 固定兩者不會分歧。

### Finding 2

Severity: Minor
Category: Requirement Precision / Boundary Tests
Status: Open

Description:

需求使用「有效 `SPI_NUM`」，但 plan 沒有把有效性的規則寫成可執行定義。現行列表 policy 是取 START waypoint 的 `SPI_NUM` 後 `trim()` 再判斷非空，而 `PendingDraftsBottomSheet` 的標題仍使用未 trim 的 `isNotEmpty()`；若 whitespace、缺少 START waypoint 或 `SPI_NUM` 出現在非 START waypoint，列表 tag 與恢復／標題可能採不同判定。

Recommendation:

在 `analysis.md`／`plan.md` 明定 valid predicate 的資料來源與 normalization（至少固定 START waypoint、`SPI_NUM.trim().isNotEmpty()`；若產品有格式要求則列出格式），並在 JVM policy tests 覆蓋有效值、空字串、純 whitespace、缺少 START 與非 START 欄位。需要時讓相關 title／restore 判定也重用同一 helper，避免同一草稿在不同入口得到不同身份。

### Finding 3

Severity: Minor
Category: State Evidence
Status: Resolved

Description:

本次新增 AC-008 後，`state.yaml` 原本的 `verification.not_verified_acceptance_criteria` 未包含 AC-008。

Planning Response:

已在本輪 review 更新 `state.yaml`，將 AC-008 納入未驗證清單；實作與 independent verification 仍不得把本輪 plan review 當成 runtime evidence。

## Improvement Suggestions

- 將 `PendingDraftsBottomSheet` 的刪除確認標題與 adapter 使用同一個 valid `SPI_NUM` helper，避免 whitespace 值造成顯示與 tag 身份不一致。
- `plan.md` 的 affected files 目前重複列出 `strings.xml`；可在修訂時合併並補上 policy／test fixture 的精確檔案責任，讓 implementation diff 更容易核對。
- 保留現有 `PendingDraftAdapterUiTest` 與 `GutterDraftSubmissionStateTest` 的歷史測試意圖，但將原本「hides submission tag」案例改名並改成驗證 existing-gutter tag，避免舊測試名稱掩蓋新產品規則。

## Blocking Issues

本輪沒有需要外部產品決策的 blocker。上述 Major planning gap 可由 Planning 修正後重新送審。

## Decision

**REQUEST_CHANGES**

計畫需先補上 tag policy 的實際 affected file、列表與恢復共用的有效 `SPI_NUM` 判定，以及對應邊界測試；完成後將 state 留在 plan review／planning re-entry，再進行下一輪 Plan Review。不得直接進入 implementation。

## Next Action

- [x] Planning
- [ ] Implementation
- [ ] Requirement Clarification
- [ ] Human Review

## Definition of Done

- [x] Required inputs reviewed
- [x] All checklist items reviewed
- [x] Blocking Issues separated from suggestions
- [x] Findings documented with severity and recommendation
- [x] Decision recorded
- [x] `state.yaml` updated

## Planning Response — Iteration 8 Preparation

The Iteration 7 decision remains `REQUEST_CHANGES` as historical review evidence. The planning revision addresses the open policy-consistency and boundary-test findings below; it is prepared for a fresh Plan Critic review and is not self-approved.

### Finding 1 — Policy ownership and consistency

Resolved in `analysis.md` and `plan.md`. The affected-file list now explicitly includes `PendingDraftTagPolicy.kt`, `SubmittedDraftResumePolicy.kt`, `PendingDraftAdapter.kt`, and `PendingDraftsBottomSheet.kt`. `PendingDraftTagPolicy` owns the shared pure `hasValidSpiNum` predicate and the `EXISTING_GUTTER` kind. Its precedence is fixed as valid `SPI_NUM` → existing-gutter kind → submitted/unsubmitted fallback. `SubmittedDraftResumePolicy`, title rendering, and delete identity must reuse the same predicate. The plan explicitly asserts that a valid `SPI_NUM` draft produces the existing-gutter tag and `submittedDraftReadOnly=false`.

### Finding 2 — Validity boundaries

Resolved in `analysis.md` and `plan.md`. Validity is now executable and normalized: only the first waypoint with type exactly `START` is inspected; `basicData["SPI_NUM"].trim().isNotEmpty()` is required. Empty, whitespace-only, missing-START and non-START-only values are invalid, with no additional format validation. JVM policy tests cover every boundary and cross-check tag kind against restore read-only mode; title and delete paths reuse the same helper.

### Finding 3 — State evidence

Already resolved by the current `state.yaml`; AC-008 remains in `verification.not_verified_acceptance_criteria` until independent runtime verification. This planning revision does not convert review or compile evidence into runtime PASS.

### Re-review request

The historical review remains `REQUEST_CHANGES`; the revised artifacts are now set to `phase: planning`, `status: plan_ready`, with `next_action: plan_review`. A fresh Plan Critic review is still required; implementation remains pending.

## Plan Review — Iteration 8

Reviewer: Plan Critic Agent
Review Date: 2026-09-24

### Review Result

- [x] APPROVED
- [ ] REQUEST_CHANGES
- [ ] BLOCKED

### Re-check of Iteration 7 Findings

| Finding | Result | Evidence |
|---|---|---|
| Policy ownership and consistency | RESOLVED | `plan.md` now lists `PendingDraftTagPolicy.kt`, `SubmittedDraftResumePolicy.kt`, `PendingDraftAdapter.kt` and `PendingDraftsBottomSheet.kt`; `PendingDraftTagPolicy` owns `hasValidSpiNum` and `EXISTING_GUTTER` precedence, while restore/title/delete paths reuse it. |
| Validity boundaries | RESOLVED | `analysis.md` and `plan.md` define exact `START` lookup plus `SPI_NUM.trim().isNotEmpty()` and cover empty, whitespace-only, missing-START and non-START-only fixtures. |
| AC-008 state evidence | RESOLVED | `state.yaml` retains AC-008 as `NOT VERIFIED`; AC-003 is also now listed because the three-tag behavior is new and has not yet received runtime verification. |

### Checklist

| Check | Result | Evidence |
|---|---|---|
| Requirements fully understood | PASS | AC-001～AC-008 distinguish general submitted/unsubmitted drafts from valid-`SPI_NUM` existing-gutter drafts, including the unique tag and editable/resubmit behavior. |
| Acceptance Criteria complete | PASS | Each AC has implementation steps and validation mapping; AC-008 is explicitly covered by policy, cross-policy and Android flow tests. |
| Repository analysis complete | PASS | The revised analysis identifies the actual policy owners, restore flow, Room source, API boundary, title/delete paths and existing test seams. |
| Affected modules correct | PASS | Persistence, submission boundary, restore chain, tag policy, adapter/list sheet, form UI, resources and both test source sets are named. |
| Dependencies identified | PASS | Room migration, draft repository/coordinator, Host callbacks, form/navigation chain, existing drawable/overlay and Android instrumentation dependencies are documented. |
| Risks evaluated | PASS | Policy divergence, normalization, read-only misclassification, tag layout, no-write retry, payload projection and migration risks are recorded. |
| Test Plan complete | PASS | JVM boundary and cross-policy tests cover the normalized identity rule; Android tests cover tag rendering, restore/editability, retry/failure, migration and existing cleanup flows. |
| Regression Plan complete | PASS | Existing-gutter retry/delete/success cleanup, general submitted read-only/re-upload, legacy drafts and title/list layout are covered. |
| Open Questions documented | PASS | No product decision remains; the valid-`SPI_NUM` definition is explicitly fixed as an implementation contract. |
| Implementation steps actionable | PASS | Steps 4–5 now specify the policy owner, precedence, shared predicate and restore boundary before the existing UI/retry steps. |
| Scope appropriate | PASS | The change remains limited to pending drafts and their restore/submit state; API payload and unrelated workspace edits remain out of scope. |
| Task size appropriate | PASS | The feature is broad but its persistence, UI, restore, retry and test boundaries are separated and traceable. |
| Rollback strategy exists | PASS | Rollback is limited to this task's submitted-mode and tag-policy changes while retaining the validated baseline and unrelated edits. |

### Improvement Suggestions

- During implementation, rename the existing `GutterDraftSubmissionStateTest.existingGutterDraftWithSpiNumHidesSubmissionTag` case and change its `assertNull` expectation to `EXISTING_GUTTER`; the new plan's `PendingDraftTagPolicyTest` should remain the dedicated boundary suite.
- Keep `PendingDraftsBottomSheet` title/delete rendering covered by the same helper as the adapter, as specified, and record the relevant UI evidence separately from JVM policy evidence.
- The current review approves planning only; build, instrumentation, CI and independent verification remain required gates and are not implied by this decision.

### Blocking Issues

None. Iteration 7 planning gaps are resolved without requiring external product clarification.

### Decision

**APPROVED**

The revised plan is implementation-ready for the added existing-gutter tag scope. Implementation may begin; no production code was changed during this review.

### Next Action

- [ ] Planning
- [x] Implementation
- [ ] Requirement Clarification
- [ ] Human Review

### Definition of Done

- [x] Required inputs reviewed
- [x] All checklist items reviewed
- [x] Previous findings re-checked
- [x] Findings and suggestions documented separately
- [x] Decision recorded
- [x] `state.yaml` updated
