package com.example.taoyuangutter.map

import android.graphics.Color
import com.example.taoyuangutter.api.GeoFeature
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions

/**
 * 管理 scopeSearch 載入後的正式側溝線段，
 * 將 MainActivity 從 polyline 建立/替換/移除的細節中解耦。
 */
class ScopeGutterPolylineController(
    private val mapProvider: () -> GoogleMap?
) {
    data class ScopePolylineSet(
        val inner: Polyline,
        val outline: Polyline?
    ) {
        fun remove() {
            inner.remove()
            outline?.remove()
        }
    }

    private val scopePolylines = mutableMapOf<String, ScopePolylineSet>()

    fun clear() {
        scopePolylines.values.forEach { it.remove() }
        scopePolylines.clear()
    }

    fun remove(spiNum: String) {
        scopePolylines.remove(spiNum)?.remove()
    }

    fun entries(): Set<Map.Entry<String, ScopePolylineSet>> = scopePolylines.entries

    fun highlightInner(spiNum: String, color: Int) {
        scopePolylines[spiNum]?.inner?.color = color
    }

    fun drawFeatures(
        features: List<GeoFeature>,
        savedGroupId: Int
    ) {
        val map = mapProvider() ?: return
        features.forEach { feature ->
            val spiNum = feature.properties?.spiNum ?: return@forEach
            val groupId = feature.properties?.groupId ?: ""
            val spiState = feature.properties?.spiState
            val isPendingDeploy = feature.properties?.isPendingDeploy == 1
            val coords = feature.geometry?.coordinates ?: return@forEach
            if (coords.size < 2) return@forEach
            if (spiState !in setOf(1, 2, 3, 4)) return@forEach

            remove(spiNum)

            val points = coords.map { LatLng(it[1], it[0]) }
            val isSameGroup = savedGroupId != -1 &&
                groupId.isNotBlank() &&
                groupId.toIntOrNull() == savedGroupId
            val color = if (!isSameGroup) {
                Color.parseColor("#B4B4B4")
            } else {
                when (spiState) {
                    1 -> Color.parseColor("#000000")
                    2 -> Color.parseColor("#FF58E0")
                    3 -> Color.parseColor("#FFC300")
                    4 -> Color.parseColor("#1962FF")
                    else -> return@forEach
                }
            }
            val width = when {
                !isSameGroup -> 12f
                else -> 8f
            }
            val outline = if (isPendingDeploy && isSameGroup) {
                map.addPolyline(
                    PolylineOptions()
                        .addAll(points)
                        .color(Color.parseColor("#AD3A36"))
                        .width(12f)
                        .zIndex(0f)
                        .clickable(false)
                )
            } else {
                null
            }

            val inner = map.addPolyline(
                PolylineOptions()
                    .addAll(points)
                    .color(color)
                    .width(width)
                    .zIndex(1f)
                    .clickable(true)
            )
            inner.tag = Pair(spiNum, groupId)
            scopePolylines[spiNum] = ScopePolylineSet(inner = inner, outline = outline)
        }
    }
}
