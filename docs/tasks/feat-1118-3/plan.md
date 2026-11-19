# Implementation Plan

## Goal
- 移除 Android client 的「銜接點」／`IS_TIEINPOINT` 執行中行為，同時安全相容舊資料並確保 App 更新後不閃退。

## Scope
- 僅處理表單、檢視、點位下拉、Intent／draft 邊界、`storeDitch` request 與相關測試；保留 `IS_CANTOPEN`、虛擬點、照片、一般點驗證與其他 API 行為。

## Affected Files
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormContract.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterInspectActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterInspectPhotosFragment.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`
- `app/src/main/java/com/example/taoyuangutter/pending/GutterDraftCoordinator.kt`
- `app/src/main/java/com/example/taoyuangutter/api/GutterApiModels.kt`
- `app/src/main/java/com/example/taoyuangutter/api/StoreDitchNodeRequestMapper.kt`
- `app/src/main/res/layout/fragment_gutter_basic_info.xml`
- `app/src/main/res/values/strings.xml`
- 相關 JVM／instrumentation 測試與 fixtures

## Implementation Steps
- 盤點所有 `IS_TIEINPOINT` 與「銜接點」讀寫、顯示、驗證、互斥、照片豁免及 request 入口，建立移除清單。
- 移除表單控制項、文案、預設值、監聽與銜接點特殊清除／互斥邏輯，讓舊值載入後按一般點規則驗證。
- 移除檢視頁與點位下拉的銜接點標記及相關 renderer／photo preload 分支；保留虛擬點與 `IS_CANTOPEN` 行為。
- 調整 Intent／Bundle／form result／draft normalization，使 legacy key 可讀但不回寫；對部分損壞 draft 採保留可解析資料與略過不可解析 waypoint。
- 依既有 submitted/read-only policy 實作唯一例外：已上傳唯讀舊銜接點草稿可解鎖修復，其他唯讀草稿維持鎖定。
- 從 request model／mapper 的所有分支移除 `IS_TIEINPOINT` 輸出，確認一般、無法開蓋、虛擬及 legacy input 的 JSON 均省略 key。
- 新增／更新 JVM 與 instrumentation 測試，覆蓋 UI 不顯示、一般點必填、legacy response／draft、唯讀例外、損壞 draft、request omission 與既有回歸。

## Test Plan
- JVM：request mapper JSON、Gson legacy response、表單正規化、一般點驗證、draft cleanup、損壞資料保留與 read-only 例外。
- Instrumentation：新增／編輯／檢視／點位下拉 UI、legacy draft restore、唯讀銜接點解鎖、一般點補填後送出。
- Build／source scan：Debug build、相關 unit／instrumentation、確認執行中 UI 與 request 不再使用銜接點行為。
- Upgrade：以 commit `78d6df31` 建立 baseline APK，安裝並建立含舊銜接點／草稿資料，再更新至本 branch APK，保留 app data 驗證啟動與流程。

### Physical Device Test Scope
- Requires physical device: Yes
- Device/environment: Android 9+ 裝置或穩定 emulator；先安裝 commit `78d6df31` baseline APK，再以保留資料方式更新本版本
- In-scope Acceptance Criteria: AC-001, AC-002, AC-004, AC-005, AC-006, AC-007, AC-008
- Regression risk: 升級資料遺失／閃退、舊銜接點被錯誤套用豁免、唯讀草稿狀態錯誤、一般點補填流程失效
- Full regression required: No
- Full regression trigger: 升級或 legacy draft 失敗、`IS_CANTOPEN`／虛擬點回歸，或 instrumentation 失敗證據
- Stop condition: 所有指定案例完成，或取得足夠失敗證據並記錄為對應 issue

| AC | Environment | Steps | Expected | Evidence |
|---|---|---|---|---|
| AC-001 | Device/emulator | 開啟新增與編輯表單 | 無銜接點控制項或文案 | UI test result |
| AC-002 | Device/emulator | 開啟檢視頁與點位下拉，載入 legacy value | 不顯示銜接點標記 | UI result/screenshot on failure |
| AC-004 | Upgrade/device | 更新 baseline app，開啟舊 response、Intent 與 draft | 啟動及載入不閃退，按一般／虛擬點規則 | logcat + test result |
| AC-005 | Device/emulator | 使用缺少一般必填欄位的 legacy tie-in 資料送出，再補填 | 首次阻止送出，補填後可送出 | UI/request evidence |
| AC-006 | Device/emulator | 開啟已上傳唯讀 legacy tie-in draft | 可例外解鎖；其他唯讀 draft 仍鎖定 | UI result |
| AC-007 | Upgrade/device | 從 `78d6df31` 更新並開啟草稿列表 | App、草稿列表與資料恢復不閃退 | logcat + screenshot/result |
| AC-008 | Upgrade/device | 載入部分損壞 draft | 可用資料保留，損壞 waypoint 略過並可重建 | database/UI result |

## Regression Plan
- 驗證 `IS_CANTOPEN` 與一般點的互斥、必填、照片豁免及 request 行為不變。
- 驗證虛擬點的欄位隱藏、提交與檢視流程不變，且優先於 legacy 銜接點值。
- 驗證照片、草稿保存／恢復、inspect-to-edit、已上傳唯讀草稿及其他 waypoint 不受影響。
- 驗證所有 `storeDitch` request 路徑均不輸出 `IS_TIEINPOINT`。

## Risks
- Legacy data cleanup 若在啟動時批次改寫，可能造成資料遺失；採載入時記憶體清理、下次保存時再寫回。
- 強制移除 model／constant 可能破壞舊 Bundle 或 Gson；保留相容讀取邊界並以未知欄位忽略。
- 唯讀例外若判斷錯誤可能擴大可修改範圍；只允許明確識別的已上傳 legacy tie-in draft。
- 無法取得 baseline APK、穩定裝置或資料格式時，驗證標為 `NOT VERIFIED`。

## Rollback Plan
- 若驗證失敗，保留 task branch 與 evidence，回退本任務新增 commit；不重置或刪除原有工作區資料。

## Current Behavior 
- App 目前將 `IS_TIEINPOINT` 串接到表單、檢視、draft、Intent／result 與 `storeDitch` request，銜接點會影響互斥、明細／照片豁免與標記顯示。

## Expected Behavior
- App 不再顯示或產生銜接點狀態，所有 request JSON 省略 `IS_TIEINPOINT`；舊值安全忽略並按一般點處理，虛擬點規則優先，更新資料不得造成閃退。

## Acceptance Criteria Traceability
| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | 移除表單控制項與狀態邏輯 | 表單 instrumentation + source scan |
| AC-002 | 移除 inspect／下拉標記 | inspect/dropdown instrumentation |
| AC-003 | 移除 request model／mapper 輸出 | mapper JVM tests + JSON assertions |
| AC-004 | legacy read／draft normalization 與虛擬點優先 | Gson/draft JVM + upgrade test |
| AC-005 | 恢復一般點驗證與補填流程 | form instrumentation + request assertion |
| AC-006 | 限定唯讀 legacy tie-in 解鎖 | draft policy JVM/instrumentation |
| AC-007 | 保留資料升級與啟動防護 | baseline-to-target device test + logcat |
| AC-008 | 部分損壞 draft 容錯 | draft migration/restore tests |
| AC-009 | 全套建置與測試 | Gradle build/test/source scan |

## Failure Behavior
- 舊 `IS_TIEINPOINT` 值被忽略，不顯示錯誤提示。
- 一般點缺少必填資料時沿用既有驗證阻止送出，使用者可補齊後重試。
- draft 欄位損壞時保留可解析資料、清除不可用欄位；不可解析 waypoint 略過並允許重新建立。
- 升級／解析錯誤不得中斷 App 啟動或草稿列表；無法恢復的資料記錄於驗證 evidence，不能假報 PASS。

## Security and Privacy
- 無新增權限、憑證、個資欄位或後端 API。

## Open Questions
- 無
