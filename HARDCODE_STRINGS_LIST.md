# TaoYuanGutter 硬編碼文字清單

## 📋 概況
- **總計**: 88 個 XML 硬編碼 + 35+ 個 Kotlin 硬編碼 = **120+ 個用戶可見文字**
- **問題**: 這些硬編碼文字應該使用字符串資源 (strings.xml)，便於維護和國際化

---

## 第一部分：XML 布局文件中的硬編碼文本（88 個）

### 📄 activity_gutter_form.xml
| 行號 | 文字 | 類型 | 建議 |
|------|------|------|------|
| 76 | 起點 | 標題 | 應移至 strings.xml |
| 100 | 完成 | 按鈕 | 已移至 strings.xml (form_finish_button) |
| 129 | 匯入既有點位資料 | 標籤 | 應移至 strings.xml |
| 137 | 選擇 | 按鈕 | 應移至 strings.xml |
| 238 | 正在上傳照片… | 提示 | 應移至 strings.xml |

---

### 📄 activity_gutter_inspect.xml
| 行號 | 文字 | 類型 | 建議 |
|------|------|------|------|
| 47 | 側溝檢視 | 標題 | 應移至 strings.xml |

---

### 📄 activity_import_existing_waypoint.xml
| 行號 | 文字 | 類型 | 建議 |
|------|------|------|------|
| 43 | 選擇點位 | 標題 | 應移至 strings.xml |
| 107 | 暫無點位資料 | 提示 | 應移至 strings.xml |
| 120 | 匯入 | 按鈕 | 已在 strings.xml (insert_waypoint) |
| 157 | 正在載入點位資料… | 提示 | 應移至 strings.xml |

---

### 📄 activity_landscape_camera.xml
| 行號 | 文字 | 類型 | 建議 |
|------|------|------|------|
| 96 | 請轉為橫向拍照 | 提示 | 應移至 strings.xml |
| 105 | 請將手機旋轉至橫向後即可拍照 | 提示 | 應移至 strings.xml |

---

### 📄 activity_login.xml
| 行號 | 文字 | 類型 | 建議 |
|------|------|------|------|
| 161 | 離線填寫表單 | 連結文字 | 應移至 strings.xml |

---

### 📄 activity_main.xml
| 行號 | 文字 | 類型 | 建議 |
|------|------|------|------|
| 39 | 登出 | 按鈕 | 應移至 strings.xml |

---

### 📄 bottom_sheet_add_gutter.xml
| 行號 | 文字 | 類型 | 建議 |
|------|------|------|------|
| 51 | 取消 | 按鈕 | ✅ 已在 strings.xml (cancel) |
| 67 | 新增側溝 | 按鈕 | ✅ 已在 strings.xml (btn_add_gutter) |
| 128 | 刪除側溝 | 按鈕 | 應移至 strings.xml |
| 175 | 載入點位資料中... | 提示 | 應移至 strings.xml |

---

### 📄 bottom_sheet_pending_drafts.xml
| 行號 | 文字 | 類型 | 建議 |
|------|------|------|------|
| 41 | 待上傳草稿 | 標題 | ✅ 已在 strings.xml (unupload_data) |
| 61 | 目前沒有離線草稿 | 提示 | 應移至 strings.xml |

---

### 📄 fragment_gutter_basic_info.xml
| 行號 | 文字 | 類型 | 說明 |
|------|------|------|------|
| 24 | 側溝編號 | 欄位標籤 | ✅ 已在 strings.xml (field_gutter_id) |
| 55 | 側溝形式 | 欄位標籤 | ✅ 已在 strings.xml (field_gutter_type) |
| 76 | U形溝（明溝） | 單選選項 | ✅ 已在 strings.xml (gutter_type_u_open) |
| 87 | U形溝（加蓋） | 單選選項 | ✅ 已在 strings.xml (gutter_type_u_covered) |
| 98 | L形溝與暗溝渠併用 | 單選選項 | ✅ 已在 strings.xml (gutter_type_l_mixed) |
| 109 | 其他 | 單選選項 | ✅ 已在 strings.xml (gutter_type_other) |
| 122 | 側溝X（E）座標 | 欄位標籤 | ✅ 已在 strings.xml (field_coord_x) |
| 152 | 側溝Y（N）座標 | 欄位標籤 | ✅ 已在 strings.xml (field_coord_y) |
| 183 | 側溝Z座標 | 欄位標籤 | ✅ 已在 strings.xml (field_coord_z) |
| 250 | 側溝測量深度（公分） | 欄位標籤 | 應改為 field_depth |
| 259 | 無法開蓋 | 複選框標籤 | 應移至 strings.xml |
| 290 | 側溝頂寬度（公分） | 欄位標籤 | 應改為 field_top_width |
| 323 | 側溝材質 | 欄位標籤 | 應移至 strings.xml |
| 344 | 混凝土 | 單選選項 | 應移至 strings.xml |
| 356 | 卵礫石 | 單選選項 | 應移至 strings.xml |
| 368 | 紅磚 | 單選選項 | 應移至 strings.xml |
| 381 | 溝體結構受損 | 欄位標籤 | 應移至 strings.xml |
| 402 | 否 | 單選選項 | 應移至 strings.xml |
| 414 | 是 | 單選選項 | 應移至 strings.xml |
| 427 | 附掛或過路管線 | 欄位標籤 | 應移至 strings.xml |
| 448 | 無 | 單選選項 | 應移至 strings.xml（多個） |
| 460 | 有 | 單選選項 | 應移至 strings.xml |
| 473 | 淤積程度 | 欄位標籤 | 應移至 strings.xml |
| 494 | 無 | 單選選項 | 重複 |
| 506 | 輕度 | 單選選項 | 應移至 strings.xml |
| 518 | 中度 | 單選選項 | 應移至 strings.xml |
| 530 | 嚴重 | 單選選項 | 應移至 strings.xml |
| 543 | 補充說明 | 欄位標籤 | ✅ 已在 strings.xml (form_field_notes) |

---

### 📄 fragment_gutter_photos.xml
| 行號 | 文字 | 類型 | 建議 |
|------|------|------|------|
| 24 | 測量位置及側溝概況 | 欄位標籤 | ✅ 已在 strings.xml (photo_slot_overview) |
| 81 | 點擊拍照 | 提示 | 應移至 strings.xml |

---

### 其他 XML 文件（省略，類似模式）

---

## 第二部分：Kotlin 代碼中的硬編碼文本

### 🔴 Toast 消息（35+ 個）

#### MainActivity.kt
```kotlin
Line 480: "側溝更新成功"           → 應改為字符串資源
Line 483: "側溝上傳成功"           → 應改為字符串資源
Line 502: "請先登入"               → 應改為字符串資源
Line 530: "側溝「$spiNum」已成功刪除" → 應改為字符串資源
Line 535: "刪除失敗：${result.message}" → 應改為字符串資源
Line 849: "查無線段資料"           → 應改為字符串資源
```

#### GutterFormActivity.kt
```kotlin
Line 679: binding.fabSubmit.text = "完成"  → ✅ 已改用字符串資源
Line 699: "請填寫「$basicError」"   → 應改為字符串資源
Line 705: "請拍攝「$photoError」照片" → 應改為字符串資源
Line 923: "正在上傳照片…"          → ✅ 已在 strings.xml
Line 989: "草稿已儲存至本機"       → 應改為字符串資源
```

#### AddGutterBottomSheet.kt
```kotlin
Line 295: binding.tvSheetTitle.text = "離線草稿"  → 應改為字符串資源
Line 601: binding.btnSubmitGutter.text = "更新側溝" → 應改為字符串資源
Line 623: binding.btnSubmitGutter.text = "完成"  → 應改為字符串資源
Line 646: "請先設定起點座標"       → 應改為字符串資源
Line 650: "請先設定終點座標"       → 應改為字符串資源
Line 678: "請先登入"               → 應改為字符串資源
Line 703: "上傳失敗，已儲存為待上傳草稿" → 應改為字符串資源
Line 745: "點位資料載入中，請稍候"  → 應改為字符串資源
Line 937: "部分點位資料載入失敗"    → 應改為字符串資源
```

#### GutterPhotosFragment.kt
```kotlin
Line 54: "需要相機權限才能拍照"     → 應改為字符串資源
Line 259: "無法準備拍照檔案"        → 應改為字符串資源
```

#### WaypointAdapter.kt
```kotlin
Line 66: statusView.text = "已填寫資料"  → 應改為字符串資源
Line 78: statusView.text = "暫無資料"    → 應改為字符串資源
```

#### LandscapeCameraActivity.kt
```kotlin
Line 131: "相機初始化失敗"          → 應改為字符串資源
Line 168: "拍照失敗"                → 應改為字符串資源
```

#### MapPointPickerActivity.kt
```kotlin
Line 52: "需要定位權限才能使用「現在位置」功能" → 應改為字符串資源
Line 139: "尚未取得定位，請稍後再試" → 應改為字符串資源
Line 142: "定位失敗：${it.message}" → 應改為字符串資源
```

#### ImportExistingWaypointActivity.kt
```kotlin
Line 142: "請先登入"                → 應改為字符串資源
```

#### LoginActivity.kt
```kotlin
Line 114: Toast 顯示 result.message → 應改為字符串資源
```

#### PendingDraftAdapter.kt
```kotlin
Line 38: tvPendingDraftTitle.text = "離線草稿" → 應改為字符串資源
```

---

## 統計和建議

### 已正確使用字符串資源的 ✅
- `form_finish_button` = "完成"
- `btn_add_gutter` = "新增側溝"
- `unupload_data` = "待上傳草稿"
- `cancel` = "取消"
- 大多數欄位標籤（field_*）
- 大多數選項值（gutter_type_*）

### 急需修正的硬編碼文字 🔴

#### 優先級 1 - 使用頻繁的文字
1. `完成` (appears in 2+ files)
2. `請先登入` (appears in 4+ places)
3. `側溝更新成功` / `側溝上傳成功`
4. 各種 Toast 提示消息

#### 優先級 2 - 欄位和選項
1. 側溝材質選項（混凝土、卵礫石、紅磚）
2. 狀態選項（無、有、否、是）
3. 淤積程度選項（無、輕度、中度、嚴重）
4. 欄位標籤（測量深度、頂寬度等）

#### 優先級 3 - 提示和對話框
1. 各種加載提示
2. 錯誤消息
3. 確認對話框

---

## 修正方案

### 推薦步驟

**步驟 1: 補充字符串資源 (strings.xml)**
```xml
<!-- 按鈕文字 -->
<string name="btn_done">完成</string>
<string name="btn_update_gutter">更新側溝</string>

<!-- 提示消息 -->
<string name="msg_login_first">請先登入</string>
<string name="msg_gutter_updated">側溝更新成功</string>
<string name="msg_gutter_uploaded">側溝上傳成功</string>

<!-- 欄位和選項 -->
<string name="gutter_material_concrete">混凝土</string>
<string name="gutter_material_gravel">卵礫石</string>
<string name="gutter_material_brick">紅磚</string>

<!-- 淤積程度 -->
<string name="silt_level_none">無</string>
<string name="silt_level_light">輕度</string>
<string name="silt_level_medium">中度</string>
<string name="silt_level_severe">嚴重</string>
```

**步驟 2: 更新 XML 布局文件**
```xml
<!-- 替換硬編碼文字 -->
android:text="完成"  →  android:text="@string/btn_done"
```

**步驟 3: 更新 Kotlin 代碼**
```kotlin
// 替換硬編碼文字
Toast.makeText(this, "完成", ...)
  →  Toast.makeText(this, getString(R.string.btn_done), ...)

binding.fabSubmit.text = "完成"
  →  binding.fabSubmit.text = getString(R.string.btn_done)
```

---

## 影響範圍

| 影響層面 | 詳細說明 |
|--------|--------|
| **維護性** | 修改文字需要改 XML/Kotlin，不方便 |
| **國際化** | 無法進行多語言翻譯 |
| **一致性** | 相同文字可能因手打有不同拼寫 |
| **測試** | 單元測試需要硬編碼相同文字 |

---

## 結論

**建議優先處理**：
1. ✅ 所有 Toast 消息移至 strings.xml
2. ✅ 所有按鈕文字移至 strings.xml
3. ✅ 所有選項值移至 strings.xml
4. ✅ 所有提示和標籤移至 strings.xml

**預期工作量**：
- 約 120+ 個硬編碼文字
- 需要在 strings.xml 中新增 ~80 個字符串資源
- 需要修改 ~50+ 處代碼和 XML 布局

**優先級**：🔴 高 - 建議在下一個迭代中完成

---

**生成日期**: 2026-04-07
**掃描工具**: grep, bash
**總計找到**: 120+ 個硬編碼文字
