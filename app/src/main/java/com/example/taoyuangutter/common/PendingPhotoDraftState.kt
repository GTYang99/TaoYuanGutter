package com.example.taoyuangutter.common

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object PendingPhotoDraftState {
    private const val KEY_PREFIX = "_pending_photo_"
    private const val KEY_SUFFIX_PATH = "_path"

    fun pathKey(slot: Int): String = "$KEY_PREFIX${slot}$KEY_SUFFIX_PATH"

    fun readPath(data: Map<String, String>, slot: Int): String? =
        data[pathKey(slot)]?.takeIf { it.isNotBlank() }

    fun writePath(data: MutableMap<String, String>, slot: Int, path: String?) {
        val key = pathKey(slot)
        if (path.isNullOrBlank()) {
            data.remove(key)
        } else {
            data[key] = path
        }
    }

    fun clearAll(data: MutableMap<String, String>) {
        (1..3).forEach { writePath(data, it, null) }
    }

    fun promotePendingFilesToPhotos(context: Context, source: Map<String, String>): HashMap<String, String> {
        val out = HashMap(source)
        (1..3).forEach { slot ->
            val photoKey = "photo$slot"
            val hasPhoto = out[photoKey]?.isNotBlank() == true
            if (hasPhoto) return@forEach
            val pendingPath = readPath(out, slot) ?: return@forEach
            val file = File(pendingPath)
            if (!file.exists() || file.length() <= 0L) {
                writePath(out, slot, null)
                return@forEach
            }
            val uri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (_: Exception) {
                Uri.fromFile(file)
            }
            out[photoKey] = uri.toString()
            writePath(out, slot, null)
        }
        return out
    }
}
