# Root Cause Analysis

## Scope and Conclusion

本任務分析「debug build 中讓測試人員切換 API 後端」的程式缺口；入口位置經使用者確認為登入頁。

這不是已知回歸：目前沒有證據顯示環境切換曾存在。直接原因是 app 的 API endpoint 在程式中固定，沒有 runtime 選擇狀態或登入頁入口。`GutterApiClient` 第一次被讀取時以 Taipei URL 建立 Retrofit singleton；之後新選項若只改一個變數，已建立的 `GutterRepository` 仍可能保留舊 `GutterApiService`。

## Evidence Chain

1. `MainShellActivity` 啟動時預設選擇 `nav_map`，並載入 `MapWorkspaceFragment`，這是正式首頁地圖工作區。
2. `GutterApiClient.ENABLE_GROUP_SIMULATION` 等於 `BuildConfig.DEBUG`。目前 debug build 的 gate 已開啟，release build 關閉。
3. `BackendEndpoints` 有 Taipei 與 DEMO 的 API 常數，但沒有目前有效的 `base` endpoint。`ACTIVE_API_TAPIEI_URL` 是 Retrofit 唯一使用的 base URL。
4. `GutterApiClient.instance` 是 lazy singleton；首次建立後固定使用 Taipei base URL。
5. `GutterRepository` 初始化時把 `GutterApiClient.instance` 存在自己的 `api` 欄位中。單純更新全域所選環境而不處理既有物件，不能保證 request 改道。
6. 地圖資料的 WMS／WMTS 走各自固定 URL，不由 Retrofit 控制。因此「只切換 GutterApiClient」不會同步切換地圖服務。

## Why the Issue Occurs

系統目前只支援 build-time/source-level endpoint 選擇，沒有 runtime environment model。雖然存在 debug-only simulation gate，它目前只控制既有的上傳失敗模擬入口，並未連接 API endpoint。故測試人員無法在登入前把後續 API request 從固定 Taipei service 改到其他環境。

## Classification

- Issue: `ISS-001`
- Category: `enhancement_request`
- Priority: `P2`（測試工作流程缺少入口；目前沒有已證實的正式使用者故障）
- Route: `planning` after endpoint identity and scope questions are resolved.

## Unverified Items

- None remaining for task scope; the user confirmed the `base` URL and API-only/session-local behavior is recorded in knowledge resolution.
- API-only scope follows the request naming `GutterApiClient`; WMS／WMTS remain unchanged.
- Selection is assumed process-local and resets on next launch.
- 各目標的實際可達性、TLS 設定與服務相容性。

## Minimum Fix Boundary

在 `base` 目標及切換範圍確認後，集中維護 endpoint 清單與選擇狀態；在 `ENABLE_GROUP_SIMULATION` 為 true 時，從登入頁提供隱藏選擇入口；令後續 `GutterApiClient` request 使用所選 service，處理已建立 repository 的 service lifecycle，並確保 release 沒有入口、request 不會自動跨環境 fallback。
