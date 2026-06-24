package com.example.taoyuangutter.common

import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * 通用請求體構建器，提供 DSL 風格簡化 MultipartBody 的組裝。
 */
class RequestBodyBuilder {
    private val params = mutableMapOf<String, String>()
    private val fileParts = mutableListOf<MultipartBody.Part>()

    fun param(key: String, value: Any?) {
        value?.toString()?.let { strValue ->
            if (strValue.isNotEmpty()) {
                params[key] = strValue
            }
        }
    }

    private fun addFilePart(name: String, filename: String, body: RequestBody) {
        val filePart = MultipartBody.Part.createFormData(name, filename, body)
        fileParts.add(filePart)
    }

    fun addFile(name: String, file: File, contentType: String = "application/octet-stream") {
        val mediaType = contentType.toMediaType()
        val requestBody = file.readBytes().toRequestBody(mediaType)
        addFilePart(name, file.name, requestBody)
    }

    fun addFile(name: String, filename: String, bytes: ByteArray, contentType: String = "application/octet-stream") {
        val mediaType = contentType.toMediaType()
        val requestBody = bytes.toRequestBody(mediaType)
        addFilePart(name, filename, requestBody)
    }

    fun buildFormBody(): FormBody {
        return FormBody.Builder().apply {
            params.forEach { (key, value) ->
                add(key, value)
            }
        }.build()
    }

    fun buildMultipartBody(): MultipartBody {
        return MultipartBody.Builder().apply {
            setType(MultipartBody.FORM)
            fileParts.forEach { part ->
                addPart(part)
            }
            params.forEach { (key, value) ->
                addFormDataPart(key, value)
            }
        }.build()
    }

    private fun hasFileParts(): Boolean = fileParts.isNotEmpty()

    fun build(): RequestBody {
        return if (hasFileParts()) {
            buildMultipartBody()
        } else {
            buildFormBody()
        }
    }
}

fun buildRequestBody(block: RequestBodyBuilder.() -> Unit): RequestBody {
    return RequestBodyBuilder().apply(block).build()
}
