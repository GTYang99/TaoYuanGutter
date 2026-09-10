# Implementation Plan

## Goal
- 依 UI 規格重整側溝基本資料表單、設定新建預設選項，並保留所有既有資料與照片流程。

## Scope
- 僅限基本資料表單的呈現順序、狀態群組、拍照按鈕文字、新建表單的兩個 RadioGroup 預設值與相應測試。

## Affected Files
- `app/src/main/res/layout/fragment_gutter_basic_info.xml`：將完整欄位區塊依規格重排，建立「測量狀態」列，移入兩個 Checkbox，更新三個按鈕文字。
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt`：區分新建初始化與既有資料預填，在不覆寫持久化資料的前提下設定破損／淤積預設值。
- `app/src/androidTest/java/com/example/taoyuangutter/GutterBasicInfoUiTest.kt`（若現有測試不適合擴充）：驗證 UI 規格、初始值與回填保留行為。

## Implementation Steps
1. 盤點 `fragment_gutter_basic_info.xml` 的現有頂層欄位區塊、ID、visibility 與 slot 對應，建立維持 ID／資料綁定不變的重排清單。
2. 在 XML 建立「測量狀態」標題與容器，將 `btnPendingDeploy` 和 `cbCantOpen` 移入該容器；依 AC-002 重排每個標題與完整控制項／照片區塊，不變更控制項 ID。
3. 更新 slot 1、3、2 按鈕文字為概況、深度、寬度；保留 slot 1=概況、slot 2=寬度、slot 3=深度的呼叫、image ID 與上傳 category 對應。
4. 在 `GutterBasicInfoFragment` 的新建且無既有資料初始化路徑，設定 `rgIsBroken` 為「否」與 `rgIsSilt` 為「無」；讓 `prefillData()`、草稿回填、匯入與 Android state restoration 繼續以已保存值覆寫或保留選取。
5. 檢查 `collectData()`、`validate()`、`applyCantOpenUi()`、`setVirtualMode()`、`setImportLocked()` 與 `setEditable()` 對移動後 View 的行為，修正僅因 UI 容器移動造成的 visibility／enabled 問題。
6. 新增或擴充 Android UI 測試：驗證狀態群組、三個按鈕文字、表單順序、純新建預設值，以及已有資料／匯入／草稿資料不被覆寫；保留無法開蓋回歸測試。

## Test Plan
- 執行基本資料與無法開蓋的 Android UI tests，涵蓋新增、編輯、匯入、草稿與 view／virtual mode。
- 新增表單測試：破損為「否」、淤積為「無」；已有資料／匯入／草稿測試：原值維持。
- 以可滾動表單的畫面座標或可測試的 view hierarchy 驗證需求列出的完整相對順序，並確認每個標題和控制項相鄰。
- 執行相關 unit tests、debug build 與靜態檢查；確認 `collectData()` 的 keys、照片 slot 和 upload metadata 未改變。

## Regression Plan
- 三張照片的拍攝、替換、刪除、草稿回填與上傳仍使用原本 slot／image ID／category。
- 無法開蓋確認清除、待架站、必填驗證、檢視、匯入鎖定與虛擬點維持目前行為。
- configuration recreation 不應以新建預設覆蓋系統還原或草稿資料。

## Risks
- XML 重排可能影響 virtual mode 隱藏容器或標題／控制項分離。
- 設定預設值的時間點錯誤可能覆蓋編輯、匯入或草稿值。
- 將深度照片顯示在寬度照片之前時，可能誤換 slot 2／3 的資料關聯。

## Rollback Plan
- 回退本任務單一 commit 可恢復既有表單順序與初始選取行為，且不影響 API 或已保存資料格式。

## Current Behavior
- 狀態 Checkbox 分散在測量座標編號與溝蓋板厚度附近；欄位／照片順序及按鈕文字尚未符合規格，且新建表單未設定兩個指定預設值。

## Expected Behavior
- 使用者在新增或編輯頁面依規格順序填寫，首先看到包含待架站與無法開蓋的測量狀態；新建表單預選「否」與「無」，已保存資料不變。

## Acceptance Criteria Traceability
| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | 2 | UI test 確認「測量狀態」及兩個 Checkbox 顯示 |
| AC-002 | 1、2 | UI hierarchy／scroll test 確認完整相對順序與標題—控制項配對 |
| AC-003 | 4 | 新建表單 UI test 確認 RadioGroup 選取值 |
| AC-004 | 4、6 | 編輯、匯入、草稿回填 UI test 確認既有值不變 |
| AC-005 | 3 | UI test 確認三個按鈕精確文字 |
| AC-006 | 3、5、6 | targeted regression tests、`collectData()`／upload mapping review、debug build |

## Failure Behavior
- 資料缺失時仍沿用既有驗證與空值處理；不得因 UI 重排或預設值使照片、匯入或草稿資料遺失。

## Security and Privacy
- 無新增資料、權限、憑證或外部傳輸；照片沿用既有 URI 與上傳流程。

## Open Questions
- 無。若產品要求將既有「側溝形式」的顯示文字改為「截面形式」，需以明確 UI 文案決策另行確認。
