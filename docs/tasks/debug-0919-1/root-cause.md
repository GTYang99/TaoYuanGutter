# Root Cause Analysis

## Scope

Task: `debug-0919-1`
Branch: `fix/debug-0919-1-UI流程`

本分析只針對需求 AC-001～AC-004，尚未修改 production code。

## AC-001 — 銜接點未沿用無法開蓋規則

### Root cause

`GutterBasicInfoFragment.setupConnectPointAndPipe()` 的 listener 只處理 `cbConnectPoint` 與 `cbCantOpen` 的互斥，接著呼叫 `applyConnectionMutualExclusionUi()`；它沒有把銜接點視為 `validateRequiredFields()` 與 `validateAllPhotos()` 中的免填模式。

目前驗證邏輯只對 `IS_CANTOPEN` 分支免除細節欄位及第 2、3 張照片。銜接點即使被勾選，仍會落入一般可開蓋流程，要求 `MAT_TYP`、`COVER_DEP`、`NODE_DEP`、`NODE_WID`、`IS_BROKEN`、`IS_HANGING`、`IS_SILT` 及必要照片。

### Evidence

- `GutterBasicInfoFragment.kt`: `setupConnectPointAndPipe()` 只處理互斥與 draft notification。
- `GutterBasicInfoFragment.kt`: `validateRequiredFields()` 只以 `isCantOpen` 提前返回。
- `GutterBasicInfoFragment.kt`: `validateAllPhotos()` 只以 `isCantOpen` 決定第 2、3 張照片是否必填。
- `AddGutterBottomSheet.kt`: `validateWaypointPhotosAndFieldsOrAlert()` 的 required keys 與 photo keys 也只判斷 `IS_CANTOPEN`。

### Affected behavior

深度照片、頂寬度照片、溝蓋板厚度、深度、頂寬、材質、破損、懸掛、淤積、接管沒有依銜接點狀態被免除；送出與表單內驗證可能不一致。

## AC-002 — 空白新增項目顯示已填寫資料

### Root cause

`WaypointAdapter.onBindViewHolder()` 用 `hasFilledFormData` 對部分 `basicData` 欄位做「任一非空」判定。該判定不是送出前的完整上傳條件：它會把預填座標、預設狀態或單一不完整欄位當作已填寫，且不檢查必要照片是否通過 `PhotoUploadValidator.isUsableForUpload()`。

既有送出流程在 `AddGutterBottomSheet.validateWaypointPhotosAndFieldsOrAlert()` 會依虛擬點、無法開蓋及一般點位選出必要欄位與照片，並以 `PhotoUploadValidator.isUsableForUpload()` 判斷照片是否真的可上傳；列表標籤沒有重用這個完整條件，形成判定分歧。

### Evidence

- `WaypointAdapter.kt`: `hasFilledFormData` 使用 `any { !item.basicData[it].isNullOrBlank() }`。
- `WaypointAdapter.kt`: `NODE_X`、`NODE_Y` 及狀態欄位在列表完成判定中。
- `AddGutterBottomSheet.kt`: 送出前依必要欄位與照片條件完整驗證。
- `PhotoUploadValidator.kt`: 可上傳照片必須是可用的 http/https、可讀 content URI 或可讀 file URI。

### Affected behavior

新增點位只要有預填座標或部分資料，就可能顯示「已填寫資料」，即使送出前仍會因欄位或照片不完整而失敗。

## AC-003 — 匯入後定位仍有重新啟用風險

### Root cause

匯入後 `GutterFormActivity.handleImportedNodeDetails()` 會設定 `importedWaypointLocked`，並呼叫 Fragment 的 `setImportLocked(true)`；但 `GutterBasicInfoFragment.setEditable()` 沒有在 import lock 狀態下明確重設 `btnPickLocation.isEnabled`。初始化時則直接使用 `binding.btnPickLocation.isEnabled = !isViewMode`。

此外，定位 click listener 本身只檢查 `isViewMode`，沒有再檢查 `isImportLocked`。因此只靠畫面 enabled 狀態不足以保證匯入後不可定位。

### Evidence

- `GutterBasicInfoFragment.kt`: 初始化以 `!isViewMode` 設定 `btnPickLocation`。
- `GutterBasicInfoFragment.kt`: `setEditable()` 的可編輯欄位清單未包含 `btnPickLocation`。
- `GutterBasicInfoFragment.kt`: `setupLocationPickerButton()` click guard 只檢查 `isViewMode`。
- `GutterFormActivity.kt`: 匯入後透過 `setImportedWaypointLocked(true)` 傳遞鎖定狀態。

## AC-004 — 匯入後虛擬點可能被重新啟用

### Root cause

匯入流程會設定 `importedWaypointLocked` 並同步 API 回傳的虛擬點狀態，但 `GutterFormActivity.enterEditMode()` 在進入編輯模式時無條件執行 `binding.cbIsVirtual.isEnabled = true`。這會覆蓋匯入鎖定的產品規則。

`setupVirtualPointToggle()` 的初始 enabled 判定也只看 `isViewMode`，沒有合併 `importedWaypointLocked`；Activity 狀態恢復時同樣只保存／恢復 checked value，未形成單一的 enabled 狀態來源。

### Evidence

- `GutterFormActivity.kt`: `enterEditMode()` 無條件啟用 `cbIsVirtual`。
- `GutterFormActivity.kt`: `setupVirtualPointToggle()` 使用 `!isViewMode` 設定 enabled。
- `GutterFormActivity.kt`: `handleImportedNodeDetails()` 先鎖定再同步 API 的虛擬點狀態，後續 edit-mode 可覆蓋鎖定。

## Regression Risk

- 銜接點與無法開蓋的共同免填規則若只修 UI、不同步三個驗證入口，可能造成畫面可填但送出仍失敗。
- 重用完整完成判定時需區分虛擬點、無法開蓋、一般點位，避免把匯入既有伺服器照片誤判為無效。
- 定位與虛擬點鎖定需保留一般新增／一般編輯流程的可操作性。

## Root Cause Status

Root cause 已由靜態程式證據確認；尚缺 physical device reproduction 與修正後驗證證據。
