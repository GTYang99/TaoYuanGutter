package com.example.taoyuangutter.common

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern

object PhotoCapturedAtResolver {

    private val filenameTimestampPattern =
        Pattern.compile("(\\d{8}_\\d{6})")
    private val apiFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }
    private val filenameFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }
    private val exifFormats = listOf(
        SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    ).onEach { it.timeZone = TimeZone.getDefault() }

    fun basicDataKey(slot: Int): String = "photo${slot}CapturedAt"

    fun readBasicData(source: Map<String, String>, slot: Int): String? =
        source[basicDataKey(slot)]?.takeIf { it.isNotBlank() }

    fun writeBasicData(target: MutableMap<String, String>, slot: Int, capturedAt: String?) {
        target[basicDataKey(slot)] = capturedAt?.takeIf { it.isNotBlank() } ?: ""
    }

    fun resolveBestEffort(context: Context, uriString: String?): String? {
        if (uriString.isNullOrBlank()) return null
        val uri = runCatching { Uri.parse(uriString) }.getOrNull()
        val directCandidates = buildList {
            add(uriString)
            uri?.lastPathSegment?.let(::add)
            if (uri?.scheme?.equals("file", ignoreCase = true) == true) {
                uri.path?.let(::add)
            }
        }
        directCandidates.firstNotNullOfOrNull(::extractFromText)?.let { return it }

        if (uri == null) return null

        queryDisplayName(context, uri)?.let { extractFromText(it) }?.let { return it }
        readExifCapturedAt(context, uri)?.let { return it }
        queryDateTaken(context, uri)?.let { return it }
        resolveFromFileLastModified(uri)?.let { return it }

        return null
    }

    private fun extractFromText(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val matcher = filenameTimestampPattern.matcher(text)
        if (!matcher.find()) return null
        val raw = matcher.group(1) ?: return null
        val parsed = runCatching { filenameFormat.parse(raw) }.getOrNull() ?: return null
        return apiFormat.format(parsed)
    }

    private fun readExifCapturedAt(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                listOf(
                    ExifInterface.TAG_DATETIME_ORIGINAL,
                    ExifInterface.TAG_DATETIME_DIGITIZED,
                    ExifInterface.TAG_DATETIME
                ).firstNotNullOfOrNull { tag ->
                    exif.getAttribute(tag)?.let(::normalizeExifTime)
                }
            }
        }.getOrNull()
    }

    private fun normalizeExifTime(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        exifFormats.firstNotNullOfOrNull { format ->
            runCatching { format.parse(trimmed) }.getOrNull()
        }?.let { return apiFormat.format(it) }
        return null
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return querySingleString(
            context,
            uri,
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
            MediaStore.MediaColumns.DISPLAY_NAME
        )
    }

    private fun queryDateTaken(context: Context, uri: Uri): String? {
        val dateTaken = querySingleLong(
            context,
            uri,
            arrayOf(MediaStore.Images.ImageColumns.DATE_TAKEN),
            MediaStore.Images.ImageColumns.DATE_TAKEN
        )
        if (dateTaken != null && dateTaken > 0L) {
            return apiFormat.format(Date(dateTaken))
        }
        val dateModifiedSeconds = querySingleLong(
            context,
            uri,
            arrayOf(MediaStore.MediaColumns.DATE_MODIFIED),
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        if (dateModifiedSeconds != null && dateModifiedSeconds > 0L) {
            return apiFormat.format(Date(dateModifiedSeconds * 1000L))
        }
        return null
    }

    private fun resolveFromFileLastModified(uri: Uri): String? {
        if (!uri.scheme.equals("file", ignoreCase = true)) return null
        val path = uri.path ?: return null
        val file = File(path)
        val lastModified = file.lastModified()
        if (lastModified <= 0L) return null
        return apiFormat.format(Date(lastModified))
    }

    private fun querySingleString(
        context: Context,
        uri: Uri,
        projection: Array<String>,
        column: String
    ): String? {
        return queryCursor(context, uri, projection) { cursor ->
            val index = cursor.getColumnIndex(column)
            if (index >= 0 && !cursor.isNull(index)) cursor.getString(index) else null
        }
    }

    private fun querySingleLong(
        context: Context,
        uri: Uri,
        projection: Array<String>,
        column: String
    ): Long? {
        return queryCursor(context, uri, projection) { cursor ->
            val index = cursor.getColumnIndex(column)
            if (index >= 0 && !cursor.isNull(index)) cursor.getLong(index) else null
        }
    }

    private fun <T> queryCursor(
        context: Context,
        uri: Uri,
        projection: Array<String>,
        reader: (Cursor) -> T?
    ): T? {
        return runCatching {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) reader(cursor) else null
            }
        }.getOrNull()
    }
}
