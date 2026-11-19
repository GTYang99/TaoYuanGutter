# Repository Analysis

## Current Behavior
- `IS_TIEINPOINT` 目前存在於 `NodeDetails`、`StoreDitchNodeRequest`、request mapper、表單 Intent／result、草稿同步與多個表單／檢視 UI 流程。
- 銜接點目前會參與 `IS_CANTOPEN` 互斥、明細與照片豁免、表單完成判定、檢視標籤及下拉名稱。
- draft 以 Room 儲存 Gson-backed waypoint／basicData；升級後舊 JSON 可能仍含 `IS_TIEINPOINT`，且本分支目前尚未建立本 task 文件。

## Expected Behavior
- UI 不再顯示或產生銜接點狀態；舊值靜默忽略並按一般點處理，虛擬點規則優先。
- 所有 `storeDitch` JSON 完全省略 `IS_TIEINPOINT`；一般點必填、照片、`IS_CANTOPEN` 與虛擬點既有規則維持。
- 升級後能安全載入舊 response、Intent、Bundle 與 draft；可解析資料保留，損壞資料可由使用者重填。

## Affected Modules
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt`：移除表單欄位與銜接點特殊狀態，恢復一般點驗證。
- `app/src/main/res/layout/fragment_gutter_basic_info.xml`、`app/src/main/res/values/strings.xml`：移除表單控制項與直接使用的文案。
- `GutterFormActivity.kt`、`GutterFormContract.kt`：調整 Intent／result／表單資料正規化與舊 key 相容讀取。
- `GutterInspectActivity.kt`、`GutterInspectPhotosFragment.kt`、`AddGutterBottomSheet.kt`：移除檢視與點位下拉標記及銜接點特殊渲染／預載入。
- `GutterApiModels.kt`、`StoreDitchNodeRequestMapper.kt`：request 不再輸出欄位；保留必要的舊 response 相容策略。
- `GutterDraftCoordinator.kt`：載入／同步 draft 時清理 legacy key、保留可用資料並支援唯讀銜接點例外。
- 相關 JVM／instrumentation fixtures：更新銜接點、升級、損壞 draft 與 request JSON 測試。

## Dependencies
- Gson 對未知 JSON 欄位的容錯與 request null serialization 行為。
- Room draft repository、Gson-backed `Waypoint.basicData`、submitted/read-only draft policy。
- `GutterCompletionPolicy`、`IS_CANTOPEN` 互斥與照片驗證流程。
- Android package update preserving app data；baseline APK 來自 commit `78d6df31`。

## Risks
- 移除銜接點判定時誤刪 `IS_CANTOPEN`、虛擬點或照片豁免規則。
- 舊銜接點資料被按一般點處理後缺少必填欄位，可能在送出前被阻止；需確保可編輯與補填，不得閃退。
- draft JSON／saved state／Intent 中殘留舊 key，若直接強制讀取或型別轉換可能造成升級崩潰。
- 已上傳唯讀草稿解鎖例外可能影響既有 read-only policy。
- request mapper 任何漏徑都可能重新送出 `IS_TIEINPOINT`。

## Unknowns
- 無；若升級裝置或 baseline APK 不可取得，相關驗證需標記 `NOT VERIFIED`，不得視為 PASS。

## Issue Categories to Track
- `requirement_gap`：驗收對舊唯讀銜接點例外的定義變更。
- `implementation_regression`：`IS_CANTOPEN`、虛擬點、照片或一般點驗證回歸。
- `environment`：無法安裝 baseline／更新 APK 或無穩定裝置執行 instrumentation。
- `unknown`：legacy draft 損壞資料於實際資料庫中的未知格式。
