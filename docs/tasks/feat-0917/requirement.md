# Requirement

## Goal

- 新增每個側溝點位的「銜接點」與「連接管」屬性，改由系統產生新增點位的測量座標編號，並把既有點位匯入改為載入最近存檔點位。

## Functional Requirements

### 銜接點與連接管

- 新增、草稿、檢視與編輯的非虛擬點都必須保存並顯示兩個屬性。
- 銜接點位於「測量狀態」中，排在「無法開蓋」之後；與「無法開蓋」互斥，兩者可同時未選。
- 銜接點送至 `storeDitch` 的暫定 key 為 `is_connect_point`，以整數 `0`／`1` 表示未選／選取；檢視／編輯 response 使用同名 key、`Boolean`，缺值為 `false`。
- 連接管位於「淤積程度」下方，選項為必選「無」／「有」，預設「無」；暫定 API key 為 `is_connect_pipe`，以整數 `0`／`1` 表示；檢視／編輯 response 使用同名 key、`Boolean`，缺值為 `false`。
- 點擊虛擬點後，必須關閉並自動取消「無法開蓋」與「銜接點」（均設為 `false`），並關閉連接管；虛擬點的 `storeDitch` payload 必須省略這三個 key。

### 系統自帶節點名

- 新增表單不顯示、也不要求使用者輸入「測量座標編號」；新增 `storeDitch` 必須完全省略 `XY_NUM` key；系統產生的名稱以既有 response `XY_NUM` 接收並回填。
- 檢視頁仍需顯示測量座標編號。
- 檢視頁進入編輯後仍需顯示測量座標編號，但不可修改，並以半透明遮罩表達鎖定。
- 編輯 `storeDitch` 對既有 `XY_NUM` 的行為保持不變。

### 匯入既有點位

- `bottom_sheet_import_existing_waypoint.xml` 的「附近點位」改為「最近存檔點位」。
- 移除查詢位置文字及右上角定位按鈕。
- 畫面進入前即呼叫無 query 的 `GET /v1/node/closestNodeDetails`；成功 response 的 `data` 是既有 `NodeDetails` 陣列，沿用既有空清單／錯誤列表狀態。

## Acceptance Criteria

- AC-001：非虛擬點在新增、草稿、檢視與編輯流程皆正確呈現銜接點與連接管；銜接點緊接無法開蓋、兩者互斥，連接管位於淤積程度下方且預設／必選「無」。虛擬點須關閉這兩個欄位與無法開蓋，並將三者重置為 false。
- AC-002：非虛擬點的新增與草稿送出 payload 為每個點位帶入 `is_connect_point` 與 `is_connect_pipe` 整數值；編輯與檢視以同名 Boolean response key 回填，缺值顯示為「無」。虛擬點 payload 必須省略 `IS_CANTOPEN`、`is_connect_point`、`is_connect_pipe`。
- AC-003：新增表單不顯示或驗證 `XY_NUM`，新增送出完全省略 `XY_NUM`；系統產生的 `XY_NUM` 回填並保留於檢視與編輯，編輯時不可變更；既有編輯送出維持原有 `XY_NUM` 行為。
- AC-004：匯入頁進入即無參數呼叫最近存檔點位 API，無定位權限／GPS／地圖點選依賴；文案、查詢位置提示與右上定位按鈕符合需求，既有搜尋與匯入流程不回歸。

## Constraints and Sources

- 需求來源：`/Users/a10362/Desktop/markdown file/ty_feat_0917.md`。
- 視覺來源：Figma `IfmNbZKhr4wojZ2bF5rYHG`，節點 `61:1950`；其子節點顯示銜接點與連接管的位置及選項。
- Figma 的「測量座標編號」仍呈現可編輯必填欄位，與本需求衝突；本任務以文字需求為優先。

## Open Questions

- 無。
