package com.example.taoyuangutter.common

import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import okhttp3.OkHttpClient
import okhttp3.Request

/** Downloads image tiles through the shared backend client on Maps' worker thread. */
open class HttpTileProvider(
    private val urlBuilder: (x: Int, y: Int, zoom: Int) -> String,
    private val tileSize: Int = 256,
    private val client: OkHttpClient = BackendHttpClient.instance
) : TileProvider {
    override fun getTile(x: Int, y: Int, zoom: Int): Tile? = runCatching {
        val request = Request.Builder()
            .url(urlBuilder(x, y, zoom))
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@use null
            }
            val bytes = response.body?.bytes()?.takeIf { it.isNotEmpty() } ?: return@use null
            Tile(tileSize, tileSize, bytes)
        }
    }.getOrNull()
}
