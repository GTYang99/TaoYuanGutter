# Repository Analysis

## Task Classification
- `feature`：調整使用者可見的表單資訊架構，並新增新建表單預設選取行為；需要完整 Plan Review。

## Current Behavior
- `fragment_gutter_basic_info.xml` 將「待架站」放在測量座標編號標題列，而「無法開蓋」放在溝蓋板厚度標題列；沒有「測量狀態」群組。
- 基本資料表單目前的欄位與照片區塊順序不符合需求。三個拍照按鈕皆為「拍攝照片」。
- `prefillData()`、`prefillDataFromImport()` 與草稿回填以既有資料設定破損／淤積 RadioGroup；空值會清除選取，因此新建表單目前沒有指定預設值。

## Expected Behavior
- 基本資料頁面以需求指定的完整順序呈現標題和控制項，並將兩個狀態 Checkbox 集中在新「測量狀態」欄位。
- 只有沒有既有資料的新建表單，預設選取破損「否」及淤積「無」；既有、匯入與草稿資料仍以其持久化值為準。
- 照片按鈕更具體地說明其概況、深度與寬度用途，但不改變其既有 slot 1、3、2 的資料／上傳對應。

## Affected Modules
- `app/src/main/res/layout/fragment_gutter_basic_info.xml`：重組基本資料頁的 XML 區塊、狀態群組及三個按鈕文字。
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt`：僅在新建、無既有資料的初始化路徑設定兩個預設 RadioButton，並確保預填、草稿與匯入路徑優先保留資料。
- `app/src/androidTest/java/com/example/taoyuangutter/`：擴充或新增基本資料 UI 測試，驗證文字、順序、預設值與保存資料不被覆寫。

## Dependencies
- ViewBinding ID 與 `GutterBasicInfoFragment` 的初始化、`collectData()`、驗證及可編輯性控制。
- `GutterFormActivity` 的新建、編輯、草稿與匯入 intent／fragment args。
- 既有 `GutterCantOpenUiTest` 和 Android Espresso／UI Automator 測試基礎設施。

## Risks
- 僅移動 XML 時可能把標題與控制項拆開，或漏掉 virtual／import／view mode 的既有 visibility 與 enabled 設定。
- slot 2 為寬度、slot 3 為深度；畫面順序必須改變，但不可交換資料 slot、camera request、upload category 或 image ID 對應。
- 若以無條件初始化預設值，會覆寫編輯、匯入、草稿或 configuration recreation 的原有資料。

## Unknown Assumptions
- 「截面形式」為既有側溝形式控制項的排序名稱，未明確要求變更其 UI 字串；維持既有字串直到產品提供文字規格。
- 「新增」指沒有既有／草稿／匯入資料的初始建立流程；其餘開啟路徑應被視為已有資料。

## Potential Issues
- `implementation_regression`：表單重排造成照片、驗證、可編輯性或特殊模式失效。
- `requirement_gap`：若「截面形式」是否需重新命名與既有「側溝形式」不同，需回到規格確認。
