# Implementation Plan

## Goal
- 將三個完整 GPKG 與 26 個行政區分區 GPKG 納入離線圖層，依選區以平滑、可回復的方式切換來源。

## Scope
- 29 個 GPKG 資產、source manifest、分區／完整來源選擇、fallback、效能與 lifecycle；不變更後端與無關圖層。

## Affected Files
- `app/src/main/assets/` — 保留 3 個完整 GPKG，新增 13 個可能側溝與 13 個水務局既有資料 GPKG。
- `docs/tasks/feat-1118-2/evidence/asset-manifest.*` — 記錄 29 個檔案的 hash、大小、schema、feature count、extent 與一致性結果。
- `OfflinePolygonDataStore.kt` — 建立 typed source manifest、來源 staging/index、欄位篩選與 per-district fallback。
- `OfflinePolygonTileProvider.kt` — 支援一個或多個 source、generation、空結果與 release。
- `MapOverlayController.kt` — 管理完整／分區模式、差異化來源、atomic overlay swap 與 bounded cache。
- `LayersBottomSheet.kt`、`sheet_layers.xml`、`strings.xml` — 固定順序 13 區、三態全選與立即狀態 callback。
- `MainViewModel.kt`、`MainActivity.kt`、`MapWorkspaceFragment.kt` — 保存／還原選區與既有圖層開關。
- 相關 JVM/instrumentation tests — 覆蓋 manifest、source selection、fallback、state、lifecycle 與效能觀察。

## Implementation Steps
- 在開發機盤點 29 個 GPKG，產生 hash／size／schema／feature count／extent manifest，並比對 13 個分區集合與兩個完整來源。
- 使用 host-side `sqlite3`/Python validator（規劃新增 `docs/tasks/feat-1118-2/evidence/validate_gpkg_sources.py`）讀取每個 GPKG 的 `gpkg_contents`、`gpkg_geometry_columns`、feature rows 與 bounds；以 `layer + district + canonical non-geometry attributes + canonical decoded geometry` 建立 row signature，不使用分區檔自己的 `fid` 作為跨檔 identity。
- Validator 對每個完整來源與 13 個分區集合執行 deterministic diff：metadata、CRS、geometry type、district mapping、feature count 必須符合 manifest；district 檔案作為部分選取的權威來源，完整檔與分區集合需通過 canonical geometry union 比對；逐行政區 signature 差異保留為診斷證據，不作 blocker。
- 定義 29 個來源的 immutable mapping：三個完整來源、13 個 possible 分區、13 個 existing 分區；驗證 table、geometry type、CRS 與檔名對應。
- 將 source selection 抽象為 `Full`、`DistrictSources` 與 per-district fallback，保留未來替換水務局資料來源的擴充點。
- 改造 staging/index，使每個來源以 source hash、index version、layer、district 隔離；完整行政區來源支援 `TOWNNAME` 篩選。
- 依目前圖層開關只建立必要 provider；部分選取時載入對應兩組分區來源，全選時只載入三個完整來源。
- 以受控背景併發建置或取得 source，採差異化 add/remove；保留仍使用的 provider/cache，釋放取消的來源。
- 在背景完成新 generation 的所有必要來源後，原子替換 overlay；舊 generation callback 與 tile 結果直接丟棄。
- 對單一缺失或損毀分區來源執行 per-district fallback；成功來源仍顯示，錯誤記錄且不讓 App 閃退。
- 保留三態全選、半選、狀態保存與既有圖層行為；不讓完整與分區 overlay 同時存在。
- 為 source manager 保留可注入的受控 loader／resource factory 測試 seam；以 deferred loader 先阻塞 generation 1，再完成 generation 2，最後完成 generation 1，驗證只提交 generation 2、上一個完整 overlay 在提交前仍可見，且每個啟用圖層最多一個 active overlay。
- 以固定 20 回合 `full -> district(1) -> district(2,3) -> empty -> full` lifecycle scenario 驗證每次切換先 release 被移除的 source/provider；每回合 commit 後 active provider/overlay 數量不超過當前模式所需數量，`release()` 後為 0，tile cache 只保留 active generation 且不超過每 provider 的 48-entry bound。
- 執行 source consistency、JVM、instrumentation、Debug build、source scan 與 emulator UX/performance validation，補齊 evidence。

## Test Plan
- Host-side asset validation：執行 `python3 docs/tasks/feat-1118-2/evidence/validate_gpkg_sources.py --manifest docs/tasks/feat-1118-2/evidence/asset-manifest.json`；29 個檔案存在、hash/size/schema/feature count/extent 與 full/district geometry union 通過；逐行政區 label 差異列入 manifest 診斷，不阻擋來源採用。
- JVM：source mapping、full/district selection、13 區全選切換、OR、空選擇、per-district fallback、cache key、generation、state persistence；使用 controlled deferred loader 覆蓋 out-of-order completion。
- Instrumentation：UI 三態 CheckBox、預設與重啟恢復、三圖層部分／全選顯示、單一來源失敗、overlay 不重複與 lifecycle。
- Emulator performance：全選、單區、2～3 區、13 區、模式反覆切換；記錄 UI 是否卡頓、ANR、閃退、切換時間、記憶體與 overlay 數量，並以固定 lifecycle scenario 的 provider/overlay/cache bound 作為 pass/fail 輔助證據。
- Build/source scan：Debug build、相關 lint/static checks、29 個 assets package inspection 與無不當完整／分區同時載入。

### Physical Device Test Scope
- Requires physical device: No
- Device/environment: Android Emulator only; host-side GPKG validation on development machine/CI
- In-scope Acceptance Criteria: AC-001–AC-011
- Regression risk: 資產大小、分區 source lifecycle、完整／分區切換、UI smoothness、既有 overlay 行為
- Full regression required: No
- Full regression trigger: Plan/Release Risk 或失敗證據
- Stop condition: 所有指定案例完成，或取得足夠失敗證據

| AC | Environment | Steps | Expected | Evidence |
|---|---|---|---|---|
| AC-001 | Host/CI + APK inspection | 執行 validator 並檢查 APK | metadata/hash/manifest 正確；完整檔與分區集合的 canonical geometry union 一致，逐行政區 label 差異保留診斷 | validator JSON/CSV + APK readback |
| AC-002–AC-005 | Emulator | 操作全選、半選、單區、多區、空選擇 | 三態同步、來源選擇與三層篩選正確 | instrumentation + screenshots |
| AC-006–AC-008 | JVM + Emulator | 以 deferred loader 反序完成 generation，並執行 20 回合 lifecycle | 最新 generation 勝出；舊結果不提交；active overlay/provider/cache 符合 bound，release 後為 0；fallback 無重複 | deterministic test output + map evidence |
| AC-009–AC-010 | Emulator | 重啟 App、切換底圖與其他 overlay | 狀態恢復且既有行為不回歸 | instrumentation + regression results |
| AC-011 | Host/CI + Emulator | 執行測試、build、scan、效能場景 | 無 UI parse、ANR、閃退、重複 overlay | command output + performance report |

## Regression Plan
- 驗證計畫調查、無側溝點、刪除區域、底圖與其他 WMS/WMTS overlay 不受 district source selection 影響。
- 驗證完整 GPKG 的既有顏色、孔洞、MultiPolygon、EPSG:3826、z-index 與 map host 行為。
- 執行固定 out-of-order generation test：generation 1 先開始、generation 2 後開始並先完成；可見 generation 必須為 2，generation 1 完成後不得覆蓋，active overlay 每圖層最多一個。
- 執行固定 20 回合 full/district/empty 切換；每回合 assert 被移除來源已 release、active provider/overlay 不超過當前需求，cache 不超過 `48 * active provider count`；最終 `release()` 後 provider、overlay、cache count 必須為 0。
- 驗證 provider release、map destruction、重啟恢復與快速切換不重複；既有 execution/verification report 僅作歷史背景，不作本次 11 個 AC 的通過證據。

## Risks
- 29 個檔案增加 package/storage/index build cost。
- 多來源載入造成 I/O/CPU/記憶體壓力。
- fallback 與 full/district switching 的資料差異或 overlay race。

## Rollback Plan
- 回退本任務的 26 個分區資產、source selection、fallback、cache/lifecycle 與 UI state 變更，恢復三個完整 GPKG 的既有路徑。

## Current Behavior
- 現有分支已有三個完整 GPKG 與行政區篩選 UI，但沒有 26 個分區資產與分區優先載入策略。

## Expected Behavior
- 全選使用三個完整檔；部分選取使用兩組對應分區檔與行政區完整檔篩選，背景原子切換並能 per-district fallback。

## Acceptance Criteria Traceability
| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | Asset manifest, canonical signature validator and package inspection | Host/CI validator output + APK readback |
| AC-002 | UI/default state | JVM + instrumentation |
| AC-003 | Three-state selection | JVM + instrumentation |
| AC-004 | Source selection strategy | JVM + emulator |
| AC-005 | Multi-source query/filter | JVM + emulator map evidence |
| AC-006 | Generation and atomic swap with controlled out-of-order completion | JVM deterministic generation test + instrumentation |
| AC-007 | Fallback/error handling | JVM + instrumentation |
| AC-008 | Resource lifecycle with repeated fixed scenario and bounds | JVM counters + emulator lifecycle/performance evidence |
| AC-009 | Persisted state | MainViewModel + restart instrumentation |
| AC-010 | Regression preservation | Existing map tests + source review |
| AC-011 | Build/performance/source scan | CI-equivalent commands + emulator report |

## Failure Behavior
- 缺失／損毀分區來源：該行政區 per-district fallback 到完整檔篩選；完整來源也失敗則該來源空白、記錄錯誤、App 繼續運作。
- 新來源尚未完成：保留上一個完整結果；新 generation 全部準備完成後才切換。
- 過期 callback/tile：依 generation 丟棄。
- 無選區：三個指定圖層空白，不顯示提示。

## Security and Privacy
- 不新增權限、憑證、個資或後端 API；GPKG 與選擇狀態存於 App 資產／app-private cache／本地偏好設定。

## Open Questions
- 無。
