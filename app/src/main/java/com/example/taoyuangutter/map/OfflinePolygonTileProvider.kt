package com.example.taoyuangutter.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.LruCache
import android.util.Log
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.TileProvider
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ExecutionException

internal class OfflinePolygonTileProvider(
    context: Context,
    private val layer: OfflinePolygonLayer
) : TileProvider {
    private data class TileKey(val x: Int, val y: Int, val zoom: Int)

    private val sourceFuture = OfflinePolygonDataStore.load(context, layer)
    private val tileCache = object : LruCache<TileKey, Tile>(MAX_CACHED_TILES) {}
    private val cacheLock = Any()
    private val geometryFailureLogged = AtomicBoolean(false)
    @Volatile private var released = false

    override fun getTile(x: Int, y: Int, zoom: Int): Tile? {
        if (released || zoom !in 0..30) return NO_TILE
        val count = 1L shl zoom
        if (x < 0 || y < 0 || x.toLong() >= count || y.toLong() >= count) return NO_TILE
        val key = TileKey(x, y, zoom)
        synchronized(cacheLock) {
            if (released) return NO_TILE
            tileCache.get(key)?.let { return it }
        }

        return try {
            val bounds = WebMercatorTileMath.bounds3826(x, y, zoom)
            val source = sourceFuture.get()
            if (released) return NO_TILE
            val features = source.query(bounds)
            if (features.isEmpty()) return NO_TILE
            val rendered = render(features, x, y, zoom)
            synchronized(cacheLock) {
                if (released) return NO_TILE
                tileCache.put(key, rendered)
                rendered
            }
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            NO_TILE
        } catch (failure: ExecutionException) {
            Log.e(TAG, "Unable to load local ${layer.name} data", failure.cause ?: failure)
            NO_TILE
        } catch (failure: Exception) {
            Log.e(TAG, "Unable to render local ${layer.name} tile $zoom/$x/$y", failure)
            NO_TILE
        }
    }

    fun release() {
        synchronized(cacheLock) {
            released = true
            tileCache.evictAll()
        }
    }

    private fun render(features: List<ByteArray>, x: Int, y: Int, zoom: Int): Tile {
        val bitmap = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = layer.color
            style = Paint.Style.FILL
            strokeWidth = 0f
        }
        features.forEach { blob ->
            val geometry = try {
                if (layer == OfflinePolygonLayer.LEGACY) OfflineGeometryCodec.fromGeoPackage(blob)
                else OfflineGeometryCodec.fromWkb(blob)
            } catch (failure: Exception) {
                if (geometryFailureLogged.compareAndSet(false, true)) {
                    Log.e(TAG, "Skipping an unreadable ${layer.name} polygon geometry", failure)
                }
                return@forEach
            }
            geometry.polygons.forEach { polygon ->
                val path = Path().apply { fillType = Path.FillType.EVEN_ODD }
                polygon.rings.forEach { ring ->
                    if (ring.isEmpty()) return@forEach
                    val first = WebMercatorTileMath.tilePixel(ring[0].x, ring[0].y, x, y, zoom)
                    path.moveTo(first.x.toFloat(), first.y.toFloat())
                    for (index in 1 until ring.size) {
                        val point = WebMercatorTileMath.tilePixel(ring[index].x, ring[index].y, x, y, zoom)
                        path.lineTo(point.x.toFloat(), point.y.toFloat())
                    }
                    path.close()
                }
                canvas.drawPath(path, paint)
            }
        }
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        bitmap.recycle()
        return Tile(TILE_SIZE, TILE_SIZE, stream.toByteArray())
    }

    private companion object {
        const val TAG = "OfflinePolygonTile"
        const val TILE_SIZE = WebMercatorTileMath.TILE_SIZE
        const val MAX_CACHED_TILES = 48
        val NO_TILE = TileProvider.NO_TILE
    }
}

internal class OfflinePolygonOverlayManager(
    context: Context,
    private val mapProvider: () -> GoogleMap?
) {
    private val appContext = context.applicationContext
    private var legacyProvider: OfflinePolygonTileProvider? = null
    private var possibleProvider: OfflinePolygonTileProvider? = null
    private var legacyOverlay: TileOverlay? = null
    private var possibleOverlay: TileOverlay? = null

    fun setVisibility(showLegacy: Boolean, showPossible: Boolean) {
        setLegacyVisible(showLegacy)
        setPossibleVisible(showPossible)
    }

    fun release() {
        setLegacyVisible(false)
        setPossibleVisible(false)
    }

    private fun setLegacyVisible(visible: Boolean) {
        if (visible) {
            if (legacyOverlay == null) {
                val provider = OfflinePolygonTileProvider(appContext, OfflinePolygonLayer.LEGACY)
                legacyProvider = provider
                legacyOverlay = mapProvider()?.addTileOverlay(
                    TileOverlayOptions().tileProvider(provider).zIndex(0.1f).transparency(0f)
                )
                if (legacyOverlay == null) {
                    provider.release()
                    legacyProvider = null
                }
            }
        } else {
            legacyOverlay?.remove()
            legacyOverlay = null
            legacyProvider?.release()
            legacyProvider = null
        }
    }

    private fun setPossibleVisible(visible: Boolean) {
        if (visible) {
            if (possibleOverlay == null) {
                val provider = OfflinePolygonTileProvider(appContext, OfflinePolygonLayer.POSSIBLE)
                possibleProvider = provider
                possibleOverlay = mapProvider()?.addTileOverlay(
                    TileOverlayOptions().tileProvider(provider).zIndex(0f).transparency(0f)
                )
                if (possibleOverlay == null) {
                    provider.release()
                    possibleProvider = null
                }
            }
        } else {
            possibleOverlay?.remove()
            possibleOverlay = null
            possibleProvider?.release()
            possibleProvider = null
        }
    }
}
