package com.example.taoyuangutter.common

import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.gutter.PhotoUploadManager
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class UploadFailureUiModel(
    val categoryLabel: String,
    val userMessage: String,
    val referenceCode: String,
    val detailSummary: String? = null
) {
    fun buildDialogMessage(): String {
        val parts = mutableListOf(
            "失敗類型：$categoryLabel",
            "說明：$userMessage",
            "參考代碼：$referenceCode"
        )
        detailSummary?.takeIf { it.isNotBlank() }?.let { parts += it }
        return parts.joinToString(separator = "\n")
    }
}

object UploadFailureClassifier {

    fun forStoreDitchNetworkTimeout(error: ApiResult.Error): UploadFailureUiModel {
        return build(
            categoryLabel = "網路連線逾時",
            userMessage = "目前網路不穩或無法連線，請確認側溝資料是否有完整上傳成功，若有缺漏請至草稿重新嘗試上傳。",
            referenceCode = "NETWORK_ERROR",
            detailSummary = buildApiDetail(error)
        )
    }

    fun forStoreDitchNetworkTimeout(message: String?): UploadFailureUiModel {
        return build(
            categoryLabel = "網路連線逾時",
            userMessage = "目前網路不穩或無法連線，請確認側溝資料是否有完整上傳成功，若有缺漏請至草稿重新嘗試上傳。",
            referenceCode = "NETWORK_ERROR",
            detailSummary = normalizeExceptionDetail(message)
        )
    }

    fun forStoreDitchNetworkFailure(error: ApiResult.Error): UploadFailureUiModel {
        return build(
            categoryLabel = "網路連線失敗",
            userMessage = "目前無法連線至伺服器，請確認網路或稍後再試。側溝資料尚未確認成功送出，必要時可存入草稿後重新上傳。",
            referenceCode = "NETWORK_ERROR",
            detailSummary = buildApiDetail(error)
        )
    }

    fun forStoreDitchNetworkFailure(message: String?): UploadFailureUiModel {
        return build(
            categoryLabel = "網路連線失敗",
            userMessage = "目前無法連線至伺服器，請確認網路或稍後再試。側溝資料尚未確認成功送出，必要時可存入草稿後重新上傳。",
            referenceCode = "NETWORK_ERROR",
            detailSummary = normalizeExceptionDetail(message)
        )
    }

    fun isNetworkFailureMessage(message: String?): Boolean {
        return message?.let { isNetworkMessage(it) } == true
    }

    fun isTimeoutFailureMessage(message: String?): Boolean {
        return message?.let { isTimeoutMessage(it) } == true
    }

    fun forStoreDitchError(error: ApiResult.Error): UploadFailureUiModel {
        return when {
            error.code == 401 -> build(
                categoryLabel = "側溝資料上傳失敗",
                userMessage = "登入狀態已失效，請重新登入後再試。",
                referenceCode = "STORE_DITCH_AUTH_FAILED",
                detailSummary = buildApiDetail(error)
            )
            error.code == 422 -> build(
                categoryLabel = "側溝資料上傳失敗",
                userMessage = "側溝資料未成功送出，請檢查填寫內容後再試。",
                referenceCode = "STORE_DITCH_VALIDATION_FAILED",
                detailSummary = buildApiDetail(error)
            )
            isNetworkMessage(error.message) -> build(
                categoryLabel = "網路連線失敗",
                userMessage = "目前網路不穩或無法連線，請確認訊號後再試。",
                referenceCode = "NETWORK_ERROR",
                detailSummary = buildApiDetail(error)
            )
            else -> build(
                categoryLabel = "側溝資料上傳失敗",
                userMessage = "側溝資料未成功送出，請稍後再試。",
                referenceCode = "STORE_DITCH_FAILED",
                detailSummary = buildApiDetail(error)
            )
        }
    }

    fun forStoreDitchException(message: String?): UploadFailureUiModel {
        return build(
            categoryLabel = "網路連線失敗",
            userMessage = "側溝資料送出時發生連線異常，請稍後再試。",
            referenceCode = "NETWORK_ERROR",
            detailSummary = normalizeExceptionDetail(message)
        )
    }

    fun forPhotoBatchFailures(failures: List<PhotoUploadManager.PhotoUploadFailure>): UploadFailureUiModel {
        val first = failures.firstOrNull()
        val category = when {
            failures.any { it.reasonType == PhotoUploadManager.PhotoFailureReasonType.IMAGE_PROCESSING } -> {
                "照片處理失敗" to "照片檔案無法讀取或處理，請重新拍攝或重新選取照片。"
            }
            failures.any { it.reasonType == PhotoUploadManager.PhotoFailureReasonType.NETWORK } -> {
                "網路連線失敗" to "照片上傳時網路不穩或無法連線，請確認訊號後再試。"
            }
            else -> {
                "照片上傳失敗" to "照片未全部上傳成功，請重新嘗試或存入草稿稍後再傳。"
            }
        }
        val code = when (first?.reasonType) {
            PhotoUploadManager.PhotoFailureReasonType.IMAGE_PROCESSING -> "PHOTO_PROCESS_FAILED"
            PhotoUploadManager.PhotoFailureReasonType.NETWORK -> "PHOTO_NETWORK_ERROR"
            PhotoUploadManager.PhotoFailureReasonType.API -> "PHOTO_UPLOAD_FAILED"
            null -> "PHOTO_UPLOAD_FAILED"
        }
        return build(
            categoryLabel = category.first,
            userMessage = category.second,
            referenceCode = code,
            detailSummary = buildPhotoFailureDetails(failures)
        )
    }

    fun forPhotoTimeout(failCount: Int, completedCount: Int): UploadFailureUiModel {
        val progress = when {
            completedCount > 0 || failCount > 0 ->
                "本輪已完成 $completedCount 張，另有 $failCount 張失敗或未確認完成。"
            else -> null
        }
        return build(
            categoryLabel = "伺服器回應逾時",
            userMessage = "已送出上傳請求，但等待伺服器回應逾時。請稍後確認是否已上傳成功，或重新嘗試。",
            referenceCode = "SERVER_TIMEOUT",
            detailSummary = progress
        )
    }

    fun forPhotoException(message: String?): UploadFailureUiModel {
        return build(
            categoryLabel = "未知錯誤",
            userMessage = "照片上傳時發生未預期錯誤，請重試；若持續發生請回報管理人員。",
            referenceCode = "UNKNOWN_ERROR",
            detailSummary = normalizeExceptionDetail(message)
        )
    }

    fun forPhotoApiError(error: ApiResult.Error): UploadFailureUiModel {
        return when {
            isImageProcessingMessage(error.message) -> build(
                categoryLabel = "照片處理失敗",
                userMessage = "照片檔案無法讀取或處理，請重新拍攝或重新選取照片。",
                referenceCode = "PHOTO_PROCESS_FAILED",
                detailSummary = buildApiDetail(error)
            )
            isNetworkMessage(error.message) -> build(
                categoryLabel = "網路連線失敗",
                userMessage = "照片上傳時網路不穩或無法連線，請確認訊號後再試。",
                referenceCode = "PHOTO_NETWORK_ERROR",
                detailSummary = buildApiDetail(error)
            )
            else -> build(
                categoryLabel = "照片上傳失敗",
                userMessage = "照片未全部上傳成功，請重新嘗試。",
                referenceCode = "PHOTO_UPLOAD_FAILED",
                detailSummary = buildApiDetail(error)
            )
        }
    }

    private fun build(
        categoryLabel: String,
        userMessage: String,
        referenceCode: String,
        detailSummary: String?
    ) = UploadFailureUiModel(categoryLabel, userMessage, referenceCode, detailSummary)

    private fun buildApiDetail(error: ApiResult.Error): String {
        val codeText = error.code?.let { "HTTP $it" } ?: "未提供 HTTP code"
        return "詳細原因：$codeText，${error.message}"
    }

    private fun buildPhotoFailureDetails(failures: List<PhotoUploadManager.PhotoUploadFailure>): String? {
        if (failures.isEmpty()) return null
        val summary = failures.take(3).joinToString(separator = "\n") { failure ->
            "失敗項目：節點${failure.nodeId} 照片${failure.fileCategory}，原因：${failure.message}"
        }
        val extra = failures.size - 3
        return if (extra > 0) "$summary\n其餘 $extra 張請查看對照表。" else summary
    }

    private fun normalizeExceptionDetail(message: String?): String? {
        return message?.takeIf { it.isNotBlank() }?.let { "詳細原因：$it" }
    }

    private fun isImageProcessingMessage(message: String): Boolean {
        val lowered = message.lowercase()
        return message.contains("無法處理圖片檔案") ||
            lowered.contains("decode") ||
            lowered.contains("bitmap")
    }

    private fun isNetworkMessage(message: String): Boolean {
        val lowered = message.lowercase()
        return message.contains("網路連線失敗") ||
            lowered.contains("timeout") ||
            lowered.contains("timed out") ||
            lowered.contains("unable to resolve host") ||
            lowered.contains("failed to connect") ||
            lowered.contains("connection reset")
    }

    private fun isTimeoutMessage(message: String): Boolean {
        val lowered = message.lowercase()
        return message.contains("逾時") ||
            lowered.contains("timeout") ||
            lowered.contains("timed out") ||
            lowered.contains("sockettimeoutexception")
    }

    fun classifyThrowable(throwable: Throwable): String {
        return when (throwable) {
            is UnknownHostException,
            is ConnectException,
            is SocketException,
            is SocketTimeoutException -> throwable.javaClass.simpleName
            else -> throwable.javaClass.simpleName
        }
    }
}
