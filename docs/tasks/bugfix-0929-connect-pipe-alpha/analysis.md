# Analysis

`applyConnectionMutualExclusionUi()` 對 `layoutConnectPipe` 設定 `alpha = 0.5`；同一個狀態更新流程中的 `setCantOpenFieldsEnabled()` 又對子層 `rgConnectPipe` 設定 `alpha = 0.5`。Android View 階層的 alpha 會相乘，使連接管選項有效透明度為 `0.25`，而其他欄位只有一層 `0.5`。

`layoutConnectPipe` 同時包含標題列，因此父層 alpha 也會額外影響標題與必填星號。其他欄位的標題列不是同一個被 alpha 套用的父層，造成視覺不一致。

結論：這是 UI 狀態控制的重複 alpha，不是 drawable、材質或主題顏色差異。
