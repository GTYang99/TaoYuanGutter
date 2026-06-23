package com.example.taoyuangutter.common

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * 統一照片驗證與上傳前判定的規則。
 *
 * 這裡只回答一件事：某個照片 URI 是否可作為上傳來源。
 * - http/https：視為可用，後續會先下載到本機再上傳
 * - content/file：必須真的可讀
 * - 其他 / 空值：不可用
 */
object PhotoUploadValidator {

    fun isUsableForUpload(context: Context, uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false

        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return false
        return when (uri.scheme?.lowercase()) {
            "http", "https" -> true
            "content" -> canReadContentUri(context, uri)
            "file" -> canReadFileUri(uri)
            else -> false
        }
    }

    private fun canReadContentUri(context: Context, uri: Uri): Boolean {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.read() != -1
            } ?: false
        }.getOrDefault(false)
    }

    private fun canReadFileUri(uri: Uri): Boolean {
        val path = uri.path ?: return false
        val file = File(path)
        return file.exists() && file.canRead()
    }
}
