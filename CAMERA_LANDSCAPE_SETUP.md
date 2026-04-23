# 相機橫向拍攝設置指南

**日期**: 2026-04-23  
**更新內容**: 強制相機橫向拍攝，照片尺寸為 4:3 橫向（寬度 4、高度 3）

---

## 📋 修改內容概要

### 1. **XML 布局修改** - `fragment_camera_overlay.xml`

**變更點**：
- 預覽區尺寸比例：`3:4` → **`4:3`**（橫向）
- orientationWarning 初始狀態：`visible` → **`gone`**（隱藏）

**文件路徑**：`app/src/main/res/layout/fragment_camera_overlay.xml`

**修改詳情**：
```xml
<!-- 修改前 -->
<FrameLayout
    app:layout_constraintDimensionRatio="W,3:4">

<!-- 修改後 -->
<FrameLayout
    app:layout_constraintDimensionRatio="W,4:3">  <!-- 橫向 4:3 -->
```

```xml
<!-- 修改前 -->
<FrameLayout
    android:visibility="visible">  <!-- 一開始顯示提示 -->

<!-- 修改後 -->
<FrameLayout
    android:visibility="gone">  <!-- 一開始隱藏 -->
```

---

### 2. **CameraOverlayFragment.kt - bindUseCases() 方法**

**變更點**：
- 初始化時設置 `targetRotation = ROTATION_0`（橫向）
- 同時應用於 `Preview` 和 `ImageCapture`
- 初始狀態設置為 `deviceIsLandscape = true`

**文件路徑**：`app/src/main/java/com/example/taoyuangutter/gutter/CameraOverlayFragment.kt`

**代碼變更**：
```kotlin
// 預覽區初始化
val preview = Preview.Builder()
    .setTargetAspectRatio(AspectRatio.RATIO_4_3)  // ← 4:3 比例
    .build().also {
        it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
    }
this.preview = preview

// 設置初始橫向旋轉
preview.targetRotation = Surface.ROTATION_0  // ← 初始橫向

// 照片捕捉初始化
imageCapture = ImageCapture.Builder()
    .setTargetAspectRatio(AspectRatio.RATIO_4_3)  // ← 4:3 比例
    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
    .setTargetRotation(Surface.ROTATION_0)  // ← 初始橫向，控制 EXIF 方向
    .build()

// 綁定後設置初始狀態
deviceIsLandscape = true
updateOrientationUi(true)  // ← 快門啟用，隱藏提示
```

---

### 3. **CameraOverlayFragment.kt - setupOrientationListener() 方法**

**變更點**：
- 初始狀態改為 `deviceIsLandscape = true`（假設已橫放）
- 優化註解說明邏輯

**代碼變更**：
```kotlin
private fun setupOrientationListener() {
    orientationListener = object : OrientationEventListener(requireContext()) {
        override fun onOrientationChanged(orientation: Int) {
            if (orientation == ORIENTATION_UNKNOWN) return
            
            // 強制橫向模式
            val landscape = orientation in 60..120 || orientation in 240..300
            if (landscape != deviceIsLandscape) {
                deviceIsLandscape = landscape
                updateOrientationUi(landscape)
            }

            // 根據物理方向更新相機旋轉
            val rotation = when {
                orientation in 60..120 -> Surface.ROTATION_90    // 右傾
                orientation in 240..300 -> Surface.ROTATION_270  // 左傾
                else -> Surface.ROTATION_0                       // 直立
            }
            if (rotation != lastSurfaceRotation) {
                lastSurfaceRotation = rotation
                updateControlsPosition(rotation)
                preview?.targetRotation = rotation
                imageCapture?.targetRotation = rotation  // ← 這決定了照片方向
            }
        }
    }

    // 初始狀態：假設已經橫放
    deviceIsLandscape = true
    updateOrientationUi(true)
    lastSurfaceRotation = Surface.ROTATION_0
    updateControlsPosition(Surface.ROTATION_0)
}
```

---

## 🎯 功能流程

### 拍照時的方向確定機制

```
用戶打開相機
    ↓
初始化時設置 targetRotation = ROTATION_0（假設橫放）
    ↓
預覽區顯示 4:3 橫向畫面，快門啟用
    ↓
OrientationEventListener 監聽設備方向
    ├─ 如果 orientation in 60..120 → 更新 targetRotation = ROTATION_90
    ├─ 如果 orientation in 240..300 → 更新 targetRotation = ROTATION_270
    └─ 其他 → targetRotation = ROTATION_0
    ↓
用戶按下快門
    ↓
CameraX 保存 JPEG 文件
    ├─ 根據當前 targetRotation 寫入 EXIF Orientation Tag
    └─ 照片像素不變，方向信息存在 EXIF 中
    ↓
上傳到伺服器
    ├─ copyUriToTempFile() 復製照片字節流（保留 EXIF）
    └─ 作為 multipart/form-data 上傳
    ↓
相冊查看
    └─ 根據 EXIF Orientation Tag 自動旋轉顯示
```

---

## ✅ 修改清單

- [x] `fragment_camera_overlay.xml`：預覽區比例改為 4:3
- [x] `fragment_camera_overlay.xml`：orientationWarning 初始狀態改為 gone
- [x] `CameraOverlayFragment.kt`：bindUseCases() 設置 targetRotation
- [x] `CameraOverlayFragment.kt`：setupOrientationListener() 初始化檢查
- [x] 更新 XML 註解
- [x] 驗證照片上傳方法保留 EXIF（無需修改，已正確實現）

---

## 🔧 測試步驟

1. **構建應用**
   ```bash
   ./gradlew clean build
   ```

2. **安裝並運行**
   ```bash
   ./gradlew installDebug
   ```

3. **測試拍照**
   - 進入側溝表單
   - 點擊照片格
   - 確保預覽區是 4:3 橫向
   - 確保快門按鈕啟用（不被禁用）

4. **測試方向**
   - 向右傾斜手機 (60-120°) → 照片應右轉 90°
   - 向左傾斜手機 (240-300°) → 照片應左轉 90°
   - 豎直拿手機 → 快門禁用，顯示提示

5. **驗證 EXIF**
   ```bash
   # 拉取照片
   adb pull /data/data/com.example.taoyuangutter/files/Pictures/GUTTER_1_*.jpg
   
   # 查看 EXIF
   exiftool GUTTER_1_*.jpg | grep Orientation
   ```

---

## 📐 尺寸說明

### 預覽窗口（布局）
- **尺寸比例**：4:3（寬度 4、高度 3）
- **寬度**：手機屏幕寬度
- **高度**：手機屏幕寬度 × 3/4

### 最終照片（JPEG 文件）
- **尺寸比例**：4:3（寬度 4、高度 3）
- **像素尺寸**：由相機傳感器決定（通常 4000×3000 pixel）
- **方向標籤**：存在 EXIF Orientation 中
  - `1` = 正常（0° 旋轉）
  - `6` = 右轉 90°（ROTATION_90）
  - `8` = 右轉 270°（ROTATION_270）

---

## 🚨 常見問題排查

### 快門被禁用
- **原因**：設備未橫放（orientation 不在 60~120 或 240~300 範圍）
- **解決**：橫向拿手機，快門應自動啟用

### 照片方向錯誤
- **原因**：设备转向与 EXIF 不匹配或查看器不支持 EXIF
- **解決**：
  1. 檢查 EXIF Orientation Tag
  2. 使用支持 EXIF 的查看器（文件管理器、Google 相冊等）
  3. 如果伺服器需要自動旋轉，需後端處理

### EXIF 信息丟失
- **原因**：照片在上傳前被重新編碼或轉換
- **解決**：目前的 copyUriToTempFile() 只復製字節流，EXIF 應該被保留

---

## 📚 相關文件

- `AGENTS.md` - 相機系統詳解（第 103-173 行）
- `CameraOverlayFragment.kt` - 相機實現
- `fragment_camera_overlay.xml` - 相機布局
- `GutterRepository.kt` - 上傳邏輯（第 501-593 行）

---

**最後更新**: 2026-04-23 | **狀態**: ✅ 完成

