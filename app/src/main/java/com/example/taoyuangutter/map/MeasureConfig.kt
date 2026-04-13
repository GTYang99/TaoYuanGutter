package com.example.taoyuangutter.map

import androidx.annotation.DrawableRes
import com.example.taoyuangutter.R
import com.google.android.gms.maps.model.BitmapDescriptor

/**
 * 測距模式的客製化設定。
 *
 * 使用方式（在 MainActivity 宣告一個欄位）：
 * ```kotlin
 * private var measureConfig = MeasureConfig()
 * // 或替換圖示：
 * private var measureConfig = MeasureConfig(
 *     startMarkerIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
 *     crosshairResId  = R.drawable.my_custom_crosshair
 * )
 * ```
 *
 * @param startMarkerIcon  起點大頭針圖示。null = 使用 SDK 預設紅色大頭針。
 * @param crosshairResId   畫面中央準星的 drawable resource ID。
 */
data class MeasureConfig(
    val startMarkerIcon: BitmapDescriptor? = null,
    @DrawableRes val crosshairResId: Int = R.drawable.ic_crosshair
)
