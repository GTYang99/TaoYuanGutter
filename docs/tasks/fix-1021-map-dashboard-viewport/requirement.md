# Requirement

## Background
- 登入後主導覽可在主地圖與儀表板間切換。切回主地圖時，目前會重新顯示桃園市政府附近的預設中心。

## Goal
- 往返切換主地圖與儀表板時，保留地圖目前的鏡頭中心與縮放，不因切換回到固定預設座標。

## Functional Requirements
- 地圖 tab 與儀表板 tab 互斥顯示。
- 返回地圖 tab 時保留同一地圖畫面狀態，包括相機中心與 zoom。
- 初次進入地圖時的既有 fallback／定位行為，以及明確的定位按鈕行為維持不變。

## Non-functional Requirements
- 不新增地圖資料、定位權限、API 或持久化需求。
- 將測試限制在 Android 測試 emulator；不得對連線的實體裝置安裝或執行測試。

## Acceptance Criteria
- AC-001：在地圖上移動並縮放後切到儀表板再返回，地圖保留切換前的中心與 zoom，不重新套用桃園預設中心。
- AC-002：地圖與儀表板仍互斥顯示；切換兩個 tab 往返時各自保留原 fragment 實例。
- AC-003：首次開啟地圖仍走既有初始位置／定位流程，明確定位按鈕仍可定位；切回 tab 不觸發新的初始化定位。

## Constraints
- 不改變 `MapWorkspaceFragment.onMapReady()` 在首次建立地圖時的既有初始化定位策略。
- 只在明確 Android 測試 emulator 執行測試；實體裝置不作為測試目標。

## Open Questions
- 無
