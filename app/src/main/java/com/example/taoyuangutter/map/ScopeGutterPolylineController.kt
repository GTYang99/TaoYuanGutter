package com.example.taoyuangutter.map

import android.graphics.Color
import android.location.Location
import com.example.taoyuangutter.api.GeoFeature
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import kotlin.math.ceil

/**
 * 管理 scopeSearch 載入後的正式側溝線段，
 * 將 MainActivity 從 polyline 建立/替換/移除的細節中解耦。
 */
class ScopeGutterPolylineController(
    private val mapProvider: () -> GoogleMap?
) {
    private data class LngLat(
        val lng: Double,
        val lat: Double
    ) {
        fun toLatLng(): LatLng = LatLng(lat, lng)
    }

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
    private var isGlobalVisible = true

    fun setVisible(visible: Boolean) {
        isGlobalVisible = visible
        scopePolylines.values.forEach {
            it.inner.isVisible = visible
            it.outline?.isVisible = visible
        }
    }

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
        savedGroupId: Int,
        clickable: Boolean = true
    ) {
        val map = mapProvider() ?: return
        features.forEach { feature ->
            val spiNum = feature.properties?.spiNum ?: return@forEach
            val groupId = feature.properties?.groupId ?: ""
            val spiState = feature.properties?.spiState
            val isPendingDeploy = feature.properties?.isPendingDeploy == 1
            val isCurve = feature.properties?.isCurve == 1
            val coords = feature.geometry?.coordinates ?: return@forEach
            if (coords.size < 2) return@forEach
            if (spiState !in setOf(1, 2, 3, 4)) return@forEach

            remove(spiNum)

            val points = buildPolylinePoints(
                rawCoordinates = coords,
                isCurve = isCurve
            ) ?: return@forEach
            val isSameGroup = savedGroupId != -1 &&
                groupId.isNotBlank() &&
                groupId.toIntOrNull() == savedGroupId
            val isAdmin = savedGroupId == 1

            val color = if (!isAdmin && !isSameGroup) {
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
                !isAdmin && !isSameGroup -> 12f
                else -> 8f
            }
            val outline = if (isPendingDeploy && (isAdmin || isSameGroup)) {
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
                    .clickable(clickable)
                    .visible(isGlobalVisible)
            )
            inner.tag = Pair(spiNum, groupId)
            scopePolylines[spiNum] = ScopePolylineSet(inner = inner, outline = outline)
        }
    }

    private fun buildPolylinePoints(
        rawCoordinates: List<List<Any?>?>,
        isCurve: Boolean
    ): List<LatLng>? {
        if (isCurve && rawCoordinates.size == 3) {
            val start = parseLngLatOrNull(rawCoordinates[0]) ?: return null
            val control = parseLngLatOrNull(rawCoordinates[1]) ?: return null
            val end = parseLngLatOrNull(rawCoordinates[2]) ?: return null
            val steps = resolveCurveSteps(start, control, end)
            return generateCurvePointsWithControl(start, end, control, steps)
        }

        // Fallback or Normal mode: return straight line points if size >= 2
        val points = rawCoordinates.mapNotNull { parseLngLatOrNull(it) }.map { it.toLatLng() }
        return points.takeIf { it.size >= 2 }
    }

    private fun parseLngLatOrNull(pair: List<Any?>?): LngLat? {
        if (pair == null || pair.size < 2) return null
        val lng = pair[0].asDoubleOrNull() ?: return null
        val lat = pair[1].asDoubleOrNull() ?: return null
        return LngLat(lng = lng, lat = lat)
    }

    private fun Any?.asDoubleOrNull(): Double? {
        val value = this ?: return null
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.sanitizeNumberStringOrNull()?.toDoubleOrNull()
            else -> null
        }
    }

    private fun String.sanitizeNumberStringOrNull(): String? {
        val normalized = trim()
            .replace("\u3000", "") // full-width space
            .replace(",", "")
        return normalized.ifBlank { null }
    }

    private fun resolveCurveSteps(
        start: LngLat,
        control: LngLat,
        end: LngLat
    ): Int {
        val distance =
            distanceMeters(start.toLatLng(), control.toLatLng()) +
                distanceMeters(control.toLatLng(), end.toLatLng())
        val stepsByDistance = ceil(distance / 5.0).toInt()
        return stepsByDistance.coerceAtLeast(100).coerceAtMost(1000)
    }

    private fun distanceMeters(a: LatLng, b: LatLng): Double {
        val results = FloatArray(1)
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, results)
        return results[0].toDouble()
    }

    private fun generateCurvePointsWithControl(
        start: LngLat,
        end: LngLat,
        control: LngLat,
        steps: Int
    ): List<LatLng> {
        val points = ArrayList<LatLng>(steps + 1)
        for (i in 0..steps) {
            val t = i.toDouble() / steps.toDouble()
            val inv = 1.0 - t
            val lat = inv * inv * start.lat + 2.0 * inv * t * control.lat + t * t * end.lat
            val lng = inv * inv * start.lng + 2.0 * inv * t * control.lng + t * t * end.lng
            points.add(LatLng(lat, lng))
        }
        return points
    }
}
