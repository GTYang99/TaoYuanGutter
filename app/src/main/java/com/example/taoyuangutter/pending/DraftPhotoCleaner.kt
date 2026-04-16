package com.example.taoyuangutter.pending

import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File

/**
 * 清理草稿中引用的本機照片檔案。
 *
 * 目標：
 * - 草稿存在期間：照片留在 app private 目錄，避免 cache 被系統回收導致「放很久照片不見」
 * - 草稿刪除或上傳成功：同步刪除本機照片，降低機敏資料殘留與空間佔用
 *
 * 僅刪除本 App 自己產生的 FileProvider URI（authority = {package}.fileprovider），
 * 且檔案必須位於 filesDir 或 getExternalFilesDir(Pictures) 之下。
 */
object DraftPhotoCleaner {

    fun deleteDraftLocalPhotos(context: Context, draft: GutterSessionDraft) {
        draft.waypoints.forEach { wp ->
            deleteLocalPhoto(context, wp.basicData["photo1"])
            deleteLocalPhoto(context, wp.basicData["photo2"])
            deleteLocalPhoto(context, wp.basicData["photo3"])
        }
    }

    fun deleteWaypointsLocalPhotos(context: Context, waypoints: List<Map<String, String>>) {
        waypoints.forEach { data ->
            deleteLocalPhoto(context, data["photo1"])
            deleteLocalPhoto(context, data["photo2"])
            deleteLocalPhoto(context, data["photo3"])
        }
    }

    private fun deleteLocalPhoto(context: Context, uriString: String?) {
        if (uriString.isNullOrBlank()) return

        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return
        val scheme = uri.scheme?.lowercase()
        if (scheme == "http" || scheme == "https") return

        val candidateFile = when (scheme) {
            "content" -> fileFromOurFileProviderUri(context, uri)
            "file" -> uri.path?.let { File(it) }
            else -> null
        } ?: return

        if (!isUnderAppPrivateDirs(context, candidateFile)) return
        runCatching {
            if (candidateFile.exists()) candidateFile.delete()
        }
    }

    private fun fileFromOurFileProviderUri(context: Context, uri: Uri): File? {
        val expectedAuthority = "${context.packageName}.fileprovider"
        if (uri.authority != expectedAuthority) return null

        // FileProvider URI format: content://authority/<rootName>/<relativePath...>
        val segments = uri.pathSegments
        if (segments.isEmpty()) return null
        val rootName = segments.firstOrNull() ?: return null
        val relativePath = segments.drop(1).joinToString(separator = "/")
        if (relativePath.isBlank()) return null

        val baseDir: File = when (rootName) {
            "gutter_images_external" ->
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return null
            "gutter_images_internal" -> context.filesDir
            else -> return null
        }
        return File(baseDir, relativePath)
    }

    private fun isUnderAppPrivateDirs(context: Context, file: File): Boolean {
        val filesDir = context.filesDir
        val extPicDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        val filePath = runCatching { file.canonicalPath }.getOrNull() ?: return false
        val allowedBases = buildList {
            add(runCatching { filesDir.canonicalPath }.getOrNull())
            add(extPicDir?.let { runCatching { it.canonicalPath }.getOrNull() })
        }.filterNotNull()

        return allowedBases.any { base -> filePath.startsWith(base + File.separator) || filePath == base }
    }
}

