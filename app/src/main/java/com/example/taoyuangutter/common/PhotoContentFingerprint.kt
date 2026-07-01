package com.example.taoyuangutter.common

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

object PhotoContentFingerprint {

    suspend fun fingerprint(context: Context, uriString: String?): String? {
        if (uriString.isNullOrBlank()) return null
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null
        return withContext(Dispatchers.IO) {
            runCatching {
                when (uri.scheme?.lowercase()) {
                    "http", "https" -> uriString
                    "file" -> uri.path?.let { fingerprint(File(it)) }
                    "content" -> context.contentResolver.openInputStream(uri)?.use { input ->
                        fingerprint(input.readBytes())
                    }
                    else -> uriString
                }
            }.getOrNull()
        }
    }

    suspend fun samePhoto(context: Context, left: String?, right: String?): Boolean {
        val leftFp = fingerprint(context, left) ?: return false
        val rightFp = fingerprint(context, right) ?: return false
        return leftFp == rightFp
    }

    private fun fingerprint(file: File): String? {
        if (!file.exists() || !file.canRead()) return null
        return file.inputStream().use { input -> fingerprint(input.readBytes()) }
    }

    private fun fingerprint(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
