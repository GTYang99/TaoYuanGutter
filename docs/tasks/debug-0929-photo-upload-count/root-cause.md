# Root Cause Analysis

## Scope and direct answer

本分析固定在 `fbb198370a2dbf7bad38876f2d1e21be5bca75f1`，只做 source review、既有測試與歷史證據比對。
沒有修改 production code。

結論分兩層：

1. 如果「少於 1／少於 3」指使用者在畫面上沒有選足照片，現行驗證不會放行：特殊模式要求
   `photo1`，正常模式要求 `photo1`～`photo3`。
2. 如果指最後送到 server 的照片關聯少於要求數量，程式存在一條可行的少傳路徑：
   `nodeImage` 回報 `success=true` 但 `data.img_id` 為空、非數字或缺少時，client 仍可能把槽位標為
   success，之後 `storeDitch.img_ids` 會把該槽位靜默濾掉。這條路徑在 source 層面成立；本 repository
   沒有本次案例的 live response 可證明它曾實際發生，因此「可能發生」是成立的，「本次回報已發生」仍是
   `NOT VERIFIED`。

## Evidence chain

### 1. Required photo rules are not the immediate gate failure

- `GutterCompletionPolicy.isDetailExempt()` 將 `IS_CANTOPEN` 或 `IS_TIEINPOINT` 視為特殊模式，
  `requiredPhotoSlots()` 回傳 `[1]`；一般模式回傳 `[1, 2, 3]`。
- `GutterBasicInfoFragment.validateAllPhotos()` 先檢查特殊模式的第 1 張，再依 shared policy
  檢查第 2、3 張。
- `AddGutterBottomSheet.validateWaypointPhotosAndFieldsOrAlert()` 也使用同一 policy 與可上傳路徑
  判斷；缺槽位時不進入照片上傳及 `storeDitch`。

因此，單純「使用者沒有選到足夠照片」會在本地送出前被擋下，不是目前可見的靜默少傳原因。

### 2. Success response can be accepted without an image ID

`GutterApiModels.NodeImageUploadData.imgId` 是 nullable。`GutterRepository.uploadNodeImage()` 的
成功條件是 HTTP success 且 body `success == true`，沒有要求 `body.data?.imgId` 必須存在或可解析。

在 `AddGutterBottomSheet.ensureWaypointPhotosUploadedBeforeSubmit()` 中，`ApiResult.Success` 會：

1. 回報進度成功；
2. 寫入 `photo{slot}UploadState=success`；
3. 以 `result.data.data?.imgId` 寫入 nullable 的 `photo{slot}ImgId`。

同樣的 nullable ID 寫入也存在於 `PhotoSlotUploadCoordinator` 的背景上傳路徑。

### 3. The same slot can then be skipped while its ID is absent

`PhotoUploadSlotState.isAlreadyUploaded()` 的條件是「有數字 `img_id` **或** `UploadState=success`」。
這個設計原本是為了 URL-only 的既有照片匯入：照片可下載、可顯示，但 response 沒有 ID 時仍應避免
把未替換的既有照片重新上傳。

然而同一個 predicate 也會套用到「剛剛新增／替換、API 回成功但沒有 `img_id`」的槽位。下一次流程會
把它當成已完成，跳過重新上傳；而 `StoreDitchNodeRequestMapper` 只使用
`PhotoUploadSlotState.readImgId()` 取得可解析的數字 ID。

### 4. Final request silently loses the slot

`StoreDitchNodeRequestMapper` 對 slot 1～3 使用 `mapNotNull`：

- 無法開蓋／銜接點：只保留 slot 1；若 slot 1 沒有數字 ID，`img_ids` 可能完全不產生，等效為少於 1。
- 正常模式：保留三個槽位中能解析的 ID；任一槽位缺 ID，`img_ids` 可能只有 2、1 或 0 個，等效為少於 3。

此處沒有把「路徑存在、state 成功、但 ID 缺失」轉成 error，因此是靜默少傳風險。

## Scenario assessment

| 情境 | 本地驗證 | 最終 request 風險 | 判定 |
|---|---|---|---|
| 無法開蓋／銜接點，沒有 photo1 路徑 | 驗證阻擋 | 不呼叫 `storeDitch` | 不支持「靜默少於 1」|
| 無法開蓋／銜接點，photo1 上傳成功且有數字 ID | 通過 | `img_ids=[id]` | 符合要求|
| 無法開蓋／銜接點，photo1 成功但 ID 缺失 | 路徑可用，驗證通過 | `img_ids` 可能缺失 | **可能少於 1**|
| 正常模式，任一必要路徑缺失 | 驗證阻擋 | 不呼叫 `storeDitch` | 不支持「靜默少於 3」|
| 正常模式，三張皆成功且都有數字 ID | 通過 | `img_ids` 三個 ID | 符合要求|
| 正常模式，任一張成功但 ID 缺失 | 路徑可用，驗證通過 | `img_ids` 只含其餘 ID | **可能少於 3**|
| 進度候選數與執行時候選數不一致 | 可能顯示 `0/X` | 不直接改變 payload | 另一個 P2 UI/observability issue|

## Historical and live evidence

- `docs/tasks/dbg-0910/evidence/node_details_A0910pt52.json` 證明既有 `nodeDetails` 匯入 response
  可以只有 URL 和 `fileCategory`，沒有 `id`／`img_id`。這是 URL-only 既有照片規則的證據，不等於
  新拍照片的上傳 API 一定會缺 ID。
- `docs/tasks/debug-0919-2/verification.md` 記錄一次 live 新照片流程：`nodeImage` HTTP 200 回傳
  `img_id=51863`，後續 `storeDitch` 帶 `img_ids=[51863]`。這支持正常成功路徑，但只有單一樣本，
  不能證明所有成功 response 都保證 ID。
- `StoreDitchNodeRequestMapperTest` 已覆蓋特殊模式保留 `[101]` 與省略 slot 2、3，也覆蓋正常 request
  的部分 ID；沒有覆蓋「`UploadState=success` 但 `photo{slot}ImgId` 缺失」的少傳案例。

## Classification and route

- `ISS-001`：`implementation_regression`，P1。原因是 client 允許成功結果缺少 ID，且後續用
  `mapNotNull` 靜默省略，具資料完整性風險。修正應路由到 `debug`，但本次沒有實作修正。
- `ISS-002`：`unknown`，P1。缺少本次案例的實際 `nodeImage` response、`storeDitch` request 與 server
  record，無法證明 live backend 是否觸發 ISS-001；保留在 `investigation`，不得把條件路徑宣稱為已發生。
- `ISS-003`：`implementation_regression`，P2。歷史上已知 `countPendingPhotoUploads()` 與 ensure
  流程候選集合不一致，可能產生 `0/X` 閃現；它解釋進度提示異常，但不是照片 payload 少傳的充分證據。

## Required next evidence

要把「可能」提升成「現場已發生」，需要同一筆操作的 correlation evidence：

1. 每一張必要照片的 slot、local path、`UploadState`、`img_id`。
2. 每次 `nodeImage` 的 HTTP status、response `success`、`data.url`、`data.img_id`。
3. 隨後 `storeDitch` request 的完整 `img_ids` 與同一節點／slot 對應。
4. server 最終節點的照片關聯數量。

在取得這些證據前，不應把 URL-only 匯入照片與新拍／替換照片混為同一根因，也不應把 `0/X` 進度提示
直接解讀為 server 少存照片。
