package com.example.taoyuangutter.gutter

import android.content.Context
import android.net.Uri
import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.api.DitchNode
import com.example.taoyuangutter.api.GutterRepository
import com.example.taoyuangutter.common.PhotoUploadValidator
import com.example.taoyuangutter.pending.DraftPhotoCleaner
import com.example.taoyuangutter.pending.WaypointSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * 照片上傳管理器 (PhotoUploadManager)
 * 負責協調多點位照片的背景/並行上傳邏輯與狀態通知，將業務邏輯自 MainActivity 抽離。
 */
class PhotoUploadManager(
    private val context: Context,
    private val gutterRepository: GutterRepository = GutterRepository()
) {

    enum class PhotoFailureReasonType {
        API,
        NETWORK,
        IMAGE_PROCESSING
    }

    data class PhotoUploadFailure(
        val nodeId: Int,
        val fileCategory: Int,
        val attempt: Int,
        val message: String,
        val code: Int? = null,
        val reasonType: PhotoFailureReasonType = PhotoFailureReasonType.API
    )

    sealed class UploadBatchResult {
        data class Completed(
            val failCount: Int,
            val failures: List<PhotoUploadFailure> = emptyList()
        ) : UploadBatchResult()
        data class TimedOut(
            val failCount: Int,
            val completedCount: Int,
            val failures: List<PhotoUploadFailure> = emptyList()
        ) : UploadBatchResult()
    }

    private class LastPhotoUploadTimeoutException(
        val result: UploadBatchResult.TimedOut
    ) : Exception()

    interface UploadListener {
        /**
         * 當有照片上傳成功時觸發，更新前台 UI 進度。
         */
        fun onProgressUpdate(completedCount: Int, totalCount: Int)

        /**
         * 單張照片的上傳結果。
         */
        fun onPhotoUploadResult(success: Boolean)
    }

    /**
     * 計算待上傳的可用照片數量。
     * 會把可讀的本機 URI 與可重新下載的遠端 URL 都算進來。
     * 可供呼叫端在啟動上傳 UI 前先判斷是否有需要上傳的項目。
     */
    suspend fun countPendingPhotos(
        waypoints: List<Waypoint>,
        nodes: List<DitchNode>,
        originalWaypoints: List<WaypointSnapshot>? = null
    ): Int {
        var count = 0
        for (i in nodes.indices) {
            val wp = waypoints.getOrNull(i) ?: continue
            if (wp.isVirtual) continue
            val slots = listOf(
                wp.basicData["photo1"],
                wp.basicData["photo2"],
                wp.basicData["photo3"]
            )
            for (path in slots) {
                if (PhotoUploadValidator.isUsableForUpload(context, path)) count++
            }
        }
        return count
    }

    /**
     * 上傳所有點位的可用照片，回傳失敗張數。
     */
    suspend fun uploadWaypointPhotos(
        waypoints: List<Waypoint>,
        nodes: List<DitchNode>,
        token: String,
        originalWaypoints: List<WaypointSnapshot>? = null,
        listener: UploadListener? = null
    ): UploadBatchResult {
        val pending = mutableListOf<Triple<DitchNode, String, Int>>()
        for (i in nodes.indices) {
            val node = nodes[i]
            val wp = waypoints.getOrNull(i) ?: continue
            if (wp.isVirtual) {
                android.util.Log.d("PhotoUpload", "節點 ${node.nodeId} 為虛擬點，略過所有照片上傳")
                continue
            }
            val slots: List<Pair<String?, Int>> = listOf(
                wp.basicData["photo1"] to 1,
                wp.basicData["photo2"] to 2,
                wp.basicData["photo3"] to 3
            )
            for ((path, category) in slots) {
                if (!PhotoUploadValidator.isUsableForUpload(context, path)) continue
                val usablePath = path ?: continue
                pending.add(Triple(node, usablePath, category))
            }
        }

        val total = pending.size
        if (total == 0) return UploadBatchResult.Completed(0) // 無需上傳

        var failedCount = 0
        var completedCount = 0
        val lastPendingIndex = pending.lastIndex
        val failures = mutableListOf<PhotoUploadFailure>()

        try {
            // 控制併發數 (最多同時上傳 3 張照片)
            val semaphore = Semaphore(3)

            coroutineScope {
                pending.mapIndexed { index, entry ->
                    launch {
                        semaphore.withPermit {
                            val node = entry.first
                            val path = entry.second
                            val category = entry.third

                            suspend fun downloadIfRemoteUrl(url: String): String? {
                                val scheme = Uri.parse(url).scheme?.lowercase()
                                if (scheme != "http" && scheme != "https") return url
                                val prefix = "REUPLOAD_${node.nodeId}_${category}_"
                                return gutterRepository
                                    .downloadImageToLocalContentUri(context, url, prefix = prefix)
                                    ?.toString()
                            }

                            var success = false
                            var tempDownloadedUri: String? = null
                            var lastFailure: PhotoUploadFailure? = null
                            try {
                                for (attempt in 1..3) {
                                    val uploadPath = downloadIfRemoteUrl(path) ?: break
                                    if (uploadPath != path && Uri.parse(uploadPath).scheme?.lowercase() == "content") {
                                        tempDownloadedUri = uploadPath
                                    }
                                    val uploadResult = if (index == lastPendingIndex) {
                                        withTimeoutOrNull(LAST_PHOTO_WAIT_TIMEOUT_MS) {
                                            gutterRepository.uploadNodeImage(
                                                context = context,
                                                nodeId = node.nodeId,
                                                fileCategory = category,
                                                imageUri = Uri.parse(uploadPath),
                                                token = token
                                            )
                                        } ?: throw LastPhotoUploadTimeoutException(
                                            run {
                                                android.util.Log.w(
                                                    "PhotoUpload",
                                                    "最後一張照片 node${node.nodeId} photo$category 等待回應逾時 ${LAST_PHOTO_WAIT_TIMEOUT_MS}ms"
                                                )
                                                UploadBatchResult.TimedOut(
                                                    failCount = failedCount,
                                                    completedCount = completedCount,
                                                    failures = failures.toList()
                                                )
                                            }
                                        )
                                    } else {
                                        gutterRepository.uploadNodeImage(
                                            context = context,
                                            nodeId = node.nodeId,
                                            fileCategory = category,
                                            imageUri = Uri.parse(uploadPath),
                                            token = token
                                        )
                                    }
                                    when (uploadResult) {
                                        is ApiResult.Success -> {
                                            success = true
                                            lastFailure = null
                                        }
                                        is ApiResult.Error -> {
                                            lastFailure = PhotoUploadFailure(
                                                nodeId = node.nodeId,
                                                fileCategory = category,
                                                attempt = attempt,
                                                message = uploadResult.message,
                                                code = uploadResult.code,
                                                reasonType = classifyFailureReason(uploadResult.message)
                                            )
                                            android.util.Log.w(
                                                "PhotoUpload",
                                                "node${node.nodeId} photo$category attempt$attempt 失敗: ${uploadResult.message}"
                                            )
                                        }
                                    }
                                    if (success) break
                                }
                            } finally {
                                // 立即清除「為了重傳而下載」的暫存照片檔
                                tempDownloadedUri?.let { downloaded ->
                                    DraftPhotoCleaner.deleteWaypointsLocalPhotos(
                                        context = context,
                                        waypoints = listOf(mapOf("photo1" to downloaded))
                                    )
                                }
                            }

                            withContext(Dispatchers.Main) {
                                listener?.onPhotoUploadResult(success)
                                if (success) {
                                    completedCount++
                                    listener?.onProgressUpdate(completedCount, total)
                                } else {
                                    failedCount++
                                    lastFailure?.let { failures += it }
                                }
                            }
                        }
                    }
                }
            }
            return UploadBatchResult.Completed(failedCount, failures.toList())
        } catch (e: LastPhotoUploadTimeoutException) {
            return e.result
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            android.util.Log.e("PhotoUpload", "上傳照片時發生未預期錯誤: ${e.message}", e)
            return UploadBatchResult.Completed(
                failCount = total - completedCount,
                failures = failures.toList()
            )
        }
    }

    private fun classifyFailureReason(message: String): PhotoFailureReasonType {
        val lowered = message.lowercase()
        return when {
            message.contains("無法處理圖片檔案") ||
                lowered.contains("decode") ||
                lowered.contains("bitmap") -> PhotoFailureReasonType.IMAGE_PROCESSING
            message.contains("網路連線失敗") ||
                lowered.contains("timeout") ||
                lowered.contains("timed out") ||
                lowered.contains("unable to resolve host") ||
                lowered.contains("failed to connect") ||
                lowered.contains("connection reset") -> PhotoFailureReasonType.NETWORK
            else -> PhotoFailureReasonType.API
        }
    }

    private companion object {
        private const val LAST_PHOTO_WAIT_TIMEOUT_MS = 60_000L
    }
}
