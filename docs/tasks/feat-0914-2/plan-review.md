# Plan Review

## Inputs Reviewed
- `AGENTS.md`
- `ai/architecture.md`
- `ai/plan-critic-rules.md`
- `docs/tasks/feat-0914-2/{requirement,knowledge-resolution,analysis,plan,state}.md`
- 現行入口、表單回傳、草稿持久化、Room migration 與 pending-draft 實作。

## Checklist

| Check | Result | Evidence |
|---|---|---|
| Requirements and acceptance criteria understood | Pass | `requirement.md` 的 AC-001 至 AC-007 與 traceability table 一致。 |
| Repository and architecture analysis complete | Fail | 正式登入後入口為 `MainShellActivity` 的 `MapWorkspaceFragment`，但 plan 將 `MainActivity` 列為主要入口與協調者。 |
| Affected modules and dependencies identified | Fail | 未列出實際 live map workflow 的 `MapWorkspaceFragment`；現有全域 SPI_NUM 去重／刪除 call sites 也未列為需變更項目。 |
| Implementation steps actionable | Fail | 多項資料隔離規則只描述目標，尚未指定如何改掉現有可覆寫／誤刪其他草稿的行為。 |
| Test, regression, risk, rollback plans | Fail | 有概略覆蓋，但缺少對實際入口、同 SPI_NUM 草稿隔離、建立時間不變與所有寫入路徑的具體測試。 |
| Scope, open questions, security/privacy | Pass | 範圍、關閉語意與私有資料處理符合需求；無需外部產品決策。 |

## Findings

### Finding 1
Severity: Major
Category: Affected Modules / Architecture

Description:
計畫把 `MainActivity.kt` 視為主地圖新增入口，但登入後啟動的是 `MainShellActivity`，實際 FAB 與草稿／表單流程在 `MapWorkspaceFragment.kt`。目前 `MapWorkspaceFragment` 仍直接呼叫 `openAddGutterFlow()`，並持有單一 `currentSessionDraftId`、表單 result callback 與上傳完成清理。若未把它列為主要修改與測試目標，AC-001 至 AC-006 不會在正式使用流程生效。

Recommendation:
修訂 Affected Files、步驟 3、5、7 與測試計畫，以 `MapWorkspaceFragment`／其 `childFragmentManager` 為正式入口與回傳宿主；若需維護 legacy `MainActivity`，明確列為相同行為的同步修改或說明其不在產品路徑。加入登入後進入 map tab 的 instrumentation coverage。

Planning Response:
已修訂 `analysis.md` 與 `plan.md`：`MapWorkspaceFragment` 為唯一正式新增入口、清單 sheet 的 `childFragmentManager` 宿主、表單／選點 result 與 marker／preview 協調者；`MainShellActivity` 列為登入後 map tab 的驗證入口。`MainActivity` 明確標示為非產品路徑的 legacy duplicate，不納入本工項行為修改。Test Plan 增加從 `MainShellActivity` 登入並進入 map tab 的 instrumentation。

Status: Resolved — pending re-review

### Finding 2
Severity: Major
Category: Data Isolation / Regression Plan

Description:
現有 `GutterDraftCoordinator.autoSaveSessionDraft()` 會依 `SPI_NUM` 尋找其他 draft、沿用其 ID，並刪除同 SPI_NUM 的 draft；`deleteDraftsBySpiNum()` 也會刪除全部同 SPI_NUM 草稿，且已由 `MainActivity` 與 `MapWorkspaceFragment` 呼叫。這與 AC-005／AC-006 的「各草稿互不覆寫」及「成功上傳僅移除該條」直接衝突。計畫僅以「獨立 upsert」和「單筆刪除」描述目標，沒有列出要移除、限制或保留這些既有跨草稿操作的規則與 call sites。

Recommendation:
在計畫中明定 multi-gutter 工作流程只能用 item 的 draft ID 作 upsert、submit success 與本機照片清理的唯一鍵；盤點並修改上述去重／批次刪除呼叫，避免其作用於 multi-gutter 草稿。若舊的單條恢復流程仍需 SPI_NUM 去重，須以明確 workflow boundary 隔離，並增加兩筆相同／後補 `SPI_NUM` 的交錯編輯與單筆上傳回歸測試。

Planning Response:
已在 plan steps 2–4 明定持久化 ownership：`LEGACY_SINGLE` 保留舊流程；`MULTI_GUTTER` 只能透過 item draft ID upsert、刪除、照片清理與送出成功收尾。舊 `autoSaveSessionDraft` SPI_NUM dedup 與 `deleteDraftsBySpiNum` 將只處理 legacy rows，`MapWorkspaceFragment` 的新清單 workflow 一律改用 ID-only API。Test Plan 加入同 SPI_NUM／後補 SPI_NUM 的交錯更新、legacy batch delete 不觸及 multi rows、單筆成功上傳僅刪單筆。

Status: Resolved — pending re-review

### Finding 3
Severity: Major
Category: Persistence / Test Plan

Description:
需求要求建立時間固定到秒、每次有效修改立即保存。現有模型只有 `savedAt`，所有寫入路徑都會覆寫它，包括 `GutterDraftCoordinator.ensureDraftExists()`、`autoSaveSessionDraft()`、`GutterFormActivity.syncSessionDraftNow()` 與 `PhotoSlotUploadCoordinator`；draft ID 也以 `System.currentTimeMillis()` 產生。計畫提到新增建立時間與 atomic upsert，但未定義欄位名稱、migration default/backfill、每個寫入端保留 `createdAt` 的契約，或多筆快速新增的唯一 ID 策略。因此無法證明 AC-003 與 AC-005。

Recommendation:
補上資料模型與 repository contract：不可變 `createdAt`、可變 `savedAt`、舊列 migration/backfill 規則，以及碰撞安全的 draft-ID 產生方式。逐一列出 coordinator、form immediate sync 與照片上傳狀態更新必須透過同一保留建立時間的 upsert。測試需涵蓋同毫秒／快速建立、更新與照片狀態變更後 `createdAt` 不變、舊資料 migration，以及 Activity／程序重建後兩筆草稿可由 pending list 各自恢復。

Planning Response:
已在 plan steps 1–3 定義資料契約：新增 immutable `createdAt`、保留可變 `savedAt`，migration 以 `created_at = saved_at` backfill；新 ID 用 repository-backed monotonic allocator，起始值為 `max(now, persistedMaxId + 1)`，同 process 同步遞增，insert collision 時重新配置並回寫 item/form result。`ensureDraftExists`、coordinator auto-save、`GutterFormActivity.syncSessionDraftNow` 及 `PhotoSlotUploadCoordinator` 全數改走保留 `createdAt`／ownership 的統一 upsert。Test Plan 亦新增快速建立、collision、migration、照片狀態寫入與程序重建 coverage。

Status: Resolved — pending re-review

## Suggestions
- 將「清單在重建後從 repository 恢復」明確定義為僅恢復仍屬於未關閉工作階段的項目，或改為符合需求的「各自從既有草稿列表恢復」；避免把既有不相關 pending drafts 混入新清單。
- 對清單列的節點計數共用既有「有效資料」判定規則，並以測試固定虛擬點／pending photo 的計數語意。

## Decision

REQUEST_CHANGES

規劃者可在不需要額外產品決策的前提下，補足正式入口、資料隔離、建立時間與完整測試範圍後重新送審。

## Re-review — 2026-09-14

重新核對 `state.yaml`、`requirement.md`、`analysis.md`、`plan.md`、issue log 與實際 map workflow 後，`plan.md` 未包含前一輪要求的修訂：它仍以 `MainActivity` 為主要入口，未列出 `MapWorkspaceFragment` 的正式流程；亦未定義 SPI_NUM 去重／批次刪除的 workflow boundary，或不可變 `createdAt`、migration 與碰撞安全 draft ID 的資料契約。

### Findings status
- Finding 1 — Open：正式入口與回傳宿主仍未修正。
- Finding 2 — Open：跨草稿 SPI_NUM 去重／刪除仍未有明確的替代規則與 call-site 計畫。
- Finding 3 — Open：建立時間、所有寫入路徑與 draft-ID 生成策略仍未具體化。

## Re-review Decision

REQUEST_CHANGES

`state.yaml` 維持 `phase: plan_review`、`status: changes_requested`、`next_action: planning`；規劃文件修訂後再進行下一輪審查。

## Planning Response — 2026-09-14

前一輪三項 Major 已依上述回應修訂 `analysis.md` 與 `plan.md`。規劃重新送交 Plan Review；尚未修改 production code。

## Requirement Revision — 2026-09-15

- 使用者決定 Toolbar 改為左側關閉／刪除、右側新增。
- 有未上傳項目時才顯示關閉保存 Alert；沒有未上傳項目時直接關閉。
- 成功送出後必須先進入檢視側溝頁面，關閉檢視頁後才回到本次清單並移除成功項目；失敗 Alert 確認後回到本次清單並保留失敗項目。
- 此變更新增 AC-008 並使既有已核准／已實作 revision 不再涵蓋完整 requirement；`state.yaml` 已退回 Planning，待重新 Plan Review。

## Re-review — 2026-09-14 (Approved)

| Check | Result | Evidence |
|---|---|---|
| Requirements and acceptance criteria | Pass | AC-001 至 AC-007 均保留，並更新為正式 map tab 與 ID-only flow 的 traceability。 |
| Repository and architecture analysis | Pass | `MainShellActivity → MapWorkspaceFragment`、`childFragmentManager`、表單 result 與 legacy `MainActivity` 範圍已明確。 |
| Affected modules and dependencies | Pass | 新增 multi-session UI/coordinator、Room entity/DAO/migration/repository、form、photo writer、pending UI 與正式入口皆已列出。 |
| Implementation and data-isolation plan | Pass | `MULTI_GUTTER` 的 draft-ID-only upsert/delete 與 `LEGACY_SINGLE` 的 SPI_NUM boundary 已具體定義。 |
| Time, identity, migration | Pass | `createdAt`／`savedAt`、舊列 backfill、monotonic allocator、collision retry 及所有寫入端的統一 contract 已具體規劃。 |
| Test and regression plan | Pass | 包含正式入口、交錯同 SPI_NUM、快速 ID 配置、migration、表單／選點／照片即時寫入、configuration/process 重建與單筆上傳清理。 |
| Scope, risks, rollback, open questions | Pass | 範圍適當；風險、rollback 與無阻擋開放問題均已記錄。 |

### Finding Resolution
- Finding 1 — Resolved：實作與 instrumentation 以 `MapWorkspaceFragment` 的正式 flow 為準。
- Finding 2 — Resolved：multi-gutter persistence 僅以 draft ID 操作；legacy SPI_NUM 行為以 ownership 隔離。
- Finding 3 — Resolved：時間、ID 配置、migration 和所有寫入端的契約均已可執行及測試。

## Improvement Suggestion
- 實作 Room migration 時，明確將既有列的 workflow ownership backfill 為 `LEGACY_SINGLE`；這是計畫既有相容性意圖的直接落實，應納入 migration test fixture。

## Re-review Decision

APPROVED

計畫已達實作準備條件。可進入 Implementation；尚未執行任何 production code 變更或驗證。

## Re-review — 2026-09-15 (Requirement Update)

新 requirement 新增並明確化下列行為，已與修訂後計畫逐項核對：

| Requirement change | Plan coverage | Result |
|---|---|---|
| Toolbar 左側關閉／刪除、右側新增 | Step 6 與 toolbar instrumentation 明確定義。 | Pass |
| 無未上傳項目時直接關閉 | Step 7 與有／無項目的 Alert branch instrumentation 已覆蓋。 | Pass |
| 成功後先進檢視頁，關閉後回到同一清單才移除成功項目 | Step 8 將刪除時機、ID-only 清理與禁止後端 delete API 明確化；Instrumentation 覆蓋完整回傳鏈。 | Pass |
| 失敗 Alert 確認後回到同一清單並保留項目 | Step 8、AC-008 traceability 與 failure-path instrumentation 已覆蓋。 | Pass |
| 既有檢視、上傳、照片與草稿流程不回歸 | Affected modules、Regression Plan 與 AC-007 已涵蓋。 | Pass |

### Review Checklist
- Requirements and acceptance criteria: Pass — AC-001 至 AC-008 均有 implementation step 與驗證對應。
- Repository analysis and affected modules: Pass — `MapWorkspaceFragment`、`inspectLauncher`、表單／檢視回傳、草稿與照片清理責任皆已納入。
- Implementation and failure paths: Pass — success、photo failure、submit failure 與關閉分流都有明確狀態與資料保留規則。
- Tests, risks, scope, rollback and open questions: Pass — 新增需求具體測試已列入，無需外部決策。

## Re-review Decision — 2026-09-15

APPROVED

新需求已被可執行的計畫與驗證範圍完整吸收。可進入 Implementation；本 review 未修改 production code，也未執行程式驗證。
