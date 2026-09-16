# Requirement

Task: debug-0916
Type: bugfix / debug

## Objective

修正「檢視側溝 → 編輯 → 修改照片」後，照片雖啟動上傳但側溝資料仍未替換照片的問題。

## Approved behavior

1. 檢視模式切換編輯後，替換照片應建立可追蹤的上傳任務。
2. 照片上傳成功取得的新 `img_id` 應同步保存，並在 `/v1/ditch/storeDitch` 更新既有節點時送出。
3. `/v1/ditch/storeDitch` 不送 `captured_at`；照片 multipart 上傳 API 的 `captured_at` 維持既有行為。
4. 不改變無法開蓋、明溝及其他既有欄位規則。

## Scope

- `GutterFormActivity` 的檢視轉編輯與照片替換狀態。
- `StoreDitchNodeRequestMapper` 的 `img_ids` 與 `captured_at` request mapping。
- 對應單元測試與 task evidence。
