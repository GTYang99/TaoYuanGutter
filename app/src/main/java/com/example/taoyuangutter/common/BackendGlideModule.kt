package com.example.taoyuangutter.common

import android.content.Context
import android.net.Uri
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.Registry
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.module.GlideModule
import com.bumptech.glide.signature.ObjectKey
import java.io.IOException
import java.io.InputStream
import okhttp3.Call
import okhttp3.Request
import okhttp3.Response

/**
 * Routes remote image requests through the same temporary Taipei certificate
 * handling as API and GeoServer requests. Local content/file URIs keep Glide's
 * normal loaders.
 */
@Suppress("DEPRECATION")
class BackendGlideModule : GlideModule {
    override fun applyOptions(context: Context, builder: com.bumptech.glide.GlideBuilder) = Unit

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.prepend(
            Uri::class.java,
            InputStream::class.java,
            BackendUriLoaderFactory()
        )
    }
}

private class BackendUriLoaderFactory : ModelLoaderFactory<Uri, InputStream> {
    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<Uri, InputStream> =
        BackendUriLoader()

    override fun teardown() = Unit
}

private class BackendUriLoader : ModelLoader<Uri, InputStream> {
    override fun handles(model: Uri): Boolean =
        model.scheme.equals("http", ignoreCase = true) ||
            model.scheme.equals("https", ignoreCase = true)

    override fun buildLoadData(
        model: Uri,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<InputStream> = ModelLoader.LoadData(
        ObjectKey(model.toString()),
        BackendUriDataFetcher(model.toString())
    )
}

private class BackendUriDataFetcher(
    private val url: String
) : DataFetcher<InputStream> {
    @Volatile private var call: Call? = null
    private var response: Response? = null
    private var stream: InputStream? = null

    override fun loadData(
        priority: Priority,
        callback: DataFetcher.DataCallback<in InputStream>
    ) {
        try {
            val request = Request.Builder().url(url).get().build()
            val currentCall = BackendHttpClient.instance.newCall(request)
            call = currentCall
            val currentResponse = currentCall.execute()
            response = currentResponse
            if (!currentResponse.isSuccessful) {
                currentResponse.close()
                callback.onLoadFailed(IOException("Image request failed: HTTP ${currentResponse.code}"))
                return
            }
            val body = currentResponse.body
            if (body == null) {
                currentResponse.close()
                callback.onLoadFailed(IOException("Image response body is empty"))
                return
            }
            val loadedStream = body.byteStream()
            stream = loadedStream
            callback.onDataReady(loadedStream)
        } catch (error: Exception) {
            callback.onLoadFailed(error)
        }
    }

    override fun cleanup() {
        stream?.close()
        stream = null
        response?.close()
        response = null
        call = null
    }

    override fun cancel() {
        call?.cancel()
    }

    override fun getDataClass(): Class<InputStream> = InputStream::class.java

    override fun getDataSource(): DataSource = DataSource.REMOTE
}
