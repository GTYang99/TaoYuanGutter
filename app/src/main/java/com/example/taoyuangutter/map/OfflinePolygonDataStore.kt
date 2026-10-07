package com.example.taoyuangutter.map

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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
        val rtreeName = "rtree_${layer.tableName}_geom"
        return object : OfflinePolygonDataSource {
            override fun query(bounds: DoubleArray): List<ByteArray> {
                val sql = "SELECT f.geom FROM \"${layer.tableName}\" f " +
                    "JOIN \"$rtreeName\" r ON f.fid = r.id " +
                    "WHERE r.minx <= ? AND r.maxx >= ? AND r.miny <= ? AND r.maxy >= ?"
                return queryBlobs(database, sql, bounds)
            }
        }
    }

    private fun importGeoJsonIndex(context: Context, layer: OfflinePolygonLayer): OfflinePolygonDataSource {
        val indexFile = File(context.filesDir, "offline_polygon_data/${layer.tableName}.sqlite")
        indexFile.parentFile?.mkdirs()
        val database = SQLiteDatabase.openOrCreateDatabase(indexFile, null)
        val hasMetadataTable = database.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'metadata'",
            null
        ).use { it.moveToFirst() }
        val isCurrent = hasMetadataTable && database.rawQuery(
            "SELECT value FROM metadata WHERE key = 'source_sha256'",
            null
        ).use { cursor -> cursor.moveToFirst() && cursor.getString(0) == layer.assetSha256 }
        if (!isCurrent) {
            database.close()
            indexFile.delete()
            val freshDatabase = SQLiteDatabase.openOrCreateDatabase(indexFile, null)
            try {
                buildGeoJsonIndex(context, layer, freshDatabase)
            } catch (failure: Throwable) {
                freshDatabase.close()
                indexFile.delete()
                throw failure
            }
            return sqliteDataSource(freshDatabase, layer)
        }
        return sqliteDataSource(database, layer)
    }

    private fun buildGeoJsonIndex(
        context: Context,
        layer: OfflinePolygonLayer,
        database: SQLiteDatabase
    ) {
        database.execSQL("PRAGMA journal_mode=OFF")
        database.execSQL("PRAGMA synchronous=OFF")
        database.execSQL("CREATE TABLE metadata (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
        database.execSQL("CREATE TABLE ${layer.tableName} (id INTEGER PRIMARY KEY, geom BLOB NOT NULL)")
        database.execSQL("CREATE VIRTUAL TABLE rtree_${layer.tableName}_geom USING rtree(id,minx,maxx,miny,maxy)")
        val featureInsert = database.compileStatement("INSERT INTO ${layer.tableName}(geom) VALUES (?)")
        val boundsInsert = database.compileStatement(
            "INSERT INTO rtree_${layer.tableName}_geom(id,minx,maxx,miny,maxy) VALUES (?,?,?,?,?)"
        )
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
                                val id = featureInsert.executeInsert()
                                boundsInsert.clearBindings()
                                boundsInsert.bindLong(1, id)
                                boundsInsert.bindDouble(2, bounds[0])
                                boundsInsert.bindDouble(3, bounds[2])
                                boundsInsert.bindDouble(4, bounds[1])
                                boundsInsert.bindDouble(5, bounds[3])
                                boundsInsert.executeInsert()
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
            database.execSQL("INSERT INTO metadata(key,value) VALUES('source_sha256',?)", arrayOf(layer.assetSha256))
            database.execSQL("INSERT INTO metadata(key,value) VALUES('feature_count',?)", arrayOf(count.toString()))
            database.setTransactionSuccessful()
            Log.i(TAG, "Indexed $count GeoJSON polygons")
        } finally {
            database.endTransaction()
            featureInsert.close()
            boundsInsert.close()
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

    private fun sqliteDataSource(database: SQLiteDatabase, layer: OfflinePolygonLayer): OfflinePolygonDataSource {
        return object : OfflinePolygonDataSource {
            override fun query(bounds: DoubleArray): List<ByteArray> {
                val sql = "SELECT f.geom FROM ${layer.tableName} f " +
                    "JOIN rtree_${layer.tableName}_geom r ON f.id = r.id " +
                    "WHERE r.minx <= ? AND r.maxx >= ? AND r.miny <= ? AND r.maxy >= ?"
                return queryBlobs(database, sql, bounds)
            }
        }
    }

    private fun queryBlobs(database: SQLiteDatabase, sql: String, bounds: DoubleArray): List<ByteArray> {
        val args = arrayOf(bounds[2].toString(), bounds[0].toString(), bounds[3].toString(), bounds[1].toString())
        return database.rawQuery(sql, args).use { cursor ->
            val results = ArrayList<ByteArray>(minOf(cursor.count, 512))
            while (cursor.moveToNext()) results += cursor.getBlob(0)
            results
        }
    }

    private fun File.readTextOrNull(): String? = if (isFile) runCatching { readText() }.getOrNull() else null
}
