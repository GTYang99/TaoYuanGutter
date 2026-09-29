# Root Cause

## Confirmed Cause

`layoutConnectPipe` 與其子層 `rgConnectPipe` 被分別設定 `alpha = 0.5`。當 `cbCantOpen` 或 `cbConnectPoint` 勾選時，兩個方法會先後執行這兩個設定，導致子層有效 alpha 為 `0.5 × 0.5 = 0.25`。

父層還包含 `tvConnectPipeTitle` 與 `tvConnectPipeRequired`，所以標題與必填提示也會被父層一起變淡；其他欄位只對內容群組設定 alpha，故產生不一致。

## Evidence

- `GutterBasicInfoFragment.kt`: `setCantOpenFieldsEnabled()` 對 `rgConnectPipe` 設定停用 alpha。
- `GutterBasicInfoFragment.kt`: `applyConnectionMutualExclusionUi()` 原本又對 `layoutConnectPipe` 設定 alpha。
- 新增 `GutterBasicInfoUiTest.detailExemptionKeepsConnectPipeOpacityAlignedWithOtherDisabledFields` 覆蓋兩個觸發狀態。
