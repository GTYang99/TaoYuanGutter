# Repository Analysis

## Current Behavior
- `MainActivity` 目前會在 `GoogleMap` 的 `onCameraIdle` 觸發後，以防抖方式呼叫 `scopeSearch`。
- 載入提示目前使用一般 `Toast`，內容是載入中、載入完成、載入失敗三種訊息。
- 主畫面已有其他底部 UI 元件，例如測距面板、無側溝面板與選點 overlay，但尚未有專屬的主地圖載入指示器。

## Expected Behavior
- 主地圖新增一個固定在底部 toast 區域的載入指示器。
- 指示器要能表達三種狀態：級數太小、等待載入中、載入完成後消失。
- `scopeSearch` 的觸發時機要多一層條件判斷，必須同時滿足縮放門檻與使用者操作結束。

## Affected Modules
- `app/src/main/java/com/example/taoyuangutter/MainActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/main/MainViewModel.kt`
- `app/src/main/java/com/example/taoyuangutter/map/ScopeMapCoordinator.kt`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/values/strings.xml`
- 可能新增的主地圖指示器控制類別或 layout 資源

## Dependencies
- GoogleMap camera 事件，特別是 `onCameraIdle` 與縮放資訊。
- 既有 `ScopeMapCoordinator` / `ScopeViewportLoader` 的 `scopeSearch` 流程。
- `ActivityMainBinding` 與目前主畫面 overlay 配置。
- 現有字串與 icon 資源。

## Risks
- `onCameraIdle` 可能同時被使用者拖拉與程式化移動觸發，若沒有額外保護，容易重複載入。
- 新增底部指示器可能與既有的測距、無側溝、選點 overlay 發生視覺重疊。
- 縮放門檻若直接以浮點數判斷，可能在 9.x 與 10.0 之間出現體驗落差，需要一致的顯示與載入規則。
- 載入成功或失敗後的狀態收斂如果不一致，可能讓底部指示器殘留或過早消失。

## Unknowns
- `V` / `X` icon 是否直接沿用既有 drawable，或需要新增專用圖示資源。
- 「使用者拖拉確定後」是否只要 `onCameraIdle` 即可，或還需要額外區分手勢來源。
