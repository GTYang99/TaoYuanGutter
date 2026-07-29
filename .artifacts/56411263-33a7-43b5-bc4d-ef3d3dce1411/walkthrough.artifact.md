# 手動控制模擬參數變更紀錄

已成功將模擬開關 `ENABLE_GROUP_SIMULATION` 從編譯模式解耦，現在可透過 `GutterApiService.kt` 進行手動控制。

## 變更內容

### 1. [GutterApiService.kt](file:///Users/a10362/AndroidStudioProjects/TaoYuanGutter/app/src/main/java/com/example/taoyuangutter/api/GutterApiService.kt)
將 `ENABLE_GROUP_SIMULATION` 改為手動布林值。
```diff
-    val ENABLE_GROUP_SIMULATION = BuildConfig.DEBUG
+    /**
+     * 是否開啟長按登出按鈕切換 Group ID 的模擬功能（開發測試用）。
+     * 手動控制開關：true = 開啟模擬功能 / false = 關閉（正式版建議關閉）。
+     */
+    val ENABLE_GROUP_SIMULATION = true
```

### 2. [AddGutterBottomSheet.kt](file:///Users/a10362/AndroidStudioProjects/TaoYuanGutter/app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt)
將「長按測試功能」的權限檢查改為引用自定義開關。
```diff
-                if (!BuildConfig.DEBUG) return@setOnLongClickListener false
+                if (!com.example.taoyuangutter.api.GutterApiClient.ENABLE_GROUP_SIMULATION) return@setOnLongClickListener false
```

## 驗證結果
- `MainActivity` 原本就已正確引用 `GutterApiClient.ENABLE_GROUP_SIMULATION`。
- 全案搜尋確認，除正式 Log 級別判定外，所有「模擬/測試」邏輯皆已歸口至該手動參數。
- 專案可正常編譯並依照手動設定值切換隱藏功能。
