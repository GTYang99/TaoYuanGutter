# Implementation Plan

## Goal
- 完成確認清除；取消勾選後維持清除狀態，同時維持既有上傳結構與流程。

## Current Behavior
- 目前提交版本已有 Alert，但會在取消勾選時以 session snapshot 回填資料；此行為與最新決策不符，且照片清除存在 upload metadata 不同步風險。

## Expected Behavior
- 確認後清除；取消 Alert 不改資料；確認後取消勾選仍維持清除資料。

## Affected Files
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/CameraOverlayFragment.kt`
- `app/src/androidTest/java/com/example/taoyuangutter/GutterCantOpenUiTest.kt`

## Implementation Steps
1. 在使用者勾選時先維持未勾選並顯示 Alert；取消／dismiss 不清除、不更新 upload state。
2. 確認後以既有 batch callback 清除欄位與 slot 2／3，正常同步 Activity／Waypoint upload metadata；第 1 張保留。
3. 取消勾選時只恢復欄位可編輯與必填 UI，不回填任何欄位或照片資料。
4. 移除 `CantOpenSessionViewModel`、snapshot／dirty merge 和相機 token contract，以及僅為回填加入的測試；在相機結果處理處以目前 cant-open 狀態拒絕 slot 2／3，確保確認清除後的 late result 不會寫入或觸發上傳。
5. 保留並排除 user-owned `GutterApiService.kt` base-URL 變更，不得納入 0910-1 commit；本次實作不得依賴它。移除 `notifyUploadHost=false`，保留 `currentFormSnapshot()` 的 cant-open sanitization，確保草稿與 API 都是已清除的資料。

## Test Plan
- `GutterCantOpenUiTest`：新增／編輯 Alert、cancel／dismiss、confirm、取消勾選後維持清除、view/import/virtual/open-gutter。
- late camera tests：確認清除後的 slot 2／3 回傳不得更新或 upload。
- 執行 targeted tests、unit tests、debug build，並檢查 request diff。

## Regression Plan
- 一般點位三張照片與無法開蓋第 1 張照片驗證。
- `storeDitch` request、photo image ID、upload state、pending path 與草稿同步保持既有行為。
- 旋轉、離開與新表單均不得產生舊資料回填；非 401／非本功能錯誤沿用原處理。

## Acceptance Criteria Traceability
| AC | Steps | Evidence |
|---|---|---|
| AC-001 | 1 | UI Alert test |
| AC-002 | 1 | cancel／dismiss UI test |
| AC-003 | 2 | clear-flow test |
| AC-004 | 3 | uncheck-keeps-cleared UI test |
| AC-005 | 2、5 | request and upload regression |
| AC-006 | 1、3 | mode-specific UI tests |

## Risks
- callback batching、late camera result、metadata 與 stale image ID。

## Failure Behavior
- Alert 取消／dismiss 或 Fragment view 已失效時，不切換 Checkbox、不清除資料。
- 取消勾選不回填任何資料；若相機結果在 cant-open 狀態回傳，忽略 slot 2／3 的結果。
- 草稿同步失敗沿用既有錯誤處理，不改寫 API contract；上傳失敗不得清除本地照片。

## Security and Privacy
- 照片沿用既有本機 URI 權限與清理流程，不新增外部傳輸或分享。

## Rollback Plan
- 回退本次功能 commit 可恢復既有立即清除行為；不得回退或覆寫使用者在 `GutterApiService.kt` 的 URL 工作區變更。

## Open Questions
無
