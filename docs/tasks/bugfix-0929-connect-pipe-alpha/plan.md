# Plan

1. 移除 `applyConnectionMutualExclusionUi()` 對 `layoutConnectPipe` 的父層 alpha 設定。
2. 保留 `rgConnectPipe` 與其他 RadioGroup 相同的 `alpha = 0.5` 停用視覺。
3. 新增 instrumentation 回歸測試，覆蓋「銜接點」與「無法開蓋」兩種狀態，確認父層、選項群組與標題透明度。
4. 執行 debug 建置、JVM 單元測試與單一裝置 focused UI test。

## Non-goals

不修改連接管的資料格式、互斥行為、表單驗證、照片上傳或測試機動畫設定。
