# featGutterType - Test Requirement

## 1. 測試目標
確認 `SPI_TYP_TEMPORARY` 從 UI、草稿、離線入口到 API 上傳與回傳顯示都能完整運作，且舊資料可相容。

## 2. 測試前提
- 已完成 `tvGutterTypeSelector` UI 加入
- 已完成 `SPI_TYP_TEMPORARY` 的資料模型更新
- 已完成草稿與離線入口串接

## 3. 核心驗證點

1. UI 是否出現在正確位置
2. 選值是否能正確顯示
3. 上傳時是否以 `Int` 送出
4. 回傳時是否以 `String` 接收
5. 檢視頁是否使用回傳值回填
6. 草稿是否能保存與恢復
7. 離線入口 `msg_offline_form` 是否正常
8. 舊草稿與缺值是否能 fallback 到起點的 `SPI_TYP`

## 4. 測試案例

### 4.1 UI 位置驗證
**步驟**
1. 開啟 `AddGutterBottomSheet`
2. 檢查 `layoutGutterType` 與 `rvWaypoints` 中間是否存在 `tvGutterTypeSelector`

**預期結果**
- `tvGutterTypeSelector` 顯示在正確位置
- 樣式不破壞既有版面

### 4.2 側溝類型選擇
**步驟**
1. 選擇 `1`
2. 選擇 `2`
3. 選擇 `3`
4. 選擇 `4`

**預期結果**
- 對應顯示分別為：
  - `1` → `U形溝（明溝）`
  - `2` → `U形溝（加蓋）`
  - `3` → `L形溝與暗溝渠併用`
  - `4` → `其他`

### 4.3 新增送出驗證
**步驟**
1. 在新增流程中選擇一個側溝類型
2. 填完必要欄位並送出
3. 檢查 `storeDitch` request

**預期結果**
- `SPI_TYP_TEMPORARY` 以 `Int` 送出
- 不可為空
- API 成功後流程正常結束

### 4.4 編輯回填驗證
**前置條件**
- 既有資料含 `SPI_TYP_TEMPORARY`

**步驟**
1. 開啟編輯模式
2. 觀察 `tvGutterTypeSelector`
3. 送出更新

**預期結果**
- 編輯畫面會帶入原值
- 更新後資料維持一致

### 4.5 檢視頁驗證
**前置條件**
- API 回傳包含 `SPI_TYP_TEMPORARY`

**步驟**
1. 開啟檢視頁
2. 檢查 `tvSpiTyp`

**預期結果**
- `tvSpiTyp` 顯示 API 回傳的 `SPI_TYP_TEMPORARY`
- 顯示文字正確

### 4.6 草稿保存與恢復
**步驟**
1. 選擇側溝類型
2. 產生草稿
3. 關閉並重新打開草稿

**預期結果**
- 草稿保留 `SPI_TYP_TEMPORARY`
- 恢復後 UI 顯示一致
- 再次送出時仍可帶入

### 4.7 離線入口驗證
**步驟**
1. 從 `msg_offline_form` 進入離線流程
2. 選擇側溝類型
3. 儲存草稿
4. 恢復草稿

**預期結果**
- 離線入口可正常使用
- 側溝類型資料不遺失

### 4.8 舊草稿相容性
**前置條件**
- 草稿沒有 `SPI_TYP_TEMPORARY`

**步驟**
1. 開啟舊草稿
2. 進入編輯或檢視

**預期結果**
- 系統不崩潰
- 自動取得「起點」的 `SPI_TYP`
- UI 正常顯示 fallback 值

### 4.9 缺值資料相容性
**前置條件**
- API 回傳缺少 `SPI_TYP_TEMPORARY`

**步驟**
1. 開啟檢視頁或編輯頁

**預期結果**
- 會自動 fallback 到起點的 `SPI_TYP`
- 不會出現空白錯誤或例外

## 5. AI 迭代檢查清單
每次修改後，AI 應逐項確認：

1. `tvGutterTypeSelector` 是否已加入正確位置？
2. `SPI_TYP_TEMPORARY` 是否只在側溝層級保存？
3. 上傳是否確實是 `Int`？
4. 回傳是否確實以 `String` 接收？
5. 檢視頁是否使用回傳值填入 `tvSpiTyp`？
6. 草稿是否會保留這個欄位？
7. `msg_offline_form` 是否有同步支援？
8. 舊草稿與缺值是否會 fallback 到起點的 `SPI_TYP`？

## 6. 完成判定
只有當下列條件都成立，才可以視為測試通過：

- 新增、編輯、檢視都能顯示正確側溝類型
- 上傳 request 與 response 型別規格符合需求
- 草稿與離線流程都可保存與恢復
- 舊資料與缺值可正確 fallback

