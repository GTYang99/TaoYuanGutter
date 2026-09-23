# Repository Analysis

## Current Behavior

- baseline 已在 `GutterSessionDraft`／`DraftEntity` 保存 `hasSubmittedStoreDitch`，並完成 Room 3→4 migration 與 auto-save preservation。
- baseline 的 `PendingDraftAdapter` 已顯示 submitted／unsubmitted tag，並以 `SPI_NUM` 隱藏既有側溝 tag。
- baseline 已將新增與帶 `SPI_NUM` 編輯流程接到 `GutterRepository.storeDitch` method-entry boundary；本次 re-plan 的缺口不再是 marker 時機，而是 submitted draft 恢復後仍可進入一般編輯表單。
- 待上傳列表點擊後由 `GutterSessionFlowCoordinator` 恢復 `AddGutterBottomSheet`；目前恢復流程沒有把 `hasSubmittedStoreDitch` 轉成表單唯讀狀態。
- `GutterFormActivity` 目前可透過 `setEditable(false)` 顯示檢視狀態，但該狀態會隱藏可送出的操作；沒有「內容唯讀但保留重新上傳」的模式。`AddGutterBottomSheet` 也沒有針對已提交恢復草稿鎖定新增節點、節點操作、類型與提交按鈕文案。

## Expected Behavior

- 草稿資料在兩個 `storeDitch` 呼叫邊界前先持久化 `hasSubmittedStoreDitch=true`，使失敗、逾時、無回應與重啟後仍能辨識「曾提交過上傳」。
- 未進入 API 呼叫的草稿保留 `false`，列表顯示「未提交上傳」；可辨識為既有檢視／編輯側溝的草稿則隱藏 tag。
- tag 視覺沿用 `item_waypoint.xml` 的 `tvVirtualBadge` 尺寸／排版語意，新增未填滿的邊框樣式。
- 一般已提交草稿點擊恢復後，所有表單內容與編輯入口均鎖定，只保留返回與「重新上傳」；未提交草稿維持原本可編輯流程。
- 唯讀狀態使用既有 disabled／loading overlay 視覺，遮住可編輯表單區域並攔截其觸控，不遮住返回與重新上傳按鈕。
- 重新上傳使用草稿中保存的完整內容與既有 `storeDitch` payload；失敗／逾時／中斷保留草稿與 submitted marker，成功沿用刪除及本機清理。

## Root Cause

- 原規劃把提交狀態放在 `AddGutterBottomSheet` 的 `onGutterSubmitted`／`onGutterSubmitting` Host 回呼；這些回呼雖然位於 API 呼叫前，但實際 `GutterRepository.storeDitch(...)` 尚未進入，存在「已標記但尚未真正進入 storeDitch」的控制流程空窗。
- 新增與編輯各自從 Host 進入 repository，沒有共用且可測試的 submission boundary；也沒有明確規定 current draft id 缺失或 Room row 不存在時的處理。
- 原 plan 只列出 migration／UI 測試目標，未指定 Room 3→4 fixture、repository ordering spy 或 adapter binding assertions，因此不足以直接產出 AC-001～AC-005 的可重現證據。

## Planning Resolution

- 將 `GutterRepository.storeDitch(...)` method entry 定義為新增／編輯共用的唯一提交邊界，在 Retrofit 呼叫前同步執行 `onRequestEntered` marker callback；callback 完成後才允許發出 request。
- 有固定 draft id 的新增／恢復草稿先以目前 session snapshot ensure Room row，再標記；若 draft id、snapshot 或本機寫入缺失，回傳 local error 並停止遠端 request。直接檢視編輯若沒有 pending draft id，callback 明確 no-op，維持既有 API 流程且不建立可被列表標記的草稿。
- 以 repository ordering test、手動建立 v3 schema 的 Android migration test、以及 `PendingDraftAdapter` instrumentation assertions 補足證據缺口。
- 將 `hasSubmittedStoreDitch` 沿恢復鏈路傳遞到 `AddGutterBottomSheet` 與 `GutterFormActivity`，建立獨立的 submitted-draft read-only mode，不與既有 inspect `isViewMode` 混用。
- 在 `AddGutterBottomSheet` 與 `GutterFormActivity` 分別鎖定各自擁有的表單控制；共用 `GutterBasicInfoFragment`／`GutterPhotosFragment` 的既有 `setEditable(false)` 能力，另保留重新上傳 action。
- 以現有 overlay 顏色／阻擋觸控規範實作局部遮罩，並以恢復、返回、長按刪除、重新上傳成功／失敗測試確認不覆寫草稿內容。

## Submitted Read-only State Contract

- `GutterSessionFlowCoordinator.createResumeAction` receives the full `GutterSessionDraft`. It derives `submittedDraftReadOnly` through the same policy as the list tag: `hasSubmittedStoreDitch == true` and no `SPI_NUM` on the START waypoint. The `ResumeMapSheet` keeps only the draft id, the Boolean mode flag, and the existing in-process resume object; it does not serialize a complete submitted snapshot into Fragment arguments or saved state.
- The submitted restore transport is intentionally bounded: `AddGutterBottomSheet` arguments, `GutterFormNavigator` Intent extras, and `Activity.onSaveInstanceState` carry only `draftId` and `submittedDraftReadOnly` (plus the existing small mode/index values). The full submitted draft is reread from Room when the sheet/form is restored and immediately before a retry. The existing JSON argument may remain for the ordinary unsubmitted flow, but it is not an authoritative submitted source and is not duplicated with a full snapshot for submitted mode.
- `LocationPickerHost.openWaypointForEdit(sheet, waypointIndex, submittedDraftReadOnly)` passes the Boolean explicitly to both `MainActivity` and `MapWorkspaceFragment`; both `openAddForm` implementations pass it to `GutterFormNavigator.buildAddIntent`, which writes `GutterFormActivity.EXTRA_SUBMITTED_DRAFT_READ_ONLY`. `GutterFormActivity` restores only the id/flag from the Intent and saved state; it is independent from `isViewMode`, so the inner form can be non-editable without hiding the outer full re-upload action.
- After recreation, Room is authoritative again. If the row is missing or cannot be decoded, the submitted screen stays in a local-error state and cannot call the API; it does not fall back to stale JSON or a partial saved-state payload. If Room changed after the first restore, the next retry uses the latest valid Room row and creates a fresh in-memory snapshot.
- Submitted-mode back returns a dedicated `RESULT_SUBMITTED_READ_ONLY_RETURN` without `RESULT_OK` or `RESULT_DELETE`. Both Host result handlers only re-show the existing sheet and reset the pending index; they must not merge form data, clear a waypoint, refresh mutable session data, or invoke `onWaypointsChanged`.

## Submitted Read-only No-write Contract

- `GutterFormActivity.onPause`, `queueSessionDraftSync`, `queuePhotoDraftSync`, `syncSessionDraftNowBlocking`, `syncSessionDraftNow`, `dispatchResultAfterPendingPhotoUploads`, `saveAndFinish`, `buildAndFinishWithResult`, and `handleNavigateBack` all branch on `submittedDraftReadOnly`. In this mode they never call `GutterSessionRepository.save`, `syncSessionDraftNow`, `setResult(RESULT_OK)`, or a content-mutating Host callback.
- Submitted mode disables photo capture/replacement, location picker, import, virtual toggle, field editing, node actions and inner completion/edit actions. `AddGutterBottomSheet` also suppresses `onWaypointsChanged`, draft sync and mutable result handling for the submitted session. Return and the outer re-upload button remain enabled.
- Activity recreation preserves only `draftId`／`submittedDraftReadOnly` and rebuilds a fresh immutable snapshot from Room. A canceled/read-only return is observational only; the Room row, waypoint list, submitted marker and payload source must remain byte/logically equivalent.

## Re-upload Source of Truth

- At submitted-draft restore and immediately before each re-upload attempt, read `GutterSessionRepository.getById(draftId)`. If the row is missing or cannot be decoded, return a local error and do not call either the photo-upload API or `storeDitch`.
- The Room row is the persisted source of truth. After each read, create an immutable in-memory snapshot containing draft id, `spiTyp`, `kind`, offline/single-point flags, waypoint order, uid, coordinates, every `basicData` key, photo URI/captured-at/img-id/upload-state/error metadata, and all other saved fields. The snapshot never enters a Fragment argument, Intent extra, or saved-state Bundle. The mutable `AddGutterBottomSheet.waypoints`／form result is never used as the submitted re-upload source.
- Re-upload has two API projections and keeps the existing API contract: (1) build `StoreDitchRequest` through `StoreDitchNodeRequestMapper` and compare only its supported deterministic fields—`spiNum`, `spiTyp`, curve flag, node order/type, node ids, coordinates, virtual/connection flags, form numeric/text fields, and existing `imgIds`; `storeDitch` does not carry photo URI or upload-state fields, and its `capturedAt` projection remains the mapper-defined `null`; (2) build/capture the existing node-image multipart inputs from the same immutable snapshot and compare photo URI, captured-at, file category, node ownership, existing img-id and upload-state/error transition separately. No API model or backend payload is expanded.
- Photo URI normalization is transport-only: create request/upload copies, without saving normalized values back to Room, changing the immutable snapshot, or emitting `onWaypointsChanged`. Persisted fields not transmitted by a given API projection remain unchanged in Room across retry/failure. Retry rereads Room and creates fresh projections.
- Request/photo capture tests compare each projection to the corresponding saved snapshot fields; success deletes that same draft, while every failure／timeout／cancellation leaves the source row and `hasSubmittedStoreDitch=true` available for the next retry.

## Affected Modules

- `pending`：草稿 data class、Room entity/repository、coordinator、database migration、列表 adapter。
- `api/GutterRepository.kt`：定義共用 `storeDitch` method-entry boundary 並在 Retrofit 呼叫前通知 marker。
- `gutter`：新增／編輯把目前 draft marker callback 傳入共用 repository boundary；既有 Host loading callbacks 維持原責任。
- `gutter`：`GutterSessionFlowCoordinator`／`GutterFormNavigator` 傳遞 submitted read-only flag；`AddGutterBottomSheet`、`GutterFormActivity`、`GutterBasicInfoFragment`、`GutterPhotosFragment` 實作唯讀狀態、遮罩與重新上傳入口。
- `MainActivity`、`MapWorkspaceFragment`：兩個 `LocationPickerHost` 實作傳遞 flag 並在 dedicated read-only return 僅恢復 sheet，不合併 Activity result。
- `MainActivity`、`MapWorkspaceFragment`：接收提交邊界通知並更新目前草稿。
- `res/layout`、`res/drawable`、`res/values`：列表 tag view、未提交 tag drawable、唯讀遮罩／重新上傳文案資源。
- `app/src/test`：序列化、submission ordering、狀態判定與列表 tag 規則測試。
- `app/src/test`：不依賴 Android runtime 的 submitted policy／snapshot projection／mapper 與 photo candidate pure tests。
- `app/src/androidTest`：Room migration fixture、pending adapter/layout assertions、兩個 Host 的 Intent／Activity recreation／result handling、submitted UI 與草稿列表流程回歸；所有 Android-dependent assertions 均不放在 `app/src/test`。

## Dependencies

- Room schema version 3 與既有 `MIGRATION_1_2`／`MIGRATION_2_3`。
- `GutterDraftCoordinator` 的目前 session draft id 與 `GutterSessionRepository` 的同步 API。
- `AddGutterBottomSheet.LocationPickerHost` 的新增／編輯提交生命週期。
- `tvVirtualBadge` 的現有主色、白字、padding 與文字尺寸樣式。
- `GutterFormActivity`／`GutterBasicInfoFragment`／`GutterPhotosFragment` 既有的 editable/view-mode 控制與 `activity_gutter_form.xml` overlay 層。
- `GutterSessionRepository.getById` 與 `PhotoUriStore`：分別提供 re-upload Room source 與 transport-only URI normalization。
- Android instrumentation runtime（現有 AndroidX JUnit／Espresso）負責 Host、Intent、Fragment／Activity recreation 與 UI 行為；JVM source set 不新增 Robolectric、Mockito、MockK 或其他 Android runtime dependency。
- `StoreDitchNodeRequestMapper`、`GutterRepository.uploadNodeImage` 與既有 photo candidate／multipart builder：分別提供 storeDitch request projection 與 photo-upload projection。

## Risks

- 若提交狀態只在 API 回傳失敗後才寫入，無法滿足「呼叫後逾時／閃退」的核心需求；必須在兩個實際 API 呼叫前落盤。
- Room migration、Gson Bundle 傳遞及 repository upsert 任一邊界漏欄位，都可能讓狀態重啟後遺失或舊草稿無法讀取。
- Room write 與 HTTP request 不可能跨本機／遠端形成物理 atomic transaction；本任務以進入 `GutterRepository.storeDitch` method 作為可觀測 submission boundary。
- 將既有側溝草稿誤判為一般草稿會顯示不應出現的 tag；需在 adapter 端明確以 `SPI_NUM`／既有編輯身份隱藏。
- tag 若直接塞入現有 ConstraintLayout 而未調整標題與箭頭約束，長文案可能擠壓或截斷既有內容。
- submitted read-only 若誤用既有 inspect `isViewMode`，可能隱藏重新上傳入口；需以獨立旗標鎖定內容而不移除整份上傳 action。
- 遮罩若覆蓋整個表單容器，會誤攔截返回／重新上傳；需把遮罩邊界限定在可編輯內容區。
- 任一 lifecycle/result callback 若仍把 submitted form snapshot 寫回 Room，返回或 Activity recreation 就可能改變 re-upload payload；需以 no-write guard matrix 覆蓋每條路徑。
- 若重新上傳取用 mutable `waypoints` 而非 Room snapshot，表單 normalization、result merge 或 photo worker 可能造成欄位／順序／photo metadata 漂移；request capture 必須逐欄比對。
- 若把完整 submitted snapshot 放進 Fragment arguments、Intent 或 saved state，可能超過 Android transaction／Binder 限制；只傳 `draftId`／Boolean，重建後重新讀 Room。
- 若把 Host／Intent／recreation 測試放進沒有 Android runtime 的 JVM source set，測試會無法執行或產生虛假的驗證證據；Android-dependent cases 必須在 instrumentation。
- 若把 photo URI／captured-at／upload-state 當成 `StoreDitchRequest` 欄位驗證，會與既有 API 契約衝突；需分成 storeDitch projection 與 node-image photo projection。

## Unknowns

- 舊版草稿沒有 API 呼叫歷史，無法從本機資料可靠推導提交狀態；本計畫先以 migration default false 處理，並以既有 `SPI_NUM` 識別規則優先隱藏 tag。
- `SPI_NUM` 既有側溝維持既有 inspect／edit 行為，不套用一般已提交草稿的 read-only policy；一般草稿的 submitted flag 才觸發唯讀模式。
- 目前沒有現成的 submitted-draft read-only UI instrumentation；需新增 Activity／Fragment harness，Host／Intent／recreation 測試放在 `app/src/androidTest`。若 instrumentation harness 或裝置不足，將相關案例標為 `NOT VERIFIED`，不可由 compile 或 tag unit test 代替。
- 目前 `GutterRepository.storeDitch` 的 boundary callback 是同步 `() -> Unit`；本次不改為 suspend，marker 仍在 Retrofit request 前同步完成。
- `StoreDitchRequest` 的 `capturedAt` 欄位由既有 mapper 固定為 `null`，而 URI／upload-state/error 僅屬於 persisted/photo-upload projection；本計畫不改 API model。
