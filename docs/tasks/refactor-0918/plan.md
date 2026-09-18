# Implementation Plan

## Goal
- 以共用 WMTS provider 替換三個指定背景圖層的 WMS 連結，保持既有地圖互動與降級行為。

## Scope
- 新增 WebMercatorQuad WMTS URL builder/provider，並僅更新三個背景圖層於三個地圖入口的 provider 建立方式。

## Affected Files
- `map/Wmts3857TileProvider.kt`：集中三圖層設定及 WMTS GetTile URL 建立。
- `map/MapOverlayController.kt`：主地圖改用共用 WMTS provider。
- `gutter/MapPointPickerActivity.kt`、`gutter/GutterFormActivity.kt`：背景圖層改用同一 provider。
- `test/.../Wmts3857TileProviderTest.kt`：驗證完整請求參數與限定圖層設定。

## Implementation Steps
- 建立純 Kotlin URL builder 與 `UrlTileProvider`，使用 `WebMercatorQuad` 的 `z/y/x` tile 索引並在不合法或超出支援縮放時回傳 `null`。
- 將三個受影響圖層以列舉/設定集中管理，固定 format 並傳送空白 `STYLE=`。
- 替換三個地圖入口中三個指定圖層的 WMS provider；保留未列入範圍的 provider、z-index 與開關流程。
- 新增單元測試驗證三圖層完整 WMTS URL 參數、空白 style、格式與 matrix/tile index 對應。

## Test Plan
- 執行 `./gradlew testDebugUnitTest --tests com.example.taoyuangutter.map.Wmts3857TileProviderTest`。
- 執行 `./gradlew testDebugUnitTest`，確認既有 EPSG:3826 provider 的測試仍通過。
- 執行 `./gradlew assembleDebug`，確認 Android 模組編譯。

### Physical Device Test Scope
- Requires physical device: Yes
- Device/environment: Android 9+、可連至指定 GeoServer 的測試裝置
- In-scope Acceptance Criteria: AC-001、AC-002、AC-003、AC-004
- Regression risk: 圖層開關、三個地圖入口及其他 WMS 圖層的可視性
- Full regression required: No
- Full regression trigger: 無
- Stop condition: 三個入口完成指定圖層可見性與失敗降級案例，或取得足夠失敗證據

| AC | Environment | Steps | Expected | Evidence |
|---|---|---|---|---|
| AC-001, AC-002 | JVM unit test | 建立每個 layer 的已知 tile URL | WMTS 參數、`STYLE=`、格式與 z/y/x 對應正確 | Test result |
| AC-003 | Physical device | 依序開啟主地圖、點位選擇、表單地圖 | 三背景圖層均可由相同設定顯示，未指定圖層仍可用 | Result; failure screenshot |
| AC-004 | Physical device | 在 WMTS 不可用時開啟地圖 | 受影響圖層未顯示，地圖仍可操作 | Result; failure screenshot |

## Regression Plan
- 保留所有現有 overlay z-index 與 toggle 狀態流程。
- 單元測試與完整 debug JVM 測試應繼續覆蓋既有 `Wms3826RequestBuilder`。

## Risks
- 服務端預設樣式改變可能造成視覺差異；需求已確認該結果符合預期。

## Rollback Plan
- 還原本任務的單一 commit，即可回到原本 WMS provider 行為。

## Current Behavior 
- 三個指定背景圖層在三個地圖入口以重複的 WMS GetMap provider 設定呈現。

## Expected Behavior
- 三個圖層以共用 WMTS GetTile provider 呈現，傳送空白 style 且不影響其他圖層。

## Acceptance Criteria Traceability
| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | 1–3 | WMTS URL unit test、實機入口檢查 |
| AC-002 | 1–2 | WMTS URL unit test |
| AC-003 | 2–3 | 程式碼範圍檢查、實機三入口檢查 |
| AC-004 | 1、3 | provider `null` 降級檢查、實機不可用案例 |

## Failure Behavior
- URL 建構失敗、縮放層級或 tile 索引不合法時回傳 `null`，由 Google Maps 略過該 tile；不影響其他 overlay 或底圖。

## Security and Privacy
- 僅使用既有 HTTPS GeoServer 公開圖層端點；不新增帳號、權杖或使用者資料。

## Open Questions
- 無
