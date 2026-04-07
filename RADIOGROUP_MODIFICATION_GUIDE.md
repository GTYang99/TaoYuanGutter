# RadioGroup + RadioButton 修改建議

## 📋 現狀分析

### 目前的下拉菜單字段

```
① 溝體結構受損 (IS_BROKEN)
   選項: 無 / 有

② 附掛或過路管線 (IS_HANGING)
   選項: 無 / 有

③ 淤積程度 (IS_SILT)
   選項: 無 / 輕度 / 中度 / 嚴重
```

### 為什麼改用 RadioGroup?

| 對比項 | Dropdown | RadioGroup |
|--------|----------|-----------|
| 點擊次數 | 2 次（點開+選擇） | 1 次 |
| 視覺清晰度 | 收起狀態不知道選了啥 | 一目瞭然 |
| 佔用空間 | 較少（折疊） | 較多（展開） |
| 適合場景 | 選項多（5+個） | **選項少（2-4個）** ✓ |

---

## 🎯 修改方案

### 方案比較

#### **方案 A：水平排列（推薦 for 2 選項）** ⭐

**適用**：是非題（無/有）

```
溝體結構受損 ☐ 無   ⦿ 有     ← RadioButton 水平排列
```

**優點**：
- ✅ 節省空間
- ✅ 快速掃過
- ✅ 適合二選一

**缺點**：
- ❌ 文字多時易擁擠

---

#### **方案 B：垂直排列（推薦 for 3-4 選項）** ⭐

**適用**：多選題（如：無、輕度、中度、嚴重）

```
淤積程度
  ⦿ 無
  ○ 輕度
  ○ 中度
  ○ 嚴重
```

**優點**：
- ✅ 清晰易讀
- ✅ 標籤對齐
- ✅ 容易點選

**缺點**：
- ❌ 佔用垂直空間

---

#### **方案 C：混合方案（推薦）** ⭐⭐⭐

**推薦做法**：
- **二選項**→ 水平 RadioGroup
- **多選項**→ 垂直 RadioGroup

---

## 📐 文字排列建議

### 現有文字結構

```xml
<!-- 標題 -->
<TextView text="淤積程度" />

<!-- 選項 -->
<RadioGroup>
  <RadioButton text="無" />
  <RadioButton text="輕度" />
  ...
</RadioGroup>
```

### 修改建議

#### 1️⃣ **溝體結構受損** → 水平排列

```
現況：下拉菜單「無 / 有」

修改後（推薦）：
┌─────────────────────┐
│ 溝體結構受損         │
│ ⦿ 無    ○ 有        │
└─────────────────────┘

XML 結構：
<LinearLayout (vertical)>
  <TextView text="溝體結構受損" />
  <RadioGroup orientation="horizontal">
    <RadioButton text="無" android:id="@+id/rbBrokenNo" />
    <RadioButton text="有" android:id="@+id/rbBrokenYes" />
  </RadioGroup>
</LinearLayout>
```

#### 2️⃣ **附掛或過路管線** → 水平排列

```
現況：下拉菜單「無 / 有」

修改後（推薦）：
┌──────────────────────────┐
│ 附掛或過路管線            │
│ ⦿ 無    ○ 有            │
└──────────────────────────┘

XML 結構：
<LinearLayout (vertical)>
  <TextView text="附掛或過路管線" />
  <RadioGroup orientation="horizontal">
    <RadioButton text="無" android:id="@+id/rbHangingNo" />
    <RadioButton text="有" android:id="@+id/rbHangingYes" />
  </RadioGroup>
</LinearLayout>
```

#### 3️⃣ **淤積程度** → 垂直排列

```
現況：下拉菜單「無 / 輕度 / 中度 / 嚴重」

修改後（推薦）：
┌──────────────────┐
│ 淤積程度         │
│ ⦿ 無             │
│ ○ 輕度           │
│ ○ 中度           │
│ ○ 嚴重           │
└──────────────────┘

XML 結構：
<LinearLayout (vertical)>
  <TextView text="淤積程度" />
  <RadioGroup orientation="vertical">
    <RadioButton text="無" android:id="@+id/rbSiltNone" />
    <RadioButton text="輕度" android:id="@+id/rbSiltLight" />
    <RadioButton text="中度" android:id="@+id/rbSiltMedium" />
    <RadioButton text="嚴重" android:id="@+id/rbSiltSevere" />
  </RadioGroup>
</LinearLayout>
```

---

## 📝 完整 XML 修改範本

### 替換區段（fragment_gutter_basic_info.xml）

#### ❌ 舊的（下拉菜單 - 第 312-340 行）

```xml
<!-- ⑨ 溝體結構受損 ──────────────────────────────────────────────── -->
<TextView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="溝體結構受損"
    android:textSize="13sp"
    android:textColor="@color/textColorSecondary"
    android:layout_marginBottom="4dp" />

<com.google.android.material.textfield.TextInputLayout
    android:id="@+id/tilIsBroken"
    style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox.ExposedDropdownMenu"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="16dp"
    app:hintEnabled="false"
    app:boxStrokeColor="@color/colorPrimary"
    app:boxBackgroundColor="@color/white">

    <AutoCompleteTextView
        android:id="@+id/actvIsBroken"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:inputType="none"
        android:textColor="@color/textColorPrimary"
        android:paddingTop="14dp"
        android:paddingBottom="14dp" />

</com.google.android.material.textfield.TextInputLayout>
```

#### ✅ 新的（RadioGroup - 水平）

```xml
<!-- ⑨ 溝體結構受損 ──────────────────────────────────────────────── -->
<TextView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="溝體結構受損"
    android:textSize="13sp"
    android:textColor="@color/textColorSecondary"
    android:layout_marginBottom="8dp" />

<RadioGroup
    android:id="@+id/rgIsBroken"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:layout_marginBottom="16dp">

    <RadioButton
        android:id="@+id/rbBrokenNo"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="無"
        android:paddingEnd="24dp" />

    <RadioButton
        android:id="@+id/rbBrokenYes"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="有" />

</RadioGroup>
```

---

### 淤積程度的修改（垂直排列）

#### ❌ 舊的（下拉菜單 - 第 372-399 行）

```xml
<!-- ⑪ 淤積程度 ──────────────────────────────────────────────────── -->
<TextView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="淤積程度"
    android:textSize="13sp"
    android:textColor="@color/textColorSecondary"
    android:layout_marginBottom="4dp" />

<com.google.android.material.textfield.TextInputLayout
    android:id="@+id/tilIsSilt"
    style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox.ExposedDropdownMenu"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="16dp"
    app:hintEnabled="false"
    app:boxStrokeColor="@color/colorPrimary"
    app:boxBackgroundColor="@color/white">

    <AutoCompleteTextView
        android:id="@+id/actvIsSilt"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:inputType="none"
        android:textColor="@color/textColorPrimary"
        android:paddingTop="14dp"
        android:paddingBottom="14dp" />

</com.google.android.material.textfield.TextInputLayout>
```

#### ✅ 新的（RadioGroup - 垂直）

```xml
<!-- ⑪ 淤積程度 ──────────────────────────────────────────────────── -->
<TextView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="淤積程度"
    android:textSize="13sp"
    android:textColor="@color/textColorSecondary"
    android:layout_marginBottom="8dp" />

<RadioGroup
    android:id="@+id/rgIsSilt"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:layout_marginBottom="16dp">

    <RadioButton
        android:id="@+id/rbSiltNone"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="無"
        android:paddingTop="8dp"
        android:paddingBottom="8dp" />

    <RadioButton
        android:id="@+id/rbSiltLight"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="輕度"
        android:paddingTop="8dp"
        android:paddingBottom="8dp" />

    <RadioButton
        android:id="@+id/rbSiltMedium"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="中度"
        android:paddingTop="8dp"
        android:paddingBottom="8dp" />

    <RadioButton
        android:id="@+id/rbSiltSevere"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="嚴重"
        android:paddingTop="8dp"
        android:paddingBottom="8dp" />

</RadioGroup>
```

---

## 🔤 文字排列細節調整

### 水平排列時的間距控制

```xml
<RadioGroup android:orientation="horizontal">
    <!-- 左邊選項 -->
    <RadioButton
        android:text="無"
        android:layout_marginEnd="24dp" />  <!-- ← 與右邊的距離

    <!-- 右邊選項 -->
    <RadioButton
        android:text="有" />
</RadioGroup>
```

**建議間距**：
- 選項短（1-2字）：`24dp`
- 選項長（3-5字）：`16dp`

### 垂直排列時的高度控制

```xml
<RadioGroup android:orientation="vertical">
    <RadioButton
        android:text="無"
        android:paddingTop="8dp"     <!-- ← 上下間距
        android:paddingBottom="8dp" />
    <!-- ... -->
</RadioGroup>
```

**建議高度**：
- `paddingTop="8dp"` + `paddingBottom="8dp"` = 單項高度 ~48dp
- 4 個選項 = 約 192dp 總高度

---

## 💻 Kotlin 代碼修改

### 舊的方式（AutoCompleteTextView）

```kotlin
// 取值
val brokenValue = binding.actvIsBroken.text.toString()

// 設值
binding.actvIsBroken.setText("有")
```

### 新的方式（RadioGroup）

```kotlin
// 取值
val selectedId = binding.rgIsBroken.checkedRadioButtonId
val selectedValue = when (selectedId) {
    R.id.rbBrokenNo -> "0"
    R.id.rbBrokenYes -> "1"
    else -> ""
}

// 設值
val valueToSet = waypoint.basicData["IS_BROKEN"] ?: "0"
when (valueToSet) {
    "0" -> binding.rgIsBroken.check(R.id.rbBrokenNo)
    "1" -> binding.rgIsBroken.check(R.id.rbBrokenYes)
}

// 監聽變化
binding.rgIsBroken.setOnCheckedChangeListener { _, checkedId ->
    when (checkedId) {
        R.id.rbBrokenNo -> waypoint.basicData["IS_BROKEN"] = "0"
        R.id.rbBrokenYes -> waypoint.basicData["IS_BROKEN"] = "1"
    }
}
```

---

## 📊 淤積程度的對應表

### 值的映射

```kotlin
// 淤積程度（IS_SILT）
R.id.rbSiltNone -> 0      // 無
R.id.rbSiltLight -> 1     // 輕度
R.id.rbSiltMedium -> 2    // 中度
R.id.rbSiltSevere -> 3    // 嚴重
```

### 完整轉換函數

```kotlin
private fun getSiltValueFromRadioGroup(): String {
    return when (binding.rgIsSilt.checkedRadioButtonId) {
        R.id.rbSiltNone -> "0"
        R.id.rbSiltLight -> "1"
        R.id.rbSiltMedium -> "2"
        R.id.rbSiltSevere -> "3"
        else -> ""
    }
}

private fun setSiltValueToRadioGroup(value: String?) {
    when (value?.toIntOrNull()) {
        0 -> binding.rgIsSilt.check(R.id.rbSiltNone)
        1 -> binding.rgIsSilt.check(R.id.rbSiltLight)
        2 -> binding.rgIsSilt.check(R.id.rbSiltMedium)
        3 -> binding.rgIsSilt.check(R.id.rbSiltSevere)
        else -> {} // 不選
    }
}
```

---

## 📋 修改檢查清單

### XML 檔案修改

- [ ] 刪除 `tilIsBroken` TextInputLayout
- [ ] 刪除 `actvIsBroken` AutoCompleteTextView
- [ ] 新增 `rgIsBroken` RadioGroup
- [ ] 新增 2 個 RadioButton (無/有)

- [ ] 刪除 `tilIsHanging` TextInputLayout
- [ ] 刪除 `actvIsHanging` AutoCompleteTextView
- [ ] 新增 `rgIsHanging` RadioGroup
- [ ] 新增 2 個 RadioButton (無/有)

- [ ] 刪除 `tilIsSilt` TextInputLayout
- [ ] 刪除 `actvIsSilt` AutoCompleteTextView
- [ ] 新增 `rgIsSilt` RadioGroup
- [ ] 新增 4 個 RadioButton (無/輕度/中度/嚴重)

### Kotlin 代碼修改

- [ ] 刪除 `actvIsBroken` 相關初始化
- [ ] 移除 `actvIsBroken` 的 Adapter 設置
- [ ] 新增 `rgIsBroken` 的 checkedChangeListener

- [ ] 刪除 `actvIsHanging` 相關初始化
- [ ] 移除 `actvIsHanging` 的 Adapter 設置
- [ ] 新增 `rgIsHanging` 的 checkedChangeListener

- [ ] 刪除 `actvIsSilt` 相關初始化
- [ ] 移除 `actvIsSilt` 的 Adapter 設置
- [ ] 新增 `rgIsSilt` 的 checkedChangeListener

### 驗證邏輯修改

- [ ] 更新 `getBasicData()` 提取 RadioGroup 值
- [ ] 更新 `prefillData()` 設定 RadioGroup 值
- [ ] 測試新增模式
- [ ] 測試編輯模式
- [ ] 測試無法開蓋時的行為

---

## 🎨 視覺效果對比

### Before（Dropdown）

```
┌─────────────────────────────────┐
│ 溝體結構受損                      │
│ [        無        ▼]           │
└─────────────────────────────────┘

✗ 收起時看不到選擇
✗ 需要 2 次點擊
✗ 不知道有 2 個選項
```

### After（RadioGroup - 水平）

```
┌─────────────────────────────────┐
│ 溝體結構受損                      │
│ ⦿ 無    ○ 有                    │
└─────────────────────────────────┘

✓ 一眼看清選項
✓ 1 次點擊選擇
✓ 節省空間
```

### After（RadioGroup - 垂直）

```
┌──────────────────┐
│ 淤積程度         │
│ ⦿ 無             │
│ ○ 輕度           │
│ ○ 中度           │
│ ○ 嚴重           │
└──────────────────┘

✓ 清晰易讀
✓ 1 次點擊選擇
✓ 選項對齐
```

---

## 🔄 相關文件更新清單

| 檔案 | 修改內容 | 優先級 |
|------|--------|--------|
| `fragment_gutter_basic_info.xml` | 替換 3 個 Dropdown 為 RadioGroup | 🔴 高 |
| `GutterBasicInfoFragment.kt` | 更新取值/設值邏輯 | 🔴 高 |
| `GutterBasicInfoFragment.kt` | 移除 Adapter 初始化 | 🟡 中 |
| `GutterInspectBasicFragment.kt` | 同步修改（查看模式） | 🟡 中 |

---

**修改指南版本**: 1.0
**建議日期**: 2026-04-01
