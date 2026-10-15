# Repository Analysis

## Current Behavior

- 共用完成規則在 `GutterCompletionPolicy`：虛擬點不要求照片；無法開蓋或銜接點要求
  `photo1`；一般點要求 `photo1`、`photo2`、`photo3`。
- `GutterBasicInfoFragment.validateAllPhotos()` 與 `AddGutterBottomSheet` 送出前驗證都以
  可用的照片路徑判斷必要槽位；路徑缺失或不可讀時會阻擋送出。
- `AddGutterBottomSheet.ensureWaypointPhotosUploadedBeforeSubmit()` 在 `storeDitch` 前上傳
  照片，但 `ApiResult.Success` 只要求 response 的 `success=true`，接著可將 nullable 的
  `img_id` 寫成 `UploadState=success`。`StoreDitchNodeRequestMapper` 再以可解析的數字 ID
  建立 `img_ids`，無法解析的槽位會被 `mapNotNull` 靜默排除。

## Expected Behavior

- 無法開蓋／銜接點送出時，最終資料至少要保有第 1 張照片的有效 server 關聯。
- 正常模式送出時，最終資料至少要保有第 1、2、3 張照片的有效 server 關聯。
- 若單張照片 API 成功但未回傳可用 `img_id`，流程不應將該槽位當成可供最終
  `storeDitch` 使用的成功照片。
- 進度總數與實際候選不一致屬於另一個 UI/observability 問題，不應與 payload 少照片混為一談。

## Affected Modules

- `app/src/main/java/com/example/taoyuangutter/gutter/GutterCompletionPolicy.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`
- `app/src/main/java/com/example/taoyuangutter/api/GutterRepository.kt`
- `app/src/main/java/com/example/taoyuangutter/api/GutterApiModels.kt`
- `app/src/main/java/com/example/taoyuangutter/api/StoreDitchNodeRequestMapper.kt`
- `app/src/main/java/com/example/taoyuangutter/common/PhotoUploadSlotState.kt`
- `app/src/main/java/com/example/taoyuangutter/common/PhotoSlotUploadCoordinator.kt`

## Dependencies

- `POST /api/v1/node/nodeImage` 的 response schema，尤其是 `success`、`data`、`img_id`。
- `POST /api/v1/ditch/storeDitch` 對 `img_ids` 缺少項目的 server semantics。
- draft/form 內 `photo{slot}`、`photo{slot}ImgId`、`photo{slot}UploadState` 的同步時序。
- 既有 URL-only 匯入照片的特殊規則：下載成功會標成 success，但不一定有 image ID。

## Risks

- `UploadState=success` 與「有可供 `storeDitch` 使用的數字 `img_id`」目前是兩個不同概念，
  卻共用 `isAlreadyUploaded()` 判斷，可能造成成功但無 ID 的槽位被跳過。
- `mapNotNull` 使少傳的槽位不一定被視為 request 組裝錯誤，而可能只在 backend 結果或資料檢視時出現。
- `countPendingPhotoUploads()` 與實際 ensure 流程有額外 coordinator 判斷，可能造成 `0/X` 進度閃現；
  這不等同於照片 payload 遺失。

## Unknowns

- 沒有完整的 live `nodeImage` failure/成功 response matrix，尚無法證明 backend 是否曾實際回傳
  `success=true` 且 `data.img_id` 缺失或非數字。
- 沒有本次使用者回報案例的 request/response correlation、draft state snapshot 或 server record，
  無法把靜態可行路徑指認為該案例的已發生根因。
