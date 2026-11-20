# feat1118-2 收尾與驗證

補齊目前已完成的行政區篩選與離線圖層版本控制實作，接著整理剩餘 warning、補強單元測試，並在 verification 前完成 commit 與本地驗證。

## User Review Required

> [!IMPORTANT]
> 本次僅做收尾修正與驗證，不再擴大功能範圍。

## Open Questions

- 無。

## Proposed Changes

### Main map overlay state

#### [MODIFY] [MainViewModel.kt](file:///Users/a10362/AndroidStudioProjects/TaoYuanGutter/app/src/main/java/com/example/taoyuangutter/main/MainViewModel.kt)
- 保留 overlay state 的本地持久化與還原邏輯。
- 補強 load/save 的單元測試。

#### [MODIFY] [MainActivity.kt](file:///Users/a10362/AndroidStudioProjects/TaoYuanGutter/app/src/main/java/com/example/taoyuangutter/MainActivity.kt)
- 保留啟動還原與狀態保存流程。

#### [MODIFY] [MapWorkspaceFragment.kt](file:///Users/a10362/AndroidStudioProjects/TaoYuanGutter/app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt)
- 保留 fragment 層的還原與保存流程。

---

### Offline polygon overlay versioning

#### [MODIFY] [MapOverlayController.kt](file:///Users/a10362/AndroidStudioProjects/TaoYuanGutter/app/src/main/java/com/example/taoyuangutter/map/MapOverlayController.kt)
- 保留 generation-based versioning。
- 補強 generation 增量驗證。

#### [MODIFY] [OfflinePolygonTileProvider.kt](file:///Users/a10362/AndroidStudioProjects/TaoYuanGutter/app/src/main/java/com/example/taoyuangutter/map/OfflinePolygonTileProvider.kt)
- 保留 generation-based stale result discard。

---

### Tests

#### [MODIFY] [MapOverlayControllerStateTest.kt](file:///Users/a10362/AndroidStudioProjects/TaoYuanGutter/app/src/test/java/com/example/taoyuangutter/map/MapOverlayControllerStateTest.kt)
- 補強 overlay state 與 generation 行為驗證。

#### [NEW] [MainViewModelTest.kt](file:///Users/a10362/AndroidStudioProjects/TaoYuanGutter/app/src/test/java/com/example/taoyuangutter/main/MainViewModelTest.kt)
- 驗證 save/load 還原與預設行政區回填。

## Verification Plan

### Automated Tests
- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:assembleDebug`

### Manual Verification
- 檢查 `git status --short` 只剩預期變更。
- 確認 commit 已建立後再進入驗證結果整理。
