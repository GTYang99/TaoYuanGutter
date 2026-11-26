package com.example.taoyuangutter.map

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import android.util.Log
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.floor

internal enum class OfflinePolygonLayer(
    val assetName: String,
    val tableName: String,
    val geometryType: String,
    val color: Int,
    val assetSizeBytes: Long,
    val assetSha256: String,
    val srsId: Int = 3826,
    val normalizeGeographicCoordinates: Boolean = false,
    val districtFieldName: String? = null
) {
    LEGACY(
        "20260824_水務局既有側溝更新v3_線轉面.gpkg", "line_buffer_polygon", "POLYGON", OfflinePolygonColors.LEGACY_ARGB.toInt(),
        19_615_744L, "4b0df3e90dab556b3de0bd859511dee5a3af5d171255e4157e32b8d3f6e1ecaf", districtFieldName = "Town"
    ),
    POSSIBLE(
        "20260601_水務局未調查側溝_v5.gpkg", "refactored", "MULTIPOLYGON", OfflinePolygonColors.POSSIBLE_ARGB.toInt(),
        60_661_760L, "de68967c0fd607b1921a8bd3856d2a6d70e754d2bbb966beca89c82f6cc16177", districtFieldName = "TOWN"
    ),
    REGION(
        "桃園市行政區.gpkg", "桃園市行政區", "MULTIPOLYGON", OfflinePolygonColors.REGION_ARGB.toInt(),
        1_081_344L, "aeed88ae15bc53956bbea3e7ebd9a3faf1bf29fc45a6bfaa4188b5333458c1bc",
        srsId = 100000, normalizeGeographicCoordinates = true, districtFieldName = "TOWNNAME"
    )
}

internal data class IndexedGeometry(
    val blob: ByteArray,
    val bounds: DoubleArray,
    val districtName: String?
)

internal interface OfflinePolygonDataSource {
    fun query(
        bounds: DoubleArray,
        selectedDistricts: Set<String> = MapOverlayController.DEFAULT_DISTRICTS
    ): List<ByteArray>

    fun close() = Unit
}

internal object OfflinePolygonDataStore {
    private const val TAG = "OfflinePolygonData"
    private const val CELL_SIZE_METERS = 512.0
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "offline-polygon-index").apply { isDaemon = true }
    }
    private data class CachedSource(
        val future: CompletableFuture<OfflinePolygonDataSource>,
        var references: Int
    )

    private val sources = ConcurrentHashMap<String, CachedSource>()

    fun load(context: Context, layer: OfflinePolygonLayer): CompletableFuture<OfflinePolygonDataSource> =
        load(context, OfflinePolygonSourceCatalog.full(layer))

    fun load(context: Context, source: OfflinePolygonSource): CompletableFuture<OfflinePolygonDataSource> {
        val appContext = context.applicationContext
        val key = "${appContext.filesDir.absolutePath}:${source.assetName}"
        synchronized(sources) {
            sources[key]?.let {
                it.references++
                return it.future
            }
            val future = CompletableFuture.supplyAsync({
                when (source.layer) {
                    OfflinePolygonLayer.POSSIBLE -> {
                        invalidateObsoleteGeoJsonIndex(appContext)
                        openGeoPackage(appContext, source)
                    }
                    else -> openGeoPackage(appContext, source)
                }
            }, executor)
            sources[key] = CachedSource(future, references = 1)
            return future
        }
    }

    fun release(context: Context, source: OfflinePolygonSource) {
        val key = "${context.applicationContext.filesDir.absolutePath}:${source.assetName}"
        val entry = synchronized(sources) {
            val current = sources[key] ?: return
            current.references--
            if (current.references > 0) return
            sources.remove(key)
            current
        }
        entry.future.whenComplete { dataSource, _ -> dataSource?.close() }
    }

    private fun openGeoPackage(context: Context, source: OfflinePolygonSource): OfflinePolygonDataSource {
        val dataDir = File(context.filesDir, "offline_polygon_data").apply { mkdirs() }
        val file = File(dataDir, source.assetName)
        val versionFile = File(dataDir, "${source.assetName}.sha256")
        if (!file.isFile || (source.expectedSizeBytes != null && file.length() != source.expectedSizeBytes) ||
            versionFile.readTextOrNull() != (source.expectedSha256 ?: "unknown")) {
            val temporaryFile = File(dataDir, "${source.assetName}.tmp")
            context.assets.open(source.assetName).use { input ->
                temporaryFile.outputStream().use { output -> input.copyTo(output) }
            }
            check(source.expectedSizeBytes == null || temporaryFile.length() == source.expectedSizeBytes) {
                "Staged GeoPackage size does not match packaged source"
            }
            if (file.exists()) check(file.delete()) { "Unable to replace staged GeoPackage" }
            check(temporaryFile.renameTo(file)) { "Unable to finalize staged GeoPackage" }
            versionFile.writeText(source.expectedSha256 ?: file.length().toString())
        }
        val database = SQLiteDatabase.openDatabase(
            file.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
        )
        val metadata = database.rawQuery(
            "SELECT c.srs_id, g.geometry_type_name FROM gpkg_contents c " +
                "JOIN gpkg_geometry_columns g ON g.table_name = c.table_name WHERE c.table_name = ?",
            arrayOf(source.tableName)
        ).use { cursor ->
            check(cursor.moveToFirst()) { "GeoPackage layer ${source.tableName} is missing" }
            cursor.getInt(0) to cursor.getString(1)
        }
        check(metadata.first == source.srsId && metadata.second.equals(source.geometryType, ignoreCase = true)) {
            "Unsupported GeoPackage CRS/geometry: $metadata"
        }
        val indexFile = File(dataDir, "${source.assetName}.spatial.sqlite")
        val sourceVersion = source.expectedSha256 ?: file.length().toString()
        val indexDatabase = openVersionedIndex(indexFile, sourceVersion) { freshDatabase ->
            buildGeoPackageSpatialIndex(database, source, freshDatabase)
        }
        return GeoPackageDataSource(database, indexDatabase, source)
    }

    internal fun invalidateObsoleteGeoJsonIndex(context: Context) {
        val dataDirectory = File(context.filesDir, "offline_polygon_data")
        val obsoleteIndex = File(dataDirectory, "possible_polygon.sqlite")
        listOf(
            obsoleteIndex,
            File(dataDirectory, "possible_polygon.sqlite-journal"),
            File(dataDirectory, "possible_polygon.sqlite-wal"),
            File(dataDirectory, "possible_polygon.sqlite-shm")
        ).forEach { file ->
            check(!file.exists() || file.delete()) { "Unable to remove obsolete possible-layer GeoJSON cache" }
        }
    }

    private fun openVersionedIndex(
        indexFile: File,
        sourceSha256: String,
        build: (SQLiteDatabase) -> Unit
    ): SQLiteDatabase {
        indexFile.parentFile?.mkdirs()
        val existing = SQLiteDatabase.openOrCreateDatabase(indexFile, null)
        val hasMetadataTable = existing.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'metadata'",
            null
        ).use { it.moveToFirst() }
        val isCurrent = hasMetadataTable && existing.rawQuery(
            "SELECT value FROM metadata WHERE key = 'source_sha256'",
            null
        ).use { cursor -> cursor.moveToFirst() && cursor.getString(0) == sourceSha256 } && existing.rawQuery(
            "SELECT value FROM metadata WHERE key = 'index_version'",
            null
        ).use { cursor -> cursor.moveToFirst() && cursor.getString(0) == "4" }
        if (isCurrent) return existing

        existing.close()
        check(!indexFile.exists() || indexFile.delete()) { "Unable to replace stale spatial index: $indexFile" }
        val freshDatabase = SQLiteDatabase.openOrCreateDatabase(indexFile, null)
        try {
            build(freshDatabase)
            return freshDatabase
        } catch (failure: Throwable) {
            freshDatabase.close()
            indexFile.delete()
            throw failure
        }
    }

    private fun initializeCellIndex(database: SQLiteDatabase) {
        database.execSQL("PRAGMA synchronous=OFF")
        database.execSQL("CREATE TABLE metadata (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
        database.execSQL("CREATE TABLE feature_bounds (id INTEGER PRIMARY KEY, minx REAL NOT NULL, miny REAL NOT NULL, maxx REAL NOT NULL, maxy REAL NOT NULL, district_name TEXT)")
        database.execSQL("CREATE TABLE normalized_geometries (id INTEGER PRIMARY KEY, geom BLOB NOT NULL)")
        database.execSQL("CREATE TABLE spatial_cells (cell_x INTEGER NOT NULL, cell_y INTEGER NOT NULL, id INTEGER NOT NULL, PRIMARY KEY(cell_x,cell_y,id)) WITHOUT ROWID")
        database.execSQL("CREATE INDEX spatial_cells_by_y ON spatial_cells(cell_y,cell_x)")
        database.execSQL("CREATE INDEX feature_bounds_district_name ON feature_bounds(district_name)")
    }

    private fun buildGeoPackageSpatialIndex(
        sourceDatabase: SQLiteDatabase,
        source: OfflinePolygonSource,
        indexDatabase: SQLiteDatabase
    ) {
        initializeCellIndex(indexDatabase)
        val boundsInsert = indexDatabase.compileStatement(
            "INSERT INTO feature_bounds(id,minx,miny,maxx,maxy,district_name) VALUES(?,?,?,?,?,?)"
        )
        val cellInsert = indexDatabase.compileStatement("INSERT OR IGNORE INTO spatial_cells(cell_x,cell_y,id) VALUES(?,?,?)")
        val geometryInsert = indexDatabase.compileStatement("INSERT INTO normalized_geometries(id,geom) VALUES(?,?)")
        var count = 0
        indexDatabase.beginTransaction()
        try {
            val districtQuery = source.districtFieldName?.let { field -> "SELECT fid, geom, CAST($field AS TEXT) FROM \"${source.tableName}\" WHERE geom IS NOT NULL" }
                ?: "SELECT fid, geom, NULL FROM \"${source.tableName}\" WHERE geom IS NOT NULL"
            sourceDatabase.rawQuery(districtQuery, null).use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val geometryBlob = cursor.getBlob(1)
                    val districtName = if (cursor.isNull(2)) null else cursor.getString(2)
                    val geometry = OfflineGeometryCodec.fromGeoPackage(geometryBlob).let {
                        if (source.normalizeGeographicCoordinates) OfflineGeometryCodec.transformToTwd97(it) else it
                    }
                    val bounds = OfflineGeometryCodec.bounds(geometry)
                    insertBounds(boundsInsert, id, bounds, districtName)
                    insertCells(cellInsert, id, bounds)
                    if (source.normalizeGeographicCoordinates) {
                        geometryInsert.clearBindings()
                        geometryInsert.bindLong(1, id)
                        geometryInsert.bindBlob(2, OfflineGeometryCodec.fromGeoJsonMultiPolygon(geometry.polygons))
                        geometryInsert.executeInsert()
                    }
                    count++
                }
            }
            check(count > 0) { "GeoPackage contains no polygon rows" }
            recordIndexMetadata(indexDatabase, source, count)
            indexDatabase.setTransactionSuccessful()
            Log.i(TAG, "Built $count GeoPackage cell-index entries")
        } finally {
            indexDatabase.endTransaction()
            boundsInsert.close()
            cellInsert.close()
            geometryInsert.close()
        }
    }

    private fun insertBounds(statement: SQLiteStatement, id: Long, bounds: DoubleArray, districtName: String?) {
        statement.clearBindings()
        statement.bindLong(1, id)
        for (index in bounds.indices) statement.bindDouble(index + 2, bounds[index])
        if (districtName == null) statement.bindNull(6) else statement.bindString(6, districtName)
        statement.executeInsert()
    }

    private fun insertCells(statement: SQLiteStatement, id: Long, bounds: DoubleArray) {
        val minX = cellCoordinate(bounds[0])
        val minY = cellCoordinate(bounds[1])
        val maxX = cellCoordinate(bounds[2])
        val maxY = cellCoordinate(bounds[3])
        check(maxX - minX <= 100 && maxY - minY <= 100) { "Geometry spans too many spatial cells" }
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                statement.clearBindings()
                statement.bindLong(1, x.toLong())
                statement.bindLong(2, y.toLong())
                statement.bindLong(3, id)
                statement.executeInsert()
            }
        }
    }

    private fun cellCoordinate(meters: Double): Int = floor(meters / CELL_SIZE_METERS).toInt()

    private fun recordIndexMetadata(database: SQLiteDatabase, source: OfflinePolygonSource, count: Int) {
        database.execSQL("INSERT INTO metadata(key,value) VALUES('index_version', '4')")
        database.execSQL("INSERT INTO metadata(key,value) VALUES('source_sha256', ?)", arrayOf(source.expectedSha256 ?: "unknown"))
        database.execSQL("INSERT INTO metadata(key,value) VALUES('feature_count', ?)", arrayOf(count.toString()))
        database.execSQL("INSERT INTO metadata(key,value) VALUES('cell_size_meters', ?)", arrayOf(CELL_SIZE_METERS.toString()))
    }

    private fun queryCellIds(database: SQLiteDatabase, bounds: DoubleArray): List<Long> {
        val minX = cellCoordinate(bounds[0])
        val minY = cellCoordinate(bounds[1])
        val maxX = cellCoordinate(bounds[2])
        val maxY = cellCoordinate(bounds[3])
        val args = arrayOf(minX.toString(), maxX.toString(), minY.toString(), maxY.toString())
        return database.rawQuery(
            "SELECT DISTINCT b.id FROM spatial_cells s JOIN feature_bounds b ON b.id = s.id " +
                "WHERE s.cell_x BETWEEN ? AND ? AND s.cell_y BETWEEN ? AND ? " +
                "AND b.minx <= ? AND b.maxx >= ? AND b.miny <= ? AND b.maxy >= ?",
            args + arrayOf(bounds[2].toString(), bounds[0].toString(), bounds[3].toString(), bounds[1].toString())
        ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getLong(0)) } }
    }

    private fun queryDistrictNames(database: SQLiteDatabase, ids: List<Long>, source: OfflinePolygonSource): Map<Long, String?> {
        if (source.districtFieldName == null || ids.isEmpty()) return emptyMap()
        val results = mutableMapOf<Long, String?>()
        ids.chunked(500).forEach { batch ->
            val placeholders = batch.joinToString(",") { "?" }
            val args = batch.map(Long::toString).toTypedArray()
            database.rawQuery(
                "SELECT id, district_name FROM feature_bounds WHERE id IN ($placeholders)",
                args
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    results[cursor.getLong(0)] = if (cursor.isNull(1)) null else cursor.getString(1)
                }
            }
        }
        return results
    }

    private class GeoPackageDataSource(
        private val source: SQLiteDatabase,
        private val index: SQLiteDatabase,
        private val sourceDefinition: OfflinePolygonSource
    ) : OfflinePolygonDataSource {
        override fun close() {
            index.close()
            source.close()
        }

        override fun query(bounds: DoubleArray, selectedDistricts: Set<String>): List<ByteArray> {
            if (sourceDefinition.districtFieldName != null && selectedDistricts.isEmpty()) return emptyList()
            val ids = queryCellIds(index, bounds)
            if (ids.isEmpty()) return emptyList()
            val districts = queryDistrictNames(index, ids, sourceDefinition)
            val filteredIds = if (sourceDefinition.districtFieldName == null) ids else ids.filter { id ->
                val districtName = districts[id] ?: return@filter false
                selectedDistricts.contains(districtName)
            }
            if (filteredIds.isEmpty()) return emptyList()
            val results = ArrayList<ByteArray>(filteredIds.size)
            filteredIds.chunked(500).forEach { batch ->
                val placeholders = batch.joinToString(",") { "?" }
                val args = batch.map(Long::toString).toTypedArray()
                val query = if (sourceDefinition.normalizeGeographicCoordinates) {
                    "SELECT geom FROM normalized_geometries WHERE id IN ($placeholders)"
                } else {
                    "SELECT geom FROM \"${sourceDefinition.tableName}\" WHERE fid IN ($placeholders)"
                }
                val database = if (sourceDefinition.normalizeGeographicCoordinates) index else source
                database.rawQuery(query, args).use { cursor ->
                    while (cursor.moveToNext()) results += cursor.getBlob(0)
                }
            }
            return results
        }
    }

    private fun File.readTextOrNull(): String? = if (isFile) runCatching { readText() }.getOrNull() else null
}
