# Implementation Plan

## Goal

- 以新版 API 和 UI 契約完成銜接點與連結管，並重新驗證本任務既有的節點名與匯入功能。

## Scope

- 僅更新側溝點位的 read/write DTO、表單／草稿／檢視資料流、相應測試與 0917 既有回歸；不變更後端、Room schema、照片協定或一般地圖定位。

## Affected Files

- `app/src/main/java/com/example/taoyuangutter/api/GutterApiModels.kt`、`StoreDitchNodeRequestMapper.kt`：以新版 key 和型別實作 request/read DTO。
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterInspectActivity.kt`、`GutterBasicInfoFragment.kt`、`WaypointAdapter.kt`：readback 正規化、草稿與表單交接、互斥灰化、下拉標記與檢視可見性。
- `app/src/main/res/layout/fragment_gutter_basic_info.xml`、`app/src/main/res/values/strings.xml`：確認控制項位置和「連結管」文案。
- `app/src/test/java/com/example/taoyuangutter/api/StoreDitchNodeRequestMapperTest.kt`、新增 DTO／adapter test、`app/src/androidTest/java/com/example/taoyuangutter/GutterBasicInfoUiTest.kt`：新版資料契約與 UI 覆蓋。
- 既有 `XY_NUM`、`AddGutterBottomSheet`、最近存檔點位匯入檔案：只做 AC-004／AC-005 回歸，不重複改寫已完成範圍。

## Implementation Steps

1. 將 read DTO 的兩個欄位改為 `@SerializedName("IS_TIEINPOINT")`、`@SerializedName("IS_CONNECTING")` String nullable；在唯一 readback 邊界解析 `"1"` 為 true，`"0"`／缺值為 false，並在無法開蓋為 true 時強制銜接點 false。
2. 將 `StoreDitchNodeRequest` 與 mapper 改為大寫 key、Boolean nullable；非虛擬點固定設入兩個 Boolean，虛擬點把 `IS_CANTOPEN`、`IS_TIEINPOINT`、`IS_CONNECTING` 都設為 null，以既有 null-serialization 機制省略。
3. 將新版資料完整串過 `GutterInspectActivity`、`Waypoint.basicData`、Fragment argument whitelist、prefill、collectData 與草稿序列化；舊草稿／已成功讀取但缺欄位的 response 一律初始化 false，且虛擬點不保存兩欄。將 node-details 預載和照片預載失敗分流：任一節點詳情失敗時不得以 `ditchToWaypoints()` fallback 開啟可提交編輯，僅提供重試或取消；只有節點詳情完整、但照片失敗時才可沿用既有警告後繼續編輯。
4. 更新表單狀態機：銜接點位於無法開蓋後；任一互斥項被選時，另一項立刻 false、灰化、不可操作；取消選取後才恢復可選。連結管維持必選、預設無、置於淤積程度後；切換虛擬點沿用 Alert，清除三值並在切回時維持 false。
5. 更新檢視與 adapter：非虛擬點顯示「連結管」；虛擬點隱藏它。點位名稱依既有名稱後附 `(銜接點)`，僅以 `IS_PENDING_DEPLOY`／`node.isPendingDeploy` 判斷待架站並最後附 `(待架站)`；`IS_HANGING` 仍只代表附掛或過路管線。保留既有無法開蓋標記邏輯。
6. 將所有新／舊 key 的 UI text、selector、list/reorder、editable/import lock 和任何 direct `basicData` lookup 一併替換，禁止遺留 `is_connect_point`／`is_connect_pipe`。
7. 依 `test-data.md` 建立 mapper、DTO、inspect-to-edit、draft、adapter 和 instrumentation 測試；執行 focused tests、完整 JVM tests、Debug build、Android-test compile 與可用的裝置 smoke。
8. 在新版 revision 重新驗證 `XY_NUM` 與最近存檔點位匯入，不得引用舊 API 契約的 execution／verification PASS 作為證據。

## Test Plan

- DTO/readback：`"0"`、`"1"`、缺欄位、雙 `"1"`，確認無法開蓋優先和 false fallback。
- Mapper JSON：非虛擬點在 true/false 每一組都存在兩個大寫 Boolean key；虛擬點省略三個 key；不得有舊小寫 key 或數字值。
- UI／草稿：順序、文案、連結管預設、雙向互斥灰化、切換虛擬點／Alert 後 false、草稿缺值與重開、切回一般點不還原。
- Inspect-to-edit／view：String response 通過所有 handoff 邊界後可無損重送；adapter fixture 以 `IS_PENDING_DEPLOY`／`node.isPendingDeploy=true`（非 `IS_HANGING`）斷言 `起點（E001）(銜接點)(待架站)` 的精確排序；虛擬點隱藏連結管。
- Inspect-to-edit failure safety：模擬至少一個 server-side tie-in 或 connecting 值為 true 的 node-details preload 失敗，斷言不開啟可提交表單、不走 `ditchToWaypoints()` fallback 且不產生 false 覆寫 payload；另測照片失敗但 node-details 完整時可在警告後保留兩值進入編輯。
- 既有回歸：新增／編輯 `XY_NUM` 與無參數最近存檔點位匯入。
- 執行 `./gradlew :app:testDebugUnitTest`、`./gradlew :app:assembleDebug`、`./gradlew :app:compileDebugAndroidTestKotlin`；可用環境再跑 targeted connected tests 和實機 API 流程。

### Physical Device Test Scope

- Requires physical device: Yes
- Device/environment: 已登入且可連線新版 API 的 Android 9+ 實體裝置。
- In-scope Acceptance Criteria: AC-001、AC-002、AC-003、AC-004、AC-005。
- Regression risk: API 欄位型別錯誤、無變更編輯寫回 false、虛擬點錯送 key、互斥狀態錯誤、草稿遺失、節點名或匯入回歸。
- Full regression required: No
- Full regression trigger: 任一 payload/readback/草稿資料遺失，或 XY_NUM／import failure evidence。
- Stop condition: 每個 AC 的指定情境完成並取得 request/response 或 UI 證據，或取得可分類的失敗證據。

| AC | Environment | Steps | Expected | Evidence |
|---|---|---|---|---|
| AC-001 | Device | 新增與編輯分別切換兩個互斥項、連結管 | 順序／文案正確；另一項 false、灰化不可選 | 截圖與結果 |
| AC-002 | Device + API log | 以 false/true 組合送出、重開、雙值異常資料、虛擬點送出、模擬 node-details 失敗 | Boolean request、String 回填、無法開蓋優先、virtual omission；detail failure 不可進入可提交編輯 | request/response |
| AC-003 | Device | 存舊格式草稿、重開、切 virtual 再切回、檢視名稱 | fallback、清除、不還原、標記排序正確 | 截圖與草稿內容 |
| AC-004 | Device + API log | 新增後進入檢視再編輯 | create 省略 XY_NUM；系統值顯示鎖定、edit 保留 | request/response |
| AC-005 | Device | 開啟既有點位匯入 | 立即列出最近記錄，沒有定位互動 | 截圖與 API log |

## Regression Plan

- 重新跑既有照片條件、待架站、虛擬點、草稿、一般檢視／編輯與 edit storeDitch 測試。
- 驗證 XY_NUM 搜尋／匯入、401、空清單／錯誤，以及主地圖一般定位不受影響。

## Risks

- request Boolean 與 response String 的型別邊界、Gson null omission、舊草稿、雙 `"1"` 及 node-details 預載失敗皆有資料遺失風險。
- UI 僅將 checkbox unchecked 而未灰化，或僅改 mapper 而漏掉 adapter／草稿邊界，都無法符合 AC。

## Rollback Plan

- 若新版驗證失敗，回退此任務後續實作 commit 至 `ae48a82`；不得將舊契約宣告為新版完成。

## Current Behavior

- 現行 branch 使用舊小寫 key、Int request、Boolean response 和「連接管」文案，且舊驗證無法涵蓋新版 API。

## Expected Behavior

- 新版 key／型別及所有明定 UI 狀態跨新增、草稿、檢視、編輯與虛擬點一致；XY_NUM 和匯入既有功能回歸通過。

## Acceptance Criteria Traceability

| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | 3-6 | UI/instrumentation 互斥、文案、順序與預設；裝置流程 |
| AC-002 | 1-3, 6 | DTO/mapper JSON、readback、inspect-to-edit、API log |
| AC-003 | 3-5 | 草稿、virtual transition、adapter 格式與裝置流程 |
| AC-004 | 8 | create/edit JSON、locked-form 與 API log |
| AC-005 | 8 | import API/UI lifecycle 與裝置 smoke |

## Failure Behavior

- 已成功取得的 response 缺失新欄位顯示 false；雙 `"1"` 正規化為無法開蓋 true、銜接點 false。任一 node-details 預載失敗時，顯示重試／取消且禁止進入可提交編輯，不得以 `DitchNode` fallback 假造 false。照片失敗但 node details 完整時，沿用既有警告後可繼續。未知字串、序列化錯誤或 API failure 依既有錯誤流程呈現並記錄，不假造資料。

## Security and Privacy

- 不新增權限、憑證或個資欄位；最近存檔點位流程維持既有授權與錯誤處理。

## Open Questions

- 無。
