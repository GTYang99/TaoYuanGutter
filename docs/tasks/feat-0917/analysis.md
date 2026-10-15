# Repository Analysis

## Task Classification

- Type: `feature`; API 契約與 UI 行為更新，必須重新經過完整 Plan Review。

## Current Behavior

- 已提交的實作、mapper 與測試仍使用 `is_connect_point`／`is_connect_pipe`，request 為 Int `0/1`，read DTO 為 Boolean；與新版 `IS_TIEINPOINT`／`IS_CONNECTING` Boolean request、String response 不相容。
- `GutterBasicInfoFragment` 已是欄位順序、草稿通知、虛擬點模式和資料收集的所有權人；`GutterInspectActivity` 將 `NodeDetails` 交接至可編輯 `basicData`。
- `WaypointAdapter` 目前只知道舊 key，檢視點位名稱的標記組裝尚未覆蓋銜接點／待架站的新排序。
- 先前的 execution 與 verification 證據驗證的是舊契約，不能證明 AC-001 至 AC-003。

## Expected Behavior

- 非虛擬點以 Boolean 固定送出大寫 key；讀取時將 String `"0"`／`"1"` 及缺值安全正規化為 Boolean。
- 表單採互斥、灰化而非只取消勾選的 UX；虛擬點依規則清除、隱藏並省略欄位，草稿一致。
- 檢視下拉以點位名稱附加銜接點與最後的待架站標記；虛擬點不顯示連接管。

## Affected Modules and Dependencies

- `GutterApiModels.kt`、`StoreDitchNodeRequestMapper.kt`：大寫 JSON key、Boolean request、String response 與 omission 行為。
- `GutterInspectActivity.kt`、`GutterBasicInfoFragment.kt`、`WaypointAdapter.kt`：readback 正規化、表單／草稿交接、互斥灰化、檢視名稱與欄位可見性。
- `fragment_gutter_basic_info.xml`、`strings.xml`：控制項位置與「連接管」文案。
- `StoreDitchNodeRequestMapperTest.kt`、`GutterBasicInfoUiTest.kt` 及新增的 DTO／檢視格式測試：新版契約資料覆蓋。
- 既有 XY_NUM 與最近存檔點位匯入相關檔案仍在任務範圍，但其已完成證據需在新 revision 重新回歸。

## Risks

- Gson 的 Boolean request 與 String response 不可共用同一欄位型別；需要明確 DTO／轉換邊界。
- 遺漏任一 `basicData`、argument whitelist、draft serialization 或 adapter key，會造成 no-op edit／草稿資料被寫回 false。
- 雙 `"1"` 是資料異常；若未在 readback 正規化，UI 會違反互斥規則。
- 將虛擬點的 omitted key 誤序列化成 `false`，會違反後端契約。
- `nodeDetails` 預載失敗不是 response 缺欄位：前者沒有權威值，若仍以 `DitchNode` fallback 開放提交，會把 server 既有 true 值覆寫成 false。照片下載失敗不影響這兩項 readback，可維持獨立的可續行警告。

## Assumptions

- 合法 response 值為 `"0"`／`"1"`；欄位缺失的指定 fallback 為 false。
- 原有無法開蓋與待架站的既有顯示規則保留；待架站的唯一來源為 `IS_PENDING_DEPLOY`／`node.isPendingDeploy`，`IS_HANGING` 只代表附掛或過路管線；本次只新增銜接點標記及其指定順序。

## Potential Issues

- `ISS-008` 為已分類的 requirement/planning gap。新版測試若發現資料遺失、序列化或 UI 回歸，應分類為 `implementation_regression` 並進 Debug。
