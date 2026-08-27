# Repository Analysis

## Current Behavior
- App 目前是登入後直接進入 `MainActivity` 的地圖工作區，沒有可切換的 tabbar 導覽。
- 現有主流程集中在地圖、編輯、檢視、草稿與圖層控制，尚未有獨立的儀表板頁面。
- `GutterApiService` / `GutterRepository` 已有多個 API 入口，但尚未定義 dashboard 相關 endpoint 與 response model。
- `strings.xml`、`drawable/` 與 `layout/` 目前也沒有儀表板專用資源或圓餅圖元件。

## Expected Behavior
- 使用者登入後先進入新的主導覽容器，容器內有兩個 tab：`map` 與 `material-symbols:dashboard-rounded`。
- 預設顯示原本的主地圖，儀表板是第二個 tab。
- 儀表板畫面要能向 `/v1/dashboard/getDashboard` 取資料，並把「調查長度」與「調查進度與狀態」分區顯示出來。
- 畫面需支援「設定」複選組別、「篩選」日期/月分條件，以及「全部組別」單選切換。
- 圓餅圖需根據 API 回傳的進度數值即時更新，且與清單/卡片的選擇狀態一致。
- 主導覽容器需負責 tabbar 與底部安全區處理；地圖專用浮動控制項只在 map tab 顯示，切到儀表板時隱藏。

## Affected Modules
- `app/src/main/java/com/example/taoyuangutter/MainShellActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/MainActivity.kt`
- `app/src/main/res/layout/activity_main_shell.xml`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/example/taoyuangutter/api/GutterApiService.kt`
- `app/src/main/java/com/example/taoyuangutter/api/GutterApiModels.kt`
- `app/src/main/java/com/example/taoyuangutter/api/GutterRepository.kt`
- `app/src/main/java/com/example/taoyuangutter/dashboard/*` new dashboard screen, state, and chart classes
- `app/src/main/res/layout/*` new dashboard layout and dialogs
- `app/src/main/res/menu/*` or equivalent tabbar menu resource
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/drawable/*` dashboard icon and empty/error state assets
- `app/src/test/java/...` and `app/src/androidTest/java/...` for parsing, state, and UI coverage

## Dependencies
- Existing auth token flow and 401 handling, so dashboard API failure can reuse current login redirect behavior.
- Retrofit / Gson request and response handling for dynamic dashboard payload keys.
- Material Components for bottom navigation, dialogs, chips, and selector styles.
- A pie chart implementation, most likely a custom View using Canvas so the feature does not need a new third-party chart library.
- Existing `main` / map layout spacing rules, because the new tabbar must not break the current main screen’s insets.
- Host-level tab visibility coordination for map-only floating controls, dashboard-only content, and shared bottom navigation.

## Risks
- The repo currently has no true tabbar shell, so adding one requires a new host container and a refactor of the current map entry flow.
- Dashboard response keys are dynamic and partly Chinese-named, which makes model mapping and null-safety more error-prone.
- The requirement includes mutually exclusive date and month filtering, so filter state must be guarded carefully to avoid sending both.
- The summary cards are responsive and group-count dependent, so smaller devices may need adaptive wrapping and scrolling.
- Pie chart rendering must handle zero totals and partial data without crashing or drawing misleading segments.

## Unknown Assumptions
無
