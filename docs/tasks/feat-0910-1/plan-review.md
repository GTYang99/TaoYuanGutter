# Plan Review

Task: feat-0910-1
Reviewer: Plan Critic Agent
Review Iteration: 1
Review Date: 2026-09-10

---

# Summary

## Review Result

- [ ] APPROVED
- [x] REQUEST_CHANGES
- [ ] BLOCKED

## Summary

The requirement and repository analysis identify the intended user flow and the existing clearing behavior correctly. However, the plan does not yet define an executable ownership and merge model for the in-memory snapshot. In particular, it cannot demonstrate complete photo restoration or prevent stale snapshot data from overwriting later user input. The plan must be revised before implementation begins.

---

# Review Checklist

| Item | Result | Notes |
|---|---|---|
| Requirement understood | PASS | The Dialog, clearing, session-only snapshot, restore, and non-regression requirements are captured. |
| Acceptance Criteria complete | FAIL | AC-005 and AC-006 have no implementable restore/merge rule; AC-to-plan-and-validation traceability is missing. |
| Repository analysis complete | FAIL | It identifies the photo metadata risk, but not that pending-photo state is Activity-owned rather than returned by `collectData()`. |
| Architecture impact reasonable | FAIL | Snapshot ownership is assigned to the Fragment despite required state being coordinated through `GutterFormActivity`. |
| Affected modules identified | PASS | The primary Fragment, Activity, and photo helpers are relevant. |
| Dependencies identified | FAIL | The concrete `PendingPhotoDraftState`, upload callback, and lifecycle ownership dependencies need to be specified. |
| Risks evaluated | PASS | Lifecycle, callbacks, metadata, and draft risks are recognized. |
| Test Plan complete | FAIL | It does not define the AC-006 merge cases, pending/upload-state scenario, or exact acceptance-criteria mapping. |
| Regression Plan complete | PASS | It covers existing cant-open, validation, upload, virtual, open-gutter, view, and import paths. |
| Open Questions documented | PASS | The photo-1 interpretation and repeated-toggle behavior are stated. |
| Implementation steps actionable | FAIL | The snapshot content, owner, restore API, and conflict-resolution policy remain unspecified. |
| Task size appropriate | PASS | The feature is bounded to the form flow and its tests. |
| Rollback strategy (if applicable) | PASS | Reverting the isolated feature commit restores existing behavior. |

---

# Findings

## Finding 1

Severity:
- [ ] Critical
- [x] Major
- [ ] Minor
- [ ] Suggestion

Category: Snapshot ownership and photo-state integrity

Description:
The plan assigns the snapshot to `GutterBasicInfoFragment` and says it will include URI, captured time, upload state, image ID, error, and pending path. In the current implementation, clearing a photo also sends `onPhotoSlotReadyForUpload(slot, null)`, `onPhotoCapturedAtDraftChanged(slot, null)`, and `onPendingPhotoDraftChanged(slot, null)` to `GutterFormActivity` (`GutterBasicInfoFragment.kt:1476`). The Activity owns the pending path in `currentFormData` through `PendingPhotoDraftState` (`GutterFormActivity.kt:130` and `1725`) and `collectData()` does not expose that pending state (`GutterBasicInfoFragment.kt:1010`). A Fragment-only snapshot therefore has no specified way to atomically restore all affected Activity-owned state, nor to define what happens to an in-flight capture or upload.

Recommendation:
Revise the plan to name a single session-scoped owner and a concrete snapshot schema for every affected basic-data key and photo key, including pending-output state. Define the restore/clear APIs and callback suppression or batching sequence, including the policy for in-flight capture/upload. State explicitly how this owner is discarded when the form session ends and how it is not serialized into `currentFormData`, `Waypoint.basicData`, or Room.

Planning Response:
已修訂。快照 owner 改為 `GutterFormActivity` 的表單 session state，不再由 Fragment 單獨持有。schema 明確包含受影響 basic-data keys、第 2／3 slot 的 URI、capturedAt、upload state、imgId、error 與 `PendingPhotoDraftState` pending output path。Activity 提供 capture／restore／discard 與 dirty marker API；Fragment 只負責 UI 切換與呼叫 API。清除流程以 photo batch 包住，in-flight capture 在確認清除時取消其 pending state，不回填新的 capture 結果。snapshot 不進 `currentFormData`、`Waypoint.basicData`、Intent、SavedState 或 Room；Activity session 結束即丟棄。

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

Category: AC-006 conflict-resolution behavior

Description:
The plan says that unchecking will batch-restore every snapshotted field and photo, while separately noting the risk that this could overwrite values created while cant-open is selected. This is the behavior prohibited by AC-006, but the plan does not define which fields/slots are tracked as changed, when a snapshot is consumed, or how a changed empty value differs from an unchanged empty value. Existing `applyCantOpenUi()` disables the affected basic fields, so the plan must also identify every alternate mutation path that can affect the same data before a restore.

Recommendation:
Specify a field- and photo-slot-level merge policy. For example, record an initial snapshot plus dirty markers for values changed after confirmation; on uncheck, restore only snapshot entries that remain unchanged by the current cant-open state, preserve current entries that became user-modified, then consume the snapshot. Add explicit tests for changed field, replaced photo, deleted photo, and intentionally blank current value.

Planning Response:
已修訂。取消勾選採 field／photo-slot 級 dirty merge：建立「確認後清除基準」與 dirty markers；只有未被任何使用者操作或新 callback 修改、且仍等於清除基準的項目才回填。被替換、刪除、重新拍攝或明確清空的欄位／slot 保留目前值。回填完成後消耗 snapshot，下一次勾選重新建立，避免舊快照覆蓋後續輸入。

Status:
- [ ] Open
- [x] Resolved

---

## Finding 3

Severity:
- [ ] Critical
- [x] Major
- [ ] Minor
- [ ] Suggestion

Category: Acceptance traceability and verification design

Description:
The canonical plan template requires an Acceptance Criteria Traceability table, Failure Behavior, and Security and Privacy sections. The submitted plan omits those sections. Its test plan is broad but does not map AC-001 through AC-009 to implementation steps and validations, so there is no reviewable proof that the lifecycle, draft isolation, cancellation, and non-applicable mode criteria will be validated. The proposed test file names are also conditional rather than tied to a chosen test seam.

Recommendation:
Use the canonical plan structure. Add a complete AC-001–AC-009 table that maps each criterion to a numbered implementation step and named test/check. Choose the state-model or Android test seam after resolving Finding 1, and list the fixture/setup and validation commands needed to exercise it. Include negative cases for Dialog dismissal, configuration/view recreation, restoring a Room draft, view mode, import lock, virtual point, and open-gutter mode.

Planning Response:
已修訂。`plan.md` 已補上 canonical 的 Acceptance Criteria Traceability、Failure Behavior、Security and Privacy sections，並將 AC-001 至 AC-009 對應至明確 implementation steps 與固定測試名稱。測試 seam 確定為純 Kotlin `CantOpenSessionSnapshotTest` 加上 `GutterCantOpenUiTest`，另涵蓋 pending/upload metadata、dirty merge、Room draft isolation、view recreation 與非適用模式。

Status:
- [ ] Open
- [x] Resolved

---

# Blocking Issues

- The plan does not define a complete, non-persistent owner and restoration path for Activity-owned photo state, including pending capture state.
- The plan does not define the merge/dirty-state behavior required to satisfy AC-006.
- Acceptance criteria are not traceable to numbered implementation steps and validations, so completion cannot be verified.

---

# Improvement Suggestions

- Keep the existing `currentFormSnapshot()` cant-open sanitization as a defense-in-depth rule, but add a test proving it cannot leak a session snapshot into a draft or submission.
- State whether an Android configuration change remains the same editing session or deliberately discards the temporary snapshot, then cover that decision with a test.

---

# Decision

## REQUEST_CHANGES

Planning Agent should revise the plan to resolve the three Major findings and submit it for review again.

---

# Next Action

- [x] Planning
- [ ] Implementation
- [ ] Requirement Clarification
- [ ] Human Review

---

# Plan Review — Iteration 9

Reviewer: Plan Critic Agent
Review Date: 2026-09-10

## Review Result

- [x] APPROVED
- [ ] REQUEST_CHANGES
- [ ] BLOCKED

## Summary

Finding 8 is resolved. Step 8 now preserves and excludes the user-owned `GutterApiService.kt` base-URL change, while retaining only the 0910-1-specific cleanup instruction. The plan contains the required snapshot, dirty merge, lifecycle, camera-token, failure, security, rollback, and AC-001–AC-009 validation coverage. Implementation may begin.

## Checklist Delta

| Item | Result | Notes |
|---|---|---|
| Requirements and acceptance criteria | PASS | All AC-001 through AC-009 map to implementation and validation. |
| Scope and working-tree isolation | PASS | The manual base-URL change is explicitly preserved and excluded from this task and commit. |
| Affected modules and dependencies | PASS | Fragment, Activity, ViewModel, camera result contract, draft helpers, and tests are covered. |
| Risks and failure behavior | PASS | Dialog, lifecycle, stale result, URI, draft-sync, and upload behavior are documented. |
| Security and privacy | PASS | Snapshot and photo data remain session-only and non-persistent. |
| Rollback strategy | PASS | Feature rollback excludes user-owned URL work. |
| Implementation steps actionable | PASS | The plan is safe to implement without lowering requirements. |

## Finding 8 Resolution

Planning Response:
Step 8 now directs the implementer to preserve and exclude the user-owned `GutterApiService.kt` base-URL change; it no longer instructs removal of that work.

Status:
- [ ] Open
- [x] Resolved

## Blocking Issues

None.

## Improvement Suggestions

None.

## Decision

## APPROVED

Implementation may begin.

## Next Action

- [ ] Planning
- [x] Implementation
- [ ] Requirement Clarification
- [ ] Human Review

## state.yaml Update

```yaml
phase: plan_review
status: approved
next_action: implementation
```

---

# Plan Review — Iteration 6

Reviewer: Plan Critic Agent
Review Date: 2026-09-10

## Review Result

- [ ] APPROVED
- [ ] REQUEST_CHANGES
- [x] BLOCKED

## Summary

The plan has restored the required session-only snapshot, dirty merge, camera-token, and lifecycle behavior, resolving Finding 6. However, the current worktree includes an uncommitted `GutterApiService.kt` change that switches the Retrofit base URL from `DEMO_URL` to `BASE_URL`. This is outside the plan, contradicts its instruction to remove unrelated URL changes, and cannot be changed by Plan Review because it may be a user-owned modification. The task is blocked pending ownership and scope confirmation.

## Checklist Delta

| Item | Result | Notes |
|---|---|---|
| Requirement and acceptance criteria | PASS | AC-001 through AC-009 are again covered by the planned snapshot, merge, lifecycle, and mode behavior. |
| Repository analysis and affected modules | PASS | Activity, Fragment, ViewModel, camera result, and metadata dependencies are identified. |
| Working-tree isolation | FAIL | An unapproved API base-URL change is present outside the task scope. |
| Canonical plan structure | FAIL | Failure Behavior, Security and Privacy, and Rollback Plan are missing. |

## Finding 7

Severity:
- [x] Critical
- [ ] Major
- [ ] Minor
- [ ] Suggestion

Category: Working-tree scope conflict

Description:
`GutterApiService.kt` has an uncommitted change from `Retrofit.Builder().baseUrl(DEMO_URL)` to `.baseUrl(BASE_URL)`. The approved requirement prohibits backend API changes, while step 8 of the submitted plan explicitly says to remove unrelated URL changes. This change is not listed in Affected Files and may be an unrelated user modification. Plan Review cannot determine whether to preserve or remove it without owner confirmation.

Recommendation:
Confirm whether the base-URL change belongs to a separate authorized task. If it does, isolate it from `feat-0910-1` before implementation. If it belongs to this task, update the approved requirement and plan through the appropriate requirements process before proceeding.

Planning Response:
(Awaiting user direction.)

Status:
- [x] Open
- [ ] Resolved

## Finding 8

Severity:
- [ ] Critical
- [x] Major
- [ ] Minor
- [ ] Suggestion

Category: Plan completeness

Description:
The revised plan again omits the canonical `Failure Behavior`, `Security and Privacy`, and `Rollback Plan` sections. These sections were required in the earlier approved version and are required by `ai/templates/plan-template.md`.

Recommendation:
Restore those three sections when the working-tree scope conflict is resolved. Cover Dialog/camera callback failure behavior, the non-persistence boundary for photo and snapshot data, and the rollback path for this feature commit.

Planning Response:
(Planning Agent fills this section after revision.)

Status:
- [x] Open
- [ ] Resolved

## Blocking Issues

- The uncommitted API base-URL change has unresolved ownership and conflicts with this task's approved scope.

## Improvement Suggestions

None until the working-tree scope conflict is resolved.

## Decision

## BLOCKED

Plan Review cannot continue safely until the user identifies whether the `GutterApiService.kt` base-URL change belongs to this task or must remain as separate work.

## Next Action

- [ ] Planning
- [ ] Implementation
- [x] Requirement Clarification
- [ ] Human Review

---

# Plan Review — Iteration 7

Reviewer: Plan Critic Agent
Review Date: 2026-09-10

## Review Result

- [ ] APPROVED
- [x] REQUEST_CHANGES
- [ ] BLOCKED

## Summary

The user confirmed that the `GutterApiService.kt` base-URL change is pre-existing manual work and must be preserved outside `feat-0910-1`; the ownership blocker is resolved. The plan restores the core required snapshot, merge, lifecycle, and token behavior, resolving Finding 6. Two plan edits remain necessary before approval: align the URL scope statement with the user decision, and restore the three mandatory plan sections.

## Checklist Delta

| Item | Result | Notes |
|---|---|---|
| Required session-only restore behavior | PASS | Snapshot, dirty merge, draft isolation, lifecycle, and late-result handling again cover AC-005 through AC-007. |
| Working-tree ownership | PASS | The base-URL change is confirmed as separate user-owned work. |
| Scope consistency | FAIL | Step 8 still says to remove unrelated URL changes, contradicting the confirmed instruction to preserve the user-owned base-URL change. |
| Canonical plan structure | FAIL | Failure Behavior, Security and Privacy, and Rollback Plan are absent. |

## Finding 7 Resolution

Planning Response:
The user confirmed the base-URL change is manual, pre-existing work outside this task.

Status:
- [ ] Open
- [x] Resolved

## Finding 8

Severity:
- [ ] Critical
- [x] Major
- [ ] Minor
- [ ] Suggestion

Category: Plan scope consistency and completeness

Description:
The plan correctly excludes API changes from its affected files, but step 8 still directs the implementer to remove unrelated URL changes. That conflicts with the confirmed instruction to preserve the existing `GutterApiService.kt` change. In addition, the plan omits the mandatory Failure Behavior, Security and Privacy, and Rollback Plan sections, leaving error handling, data-boundary assurances, and recovery unreviewable.

Recommendation:
Replace the URL wording with an explicit instruction to preserve and exclude the user-owned `GutterApiService.kt` change from this task and commit. Add the three missing canonical sections: Dialog/camera/restore failure behavior; the session-only, non-persistent photo and snapshot data boundary; and a rollback path limited to the 0910-1 feature commit.

Planning Response:
(Planning Agent fills this section after revision.)

Status:
- [x] Open
- [ ] Resolved

## Blocking Issues

- The plan must be aligned with the confirmed treatment of the user-owned URL change and completed with the mandatory risk/recovery sections.

## Improvement Suggestions

None.

## Decision

## REQUEST_CHANGES

Planning can resolve the remaining issues without changing product intent.

## Next Action

- [x] Planning
- [ ] Implementation
- [ ] Requirement Clarification
- [ ] Human Review

---

# state.yaml Update

```yaml
phase: plan_review
status: changes_requested
next_action: planning
```

---

# Review Notes

No requirement-level blocker was found. The current gaps are resolvable by Planning without changing product intent.

---

# Definition of Done

- [x] All checklist items reviewed
- [x] Findings documented
- [x] Blocking Issues identified (if any)
- [x] Improvement Suggestions separated
- [x] Decision recorded
- [x] state.yaml updated

---

# Plan Review — Iteration 2

Reviewer: Plan Critic Agent
Review Date: 2026-09-10

## Review Result

- [ ] APPROVED
- [x] REQUEST_CHANGES
- [ ] BLOCKED

## Summary

Findings 1 through 3 from iteration 1 are resolved: the revised plan now assigns a session owner, defines the dirty-merge policy, and maps every acceptance criterion to implementation and validation. One Major lifecycle inconsistency remains and must be resolved before implementation.

## Checklist Delta

| Item | Result | Notes |
|---|---|---|
| Snapshot ownership and photo-state integrity | PASS | Activity ownership, complete schema, callback batching, and non-persistence boundary are specified. |
| AC-006 conflict-resolution behavior | PASS | The field/slot dirty-marker merge policy and key cases are defined. |
| Acceptance traceability and verification design | PASS | AC-001 through AC-009 now map to steps and named test seams. |
| Lifecycle / configuration-change behavior | FAIL | The plan promises retention across rotation but chooses an owner that is discarded by the platform's normal Activity recreation. |

## Finding 4

Severity:
- [ ] Critical
- [x] Major
- [ ] Minor
- [ ] Suggestion

Category: Session lifecycle and configuration change

Description:
The revised plan states that an Activity-held snapshot survives Fragment view recreation and that Android configuration changes are the same form session. However, `GutterFormActivity` has no `android:configChanges` declaration (`AndroidManifest.xml:66`) and the plan forbids `SavedState`; a normal rotation recreates the Activity and loses ordinary Activity-memory fields. The promised rotation test therefore cannot pass with the described owner. Conversely, persisting the snapshot through SavedState would conflict with the plan's stated non-persistence boundary unless its in-memory-only scope and process-death behavior are defined.

Recommendation:
Choose one executable behavior and align the plan, tests, and failure behavior:

- Treat Activity recreation as a new form lifecycle, discard the snapshot, and change the rotation test to assert that no stale state is restored; or
- Preserve it across configuration change with a non-saved session holder (for example, an Activity-scoped `ViewModel`), explicitly define that process death does not restore it, and add the holder source file plus rotation/process-recreation tests.

In either choice, define the cleanup hook for a real form exit and how a late `CameraOverlayFragment` result is ignored after the session is discarded.

Planning Response:
(Planning Agent fills this section after revision.)

Status:
- [x] Open
- [ ] Resolved

Planning Response:
已修訂。`CameraOverlayFragment.kt` 已加入 affected files。啟動相機前由 `CantOpenSessionViewModel` 為 slot 產生 generation token，透過 `newInstance()` 傳入；相機 producer 將 token 放入 `setFragmentResult()`，receiver 只接受符合目前 active token 的結果。勾選確認、取消／回填、同 slot 第二次拍攝及真正離開表單時使舊 token 失效並清除 pending path。測試新增四種 late-result 情境，確認不更新 UI、`currentFormData`、pending state，也不觸發 upload。

Status:
- [ ] Open
- [x] Resolved

Planning Response:
已修訂。採用 `CantOpenSessionViewModel` 作為 `GutterFormActivity` 的 Activity-scoped、非 SavedState session holder。configuration change／Fragment view recreation 保留同一 snapshot；process death 不恢復。成功送出、取消離開或其他真正結束表單時明確清除 snapshot。相機結果加入 session／slot token，session 已清除或 slot 已被取代時忽略晚到結果並清除 pending path。

Decision confirmation:
產品已確認第 1 張概況照保留；configuration change 視為同一表單 session。

Status:
- [ ] Open
- [x] Resolved

## Blocking Issues

- The configuration-change promise cannot be implemented with a plain Activity-memory owner and the current manifest configuration.

## Improvement Suggestions

- Add the concrete production source file for the pure Kotlin snapshot/merge component to Affected Files, or state that it will be an internal, test-visible type in an existing file.
- Number the implementation steps visibly so the acceptance-criteria table can be navigated without counting bullets.

## Decision

## REQUEST_CHANGES

Planning must resolve Finding 4 before implementation begins. No product-level clarification is required.

## Next Action

- [x] Planning
- [ ] Implementation
- [ ] Requirement Clarification
- [ ] Human Review

---

# Planning Response Summary

All three requested changes are addressed in the revised `plan.md`:

- Snapshot ownership is now explicitly `GutterFormActivity` session state, including pending photo state and complete upload metadata; it is never persisted.
- Restore uses field／photo-slot dirty markers and preserves values changed after confirmation, including replaced, deleted, re-captured, and intentionally blank values.
- AC-001 through AC-009 now map to numbered implementation steps and named unit／UI validations. Failure behavior and security/privacy boundaries are also documented.

The task is returned to plan review with `state.yaml` set to `phase: planning`, `status: plan_ready`, and `next_action: plan_review`.

---

# Plan Review — Iteration 3

Reviewer: Plan Critic Agent
Review Date: 2026-09-10

## Review Result

- [ ] APPROVED
- [x] REQUEST_CHANGES
- [ ] BLOCKED

## Summary

Finding 4 is resolved. `CantOpenSessionViewModel` gives the snapshot the correct configuration-change lifetime without restoring it after process death, and the proposed cleanup boundary is appropriate. One Major issue remains: the delayed-camera-result safeguard requires a `CameraOverlayFragment` contract change that the plan does not include in its scope or validation.

## Checklist Delta

| Item | Result | Notes |
|---|---|---|
| Configuration-change lifecycle | PASS | The Activity-scoped, non-SavedState ViewModel matches the chosen same-session behavior. |
| Process-death and real-exit cleanup | PASS | The plan distinguishes process death from configuration changes and requires explicit session cleanup. |
| Delayed camera-result contract | FAIL | The required producer-side token propagation and regression test are missing from the affected-file and test plans. |

## Finding 5

Severity:
- [ ] Critical
- [x] Major
- [ ] Minor
- [ ] Suggestion

Category: Camera result contract and late-result protection

Description:
Step 8 requires a session/slot token so that a late camera result can be ignored after cleanup or a replacement. The current producer, `CameraOverlayFragment`, creates a result containing only code, slot, and path (`CameraOverlayFragment.kt:783`); the receiving listener in `GutterBasicInfoFragment` likewise has no token to compare (`GutterBasicInfoFragment.kt:379`). A slot alone cannot distinguish an obsolete result from a newer capture for the same slot. Therefore the plan must change `CameraOverlayFragment`'s input arguments and result bundle, but that source file is absent from Affected Files and the Test Plan does not include the late-result scenario.

Recommendation:
Add `CameraOverlayFragment.kt` to Affected Files and define the token lifecycle precisely: generation before launching, propagation through `newInstance()`, inclusion in `setFragmentResult()`, comparison by the receiver against the active ViewModel token, and invalidation/cleanup behavior. Add unit or instrumentation tests for a late result after cant-open confirmation, after uncheck/restore, after a second capture in the same slot, and after real form exit. Ensure the test asserts that the obsolete result cannot update UI, `currentFormData`, pending-path state, or trigger upload.

Planning Response:
(Planning Agent fills this section after revision.)

Status:
- [x] Open
- [ ] Resolved

## Blocking Issues

- The plan cannot implement its token-based late-camera-result guarantee without including and validating the `CameraOverlayFragment` result-contract change.

## Improvement Suggestions

- Make step 1 visibly numbered as `1.` to match the traceability table.

## Decision

## REQUEST_CHANGES

Planning must resolve Finding 5 before implementation begins. No product-level clarification is required.

## Next Action

- [x] Planning
- [ ] Implementation
- [ ] Requirement Clarification
- [ ] Human Review

---

# Plan Review — Iteration 4

Reviewer: Plan Critic Agent
Review Date: 2026-09-10

## Review Result

- [x] APPROVED
- [ ] REQUEST_CHANGES
- [ ] BLOCKED

## Summary

Finding 5 is resolved. The revised plan includes `CameraOverlayFragment` in scope and specifies generation-token creation, propagation, validation, invalidation, and late-result regression coverage. All prior Major findings are resolved; implementation may begin.

## Checklist Delta

| Item | Result | Notes |
|---|---|---|
| Requirement understood | PASS | All acceptance criteria remain within scope. |
| Repository analysis complete | PASS | Form, draft, photo metadata, pending capture, lifecycle, and result-callback ownership are covered. |
| Architecture impact reasonable | PASS | Activity-scoped ViewModel preserves session-only state across configuration changes without persistence. |
| Affected modules and dependencies identified | PASS | Basic fragment, form Activity, ViewModel, and camera-result producer are included. |
| Risks evaluated | PASS | Lifecycle, metadata, draft batching, dirty merge, and late results have explicit handling. |
| Test and regression plans complete | PASS | AC-001 through AC-009 are traceable; late-result, session, draft, and non-applicable mode cases are included. |
| Implementation steps actionable | PASS | The snapshot, merge, cleanup, and token contracts are defined. |
| Rollback strategy | PASS | Reverting the isolated feature commit restores the current behavior. |

## Finding 5 Resolution

Planning Response:
`CameraOverlayFragment.kt` is now an affected file. The plan defines token generation in the session ViewModel, propagation through `newInstance()`, return in `setFragmentResult()`, receiver validation, and invalidation when the slot or form session is superseded. The test plan covers late results after cant-open confirmation, restore, a second capture, and real form exit.

Status:
- [ ] Open
- [x] Resolved

## Blocking Issues

None.

## Improvement Suggestions

- Keep implementation step 1 visibly numbered when the plan is next edited, so the AC traceability table remains easy to navigate.

## Decision

## APPROVED

Implementation may begin.

## Next Action

- [ ] Planning
- [x] Implementation
- [ ] Requirement Clarification
- [ ] Human Review

## state.yaml Update

```yaml
phase: plan_review
status: approved
next_action: implementation
```

---

# Plan Review — Iteration 5

Reviewer: Plan Critic Agent
Review Date: 2026-09-10

## Review Result

- [ ] APPROVED
- [x] REQUEST_CHANGES
- [ ] BLOCKED

## Summary

The submitted plan has changed materially after iteration 4 approval. It limits the task to a confirmation Alert and explicitly excludes session snapshot and restoration behavior. This lowers approved acceptance criteria and reverses the core product intent. The prior approval is therefore superseded.

## Checklist Delta

| Item | Result | Notes |
|---|---|---|
| Requirement understood | FAIL | The plan omits the required session-only restore behavior. |
| Acceptance Criteria complete | FAIL | AC-005, AC-006, and AC-007 are not implemented; the AC-005 mapping explicitly asserts the opposite behavior. |
| Affected modules identified | FAIL | Excluding the Activity/session owner makes the required snapshot, restore, and draft-isolation work impossible. |
| Test Plan complete | FAIL | The named test file does not match the existing form test seam, and the required restoration, dirty-merge, lifecycle, and draft-isolation tests are absent. |
| Implementation steps actionable | FAIL | The plan cannot satisfy the required behavior with Alert-only changes. |
| Scope appropriate | FAIL | The scope improperly removes required functionality rather than constraining implementation. |

## Finding 6

Severity:
- [x] Critical
- [ ] Major
- [ ] Minor
- [ ] Suggestion

Category: Requirement and acceptance-criteria regression

Description:
The revised plan states that it will "not implement data restoration or new session state" and lists `GutterFormActivity.kt` and `CantOpenSessionViewModel.kt` as excluded. This conflicts with the functional requirement that cleared data be kept only for the current editing session and restored when cant-open is unchecked. It also fails AC-005 (complete restoration), AC-006 (do not overwrite new values), and AC-007 (session-lifetime isolation). The traceability table maps AC-005 to `noRestoreOnUncheck`, which is the inverse of the required outcome; it also omits AC-007 through AC-009.

Recommendation:
Restore the approved implementation scope from iteration 4: session-only snapshot ownership, field/slot dirty merge, draft/API exclusion, lifecycle cleanup, and corresponding Activity/Fragment/camera coordination. Rebuild the AC-001–AC-009 traceability table so every row maps to the required behavior and validation. Do not treat an Alert-only implementation as an acceptable reduction of this feature.

Planning Response:
(Planning Agent fills this section after revision.)

Status:
- [x] Open
- [ ] Resolved

Planning Response:
已撤回 Alert-only 的錯誤收斂，恢復 Iteration 4 已核准的完整範圍。`requirement.md`、`analysis.md`、`plan.md` 現在保留 AC-005、AC-006、AC-007：session-only snapshot、完整照片 metadata 回填、dirty merge、configuration-change 隔離與 process-death／真正離開清理。AC traceability 已重新涵蓋 AC-001～AC-009，並補回 Activity、ViewModel、CameraOverlayFragment、草稿隔離與 late-result 測試。Alert-only 不再作為可接受實作方案。

Status:
- [ ] Open
- [x] Resolved

## Blocking Issues

- The plan removes required restore and session-isolation behavior, so it cannot meet AC-005 through AC-007.

## Improvement Suggestions

None until the critical acceptance-criteria regression is corrected.

## Decision

## REQUEST_CHANGES

Planning must reinstate the approved requirements before implementation can begin. No external clarification is required because the approved `requirement.md` is unambiguous.

## Next Action

- [x] Planning
- [ ] Implementation
- [ ] Requirement Clarification
- [ ] Human Review

## Planning Response — Iteration 5 Follow-up

- Finding 6：已恢復完整 approved scope，不再降低 AC-005～AC-007。
- ISS-0910-1-11：`plan.md` 已補上 Failure Behavior、Security and Privacy、Rollback Plan。
- ISS-0910-1-12：URL 變更明確列為本次 task scope 外的使用者工作區變更；implementation 不得移除、覆寫或提交該變更，只需確保 feat-0910-1 diff 不依賴它。

---

# Plan Review — Iteration 8

Reviewer: Plan Critic Agent
Review Date: 2026-09-10

## Review Result

- [ ] APPROVED
- [x] REQUEST_CHANGES
- [ ] BLOCKED

## Summary

The restored scope and the Failure Behavior, Security and Privacy, and Rollback Plan sections resolve the earlier completeness issues. However, the implementation instructions still conflict with the confirmed URL scope: Step 8 directs removal of unrelated URL changes, while the rollback section directs preservation of the user-owned `GutterApiService.kt` change. The plan must contain one unambiguous instruction before implementation.

## Finding 8 Follow-up

Severity:
- [ ] Critical
- [x] Major
- [ ] Minor
- [ ] Suggestion

Category: Plan scope consistency

Description:
`plan.md` Step 8 still says to remove unrelated URL changes. This conflicts with the explicit user decision that the base-URL change in `GutterApiService.kt` is manual work outside `feat-0910-1`, and with the plan's Rollback Plan, which says not to overwrite it. An implementer cannot safely follow both instructions.

Recommendation:
Replace the URL clause in Step 8 with: "Preserve and exclude the user-owned `GutterApiService.kt` base-URL change from this task and its commit; the 0910-1 implementation must not depend on it." Keep the existing instruction to remove only 0910-1's abandoned `notifyUploadHost=false` work.

Planning Response:
(Planning Agent fills this section after revision.)

Status:
- [x] Open
- [ ] Resolved

## Blocking Issues

- Step 8 must be reconciled with the confirmed user-owned URL change before implementation scope is safe.

## Improvement Suggestions

None.

## Decision

## REQUEST_CHANGES

Planning can resolve this single wording and scope issue without changing product intent.

## Next Action

- [x] Planning
- [ ] Implementation
- [ ] Requirement Clarification
- [ ] Human Review
