package com.example.taoyuangutter.map

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.floor

internal enum class OfflinePolygonLayer(
    val assetName: String,
    val tableName: String,
    val color: Int,
    val assetSizeBytes: Long,
    val assetSha256: String
) {
    LEGACY(
        "20260824_水務局既有側溝更新v3_線轉面.gpkg", "line_buffer_polygon", OfflinePolygonColors.LEGACY_ARGB.toInt(),
        19_615_744L, "4b0df3e90dab556b3de0bd859511dee5a3af5d171255e4157e32b8d3f6e1ecaf"
    ),
    POSSIBLE(
        "20261006_0601版可能側溝位置.geojson", "possible_polygon", OfflinePolygonColors.POSSIBLE_ARGB.toInt(),
        97_531_074L, "2f08586b3de41bd1c87af0558b65be56b18de2dda64ab03cd4dd6e67dad91af8"
    )
}

internal data class IndexedGeometry(
    val blob: ByteArray,
    val bounds: DoubleArray
)

internal interface OfflinePolygonDataSource {
    fun query(bounds: DoubleArray): List<ByteArray>
}

internal object OfflinePolygonDataStore {
    private const val TAG = "OfflinePolygonData"
    private const val CELL_SIZE_METERS = 512.0
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "offline-polygon-index").apply { isDaemon = true }
    }
    private val sources = ConcurrentHashMap<String, CompletableFuture<OfflinePolygonDataSource>>()

    fun load(context: Context, layer: OfflinePolygonLayer): CompletableFuture<OfflinePolygonDataSource> {
        val appContext = context.applicationContext
        val key = "${appContext.filesDir.absolutePath}:${layer.name}"
        return sources.computeIfAbsent(key) {
            CompletableFuture.supplyAsync({
                when (layer) {
                    OfflinePolygonLayer.LEGACY -> openGeoPackage(appContext, layer)
                    OfflinePolygonLayer.POSSIBLE -> importGeoJsonIndex(appContext, layer)
                }
            }, executor)
        }
    }

    private fun openGeoPackage(context: Context, layer: OfflinePolygonLayer): OfflinePolygonDataSource {
        val dataDir = File(context.filesDir, "offline_polygon_data").apply { mkdirs() }
        val file = File(dataDir, layer.assetName)
        val versionFile = File(dataDir, "${layer.tableName}.sha256")
        if (!file.isFile || file.length() != layer.assetSizeBytes || versionFile.readTextOrNull() != layer.assetSha256) {
            val temporaryFile = File(dataDir, "${layer.assetName}.tmp")
            context.assets.open(layer.assetName).use { input ->
                temporaryFile.outputStream().use { output -> input.copyTo(output) }
            }
            check(temporaryFile.length() == layer.assetSizeBytes) { "Staged GeoPackage size does not match packaged source" }
            if (file.exists()) check(file.delete()) { "Unable to replace staged GeoPackage" }
            check(temporaryFile.renameTo(file)) { "Unable to finalize staged GeoPackage" }
            versionFile.writeText(layer.assetSha256)
        }
        val database = SQLiteDatabase.openDatabase(
            file.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
        )
        val metadata = database.rawQuery(
            "SELECT c.srs_id, g.geometry_type_name FROM gpkg_contents c " +
                "JOIN gpkg_geometry_columns g ON g.table_name = c.table_name WHERE c.table_name = ?",
            arrayOf(layer.tableName)
        ).use { cursor ->
            check(cursor.moveToFirst()) { "GeoPackage layer ${layer.tableName} is missing" }
            cursor.getInt(0) to cursor.getString(1)
        }
        check(metadata.first == 3826 && metadata.second.equals("POLYGON", ignoreCase = true)) {
            "Unsupported GeoPackage CRS/geometry: $metadata"
        }
        val indexFile = File(dataDir, "${layer.tableName}.spatial.sqlite")
        val indexDatabase = openVersionedIndex(indexFile, layer.assetSha256) { freshDatabase ->
            buildGeoPackageSpatialIndex(database, layer, freshDatabase)
        }
        return GeoPackageDataSource(database, indexDatabase, layer)
    }

    private fun importGeoJsonIndex(context: Context, layer: OfflinePolygonLayer): OfflinePolygonDataSource {
        val indexFile = File(context.filesDir, "offline_polygon_data/${layer.tableName}.sqlite")
        indexFile.parentFile?.mkdirs()
        val database = openVersionedIndex(indexFile, layer.assetSha256) { freshDatabase ->
            buildGeoJsonIndex(context, layer, freshDatabase)
        }
        return GeoJsonDataSource(database, layer)
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
        ).use { cursor -> cursor.moveToFirst() && cursor.getString(0) == "2" }
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
        database.execSQL("CREATE TABLE feature_bounds (id INTEGER PRIMARY KEY, minx REAL NOT NULL, miny REAL NOT NULL, maxx REAL NOT NULL, maxy REAL NOT NULL)")
        database.execSQL("CREATE TABLE spatial_cells (cell_x INTEGER NOT NULL, cell_y INTEGER NOT NULL, id INTEGER NOT NULL, PRIMARY KEY(cell_x,cell_y,id)) WITHOUT ROWID")
        database.execSQL("CREATE INDEX spatial_cells_by_y ON spatial_cells(cell_y,cell_x)")
    }

    private fun createGeoJsonCellIndex(database: SQLiteDatabase, layer: OfflinePolygonLayer) {
        database.execSQL("PRAGMA synchronous=OFF")
        database.execSQL("CREATE TABLE metadata (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
        database.execSQL("CREATE TABLE ${layer.tableName} (id INTEGER PRIMARY KEY, geom BLOB NOT NULL, minx REAL NOT NULL, miny REAL NOT NULL, maxx REAL NOT NULL, maxy REAL NOT NULL)")
        database.execSQL("CREATE TABLE spatial_cells (cell_x INTEGER NOT NULL, cell_y INTEGER NOT NULL, id INTEGER NOT NULL, PRIMARY KEY(cell_x,cell_y,id)) WITHOUT ROWID")
        database.execSQL("CREATE INDEX spatial_cells_by_y ON spatial_cells(cell_y,cell_x)")
    }

    private fun buildGeoPackageSpatialIndex(
        sourceDatabase: SQLiteDatabase,
        layer: OfflinePolygonLayer,
        indexDatabase: SQLiteDatabase
    ) {
        initializeCellIndex(indexDatabase)
        val boundsInsert = indexDatabase.compileStatement(
            "INSERT INTO feature_bounds(id,minx,miny,maxx,maxy) VALUES(?,?,?,?,?)"
        )
        val cellInsert = indexDatabase.compileStatement("INSERT OR IGNORE INTO spatial_cells(cell_x,cell_y,id) VALUES(?,?,?)")
        var count = 0
        indexDatabase.beginTransaction()
        try {
            sourceDatabase.rawQuery("SELECT fid, geom FROM \"${layer.tableName}\" WHERE geom IS NOT NULL", null).use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val bounds = OfflineGeometryCodec.boundsFromGeoPackage(cursor.getBlob(1))
                    insertBounds(boundsInsert, id, bounds)
                    insertCells(cellInsert, id, bounds)
                    count++
                }
            }
            check(count > 0) { "GeoPackage contains no polygon rows" }
            recordIndexMetadata(indexDatabase, layer, count)
            indexDatabase.setTransactionSuccessful()
            Log.i(TAG, "Built $count GeoPackage cell-index entries")
        } finally {
            indexDatabase.endTransaction()
            boundsInsert.close()
            cellInsert.close()
        }
    }

    private fun buildGeoJsonIndex(
        context: Context,
        layer: OfflinePolygonLayer,
        database: SQLiteDatabase
    ) {
        createGeoJsonCellIndex(database, layer)
        val featureInsert = database.compileStatement(
            "INSERT INTO ${layer.tableName}(geom,minx,miny,maxx,maxy) VALUES (?,?,?,?,?)"
        )
        val cellInsert = database.compileStatement("INSERT OR IGNORE INTO spatial_cells(cell_x,cell_y,id) VALUES(?,?,?)")
        var count = 0
        database.beginTransaction()
        try {
            context.assets.open(layer.assetName).use { input ->
                val reader = JsonReader(InputStreamReader(input, Charsets.UTF_8))
                reader.isLenient = false
                reader.beginObject()
                var validatedCrs = false
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "type" -> check(reader.nextString() == "FeatureCollection") { "GeoJSON is not a FeatureCollection" }
                        "crs" -> {
                            val crs = JsonParser.parseReader(reader).asJsonObject
                            val name = crs.getAsJsonObject("properties")?.get("name")?.asString.orEmpty()
                            check(name.contains("3826")) { "Unsupported GeoJSON CRS: $name" }
                            validatedCrs = true
                        }
                        "features" -> {
                            check(validatedCrs) { "GeoJSON CRS must be declared before features" }
                            reader.beginArray()
                            while (reader.hasNext()) {
                                val geometry = readFeatureGeometry(reader) ?: continue
                                val blob = OfflineGeometryCodec.fromGeoJsonMultiPolygon(geometry.polygons)
                                val bounds = calculateBounds(geometry)
                                featureInsert.clearBindings()
                                featureInsert.bindBlob(1, blob)
                                featureInsert.bindDouble(2, bounds[0])
                                featureInsert.bindDouble(3, bounds[1])
                                featureInsert.bindDouble(4, bounds[2])
                                featureInsert.bindDouble(5, bounds[3])
                                val id = featureInsert.executeInsert()
                                insertCells(cellInsert, id, bounds)
                                count++
                            }
                            reader.endArray()
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
                reader.close()
            }
            check(count > 0) { "GeoJSON has no supported polygon features" }
            recordIndexMetadata(database, layer, count)
            database.setTransactionSuccessful()
            Log.i(TAG, "Indexed $count GeoJSON polygons")
        } finally {
            database.endTransaction()
            featureInsert.close()
            cellInsert.close()
        }
    }

    private fun insertBounds(statement: SQLiteStatement, id: Long, bounds: DoubleArray) {
        statement.clearBindings()
        statement.bindLong(1, id)
        for (index in bounds.indices) statement.bindDouble(index + 2, bounds[index])
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

    private fun recordIndexMetadata(database: SQLiteDatabase, layer: OfflinePolygonLayer, count: Int) {
        database.execSQL("INSERT INTO metadata(key,value) VALUES('index_version', '2')")
        database.execSQL("INSERT INTO metadata(key,value) VALUES('source_sha256', ?)", arrayOf(layer.assetSha256))
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

    private class GeoPackageDataSource(
        private val source: SQLiteDatabase,
        private val index: SQLiteDatabase,
        private val layer: OfflinePolygonLayer
    ) : OfflinePolygonDataSource {
        override fun query(bounds: DoubleArray): List<ByteArray> {
            val ids = queryCellIds(index, bounds)
            if (ids.isEmpty()) return emptyList()
            val results = ArrayList<ByteArray>(ids.size)
            ids.chunked(500).forEach { batch ->
                val placeholders = batch.joinToString(",") { "?" }
                val args = batch.map(Long::toString).toTypedArray()
                source.rawQuery("SELECT geom FROM \"${layer.tableName}\" WHERE fid IN ($placeholders)", args).use { cursor ->
                    while (cursor.moveToNext()) results += cursor.getBlob(0)
                }
            }
            return results
        }
    }

    private class GeoJsonDataSource(
        private val database: SQLiteDatabase,
        private val layer: OfflinePolygonLayer
    ) : OfflinePolygonDataSource {
        override fun query(bounds: DoubleArray): List<ByteArray> {
            val minX = cellCoordinate(bounds[0])
            val minY = cellCoordinate(bounds[1])
            val maxX = cellCoordinate(bounds[2])
            val maxY = cellCoordinate(bounds[3])
            val args = arrayOf(
                minX.toString(), maxX.toString(), minY.toString(), maxY.toString(),
                bounds[2].toString(), bounds[0].toString(), bounds[3].toString(), bounds[1].toString()
            )
            return database.rawQuery(
                "SELECT DISTINCT f.geom FROM spatial_cells s JOIN ${layer.tableName} f ON f.id = s.id " +
                    "WHERE s.cell_x BETWEEN ? AND ? AND s.cell_y BETWEEN ? AND ? " +
                    "AND f.minx <= ? AND f.maxx >= ? AND f.miny <= ? AND f.maxy >= ?",
                args
            ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getBlob(0)) } }
        }
    }

    private fun readFeatureGeometry(reader: JsonReader): OfflineGeometry? {
        var geometry: JsonObject? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "geometry" -> {
                    if (reader.peek() == com.google.gson.stream.JsonToken.NULL) reader.nextNull()
                    else geometry = JsonParser.parseReader(reader).asJsonObject
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        val value = geometry ?: return null
        val type = value.get("type")?.asString ?: return null
        val coordinates = value.getAsJsonArray("coordinates") ?: return null
        return when (type) {
            "Polygon" -> OfflineGeometry(listOf(parsePolygon(coordinates)))
            "MultiPolygon" -> OfflineGeometry(coordinates.map { parsePolygon(it.asJsonArray) })
            else -> null
        }
    }

    private fun parsePolygon(coordinates: JsonArray): OfflinePolygon = OfflinePolygon(
        coordinates.map { ringElement ->
            ringElement.asJsonArray.map { pointElement ->
                val point = pointElement.asJsonArray
                OfflineCoordinate(point[0].asDouble, point[1].asDouble)
            }
        }
    )

    private fun calculateBounds(geometry: OfflineGeometry): DoubleArray {
        val coordinates = geometry.polygons.flatMap { it.rings }.flatten()
        check(coordinates.isNotEmpty()) { "Polygon has no coordinates" }
        return doubleArrayOf(
            coordinates.minOf { it.x }, coordinates.minOf { it.y },
            coordinates.maxOf { it.x }, coordinates.maxOf { it.y }
        )
    }

    private fun File.readTextOrNull(): String? = if (isFile) runCatching { readText() }.getOrNull() else null
}
