package com.example.taoyuangutter.common

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object PhotoUriStore {

    private fun appFileProviderAuthority(context: Context): String =
        "${context.packageName}.fileprovider"

    private fun isHttpUri(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase(Locale.US)
        return scheme == "http" || scheme == "https"
    }

    /**
     * 是否已是 app 內部可長期保存的 URI。
     * - app FileProvider content://<pkg>.fileprovider/... 直接視為已在 app 儲存
     * - file:// 若落在 app 的 externalFilesDir(Pictures) 或 filesDir(Pictures) 也視為已儲存
     */
    fun isAppOwnedStableUri(context: Context, uri: Uri): Boolean {
        if (uri.scheme?.lowercase(Locale.US) == "content" && uri.authority == appFileProviderAuthority(context)) {
            return true
        }
        if (uri.scheme?.lowercase(Locale.US) == "file") {
            val path = uri.path ?: return false
            val extDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath
            if (!extDir.isNullOrEmpty() && path.startsWith(extDir)) return true
            val internalDir = File(context.filesDir, "Pictures").absolutePath
            if (path.startsWith(internalDir)) return true
        }
        return false
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor != null && cursor.moveToFirst()) cursor.getString(0) else null
        } catch (_: Exception) {
            null
        } finally {
            try {
                cursor?.close()
            } catch (_: Exception) {
                // ignore
            }
        }
    }

    private fun guessExtension(context: Context, uri: Uri): String {
        val fromName = queryDisplayName(context, uri)
            ?.substringAfterLast('.', "")
            ?.takeIf { it.isNotBlank() }
            ?.lowercase(Locale.US)
        if (!fromName.isNullOrEmpty() && fromName.length <= 5) return ".$fromName"

        val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull()?.lowercase(Locale.US)
        return when (mime) {
            "image/jpeg", "image/jpg" -> ".jpg"
            "image/png" -> ".png"
            "image/webp" -> ".webp"
            else -> ".jpg"
        }
    }

    private fun ensurePicturesDir(context: Context): File {
        val external = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (external != null) return external
        return File(context.filesDir, "Pictures").apply { mkdirs() }
    }

    /**
     * 將任意「非 app 自己持有」的 URI 複製到 app 的 Pictures 目錄，回傳 FileProvider URI。
     * - http/https：不複製，直接原樣回傳（屬於伺服器 URL）
     * - 其他 scheme（content/file）：若不是 app-owned stable uri，會複製
     */
    suspend fun ensureCopiedToAppPicturesIfNeeded(
        context: Context,
        uriString: String?,
        prefix: String = "GUTTER_EXT_"
    ): String? {
        if (uriString.isNullOrBlank()) return uriString
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return uriString
        if (isHttpUri(uri)) return uriString
        if (isAppOwnedStableUri(context, uri)) return uriString

        return withContext(Dispatchers.IO) {
            val extension = guessExtension(context, uri)
            val dir = ensurePicturesDir(context)
            val file = File.createTempFile(prefix, extension, dir)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            } ?: run {
                // 讀不到就回退原值，避免把草稿清空
                return@withContext uriString
            }

            val newUri = try {
                FileProvider.getUriForFile(context, appFileProviderAuthority(context), file)
            } catch (_: Exception) {
                Uri.fromFile(file)
            }
            newUri.toString()
        }
    }

    suspend fun normalizeBasicDataPhotoUris(
        context: Context,
        basicData: HashMap<String, String>,
        prefix: String = "GUTTER_EXT_"
    ): HashMap<String, String> {
        val out = HashMap(basicData)
        out["photo1"] = ensureCopiedToAppPicturesIfNeeded(context, out["photo1"], prefix) ?: ""
        out["photo2"] = ensureCopiedToAppPicturesIfNeeded(context, out["photo2"], prefix) ?: ""
        out["photo3"] = ensureCopiedToAppPicturesIfNeeded(context, out["photo3"], prefix) ?: ""
        return out
    }
}

