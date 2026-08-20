# featGutterType - 功能需求

## 1. 需求目的
在側溝新增、編輯、檢視、草稿保存與離線流程中，新增一個「側溝類型」欄位，並讓它完整串接 UI、資料模型、API 與草稿資料。

## 2. 功能範圍
本次變更涵蓋以下範圍：

- `AddGutterBottomSheet` 新增 `@+id/tvGutterTypeSelector`
- `SPI_TYP_TEMPORARY` 的上傳、回傳、檢視顯示
- 草稿資料保存與恢復
- 離線入口 `msg_offline_form`
- 舊草稿與缺值資料的 fallback 行為

## 3. UI 規格

### 3.1 新增位置
- 在 `AddGutterBottomSheet.kt` 對應畫面中，於 `@+id/layoutGutterType` 下方、`@+id/rvWaypoints` 上方加入 `@+id/tvGutterTypeSelector`
- 這個欄位屬於側溝層級，不是單一 waypoint 層級

### 3.2 顯示內容
- `tvGutterTypeSelector` 顯示目前選取的側溝類型文字
- 顯示文字需對應 `SPI_TYP_TEMPORARY`
- 若沒有值，需依 fallback 規則自動帶入

### 3.3 選項對應
- `1` = `U形溝（明溝）`
- `2` = `U形溝（加蓋）`
- `3` = `L形溝與暗溝渠併用`
- `4` = `其他`

## 4. 資料規格

### 4.1 欄位名稱
- 上傳欄位名稱：`SPI_TYP_TEMPORARY`
- 回傳欄位名稱：`SPI_TYP_TEMPORARY`

### 4.2 型別規格
- 上傳時型別：`Int`
- 上傳時不可為空
- 回傳完成後，資料結構中的對應值型別為 `String`

### 4.3 資料位置
- `SPI_TYP_TEMPORARY` 存在於「側溝層級」
- 不應放在單一 `Waypoint` 的點位資料中
- 草稿資料需能保存這個側溝層級欄位

## 5. 資料流規格

### 5.1 新增流程
1. 使用者在 `AddGutterBottomSheet` 選擇側溝類型
2. UI 更新 `tvGutterTypeSelector`
3. 送出時將 `SPI_TYP_TEMPORARY` 以 `Int` 帶入 `storeDitch`
4. API 回傳後，以 `String` 接收回傳值
5. 檢視或再次編輯時，以回傳值回填 UI

### 5.2 編輯流程
1. 進入編輯模式時，優先使用資料內的 `SPI_TYP_TEMPORARY`
2. 若沒有 `SPI_TYP_TEMPORARY`，則自動取「起點」的 `SPI_TYP`
3. UI 顯示對應文字
4. 送出時仍以 `SPI_TYP_TEMPORARY` 上傳

### 5.3 檢視流程
1. 檢視頁的 `tvSpiTyp` 使用 API 回傳的 `SPI_TYP_TEMPORARY` 填入
2. 若回傳缺值，採用 fallback 規則

### 5.4 草稿流程
1. 草稿必須保存 `SPI_TYP_TEMPORARY`
2. 草稿恢復後，UI 需可再次顯示相同值
3. 舊草稿若沒有此欄位，需自動 fallback

### 5.5 離線流程
1. `msg_offline_form` 入口必須支援這個欄位
2. 離線新增、離線編輯、離線草稿恢復都要保留 `SPI_TYP_TEMPORARY`

## 6. Fallback 規則

### 6.1 缺值時的預設來源
- 若 `SPI_TYP_TEMPORARY` 沒值
- 或舊草稿沒有此欄位
- 或 API 回傳缺少此欄位

則自動取得「起點」點位的 `SPI_TYP` 作為預設值。

### 6.2 顯示行為
- fallback 後的值需轉成對應的文字顯示
- 不可因缺值造成畫面空白、崩潰或資料遺失

## 7. 顯示一致性要求
以下畫面必須顯示一致：

- 新增側溝畫面
- 編輯側溝畫面
- 檢視頁 `tvSpiTyp`
- 草稿恢復畫面
- 離線入口 `msg_offline_form`

## 8. 資料模型影響
需要同步更新的資料位置：

- `StoreDitchRequest`
- `StoreDitchResponse`
- `DitchDetails`
- 草稿資料結構
- 草稿恢復邏輯
- 檢視頁資料回填邏輯

## 9. 驗收標準
以下條件全部成立，功能才算完成：

- UI 位置正確
- `tvGutterTypeSelector` 可正常顯示與操作
- `SPI_TYP_TEMPORARY` 以 `Int` 正確送出
- API 回傳可用 `String` 接收
- 檢視頁可正常顯示回傳值
- 草稿可保存與恢復
- 離線入口可使用
- 舊草稿與缺值可自動 fallback 到起點的 `SPI_TYP`

