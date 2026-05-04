package com.example.taoyuangutter.map

import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.api.GeoFeature
import com.example.taoyuangutter.api.GutterRepository
import com.google.android.gms.maps.GoogleMap

class ScopeViewportLoader(
    private val repository: GutterRepository,
    private val mapProvider: () -> GoogleMap?
) {
    data class ScopeLoadResult(
        val features: List<GeoFeature>
    )

    fun visibleBoundsOrNull(): VisibleBounds? {
        val map = mapProvider() ?: return null
        val bounds = map.projection.visibleRegion.latLngBounds
        return VisibleBounds(
            minLat = bounds.southwest.latitude,
            maxLat = bounds.northeast.latitude,
            minLng = bounds.southwest.longitude,
            maxLng = bounds.northeast.longitude
        )
    }

    suspend fun load(token: String): ApiResult<ScopeLoadResult> {
        val bounds = visibleBoundsOrNull()
            ?: return ApiResult.Error(message = "Map not ready")
        return when (val result = repository.getGuttersByScope(
            minLat = bounds.minLat,
            maxLat = bounds.maxLat,
            minLng = bounds.minLng,
            maxLng = bounds.maxLng,
            token = token
        )) {
            is ApiResult.Success -> ApiResult.Success(
                ScopeLoadResult(result.data.data?.features ?: emptyList())
            )
            is ApiResult.Error -> result
        }
    }

    data class VisibleBounds(
        val minLat: Double,
        val maxLat: Double,
        val minLng: Double,
        val maxLng: Double
    )
}
