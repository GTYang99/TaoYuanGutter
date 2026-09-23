# Plan Review

Task: feat-0923  
Reviewer: Plan Critic Agent  
Review Iteration: 6  
Review Date: 2026-09-23

---

# Summary

## Review Result

- [x] APPROVED
- [ ] REQUEST_CHANGES
- [ ] BLOCKED

## Summary

本輪確認上一輪 3 個 Major findings 已完成規劃修正，plan 已達 implementation-ready：

- submitted restore／recreation 只傳 `draftId`、Boolean 與小型狀態，完整草稿由 Room reread 後在 process memory 建立，不再把完整照片／basicData snapshot 放入 Bundle。
- Android-dependent Host、Intent、Activity recreation 與 UI 測試已移至 `app/src/androidTest`；JVM 只保留純 policy／snapshot／mapper projection 測試，且沒有新增未核准的 Android JVM dependency。
- re-upload evidence 已拆成 `StoreDitchRequest` projection 與 node-image photo projection，沒有把 URI、captured-at 或 upload-state 加入既有 API payload。

需求、AC、實際恢復鏈路、no-write contract、retry source of truth、測試 source set 與 rollback 均已對齊。沒有需要外部澄清的 blocker，批准進入 implementation。

本次審查未修改 production code；既有工作區變更仍保留。

---

# Previous Findings Re-check

| Previous finding | Result | Evidence |
|---|---|---|
| 5-1 Snapshot transport and transaction safety | RESOLVED | `analysis.md` and `plan.md` specify id/Boolean-only transport, Room reread after recreation, and process-memory snapshot. |
| 5-2 Test runtime and source set | RESOLVED | Android-dependent tests are under `app/src/androidTest`; JVM tests are explicitly runtime-independent. |
| 5-3 StoreDitch versus photo-upload evidence | RESOLVED | `plan.md` Step 8 and the retry test split deterministic StoreDitch fields from node-image photo inputs without changing API models. |

---

# Review Scope and Evidence

- Required inputs reviewed: `AGENTS.md`、`requirement.md`、`analysis.md`、`plan.md`、`state.yaml`、`ai/plan-critic-rules.md`。
- Supporting rules reviewed: `ai/planning-rules.md`、`ai/architecture.md`、`ai/testing-rules.md`、`ai/verification-rules.md`。
- Repository evidence reviewed: `GutterSessionFlowCoordinator`／`ResumeMapSheet`、`AddGutterBottomSheet`、both `LocationPickerHost` implementations, `GutterFormNavigator`, `GutterFormActivity`, `GutterSessionRepository`, `StoreDitchNodeRequestMapper`, `GutterRepository.uploadNodeImage`, and current Gradle test dependencies.
- Baseline boundary verified: `GutterRepository.storeDitch` uses synchronous `onRequestEntered: () -> Unit` before the Retrofit request; the plan now matches this contract.
- Existing test runtime verified: JVM tests use JUnit; Android-dependent assertions are planned for the existing AndroidX JUnit/Espresso instrumentation runtime without adding Robolectric or another Android JVM runtime.
- Working-tree context: branch `feat/草稿tag`; preserve existing changes in `GutterApiService.kt`、`AddGutterBottomSheet.kt`、`gradle/libs.versions.toml` and `.worktrees/`。

---

# Review Checklist

| Item | Result | Notes |
|------|--------|------|
| Requirements fully understood | PASS | Marker timing, tags, legacy defaults, existing `SPI_NUM` behavior, submitted read-only mode and retry are covered. |
| Acceptance Criteria complete | PASS | AC-001～AC-007 remain traceable to implementation and validation. |
| Repository analysis complete | PASS | Actual restore chain, lifecycle write paths, Room source and API/photo projection boundaries are documented. |
| Affected modules correct | PASS | Persistence, API boundary, both Hosts, form UI, resources and both test source sets are named. |
| Dependencies identified | PASS | Room, draft coordinator/repository, Host callbacks, existing overlays, Android instrumentation and photo upload seams are identified. |
| Risks evaluated | PASS | Migration, marker ordering, Bundle size, no-write bypass, API projection and photo metadata risks are recorded. |
| Test Plan complete | PASS | Unit, instrumentation, UI, request projection, photo projection, retry, build and compile checks are specified. |
| Regression Plan complete | PASS | Legacy migration, unsubmitted editing, existing-gutter behavior, return/recreation, cleanup and retry are covered. |
| Open Questions documented | PASS | Product behavior and technical decisions are resolved; instrumentation limitations remain explicitly `NOT VERIFIED` when unavailable. |
| Implementation steps actionable | PASS | State transport, no-write guards, immutable source, API projections and result handling are concrete. |
| Scope appropriate | PASS | Scope remains within the approved pending-draft feature and does not alter API contract. |
| Rollback strategy exists | PASS | Rollback is limited to submitted-mode changes and preserves unrelated workspace edits. |

---

# Findings

No open Critical, Major, or Minor findings.

## Resolved Finding 1

Severity: Major  
Category: State Persistence / Android Transaction Safety  
Status: Resolved

The plan now transports only `draftId`／submitted flag／small state through Fragment arguments, Intent extras and saved state. Full data is reread from Room and deep-copied only in process memory; missing or undecodable rows do not fall back to stale JSON.

## Resolved Finding 2

Severity: Major  
Category: Test Plan / Test Runtime Feasibility  
Status: Resolved

The plan now keeps pure policy and mapper projection tests in `app/src/test` and moves Host, Intent, Activity recreation, Room, UI and result-handling assertions to `app/src/androidTest`, matching the configured dependencies.

## Resolved Finding 3

Severity: Major  
Category: API Contract / Payload Evidence  
Status: Resolved

The plan now validates StoreDitch-supported fields through `StoreDitchNodeRequestMapper` and validates URI／captured-at／file category／ownership／upload state through a separate node-image photo projection. It explicitly preserves the existing `capturedAt = null` StoreDitch mapping and does not expand the API model.

---

# Blocking Issues

None.

---

# Improvement Suggestions

- During implementation, keep the existing unrelated `AddGutterBottomSheet.kt` message change isolated while editing the same file.
- If instrumentation or a physical device is unavailable, record affected acceptance criteria as `NOT VERIFIED`; do not infer UI pass from JVM tests or Android test compilation.
- Keep the final implementation and verification reports aligned with the two separate payload projections.

---

# Decision

## APPROVED

Implementation may begin according to the revised plan.

---

# Next Action

- [ ] Planning
- [x] Implementation
- [ ] Requirement Clarification
- [ ] Human Review

---

# Definition of Done

- [x] All checklist items reviewed
- [x] Previous findings re-checked
- [x] Findings documented
- [x] Blocking Issues classified separately from improvement suggestions
- [x] Decision recorded
- [x] `state.yaml` updated

