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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicBoolean

internal class OfflinePolygonTileProvider(
    context: Context,
    private val layer: OfflinePolygonLayer,
    sourceDefinitions: List<OfflinePolygonSource> = listOf(OfflinePolygonSourceCatalog.full(layer)),
    private val outlineWidthPx: Float? = null,
    private val selectedDistrictsProvider: () -> Set<String> = { MapOverlayController.DEFAULT_DISTRICTS },
    private val generationProvider: () -> Long = { 0L }
) : TileProvider {
    private data class TileKey(val x: Int, val y: Int, val zoom: Int, val generation: Long)

    private val appContext = context.applicationContext
    private val sourceFutures = sourceDefinitions.map { source ->
        source to OfflinePolygonDataStore.load(appContext, source)
    }
    private val fallbackFutures = ConcurrentHashMap<String, CompletableFuture<OfflinePolygonDataSource>>()
    private val fallbackSources = ConcurrentHashMap<String, OfflinePolygonSource>()
    private val tileCache = object : LruCache<TileKey, Tile>(MAX_CACHED_TILES) {}
    private val cacheLock = Any()
    private val geometryFailureLogged = AtomicBoolean(false)
    @Volatile private var released = false

    override fun getTile(x: Int, y: Int, zoom: Int): Tile? {
        if (released || (zoom !in 0..30)) return NO_TILE
        val count = 1L shl zoom
        if (x < 0 || y < 0 || x.toLong() >= count || y.toLong() >= count) return NO_TILE
        val generation = generationProvider()
        val key = TileKey(x, y, zoom, generation)
        synchronized(cacheLock) {
            if (released) return NO_TILE
            tileCache.get(key)?.let { return it }
        }

        return try {
            val bounds = WebMercatorTileMath.bounds3826(x, y, zoom)
            if (released || generation != generationProvider()) return NO_TILE
            val features = sourceFutures.flatMap { (definition, future) ->
                val source = runCatching { future.get() }.getOrElse { failure ->
                    val fallback = definition.fallback ?: return@flatMap emptyList()
                    val fallbackFuture = fallbackFuture(fallback)
                    runCatching { fallbackFuture.get() }
                        .getOrElse { fallbackFailure ->
                            Log.e(TAG, "Unable to load local ${layer.name} source ${definition.assetName}", fallbackFailure)
                            return@flatMap emptyList()
                        }
                }
                val selectedDistricts = if (definition.queryDistricts == MapOverlayController.DEFAULT_DISTRICTS) {
                    selectedDistrictsProvider()
                } else {
                    definition.queryDistricts
                }
                source.query(bounds, selectedDistricts)
            }
            if (released || generation != generationProvider()) return NO_TILE
            if (features.isEmpty()) return NO_TILE
            val rendered = render(
                features,
                x,
                y,
                zoom,
                outlineWidthPx ?: if (layer == OfflinePolygonLayer.REGION) OfflinePolygonStyle.REGION_STROKE_WIDTH_PX
                else OfflinePolygonStyle.outlineWidthPxAtZoom(zoom)
            )
            synchronized(cacheLock) {
                if (released || generation != generationProvider()) return NO_TILE
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

    fun whenSourcesReady(): CompletableFuture<Unit> {
        val readiness = sourceFutures.map { (definition, future) ->
            future.handle { _, failure ->
                if (released) return@handle Unit
                if (failure != null && definition.fallback != null) fallbackFuture(definition.fallback).get()
                Unit
            }
        }
        return CompletableFuture.allOf(*readiness.toTypedArray()).thenApply { Unit }
    }

    private fun fallbackFuture(source: OfflinePolygonSource): CompletableFuture<OfflinePolygonDataSource> =
        fallbackFutures.computeIfAbsent(source.assetName) {
            fallbackSources[source.assetName] = source
            OfflinePolygonDataStore.load(appContext, source)
        }

    fun release() {
        synchronized(cacheLock) {
            if (released) return
            released = true
            tileCache.evictAll()
        }
        sourceFutures.forEach { (source, _) -> OfflinePolygonDataStore.release(appContext, source) }
        fallbackSources.values.forEach { OfflinePolygonDataStore.release(appContext, it) }
    }

    private fun render(features: List<ByteArray>, x: Int, y: Int, zoom: Int, outlineWidthPx: Float): Tile {
        val bitmap = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = layer.color
            style = Paint.Style.FILL
        }
        val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = layer.color
            style = Paint.Style.STROKE
            strokeWidth = outlineWidthPx
        }
        features.forEach { blob ->
            val geometry = try {
                if (layer.normalizeGeographicCoordinates) OfflineGeometryCodec.fromWkb(blob)
                else OfflineGeometryCodec.fromGeoPackage(blob)
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
                if (layer != OfflinePolygonLayer.REGION) canvas.drawPath(path, paint)
                if (outlineWidthPx > 0f) canvas.drawPath(path, outlinePaint)
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

internal object OfflinePolygonStyle {
    const val REGION_STROKE_WIDTH_PX = 2.5f
    const val BROAD_SCALE_REFERENCE_ZOOM = 16
    const val CLOSE_SCALE_REFERENCE_ZOOM = 20
    const val BROAD_SCALE_STROKE_WIDTH_PX = 2.5f
    const val CLOSE_SCALE_STROKE_WIDTH_PX = 1.5f

    fun outlineWidthPxAtZoom(
        zoom: Int,
        broadScaleWidthPx: Float = BROAD_SCALE_STROKE_WIDTH_PX,
        closeScaleWidthPx: Float = CLOSE_SCALE_STROKE_WIDTH_PX,
        broadScaleZoom: Int = BROAD_SCALE_REFERENCE_ZOOM,
        closeScaleZoom: Int = CLOSE_SCALE_REFERENCE_ZOOM
    ): Float {
        require(closeScaleZoom > broadScaleZoom) { "Close-scale zoom must be greater than broad-scale zoom" }
        if (zoom <= broadScaleZoom) return broadScaleWidthPx
        if (zoom >= closeScaleZoom) return closeScaleWidthPx
        val progress = (zoom - broadScaleZoom).toFloat() / (closeScaleZoom - broadScaleZoom)
        return broadScaleWidthPx + (closeScaleWidthPx - broadScaleWidthPx) * progress
    }
}

internal class OfflinePolygonOverlayManager(
    context: Context,
    private val mapProvider: () -> GoogleMap?,
    private val generationProvider: () -> Long = { 0L }
) {
    constructor(context: Context, mapProvider: () -> GoogleMap?) : this(context, mapProvider, { 0L })

    private val appContext = context.applicationContext
    private var legacyProvider: OfflinePolygonTileProvider? = null
    private var possibleProvider: OfflinePolygonTileProvider? = null
    private var regionProvider: OfflinePolygonTileProvider? = null
    private var legacyOverlay: TileOverlay? = null
    private var possibleOverlay: TileOverlay? = null
    private var regionOverlay: TileOverlay? = null
    private var activeDistricts: Set<String>? = null
    private var activeGeneration: Long? = null
    private var pendingCandidate: Candidate? = null
    private var pendingRequestId = 0L

    private data class Candidate(
        val legacyProvider: OfflinePolygonTileProvider?,
        val legacyOverlay: TileOverlay?,
        val possibleProvider: OfflinePolygonTileProvider?,
        val possibleOverlay: TileOverlay?,
        val regionProvider: OfflinePolygonTileProvider?,
        val regionOverlay: TileOverlay?
    ) {
        fun providers(): List<OfflinePolygonTileProvider> = listOfNotNull(
            legacyProvider, possibleProvider, regionProvider
        )

        fun release() {
            legacyOverlay?.remove()
            possibleOverlay?.remove()
            regionOverlay?.remove()
            providers().forEach { it.release() }
        }
    }

    fun setVisibility(showLegacy: Boolean, showPossible: Boolean, showRegion: Boolean) {
        setVisibility(
            showLegacy = showLegacy,
            showPossible = showPossible,
            showRegion = showRegion,
            selectedDistricts = MapOverlayController.DEFAULT_DISTRICTS,
            generation = generationProvider()
        )
    }

    fun setVisibility(
        showLegacy: Boolean,
        showPossible: Boolean,
        showRegion: Boolean = false,
        selectedDistricts: Set<String> = MapOverlayController.DEFAULT_DISTRICTS,
        generation: Long = generationProvider()
    ) {
        if (activeDistricts != selectedDistricts || activeGeneration != generation) {
            stageVisibility(showLegacy, showPossible, showRegion, selectedDistricts, generation)
            return
        }
        setLegacyVisible(showLegacy, selectedDistricts, generation)
        setPossibleVisible(showPossible, selectedDistricts, generation)
        setRegionVisible(showRegion, selectedDistricts, generation)
    }

    fun release() {
        pendingRequestId++
        pendingCandidate?.release()
        pendingCandidate = null
        clearOverlays()
        activeDistricts = null
        activeGeneration = null
    }

    private fun stageVisibility(
        showLegacy: Boolean,
        showPossible: Boolean,
        showRegion: Boolean,
        selectedDistricts: Set<String>,
        generation: Long
    ) {
        pendingRequestId++
        val requestId = pendingRequestId
        pendingCandidate?.release()
        val candidate = Candidate(
            createProvider(showLegacy, OfflinePolygonLayer.LEGACY, selectedDistricts, generation),
            null,
            createProvider(showPossible, OfflinePolygonLayer.POSSIBLE, selectedDistricts, generation),
            null,
            createProvider(showRegion, OfflinePolygonLayer.REGION, selectedDistricts, generation),
            null
        )
        val candidateWithOverlays = candidate.copy(
            legacyOverlay = candidate.legacyProvider?.let { addOverlay(it, 0.1f) },
            possibleOverlay = candidate.possibleProvider?.let { addOverlay(it, 0f) },
            regionOverlay = candidate.regionProvider?.let { addOverlay(it, -0.5f) }
        )
        pendingCandidate = candidateWithOverlays
        CompletableFuture.allOf(*candidateWithOverlays.providers().map { it.whenSourcesReady() }.toTypedArray())
            .whenComplete { _, _ ->
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (requestId != pendingRequestId || pendingCandidate !== candidateWithOverlays) {
                        candidateWithOverlays.release()
                        return@post
                    }
                    clearOverlays()
                    legacyProvider = candidateWithOverlays.legacyProvider
                    legacyOverlay = candidateWithOverlays.legacyOverlay
                    possibleProvider = candidateWithOverlays.possibleProvider
                    possibleOverlay = candidateWithOverlays.possibleOverlay
                    regionProvider = candidateWithOverlays.regionProvider
                    regionOverlay = candidateWithOverlays.regionOverlay
                    activeDistricts = selectedDistricts
                    activeGeneration = generation
                    pendingCandidate = null
                }
            }
    }

    private fun createProvider(
        visible: Boolean,
        layer: OfflinePolygonLayer,
        selectedDistricts: Set<String>,
        generation: Long
    ): OfflinePolygonTileProvider? = if (visible) OfflinePolygonTileProvider(
        appContext,
        layer,
        sourceDefinitions = if (layer == OfflinePolygonLayer.REGION) {
            listOf(OfflinePolygonSourceCatalog.full(layer))
        } else {
            sourceDefinitions(layer, selectedDistricts)
        },
        selectedDistrictsProvider = { selectedDistricts },
        generationProvider = { generation }
    ) else null

    private fun addOverlay(provider: OfflinePolygonTileProvider, zIndex: Float): TileOverlay? =
        mapProvider()?.addTileOverlay(
            TileOverlayOptions().tileProvider(provider).zIndex(zIndex).transparency(0f)
        )

    private fun clearOverlays() {
        legacyOverlay?.remove()
        legacyOverlay = null
        legacyProvider?.release()
        legacyProvider = null
        possibleOverlay?.remove()
        possibleOverlay = null
        possibleProvider?.release()
        possibleProvider = null
        regionOverlay?.remove()
        regionOverlay = null
        regionProvider?.release()
        regionProvider = null
    }

    private fun setLegacyVisible(visible: Boolean, selectedDistricts: Set<String>, generation: Long) {
        if (visible) {
            if (legacyOverlay == null) {
                val provider = OfflinePolygonTileProvider(
                    appContext,
                    OfflinePolygonLayer.LEGACY,
                    sourceDefinitions = sourceDefinitions(OfflinePolygonLayer.LEGACY, selectedDistricts),
                    selectedDistrictsProvider = { selectedDistricts },
                    generationProvider = { generation }
                )
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

    private fun setPossibleVisible(visible: Boolean, selectedDistricts: Set<String>, generation: Long) {
        if (visible) {
            if (possibleOverlay == null) {
                val provider = OfflinePolygonTileProvider(
                    appContext,
                    OfflinePolygonLayer.POSSIBLE,
                    sourceDefinitions = sourceDefinitions(OfflinePolygonLayer.POSSIBLE, selectedDistricts),
                    selectedDistrictsProvider = { selectedDistricts },
                    generationProvider = { generation }
                )
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

    private fun setRegionVisible(visible: Boolean, selectedDistricts: Set<String>, generation: Long) {
        if (visible) {
            if (regionOverlay == null) {
                val provider = OfflinePolygonTileProvider(
                    appContext,
                    OfflinePolygonLayer.REGION,
                    sourceDefinitions = listOf(OfflinePolygonSourceCatalog.full(OfflinePolygonLayer.REGION)),
                    selectedDistrictsProvider = { selectedDistricts },
                    generationProvider = { generation }
                )
                regionProvider = provider
                regionOverlay = mapProvider()?.addTileOverlay(
                    TileOverlayOptions().tileProvider(provider).zIndex(-0.5f).transparency(0f)
                )
                if (regionOverlay == null) {
                    provider.release()
                    regionProvider = null
                }
            }
        } else {
            regionOverlay?.remove()
            regionOverlay = null
            regionProvider?.release()
            regionProvider = null
        }
    }

    private fun sourceDefinitions(layer: OfflinePolygonLayer, selectedDistricts: Set<String>): List<OfflinePolygonSource> =
        if (selectedDistricts == MapOverlayController.DEFAULT_DISTRICTS) {
            listOf(OfflinePolygonSourceCatalog.full(layer))
        } else {
            selectedDistricts.sorted().map { district -> OfflinePolygonSourceCatalog.district(layer, district) }
        }
}
