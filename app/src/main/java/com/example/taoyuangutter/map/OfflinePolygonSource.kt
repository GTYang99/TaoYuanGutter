package com.example.taoyuangutter.map

/** A packaged GeoPackage source and its optional per-district fallback. */
internal data class OfflinePolygonSource(
    val layer: OfflinePolygonLayer,
    val assetName: String,
    val tableName: String,
    val geometryType: String,
    val srsId: Int,
    val districtFieldName: String?,
    val normalizeGeographicCoordinates: Boolean = false,
    val queryDistricts: Set<String> = MapOverlayController.DEFAULT_DISTRICTS,
    val fallback: OfflinePolygonSource? = null,
    val expectedSizeBytes: Long? = null,
    val expectedSha256: String? = null
)

internal object OfflinePolygonSourceCatalog {
    fun full(layer: OfflinePolygonLayer): OfflinePolygonSource = OfflinePolygonSource(
        layer = layer,
        assetName = layer.assetName,
        tableName = layer.tableName,
        geometryType = layer.geometryType,
        srsId = layer.srsId,
        districtFieldName = layer.districtFieldName,
        normalizeGeographicCoordinates = layer.normalizeGeographicCoordinates,
        expectedSizeBytes = layer.assetSizeBytes,
        expectedSha256 = layer.assetSha256
    )

    fun district(layer: OfflinePolygonLayer, district: String): OfflinePolygonSource {
        require(layer == OfflinePolygonLayer.LEGACY || layer == OfflinePolygonLayer.POSSIBLE) {
            "District sources are only available for gutter layers"
        }
        return if (layer == OfflinePolygonLayer.POSSIBLE) {
            OfflinePolygonSource(
                layer = layer,
                assetName = "$district.gpkg",
                tableName = "refactored",
                geometryType = "MULTIPOLYGON",
                srsId = 3826,
                districtFieldName = "Town",
                queryDistricts = setOf(district),
                fallback = full(layer).copy(queryDistricts = setOf(district))
            )
        } else {
            OfflinePolygonSource(
                layer = layer,
                assetName = "TOWN_$district.gpkg",
                tableName = "TOWN_$district",
                geometryType = "POLYGON",
                srsId = 3826,
                districtFieldName = "TOWN",
                queryDistricts = setOf(district),
                fallback = full(layer).copy(queryDistricts = setOf(district))
            )
        }
    }
}
