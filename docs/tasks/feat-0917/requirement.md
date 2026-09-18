# Requirement

## Goal

- 新增每個側溝點位的「銜接點」與「連結管」屬性，採用更新後的 API 契約與 UI 行為；同時維持本任務既有的系統自帶節點名及最近存檔點位匯入範圍。

## Functional Requirements

### 銜接點與連結管

- 非虛擬點於新增、草稿、檢視與編輯必須保存並呈現銜接點與連結管。
- 銜接點位於「測量狀態」中，緊接「無法開蓋」後方。兩者可同時未選；選取其中一項時，另一項立即改為 `false`、灰化且不可選取。
- 連結管位於「淤積程度」後、補充說明前，是必填的「無／有」單選，預設「無」。所有 UI 文案使用「連結管」。
- 非虛擬點呼叫 `storeDitch` 時，每個點位都必須固定送出 Boolean `IS_TIEINPOINT` 與 `IS_CONNECTING`，即使值為 `false` 也不可省略。
- `nodeDetails` 的 `IS_TIEINPOINT` 與 `IS_CONNECTING` 回傳型別為 String，值為 `"0"` 或 `"1"`；任一欄位缺失時視為 `false`。
- 檢視頁的點位下拉選項，`IS_TIEINPOINT="1"` 時在點位名稱後加上 `(銜接點)`；若同時為待架站，待架站標記必須最後呈現，例如 `起點（E001）(銜接點)(待架站)`。
- 若 `nodeDetails` 異常同時回傳無法開蓋與銜接點為 `"1"`，以無法開蓋優先，銜接點以 `false` 呈現及回存。

### 虛擬點與草稿

- 由一般點切換為虛擬點時，既有 Alert 告知使用者後，必須關閉並取消勾選無法開蓋、銜接點與連結管，三者均重置為 `false`；虛擬點切回一般點時維持 `false`，不還原舊值。
- 虛擬點的 `storeDitch` payload 必須省略 `IS_CANTOPEN`、`IS_TIEINPOINT`、`IS_CONNECTING`；檢視頁不顯示連結管。
- 草稿與 API 使用相同規則：舊草稿缺少新欄位時視為 `false`；非虛擬點草稿保存兩個 Boolean；虛擬點不保存這兩欄。

### 系統自帶節點名

- 新增表單不顯示、也不要求使用者輸入「測量座標編號」；新增 `storeDitch` 必須完全省略 `XY_NUM` key；系統產生的名稱以既有 response `XY_NUM` 接收並回填。
- 檢視頁仍需顯示測量座標編號。檢視頁進入編輯後仍需顯示，但不可修改，並以半透明遮罩表達鎖定。
- 編輯 `storeDitch` 對既有 `XY_NUM` 的行為保持不變。

### 匯入既有點位

- `bottom_sheet_import_existing_waypoint.xml` 的「附近點位」改為「最近存檔點位」。
- 移除查詢位置文字及右上角定位按鈕。
- 畫面進入前即呼叫無 query 的 `GET /v1/node/closestNodeDetails`；成功 response 的 `data` 是既有 `NodeDetails` 陣列，沿用既有空清單／錯誤列表狀態。

## Acceptance Criteria

- AC-001：非虛擬點在新增、草稿、檢視與編輯皆正確呈現銜接點與連結管。銜接點緊接無法開蓋；任一選取時另一項為 false、灰化且不可選；連結管位於淤積程度後且預設／必選「無」。
- AC-002：非虛擬點新增、草稿與編輯的 `storeDitch` payload 固定包含 Boolean `IS_TIEINPOINT`、`IS_CONNECTING`。`nodeDetails` 以 String `"0"`／`"1"` 回填，缺值為 false；雙 `"1"` 以無法開蓋優先。虛擬點 payload 省略 `IS_CANTOPEN`、`IS_TIEINPOINT`、`IS_CONNECTING`，檢視不顯示連結管。
- AC-003：切換虛擬點後三個屬性清為 false，既有 Alert 後不還原；草稿以相同保存與缺值規則重建。下拉選項依序在名稱後顯示 `(銜接點)`，待架站存在時最後顯示 `(待架站)`。
- AC-004：新增表單不顯示或驗證 `XY_NUM`，新增送出完全省略 `XY_NUM`；系統產生的 `XY_NUM` 回填並保留於檢視與編輯，編輯時不可變更；既有編輯送出維持原有 `XY_NUM` 行為。
- AC-005：匯入頁進入即無參數呼叫最近存檔點位 API，無定位權限／GPS／地圖點選依賴；文案、查詢位置提示與右上定位按鈕符合需求，既有搜尋與匯入流程不回歸。

## Constraints and Sources

- 需求來源：[ty_feat_0917.md](/Users/a10362/Desktop/markdown%20file/ty_feat_0917.md) 與使用者於本任務的明確決議。
- 視覺來源：Figma `EaWah6suPwkJ4Jqm4vuSZo`，區段 `1691:12479`、`1701:16618`；表單元件 `2374:21501`，檢視元件 `2374:21505`。
- Figma 未繪製檢視下拉的 `(銜接點)` 標記；此項以文字需求與使用者決議為準。Figma 也不得覆蓋本需求的 API 契約。

## Open Questions

- 無。
