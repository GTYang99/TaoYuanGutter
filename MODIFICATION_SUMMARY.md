# 編輯/新增/檢視模式下的地圖線段顯示邏輯修改

## 📋 修改概述

當進入編輯（Edit）、新增（Add）或檢視（Inspect）側溝線段模式時，地圖將**隱藏所有其他已存在的線段**，只顯示正在操作的線段。退出任何模式時，地圖會自動恢復顯示所有線段。

---

## 🔧 修改位置

### 1. **編輯已有側溝時** (第 296-322 行)

**場景**：點擊地圖上的某條側溝線段，進入編輯界面

**修改內容**：
```kotlin
// ── 進入編輯模式時：隱藏所有其他已存在的線段，只顯示正在編輯的側溝 ──
scopePolylines.values.forEach { it.remove() }
scopePolylines.clear()
submittedPolylines.forEach { it.remove() }
submittedPolylines.clear()
```

**行為**：
- 移除所有 `scopePolylines`（後端加載的線段）
- 移除所有 `submittedPolylines`（已提交的線段）
- 只顯示 `workingPolyline`（正在編輯的線段）

**退出時**（第 305-312 行）：
```kotlin
sheet.onWaypointsChanged = { updated ->
    if (updated == null) {
        // 編輯 Sheet 被 dismiss（關閉）時，清除工作層並恢復其他線段顯示
        clearWorkingMarkers()
        activeSheet = null
        // 重新加載所有線段（scopePolylines 與 submittedPolylines）
        loadGuttersByViewport()
    } else {
        // ... 更新編輯中的線段
    }
}
```

---

### 2. **新增側溝時** (第 971-1000 行)

**場景**：點擊「新增側溝」按鈕

**修改內容**：
```kotlin
// ── 進入新增模式時：隱藏所有已存在的線段，只顯示新增中的側溝 ──
scopePolylines.values.forEach { it.remove() }
scopePolylines.clear()
submittedPolylines.forEach { it.remove() }
submittedPolylines.clear()
```

**退出時**：
```kotlin
sheet.onWaypointsChanged = { wps ->
    if (wps != null) {
        // ... 新增中的線段更新
    } else {
        // ── 取消新增模式時，清除工作層並恢復其他線段顯示 ──
        googleMap?.setPadding(0, 0, 0, 0)
        activeSheet = null
        // 重新加載所有線段（scopePolylines 與 submittedPolylines）
        loadGuttersByViewport()
    }
}
```

---

### 3. **從待上傳草稿恢復編輯時** (第 1038-1073 行)

**場景**：從「待上傳草稿」列表恢復編輯

**修改內容**：
```kotlin
// ── 進入草稿編輯模式時：隱藏所有已存在的線段，只顯示編輯中的側溝 ──
scopePolylines.values.forEach { it.remove() }
scopePolylines.clear()
submittedPolylines.forEach { it.remove() }
submittedPolylines.clear()
```

**退出時**：
```kotlin
sheet.onWaypointsChanged = { wps ->
    if (wps != null) {
        // ... 編輯中的線段更新
    } else {
        // ── 取消草稿編輯模式時，清除工作層並恢復其他線段顯示 ──
        googleMap?.setPadding(0, 0, 0, 0)
        activeSheet = null
        // 重新加載所有線段（scopePolylines 與 submittedPolylines）
        if (!isOfflineMainMode) loadGuttersByViewport()
    }
}
```

---

### 4. **系統重建後恢復狀態時** (第 358-377 行)

**場景**：拍照流程期間系統記憶體不足，MainActivity 被殺掉後恢復

**修改內容**：在新增模式的 onWaypointsChanged 回調中：
```kotlin
sheet.onWaypointsChanged = { wps ->
    if (wps != null) {
        // ... 新增中的線段更新
    } else {
        // ── 取消新增模式時，清除工作層並恢復其他線段顯示 ──
        googleMap?.setPadding(0, 0, 0, 0)
        activeSheet = null
        // 重新加載所有線段（scopePolylines 與 submittedPolylines）
        loadGuttersByViewport()
    }
}
```

---

### 5. **編輯完成時** (第 500-503 行)

**場景**：編輯完成，點擊「確定」保存

**修改內容**：在 `onUpdateGutter()` 函數末尾添加：
```kotlin
override fun onUpdateGutter(waypoints: List<Waypoint>, spiNum: String) {
    // ... 清除暫存資料的代碼

    // ── 退出編輯模式時：重新加載所有線段 ──
    loadGuttersByViewport()
}
```

---

### 6. **進入檢視模式時** (第 880-895 行)

**場景**：點擊地圖上的側溝線段，進入檢視詳細資料

**修改內容**：在 `openInspectBottomSheet()` 函數中，呼叫 API 後、啟動 GutterInspectActivity 前：
```kotlin
lifecycleScope.launch {
    when (val result = gutterRepository.getDitchDetails(spiNum, token)) {
        is ApiResult.Success -> {
            val ditch = result.data.data
            if (ditch != null) {
                // ── 進入檢視模式時：隱藏所有其他已存在的線段，只顯示正在檢視的側溝 ──
                scopePolylines.values.forEach { it.remove() }
                scopePolylines.clear()
                submittedPolylines.forEach { it.remove() }
                submittedPolylines.clear()

                val intent = GutterInspectActivity.newIntent(...)
                activeSheet?.hideSelf()
                inspectLauncher.launch(intent)
            }
        }
    }
}
```

**退出時**（第 327-330 行）：
```kotlin
inspectLauncher = registerForActivityResult(...) { result ->
    if (result.resultCode == GutterInspectActivity.RESULT_EDIT_DITCH) {
        // ... 進入編輯模式
    } else {
        // ── 從檢視模式返回（不編輯）時，清除工作層並恢復其他線段顯示 ──
        clearWorkingMarkers()
        loadGuttersByViewport()
    }
}
```

---

### 7. **系統重建後檢視模式** (第 378-385 行)

**場景**：系統重建後恢復檢視模式狀態

**修改內容**：在 `restoreStateAfterRecreation()` 函數中：
```kotlin
} else {
    // 檢視模式：重新綁定 inspectSheet
    inspectSheet = restoredSheet
    restoredSheet.onWaypointsChanged = { if (it == null) {
        // ── 檢視 Sheet 被 dismiss（關閉）時，清除工作層並恢復其他線段顯示 ──
        clearWorkingMarkers()
        inspectSheet = null
        // 重新加載所有線段（scopePolylines 與 submittedPolylines）
        loadGuttersByViewport()
    } }
}
```

---

## 📊 線段顯示邏輯

| 狀態 | scopePolylines | submittedPolylines | workingPolyline |
|------|:---:|:---:|:---:|
| 瀏覽模式 | ✅ 顯示 | ✅ 顯示 | ❌ 隱藏 |
| 編輯模式（進入） | ❌ 隱藏 | ❌ 隱藏 | ✅ 顯示 |
| 編輯模式（離開） | ✅ 恢復 | ✅ 恢復 | ❌ 隱藏 |
| 新增模式（進入） | ❌ 隱藏 | ❌ 隱藏 | ✅ 顯示 |
| 新增模式（離開） | ✅ 恢復 | ✅ 恢復 | ❌ 隱藏 |
| 檢視模式（進入） | ❌ 隱藏 | ❌ 隱藏 | ❌ 隱藏 |
| 檢視模式（離開） | ✅ 恢復 | ✅ 恢復 | ❌ 隱藏 |

---

## 🎯 使用者體驗改進

✨ **優點**：
1. **減少地圖干擾** - 編輯時只顯示當前線段，其他線段不會造成視覺混亂
2. **專注於編輯** - 用戶可以專注於正在編輯的線段
3. **清晰的視覺反饋** - 進入和退出編輯模式時有清晰的視覺區隔
4. **流暢的恢復** - 離開編輯模式時自動恢復之前的狀態

---

## ⚠️ 注意事項

1. **離線模式** - 在離線模式下（`isOfflineMainMode = true`），不會呼叫 `loadGuttersByViewport()`
2. **API 調用** - 恢復顯示時會重新呼叫 `loadGuttersByViewport()` 以重新加載所有線段
3. **效能** - 移除和重新添加線段可能在地圖中有大量線段時造成短暫的性能影響

---

## 🧪 建議測試場景

1. ✅ **編輯已有側溝** → 查看只顯示該側溝 → 點擊「確定」→ 驗證所有線段恢復
2. ✅ **新增側溝** → 查看只顯示新增線段 → 點擊「取消」→ 驗證所有線段恢復
3. ✅ **檢視側溝** → 點擊地圖上的線段 → 查看只顯示該側溝 → 點擊返回 → 驗證所有線段恢復
4. ✅ **檢視模式編輯** → 進入檢視 → 點擊編輯 → 進入編輯模式 → 點擊「確定」→ 驗證所有線段恢復
5. ✅ **從待上傳草稿恢復** → 查看只顯示該側溝 → 點擊「取消」→ 驗證所有線段恢復
6. ✅ **編輯/檢視過程中縮放地圖** → 驗證線段顯示正常
7. ✅ **在編輯/檢視模式中旋轉螢幕** → 驗證狀態恢復正常（系統重建）
8. ✅ **多條線段存在時** → 編輯其中一條 → 驗證其他線段確實隱藏
