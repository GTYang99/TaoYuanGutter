package com.example.taoyuangutter.common

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PhotoAlbumStore {

    private const val ALBUM_FOLDER_NAME = "TaoYuanGutter"

    fun requiresLegacyWritePermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    }

    suspend fun copyToSystemAlbum(
        context: Context,
        sourceFile: File,
        albumFolderName: String = ALBUM_FOLDER_NAME
    ): Uri? = withContext(Dispatchers.IO) {
        if (!sourceFile.exists() || !sourceFile.canRead()) return@withContext null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            copyToMediaStore(context, sourceFile, albumFolderName)
        } else {
            copyToLegacyPublicPictures(context, sourceFile, albumFolderName)
        }
    }

    private fun copyToMediaStore(
        context: Context,
        sourceFile: File,
        albumFolderName: String
    ): Uri? {
        val resolver = context.contentResolver
        val mimeType = "image/jpeg"
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val relativePath = "${Environment.DIRECTORY_PICTURES}/$albumFolderName"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, sourceFile.name)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.DATE_TAKEN, sourceFile.lastModified())
            put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val itemUri = resolver.insert(collection, values) ?: return null
        return try {
            FileInputStream(sourceFile).use { input ->
                val outputStream = resolver.openOutputStream(itemUri) ?: run {
                    runCatching { resolver.delete(itemUri, null, null) }
                    return null
                }
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }.also { publishedValues ->
                resolver.update(itemUri, publishedValues, null, null)
            }
            itemUri
        } catch (_: Exception) {
            runCatching { resolver.delete(itemUri, null, null) }
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun copyToLegacyPublicPictures(
        context: Context,
        sourceFile: File,
        albumFolderName: String
    ): Uri? {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val albumDir = File(picturesDir, albumFolderName).apply { mkdirs() }
        val destFile = uniqueDestFile(albumDir, sourceFile.name)

        return try {
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                arrayOf("image/jpeg"),
                null
            )
            Uri.fromFile(destFile)
        } catch (_: Exception) {
            null
        }
    }

    private fun uniqueDestFile(dir: File, fileName: String): File {
        val dotIndex = fileName.lastIndexOf('.')
        val baseName = if (dotIndex > 0) fileName.substring(0, dotIndex) else fileName
        val extension = if (dotIndex > 0) fileName.substring(dotIndex) else ""

        var candidate = File(dir, fileName)
        var index = 1
        while (candidate.exists()) {
            candidate = File(dir, "${baseName}_$index$extension")
            index++
        }
        return candidate
    }
}
