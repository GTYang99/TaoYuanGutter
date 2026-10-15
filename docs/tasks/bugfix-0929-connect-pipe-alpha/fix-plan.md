# Fix Plan

## Implementation

- 移除 `layoutConnectPipe.alpha` 的第二個狀態來源。
- 讓 `rgConnectPipe` 繼續沿用既有 `0.5` 停用透明度。
- 以 instrumentation test 確認：`layoutConnectPipe.alpha == 1f`、`rgConnectPipe.alpha == 0.5f`、`tvConnectPipeTitle.alpha == 1f`。

## Validation

- `git diff --check`
- `:app:testDebugUnitTest`
- `:app:assembleDebug`
- 單一 `emulator-5554` 上的 focused `GutterBasicInfoUiTest` method
