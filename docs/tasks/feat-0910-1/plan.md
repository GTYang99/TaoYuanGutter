# Implementation Plan

## Goal
- 完成確認、session-only snapshot、dirty merge 回填，同時維持既有上傳結構與流程。

## Current Behavior
- 勾選立即清除且無 Alert；取消不回填。未提交 snapshot 版本存在 upload metadata 不同步風險。

## Expected Behavior
- 確認後清除；取消不改資料；確認後取消勾選可安全回填未被後續操作修改的內容。

## Affected Files
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/CantOpenSessionViewModel.kt`（新增）
- `app/src/main/java/com/example/taoyuangutter/gutter/CameraOverlayFragment.kt`
- `app/src/test/java/com/example/taoyuangutter/gutter/CantOpenSessionSnapshotTest.kt`
- `app/src/androidTest/java/com/example/taoyuangutter/GutterCantOpenUiTest.kt`

## Implementation Steps
1. 建立 Activity-scoped `CantOpenSessionViewModel`，snapshot 包含欄位、slot 2／3 URI、capturedAt、upload state、imgId、error、pending path、cleared baseline 與 dirty markers；不使用 SavedState。
2. 在使用者勾選時先維持未勾選並顯示 Alert；取消／dismiss 不清除、不更新 upload state。
3. 確認後以既有 batch callback 清除欄位與 slot 2／3，正常同步 Activity／Waypoint upload metadata；第 1 張保留。
4. 取消勾選時由 Activity 執行 field／slot dirty merge：只有仍等於清除 baseline 且未被後續 callback 修改的值才回填；被替換、刪除、重拍或明確清空者保留現值。
5. 回填完整 metadata 後一次同步 UI、`currentFormData` 與 draft callback；consume snapshot，下一次勾選重新建立。
6. 相機啟動前產生 slot generation token，經 `CameraOverlayFragment` 傳遞／回傳；session 結束、重拍或切換時 invalidate，late result 不得更新 UI、currentFormData、pending path 或觸發 upload。
7. configuration change 保留 ViewModel snapshot；process death 不恢復；成功送出、取消離開與真正結束表單清除 snapshot。
8. 保留並排除 user-owned `GutterApiService.kt` base-URL 變更，不得納入 0910-1 commit；本次實作不得依賴它。移除 abandoned `notifyUploadHost=false` 變更，並保留 `currentFormSnapshot()` 的 cant-open sanitization，確認 session snapshot 不會寫入 Room／API。

## Test Plan
- `CantOpenSessionSnapshotTest`：完整 schema、取消、確認清除、metadata 回填、dirty field／replace／delete／re-capture／explicit blank、snapshot consume、draft isolation。
- `GutterCantOpenUiTest`：新增／編輯 Alert、cancel／dismiss、confirm、uncheck restore、view/import/virtual/open-gutter。
- late camera tests：勾選後、回填後、同 slot 重拍後、真正離開後均不得更新或 upload。
- 執行 targeted tests、unit tests、debug build，並檢查 request diff。

## Regression Plan
- 一般點位三張照片與無法開蓋第 1 張照片驗證。
- `storeDitch` request、photo image ID、upload state、pending path 與草稿同步保持既有行為。
- rotation 保留、process death／新表單不恢復；非 401／非本功能錯誤沿用原處理。

## Acceptance Criteria Traceability
| AC | Steps | Evidence |
|---|---|---|
| AC-001 | 2 | UI Alert test |
| AC-002 | 2 | cancel／dismiss UI test |
| AC-003 | 3 | clear-flow test |
| AC-004 | 1、5、8 | snapshot／draft／request inspection |
| AC-005 | 1、4、5 | restore metadata test + UI test |
| AC-006 | 4 | dirty merge cases |
| AC-007 | 1、6、7 | rotation、process death、exit、late-result tests |
| AC-008 | 3、8 | request and upload regression |
| AC-009 | 2、7 | mode-specific UI tests |

## Risks
- lifecycle、callback batching、late camera result、metadata 與 stale image ID。

## Failure Behavior
- Alert 取消／dismiss 或 Fragment view 已失效時，不切換 Checkbox、不清除資料。
- snapshot 回填時若照片 URI 已失效，保留可用的目前資料並記錄 log，不寫入失效 URI。
- 草稿同步失敗沿用既有錯誤處理，不改寫 API contract；上傳失敗不得清除本地照片。

## Security and Privacy
- snapshot 僅存在 Activity-scoped ViewModel memory，不寫入 SavedState、Intent、`Waypoint.basicData`、Room 或 API payload。
- 照片沿用既有本機 URI 權限與清理流程，不新增外部傳輸或分享。

## Rollback Plan
- 回退本次功能 commit 可恢復既有立即清除行為；不得回退或覆寫使用者在 `GutterApiService.kt` 的 URL 工作區變更。

## Open Questions
無
