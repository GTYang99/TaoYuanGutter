# Implementation Plan

## Goal

- 新增銜接點／連接管、由後端生成新增節點名，並把附近點位改為最近存檔點位，同時維持草稿、檢視、編輯與搜尋匯入流程。

## Scope

- 側溝點位表單、DTO／mapper／回填、草稿資料傳遞和既有點位匯入；不變更後端、Room schema、照片上傳協定或一般地圖定位。

## Affected Files

- `app/src/main/res/layout/fragment_gutter_basic_info.xml`：兩個新控制項與 XY_NUM 唯讀遮罩。
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt`：欄位 order、互斥、模式、預填、收集。
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterInspectActivity.kt`：將既有點位詳情的兩個 Boolean response 值交接至可編輯 waypoint `basicData`。
- `app/src/main/java/com/example/taoyuangutter/api/GutterApiModels.kt`、`StoreDitchNodeRequestMapper.kt`：read/request DTO、create omit XY_NUM、新屬性 mapping。
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`：成功 response 的系統節點名回填。
- `app/src/main/java/com/example/taoyuangutter/api/GutterApiService.kt`、`GutterRepository.kt`：無 query closest-node GET。
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`、`ImportExistingWaypointBottomSheet.kt`、`app/src/main/res/layout/bottom_sheet_import_existing_waypoint.xml`：移除 import GPS/map-location flow，改為立即載入最近存檔點位。
- `app/src/test/java/com/example/taoyuangutter/api/StoreDitchNodeRequestMapperTest.kt`、`app/src/androidTest/java/com/example/taoyuangutter/GutterBasicInfoUiTest.kt` 及必要 import/API tests：契約與回歸覆蓋。

## Implementation Steps

1. 擴充 `NodeDetails` 以 nullable Boolean 讀取兩個 response key，UI 對缺值回退 false；將 request 的 `XY_NUM` 與 `IS_CANTOPEN` 改為可省略欄位，新增 request 以 int 送出兩個新 key。
2. 更新 mapper：create node 設 `XY_NUM=null`，edit node 保留 `basicData` 值；虛擬 node 對 `IS_CANTOPEN`、`is_connect_point`、`is_connect_pipe` 均設 `null`；非虛擬點將兩個新屬性映射為 0/1。storeDitch 成功後按既有 `DitchXyNum.start`、`nodes`、`end` 對應起點、中繼點、終點回填系統名稱。
3. 在 `GutterInspectActivity` 的 inspect-to-edit waypoint 組裝處，將 nullable `NodeDetails.isConnectPoint`／`isConnectPipe` 以 `false` fallback 寫入 `basicData`；擴充 `GutterBasicInfoFragment` 的 argument constants、`newInstance()` whitelist 與 `prefillData()`，把兩個 key 完整傳至 controls，並由 `collectData()` 原值回收。依 Figma 增加表單 controls：Connect Point 在 Cant Open 後；Connect Pipe 在 Silt 後且預設「無」，並從該交接資料、草稿與 import response 預填。
4. 集中 Connect Point/Cant Open 的互斥 listener，兩者皆可不選；選擇 Connect Point 只清除 Cant Open，保留一般點位的詳細欄位、驗證、既有值與三個照片 slot。只有選擇 Cant Open 時才沿用既有欄位／照片限制；切換虛擬點時清除 Cant Open、Connect Point、Connect Pipe 並不顯示或送出它們。
5. 將新 controls 納入 reorder、editable、virtual、import-lock、required indicators、draft watcher 和 collectData；create 隱藏並不驗證 XY_NUM，edit 顯示但遮罩鎖定並保留 payload。
6. 將 closest-node service/repository 改為無 query GET；移除 import 定位權限、GPS callbacks/timeouts、host location、markers、padding 與 retry 關聯。
7. 更新 import sheet：標籤為「最近存檔點位」，移除定位按鈕與位置提示，顯示時立即載入；保留 XY_NUM 搜尋、選取、匯入、401、空與錯誤狀態。
8. 補足 targeted unit/UI/API tests，執行 build、tests 與可用裝置 smoke，記錄證據與限制。

## Test Plan

- Mapper JSON：create 不含 `XY_NUM`、edit 保留；兩個新 request key 為 0/1；virtual node 不含 `IS_CANTOPEN`、`is_connect_point`、`is_connect_pipe`。
- UI：Figma 順序、Connect Pipe 預設、Connect Point/Cant Open 互斥、草稿重建、Boolean false fallback；選擇 Connect Point 後，普通詳細欄位、既有值與三個照片 slot 仍可輸入／驗證／送出。
- Inspect-to-edit：以含 true／false 組合的 `NodeDetails` 開啟檢視後進入編輯，讓資料依序通過 `GutterInspectActivity`、`GutterBasicInfoFragment.newInstance()`、argument prefill 與 rendered controls；不修改便組建 storeDitch request，斷言兩個 request 值仍與 read response 相符，缺值才回退為 0。
- Modes：create 隱藏 XY_NUM；inspect 顯示；edit 遮罩鎖定且仍送原值；切換 virtual 時三值清除、controls 關閉、payload 省略；確認 Connect Point 保留一般點位資料／照片行為，而 Cant Open 維持既有欄位與照片限制。
- Import：request 無 `lng`/`lat`，開啟即載入，無定位權限/GPS/marker/位置文案，XY_NUM 搜尋與匯入仍可用。
- 執行 `./gradlew :app:testDebugUnitTest`、focused connected Android tests、`./gradlew :app:assembleDebug`。

### Physical Device Test Scope

- Requires physical device: Yes
- Device/environment: 已登入、可連線新 API 的 Android 9+ 實體裝置。
- In-scope Acceptance Criteria: AC-001, AC-002, AC-003, AC-004.
- Regression risk: 點位資料遺失、意外帶入 XY_NUM、互斥限制錯誤、匯入仍請求定位。
- Full regression required: No
- Full regression trigger: API payload／回填失敗、草稿資料遺失或 import lifecycle failure evidence。
- Stop condition: 所有指定 AC 完成，或取得足夠失敗證據。

| AC | Environment | Steps | Expected | Evidence |
|---|---|---|---|---|
| AC-001 | Physical device | 新增、儲草稿、重開、檢視、編輯、選銜接點與無法開蓋、切換虛擬點 | 位置、預設、互斥、Connect Point 詳細資料／照片保留、Cant Open 限制與 virtual reset 正確 | 結果；失敗時截圖/logcat |
| AC-002 | Device + API log | 設定兩屬性、送出、重開、切換虛擬點送出 | 一般 request 為 0/1、Boolean 回填；virtual 省略三 key | request/response |
| AC-003 | Device + API log | 新增後再編輯 | create 省略 XY_NUM；系統值鎖定顯示、edit 保留 | request/response |
| AC-004 | Physical device | 開啟既有點位匯入 | 立即列出最近記錄，沒有定位互動 | 結果；失敗時截圖/logcat |

## Regression Plan

- 驗證照片條件、待架站、虛擬點、草稿、一般檢視／編輯與 edit storeDitch；確認 Connect Point 不改變一般點位資料／照片流程，Cant Open 維持既有限制。
- 驗證 XY_NUM 搜尋 tab、選取、匯入鎖定、401 草稿保存、主地圖一般定位與互動。

## Risks

- JSON null serialization、response XY_NUM 對應與 Cant Open/Connect Point 共用限制必須以測試鎖定。
- 移除定位 resources 時必須取消 callbacks/jobs，避免已關閉 sheet 的 stale 更新。
- Figma 的 XY_NUM 示意與需求相反；驗收以 approved requirement 為準。

## Rollback Plan

- 回退本任務單一實作 commit，可還原原有欄位、XY_NUM 提交及定位式附近點位流程。

## Current Behavior

- 沒有兩個新屬性，新增與編輯都提交 XY_NUM，匯入需 GPS 座標與定位 UI。

## Expected Behavior

- 新屬性跨表單與草稿保存；系統名稱回填；匯入立即顯示最近存檔點位。

## Acceptance Criteria Traceability

| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | 3-5 | UI ordering/default/mutual-exclusion、inspect-to-edit prefill、草稿與 mode tests、裝置流程 |
| AC-002 | 1-5 | DTO/mapper serialization、inspect-to-edit preservation、response prefill、裝置 request/response |
| AC-003 | 1-2, 5 | create/edit JSON、locked-form、裝置 request/response |
| AC-004 | 6-7 | service URL、import UI/lifecycle、裝置 smoke |

## Failure Behavior

- 最近存檔 API 空資料顯示既有空狀態；網路、解析、401 沿用既有處理，不以定位替代。
- 新 Boolean 缺值顯示「無」，不得崩潰或阻止編輯。
- 缺少可對應系統 XY_NUM 時不虛構名稱，保留 session 並記錄可診斷錯誤。

## Security and Privacy

- 不新增權限、憑證或個資欄位；移除 import GPS flow 可減少該流程的定位資料處理。

## Open Questions

- 無。
