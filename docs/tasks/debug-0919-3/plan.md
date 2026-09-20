# Implementation Plan

## Goal
- 移除無側溝面板對 IME bottom inset 的重複位移，讓面板在鍵盤輸入期間保持可用並保留既有回報流程。

## Scope
- 調整共用 `NoDitchModeUiController` 的面板 inset 策略，補上針對 IME 顯示／收起與既有狀態流程的驗證。

## Affected Files
- `app/src/main/java/com/example/taoyuangutter/main/NoDitchModeUiController.kt`：只保留面板所需的系統底部安全區域，避免把 IME 高度再加成面板 bottom margin。
- `app/src/androidTest/java/com/example/taoyuangutter/NoDitchModeUiControllerTest.kt`：驗證共用控制器的面板位置、IME 狀態與回報狀態流程。
- `docs/tasks/debug-0919-3/`：需求、分析、根因、計畫、執行及驗證證據。

## Implementation Steps
- 以目前已保存的原始 bottom margin 為基準，只用 `systemBars.bottom` 更新面板 margin，不再使用 `ime.bottom` 作為額外位移。
- 保留控制器既有的 enter、點位選取、備註輸入、重設、送出及離開邏輯。
- 建立 focused instrumentation coverage，注入 system bar／IME inset 組合並檢查面板 margin 及輸入狀態。
- 執行靜態檢查、單元測試、instrumentation test；無法取得折疊裝置時明確記錄 `NOT VERIFIED`。

## Test Plan
- `./gradlew testDebugUnitTest`
- `./gradlew :app:assembleDebug`
- `./gradlew connectedDebugAndroidTest`（若連線裝置可執行）
- focused instrumentation test：驗證 IME 顯示時不再把 IME 高度加到面板 bottom margin，並驗證鍵盤收起及既有操作狀態。

### Physical Device Test Scope
- Requires physical device: Yes
- Device/environment: 可用 Android 裝置；AC-001 的完整證據需緊湊／折疊式視窗配置，另記錄 Android 版本、鍵盤、視窗尺寸、IME／system bar inset 及面板座標。
- In-scope Acceptance Criteria: AC-001, AC-002, AC-003
- Regression risk: 面板可能在不同 `windowSoftInputMode` 或 edge-to-edge 行為下與鍵盤重疊；既有點位回報狀態可能受控制器重繪影響。
- Full regression required: No
- Full regression trigger: 若 focused test 或裝置測試發現地圖點選、回報送出或其他底部面板回歸，才擴大至相關主畫面回歸。
- Stop condition: 指定案例完成，或在缺少折疊配置時取得足夠的限制證據並將受影響 AC 標記 `NOT VERIFIED`。

| AC | Environment | Steps | Expected | Evidence |
|---|---|---|---|---|
| AC-001 | 緊湊／折疊式視窗配置；實體鍵盤 | 進入無側溝、選取點位、聚焦備註並輸入文字 | 面板不被推到畫面上方，備註與操作仍可用 | instrumentation result；折疊裝置座標紀錄，若不可用則 `NOT VERIFIED` |
| AC-002 | 同上，鍵盤先顯示後收起 | 收起鍵盤並等待 inset 更新 | 面板回到底部相對位置且不殘留 IME 位移 | instrumentation result；裝置前後座標紀錄 |
| AC-003 | Android instrumentation host | 呼叫既有控制器狀態轉換並輸入有效備註 | 點位、重設、送出、離開 callback／狀態維持既有行為 | focused test result |

## Regression Plan
- 確認共用控制器仍能在 `MainActivity` 與 `MapWorkspaceFragment` 的流程中進入／離開無側溝模式。
- 確認選取點位後備註欄位與送出按鈕仍依輸入內容啟用，重設會清除輸入。
- 確認鍵盤收起後重新套用 inset 不會累加 margin。

## Risks
- 沒有折疊裝置時，無法證明所有廠牌的 IME resize／pan 行為；此項不以模擬結果代替實機 PASS。
- 共用控制器同時服務兩個主畫面入口，任何 inset 變更都需保留兩條路徑。

## Rollback Plan
- 若 targeted validation 顯示面板與鍵盤重疊，回退本次控制器 inset 變更，保留根因與失敗證據並重新制定可驗證的策略。

## Current Behavior 
- 面板已經 constraint 到主畫面底部，控制器卻將 `max(systemBars.bottom, ime.bottom)` 另加到原始 bottom margin；鍵盤顯示時因此可能發生重複上移。

## Expected Behavior
- 面板只因系統底部安全區域調整 margin；鍵盤顯示／收起不會再透過 IME 高度額外推高整個面板，既有內容捲動與回報狀態維持不變。

## Acceptance Criteria Traceability
| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | 只使用 system bar bottom inset；保留 inline 面板 | focused instrumentation；折疊實機座標（缺少時 `NOT VERIFIED`） |
| AC-002 | 每次 inset 更新都從保存的 base margin 計算 | IME 顯示／收起 focused test |
| AC-003 | 不改變 controller state machine | focused instrumentation 與既有主畫面測試 |

## Failure Behavior
- 若無法取得折疊裝置或鍵盤 inset 證據，相關驗收標記 `NOT VERIFIED`，不得當作 PASS。
- 若測試發現其他主畫面底部元件回歸，停止擴大修改並依 issue 分類回到 debug 或 planning。

## Security and Privacy
- 無新增權限、網路請求、憑證或個人資料處理。

## Open Questions
- 無。
