# Implementation Plan

## Goal

在不混用 `ditchDetails` 與 `nodeDetails` 的前提下，修正 URL-only 既有照片、
刪除資料圖層預設值與匯入頁標題置中行為。

## Scope

只處理 debug-0918 的照片上傳判定/API 資料邊界、主地圖 deleted-area 初始狀態、
以及既有點位匯入頁 header layout；不修改後端 API。

## Affected Files

- `app/src/main/java/com/example/taoyuangutter/common/PhotoUploadSlotState.kt`：
  區分未替換 imported photo 與 replacement upload candidate。
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`、
  `AddGutterBottomSheet.kt`：維持匯入顯示與替換上傳流程。
- `app/src/main/java/com/example/taoyuangutter/api/GutterApiModels.kt`：維持
  `nodeDetails` 與 `ditchDetails` response shape 的分離解析。
- `app/src/main/java/com/example/taoyuangutter/MainActivity.kt`、
  `map/MapWorkspaceFragment.kt`：保留 inspect waypoint 中實際 response 的照片 ID。
- `app/src/main/java/com/example/taoyuangutter/map/MapOverlayController.kt`、
  `LayersBottomSheet.kt`、`app/src/main/res/layout/sheet_layers.xml`：deleted-area
  初始關閉與明確 toggle state。
- `app/src/main/res/layout/bottom_sheet_import_existing_waypoint.xml`：完整
  header row 置中與控制區 hit area。

## Implementation Steps

1. 將 `nodeDetails` URL-only 照片視為未替換的既有照片，下載後保持顯示與
   `success` 狀態，不用 `ditchDetails` 跨資料源補 ID。
2. 保持 `PhotoUploadSlotState` 讓 imported `success` 照片略過上傳；使用者
   替換/新增照片時清除該 slot metadata，使該 slot 重新進入 `nodeImage`。
3. 保持 `NodeDetails.photoImage()` 對 `node_img[]`/`url[]` 的相容解析；
   `DitchDetails.nodes[].url[].id` 只在同一 ditch/node response 內映射。
4. 維持 main-map inspect handoff 對已實際回傳的 photo IDs 的保存，不新增
   未證實的 `ditch_id` 查詢或 API merge。
5. 將 `0910刪除資料` 的初始 state 統一改為 off，保留使用者明確切換與重建
   後的狀態傳遞。
6. 將 `既有點位資料` 標題改為完整 header row 置中，保留左右 48dp controls。
7. 以固定 revision `a8c95208e46b7cb7142f7e28bf70c309bc9726c8` 執行 targeted
   tests、Debug build、instrumentation APK build；取得裝置後再執行 runtime cases。

## Test Plan

- `NodeImgDeserializationTest`
- `PhotoImgIdResolverTest`
- `PhotoUploadCandidateResolverTest`
- `StoreDitchResponseParsingTest`
- `StoreDitchResponseWaypointMapperTest`
- `MapOverlayControllerStateTest`
- `:app:testDebugUnitTest`
- `:app:assembleDebug`
- `:app:assembleDebugAndroidTest`
- `git diff --check`

### Physical Device Test Scope

- Requires physical device: Yes
- Device/environment: Android device or emulator with app authentication and test API access
- In-scope Acceptance Criteria: AC-001, AC-002, AC-004, AC-005, AC-006
- Regression risk: imported photo display/skip behavior, replacement upload slot and
  returned ID, deleted-area overlay toggle/recreation, title rendering and controls
- Full regression required: No
- Full regression trigger: only if an in-scope case fails or release risk expands
- Stop condition: all listed cases complete, or one retry produces enough evidence to
  classify the failure

| AC | Environment | Steps | Expected | Evidence |
|---|---|---|---|---|
| AC-001 | Device | Import a nodeDetails URL-only photo and save without replacing it | Photo remains visible and no nodeImage call is made | Result and bounded API/log evidence |
| AC-002 | Device | Replace one photo slot and save | Only that slot uploads and returned img_id is used | Result and bounded API/log evidence |
| AC-004 | Device | Cold launch, toggle layer off/on, recreate map | Initial off and toggle/recreation state are consistent | UI result/screenshot on failure |
| AC-005 | Device | Open existing-waypoint import sheet | Title is full-row centered; controls remain usable | UI result/screenshot on failure |
| AC-006 | Device | Resume draft, virtual/cannot-open, existing edit flows | No affected regression | Result and bounded log |

## Regression Plan

- Verify unchanged imported photo is not uploaded while replacement photo remains uploadable.
- Verify `ditchDetails` and `nodeDetails` parsing stays independent.
- Verify virtual/cannot-open slot filtering, draft resume, and existing-node edit behavior.
- Verify other map overlays and layer state propagation are unchanged.

## Risks

- A success state could be cleared accidentally during form synchronization, causing
  an unchanged photo to upload.
- A response-order or cross-ditch merge could attach the wrong image ID.
- Changing layer defaults could overwrite explicit user state during recreation.
- Header centering could reduce control hit areas if layout constraints regress.

## Rollback Plan

Revert the debug-0918 implementation commit while preserving unrelated worktree changes.

## Current Behavior

`nodeDetails` supplies single-node form fields and URL-only photos; `ditchDetails`
supplies whole-ditch nodes and may include image IDs. The import and map flows must
keep these responsibilities separate.

## Expected Behavior

URL-only unchanged photos display without re-upload; replacements upload normally;
deleted-area starts off; and the existing-waypoint title is centered.

## Acceptance Criteria Traceability

| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | Steps 1–2 | Photo upload candidate tests + device API trace |
| AC-002 | Step 2 | Replacement/upload tests + device API trace |
| AC-003 | Steps 3–4 | JSON/mapping tests + source review |
| AC-004 | Step 5 | Map overlay unit test + device toggle/recreation |
| AC-005 | Step 6 | XML/source review + device UI result |
| AC-006 | Steps 2–5 | Full unit suite + bounded device regression |

## Failure Behavior

- Missing URL: keep the slot unavailable and show the existing import warning.
- Failed replacement upload: preserve the failed state and error; do not claim success.
- Missing photo ID in URL-only `nodeDetails`: do not invent an ID or merge another
  ditch; preserve the unchanged imported-photo state.
- API/auth failure: follow existing auth-expiry and draft-save handling.

## Security and Privacy

無；本次不新增權限、credential 或資料外傳行為。

## Open Questions

無。
