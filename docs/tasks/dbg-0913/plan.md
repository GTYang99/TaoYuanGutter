# Implementation Plan

## Goal
- 修正無法開蓋確認時機與未完成表單返回提醒，且不改草稿與檢視流程。

## Scope
- 僅變更側溝表單的無法開蓋勾選與返回 Activity 路徑，及其測試和任務紀錄。

## Affected Files
- `GutterBasicInfoFragment.kt`：判斷是否有即將清除的內容。
- `GutterFormActivity.kt`：返回前的既有驗證與通知 Alert。
- `GutterCantOpenUiTest.kt`、新增／擴充 unit tests：回歸覆蓋。

## Implementation Steps
- 新增以實際清除欄位和照片 slot 2／3 為準的判斷，僅在需要時顯示既有 Alert。
- 在 Activity 實際 finish 前檢查既有必填規則；不完整時顯示指定確認 Alert，確認後走原有 finish 路徑。
- 保留 preview 分支與既有草稿同步順序。
- 擴充測試並執行建置及相關測試。

## Test Plan
- 單元測試資料存在判斷。
- Android UI 測試無資料／有資料的無法開蓋 Alert、返回未完成與完成表單。
- `testDebugUnitTest`、`assembleDebug`，可用時執行目標 connected test。

## Regression Plan
- 確認無法開蓋確認／取消後清除和勾選狀態不變。
- 確認 slot 1 不影響無法開蓋確認，slot 2／3 仍會觸發。
- 確認檢視→編輯→預覽不 finish 且不顯示 Alert。

## Risks
- Activity 與 Fragment 建立時機可能令驗證 Fragment 暫不可得；此時維持原本返回流程，避免阻斷。

## Rollback Plan
- 還原本任務提交即可恢復既有返回與確認時機。

## Current Behavior
- 無法開蓋總是確認，返回未完成表單沒有通知。

## Expected Behavior
- 僅有待清除內容才確認；未完成返回會通知後仍保存草稿並返回。

## Acceptance Criteria Traceability
| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | 1 | unit + UI test |
| AC-002 | 1 | existing/extended UI test |
| AC-003 | 2 | UI test |
| AC-004 | 2 | UI test |
| AC-005 | 3 | UI test/manual path |
| AC-006 | Follow-up shared-validator fix | Android UI test |
| AC-007 | Follow-up shared-validator fix | Android UI test |

## Failure Behavior
- 若 Fragment 尚未可用，保留既有的草稿保存與返回，不阻塞使用者。

## Security and Privacy
- 無新增網路、權限或資料蒐集。

## Open Questions
- 無
