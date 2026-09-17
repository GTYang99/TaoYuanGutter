# Requirement

## Goal
- 使用者開啟新增側溝清單或側溝編輯面板時，只能看見並操作既有主地圖 `@+id/btnMeasureDistance`；按下後使用既有測距模式，退出後回到同一面板與資料狀態。

## Functional Requirements
- `btnMeasureDistance` 是兩個目標面板唯一的測距入口；不得在 `AddGutterListBottomSheet` 或 `AddGutterBottomSheet` 顯示代理測距按鈕。
- 目標面板正常顯示期間，隱藏其他主地圖控制項：登出、新增側溝、圖例、圖層、草稿、回報無側溝與定位；只顯示且啟用 `btnMeasureDistance`。
- `btnMeasureDistance` 位於主地圖右上控制區最上方，位置在兩種面板上緣以外且可點選。
- 清單來源：清單開啟時立即隱藏全部 scope 側溝線段；按主地圖按鈕後清單收起，但保留 working layer 的既有線段／節點；退出後回到相同清單，scope 線段依目前 `showPlan` 偏好恢復。
- 編輯來源：按主地圖按鈕後編輯面板收起，保留目前編輯側溝線段及／或節點；退出後回到相同面板與資料。
- Android 返回鍵先退出測距並回到來源面板；既有起點、虛線、距離顯示、重設及關閉行為維持不變。

## Constraints
- 兩個 BottomSheetDialog 視窗都必須將面板外、測距按鈕所在區域的觸控轉送給主地圖；提高 Activity view elevation 不足以達成可點選。
- 上傳或 blocking overlay 顯示時維持既有互動鎖定，不開放測距。
- 不改變草稿、上傳、側溝資料或測距演算法。

## Acceptance Criteria
- AC-001：兩種面板正常開啟時，其他主地圖控制項均不顯示，只有主地圖 `btnMeasureDistance` 可見且可點；兩面板內沒有代理測距按鈕。
- AC-002：主地圖測距按鈕位於兩種面板上緣以外；在兩面板中點擊都送達主地圖，且不觸發 cancel、dismiss 或關閉確認。
- AC-003：清單開啟時隱藏全部 scope 線段；清單來源量測時收起清單並保留 working layer 的既有線段／節點；退出或 Back 後回到相同清單，scope 線段依最新 `showPlan` 偏好恢復。
- AC-004：編輯來源量測時收起面板並保留工作線段／節點；退出或 Back 後回到同一面板與資料。
- AC-005：既有測距重設、關閉、距離顯示、地圖點擊 listener 回復、多草稿、草稿保存、一般編輯與其他主地圖控制項正常回復不回歸。

## Open Questions
- 無。使用者已確認面板開啟時只顯示主地圖 `btnMeasureDistance`。
