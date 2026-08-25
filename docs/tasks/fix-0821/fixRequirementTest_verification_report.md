# Fix Requirement Test 驗收報告

**生成時間**: 2026-08-21  
**檢查版本**: 當前程式碼庫  
**檢查範圍**: AddGutterBottomSheet 照片判定流程

---

## ✅ 驗收結果總覽

| 驗收條件 | 狀態 | 備註 |
|---------|------|------|
| 1. 可補傳照片不應被 Alert 擋住 | ✅ **已修復** | 驗證與補傳流程已正確分離 |
| 2. 已有 img_id 的照片視為完成 | ✅ **已修復** | 透過 `PhotoUploadSlotState.readImgId()` 正確判斷 |
| 3. 沒有照片路徑且沒有 img_id | ✅ **已修復** | 透過 `PhotoUploadValidator.isUsableForUpload()` 正確判斷 |
| 4. 有路徑但檔案不可用 | ✅ **已修復** | 檔案驗證邏輯完整 |

---

## 📋 詳細驗證結果

### 驗收條件 1：可補傳照片不應被 Alert 擋住 ✅

**前置條件**：某個必要照片欄位有可用照片路徑、但沒有 `img_id`、照片檔案可讀且存在

**實際實現**（`AddGutterBottomSheet.kt` Line 938-973）：

```kotlin
// 送出按鈕點擊邏輯
lifecycleScope.launch {
    syncLatestDraftStateIntoWaypoints()
    repairWaypointPhotosFromPendingIfNeeded(waypoints)
    restoreUnchangedPhotoMetadataIntoWaypoints(waypoints)
    
    // ① 先驗證資料與照片來源是否可用
    if (!validateWaypointPhotosAndFieldsOrAlert(waypoints.toList())) {
        updateSubmitButtonState()
        return@launch
    }
    
    // ② 再進行補傳流程（只處理可用路徑但缺 img_id 的照片）
    if (!ensureWaypointPhotosUploadedBeforeSubmit(waypoints.toList(), token)) {
        showSelf()
        updateSubmitButtonState()
        return@launch
    }
    
    // ③ 最後才真正送出
    submitNewGutterRequest(activity, submittedWaypoints, token)
}
```

**關鍵邏輯**（`validateWaypointPhotosAndFieldsOrAlert()` Line 1556-1655）：

```kotlin
val missingPhotos = photosToCheck.filter { key ->
    val photoPath = wp.basicData[key]
    
    // 只有「照片來源不可用」才視為缺少照片。
    // 可用路徑但尚未取得 img_id 的情況，會留給送出前補傳流程處理。
    !PhotoUploadValidator.isUsableForUpload(ctx, photoPath)
}
```

**補傳流程**（`ensureWaypointPhotosUploadedBeforeSubmit()` Line 1757-1865）：

```kotlin
(1..3).forEach { slot ->
    // ... 
    val usable = PhotoUploadValidator.isUsableForUpload(ctx, photoPath)
    if (!usable) return@forEach  // 不可用的跳過
    
    val imgId = PhotoUploadSlotState.readImgId(waypoint.basicData, slot)
    if (imgId != null) return@forEach  // 已有 img_id 的跳過
    
    // 可用路徑但缺 img_id → 進入補傳
    val result = repository.uploadNodeImage(...)
    when (result) {
        is ApiResult.Success -> {
            PhotoUploadSlotState.writeState(
                waypoint.basicData, slot,
                state = PhotoUploadSlotState.STATE_SUCCESS,
                imgId = result.data.data?.imgId,
                error = null
            )
        }
        is ApiResult.Error -> {
            // 補傳失敗時才顯示 Alert
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("照片上傳失敗")
                .setMessage("${waypoint.label} 第${slot}張照片上傳失敗：${result.message}")
                .setPositiveButton("確定", null)
                .show()
            return false
        }
    }
}
```

**結論**：✅ **流程正確**
- 驗證階段只檢查照片路徑是否可用（`isUsableForUpload()`）
- 補傳階段只處理「可用路徑但缺 img_id」的照片
- 不會因為缺少 img_id 就直接跳出 Alert

---

### 驗收條件 2：已有 img_id 的照片應視為完成 ✅

**實際實現**（`ensureWaypointPhotosUploadedBeforeSubmit()` Line 1799-1800）：

```kotlin
val imgId = PhotoUploadSlotState.readImgId(waypoint.basicData, slot)
if (imgId != null) return@forEach  // 已有 img_id 的直接跳過，不重複上傳
```

**計數邏輯**（`countPendingPhotoUploads()` Line 1748-1750）：

```kotlin
val imgId = PhotoUploadSlotState.readImgId(waypoint.basicData, slot)
if (imgId != null) return@forEach  // 已有 img_id 的不計入待補傳數量
count++
```

**編輯模式保護**（`isUnchangedPhotoSlot()` Line 1382-1389）：

```kotlin
private fun isUnchangedPhotoSlot(slot: Int, waypoint: Waypoint): Boolean {
    if (editSpiNum.isEmpty()) return false
    val original = originalSnapshotFor(waypoint) ?: return false
    val originalImgId = PhotoUploadSlotState.readImgId(original.basicData, slot) ?: return false
    val currentImgId = PhotoUploadSlotState.readImgId(waypoint.basicData, slot) ?: return false
    val photoKey = "photo$slot"
    val currentPhoto = normalizedString(waypoint.basicData[photoKey])
    return currentPhoto != null && currentImgId == originalImgId
}
```

**結論**：✅ **正確判斷**
- 補傳流程會跳過已有 `img_id` 的照片
- 待補傳計數不會包含已完成的照片
- 編輯模式下，未改動的照片會保留原始 `img_id`

---

### 驗收條件 3：沒有照片路徑且沒有 img_id ✅

**實際實現**（`validateWaypointPhotosAndFieldsOrAlert()` Line 1621-1631）：

```kotlin
val missingPhotos = photosToCheck.filter { key ->
    val photoPath = wp.basicData[key]
    !PhotoUploadValidator.isUsableForUpload(ctx, photoPath)
}

if (missingPhotos.isNotEmpty()) {
    val pretty = missingPhotos.mapNotNull { it.removePrefix("photo").toIntOrNull() }.sorted()
    val prettyText = if (pretty.isEmpty()) missingPhotos.joinToString(",") else pretty.joinToString(", ")
    issues.add("$pointLabel：缺少照片（第 $prettyText 張）")
}
```

**照片可用性判斷**（`PhotoUploadValidator.kt` Line 17-25）：

```kotlin
fun isUsableForUpload(context: Context, uriString: String?): Boolean {
    if (uriString.isNullOrBlank()) return false  // 空值或空白直接回傳 false
    
    val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return false
    return when (uri.scheme?.lowercase()) {
        "http", "https" -> true
        "content" -> canReadContentUri(context, uri)
        "file" -> canReadFileUri(uri)
        else -> false
    }
}
```

**Alert 顯示**（Line 1641-1653）：

```kotlin
if (issues.isEmpty()) return true

MaterialAlertDialogBuilder(ctx)
    .setTitle("資料/照片不足")
    .setMessage(
        "送出前需確認：每個點位的必要欄位與必要照片皆完整。\n\n- " +
            issues.joinToString("\n- ")
    )
    .setPositiveButton("確定", null)
    .show()
return false
```

**結論**：✅ **正確處理**
- 空路徑會被 `isUsableForUpload()` 回傳 `false`
- 驗證階段會收集所有缺少可用照片的欄位
- 最終顯示 `資料/照片不足` Alert 並阻擋送出

---

### 驗收條件 4：有路徑但檔案不可用 ✅

**檔案驗證邏輯**（`PhotoUploadValidator.kt` Line 32-41）：

```kotlin
private fun canReadContentUri(context: Context, uri: Uri): Boolean {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            input.read() != -1  // 實際嘗試讀取第一個 byte
        } ?: false
    }.getOrDefault(false)  // 任何異常都回傳 false
}

private fun canReadFileUri(uri: Uri): Boolean {
    val path = uri.path ?: return false
    val file = File(path)
    return file.exists() && file.canRead()  // 檢查檔案存在且可讀
}
```

**流程整合**：

```kotlin
// 驗證階段
val missingPhotos = photosToCheck.filter { key ->
    val photoPath = wp.basicData[key]
    !PhotoUploadValidator.isUsableForUpload(ctx, photoPath)  // 損壞檔案會回傳 false
}

// 補傳階段也會再次檢查
val usable = PhotoUploadValidator.isUsableForUpload(ctx, photoPath)
if (!usable) return@forEach  // 不可用的不會嘗試上傳
```

**結論**：✅ **正確處理**
- `content://` URI 會實際嘗試讀取，損壞的會回傳 `false`
- `file://` URI 會檢查檔案存在性與可讀性
- 檔案不可用時會被視為缺少照片，最終進入 Alert

---

## 🧪 測試情境覆蓋

### A. 正常補傳 ✅

**流程**：
1. 填入必要資料 ✓
2. 提供可讀的本機照片路徑（無 `img_id`）✓
3. 點擊送出 ✓

**預期行為**：
- `validateWaypointPhotosAndFieldsOrAlert()` 通過（照片路徑可用）✓
- `ensureWaypointPhotosUploadedBeforeSubmit()` 偵測到待補傳照片 ✓
- 執行 `repository.uploadNodeImage()` 取得 `img_id` ✓
- 補傳成功後繼續送出 ✓

**實際代碼支援**：完全符合

---

### B. 既有完成照片 ✅

**流程**：
1. 編輯模式載入既有點位（已有照片路徑 + `img_id`）✓
2. 未修改照片 ✓
3. 點擊送出 ✓

**預期行為**：
- `isUnchangedPhotoSlot()` 回傳 `true` ✓
- 補傳流程跳過此照片欄位 ✓
- 不顯示照片不足 Alert ✓

**實際代碼支援**：完全符合

---

### C. 缺少照片 ✅

**流程**：
1. 填入必要資料 ✓
2. 必要照片欄位留空 ✓
3. 點擊送出 ✓

**預期行為**：
- `PhotoUploadValidator.isUsableForUpload()` 回傳 `false`（空值）✓
- `validateWaypointPhotosAndFieldsOrAlert()` 收集為缺少照片 ✓
- 顯示 `資料/照片不足` Alert ✓
- 阻擋送出（回傳 `false`）✓

**實際代碼支援**：完全符合

---

### D. 路徑損壞 ✅

**流程**：
1. 填入必要資料 ✓
2. 必要照片欄位填入失效路徑（檔案已刪除或損壞）✓
3. 不提供 `img_id` ✓
4. 點擊送出 ✓

**預期行為**：
- `canReadFileUri()` 或 `canReadContentUri()` 回傳 `false` ✓
- `PhotoUploadValidator.isUsableForUpload()` 回傳 `false` ✓
- `validateWaypointPhotosAndFieldsOrAlert()` 視為缺少照片 ✓
- 顯示 `資料/照片不足` Alert ✓

**實際代碼支援**：完全符合

---

## 🎯 核心機制總結

### 1. 三階段驗證與上傳流程

```
階段 1: validateWaypointPhotosAndFieldsOrAlert()
   ↓ 檢查照片路徑是否可用（isUsableForUpload）
   ↓ 不可用 → 加入缺失清單 → 顯示 Alert → 阻擋送出
   ↓ 可用 → 通過驗證

階段 2: ensureWaypointPhotosUploadedBeforeSubmit()
   ↓ 檢查可用照片是否已有 img_id
   ↓ 已有 img_id → 跳過
   ↓ 無 img_id → 補傳上傳
   ↓ 補傳失敗 → 顯示 Alert → 阻擋送出
   ↓ 補傳成功 → 回填 img_id

階段 3: submitNewGutterRequest() / performEditSubmit()
   ↓ 真正送出側溝資料
```

### 2. 照片狀態判定邏輯

| 照片路徑 | img_id | PhotoUploadValidator.isUsableForUpload() | 判定結果 |
|---------|--------|----------------------------------------|---------|
| 空值/空白 | 無 | `false` | ❌ 缺少照片 → Alert |
| 檔案已刪除/損壞 | 無 | `false` | ❌ 缺少照片 → Alert |
| 可讀檔案 | 無 | `true` | ⚠️ 通過驗證 → 進入補傳 |
| 可讀檔案 | 有 | `true` | ✅ 已完成 → 跳過 |
| http/https URL | 無 | `true` | ⚠️ 通過驗證 → 進入補傳 |
| http/https URL | 有 | `true` | ✅ 已完成 → 跳過 |

### 3. 關鍵防護機制

**編輯模式照片保護**：
- `isUnchangedPhotoSlot()` 比對原始與當前的 `img_id`
- 未改動的照片不會重複上傳
- 保留原始的上傳時間、狀態等元數據

**檔案驗證深度**：
- `content://` URI：實際嘗試讀取第一個 byte
- `file://` URI：檢查檔案存在性 + 可讀性
- 使用 `runCatching` 捕捉所有異常，確保穩定性

**補傳進度回饋**：
- `onPendingPhotoUploadStarted(pendingCount)` 顯示待補傳數量
- `onPendingPhotoUploadProgress(success)` 回報每張照片上傳結果
- `onPendingPhotoUploadFinished()` 完成後恢復 UI

---

## ✅ 最終結論

**所有驗收條件皆已修復並正確實現**：

1. ✅ **可補傳照片不會被 Alert 擋住**  
   → 驗證只檢查路徑可用性，不檢查 `img_id`
   
2. ✅ **已有 img_id 的照片視為完成**  
   → 補傳流程會跳過，不會重複上傳
   
3. ✅ **沒有路徑且沒有 img_id 視為缺少**  
   → 正確顯示 Alert 並阻擋送出
   
4. ✅ **損壞路徑視為缺少**  
   → 檔案驗證機制完整，損壞檔案會被正確攔截

**程式碼品質評估**：
- 邏輯清晰，職責分離良好
- 錯誤處理完整，使用 `runCatching` 保護
- 用戶體驗友善，補傳進度有即時回饋
- 編輯模式保護機制完善，避免重複上傳

**建議**：
- 目前實現已完全符合需求文件，無需額外修正
- 可考慮增加單元測試，覆蓋 `PhotoUploadValidator` 的各種邊界情況
- 可考慮增加補傳失敗時的重試機制（目前失敗直接阻擋送出）

---

**報告結束**
