package com.example.taoyuangutter.gutter

import android.content.Context
import android.net.Uri
import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.api.DitchNode
import com.example.taoyuangutter.api.GutterRepository
import com.example.taoyuangutter.common.PhotoUploadValidator
import com.example.taoyuangutter.pending.DraftPhotoCleaner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
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
    fun countPendingPhotos(
        waypoints: List<Waypoint>,
        nodes: List<DitchNode>
    ): Int {
        var count = 0
        nodes.forEachIndexed { i, _ ->
            val wp = waypoints.getOrNull(i) ?: return@forEachIndexed
            if (wp.isVirtual) return@forEachIndexed
            listOf(
                wp.basicData["photo1"],
                wp.basicData["photo2"],
                wp.basicData["photo3"]
            ).forEach { path ->
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
        listener: UploadListener? = null
    ): Int {
        val pending = mutableListOf<Triple<DitchNode, String, Int>>()
        nodes.forEachIndexed { i, node ->
            val wp = waypoints.getOrNull(i) ?: return@forEachIndexed
            if (wp.isVirtual) {
                android.util.Log.d("PhotoUpload", "節點 ${node.nodeId} 為虛擬點，略過所有照片上傳")
                return@forEachIndexed
            }
            listOf(
                wp.basicData["photo1"] to 1,
                wp.basicData["photo2"] to 2,
                wp.basicData["photo3"] to 3
            ).forEach { (path, category) ->
                if (PhotoUploadValidator.isUsableForUpload(context, path)) {
                    val usablePath = path ?: return@forEach
                    pending.add(Triple(node, usablePath, category))
                }
            }
        }

        val total = pending.size
        if (total == 0) return 0 // 無需上傳

        var failedCount = 0
        var completedCount = 0

        try {
            // 控制併發數 (最多同時上傳 3 張照片)
            val semaphore = Semaphore(3)

            coroutineScope {
                pending.map { entry ->
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
                            for (attempt in 1..3) {
                                val uploadPath = downloadIfRemoteUrl(path) ?: break
                                if (uploadPath != path && Uri.parse(uploadPath).scheme?.lowercase() == "content") {
                                    tempDownloadedUri = uploadPath
                                }
                                when (val r = gutterRepository.uploadNodeImage(
                                    context = context,
                                    nodeId = node.nodeId,
                                    fileCategory = category,
                                    imageUri = Uri.parse(uploadPath),
                                    token = token
                                )) {
                                    is ApiResult.Success -> {
                                        success = true
                                    }
                                    is ApiResult.Error -> {
                                        android.util.Log.w(
                                            "PhotoUpload",
                                            "node${node.nodeId} photo$category attempt$attempt 失敗: ${r.message}"
                                        )
                                    }
                                }
                                if (success) break
                            }

                            // 立即清除「為了重傳而下載」的暫存照片檔
                            tempDownloadedUri?.let { downloaded ->
                                DraftPhotoCleaner.deleteWaypointsLocalPhotos(
                                    context = context,
                                    waypoints = listOf(mapOf("photo1" to downloaded))
                                )
                            }

                            withContext(Dispatchers.Main) {
                                listener?.onPhotoUploadResult(success)
                                if (success) {
                                    completedCount++
                                    listener?.onProgressUpdate(completedCount, total)
                                } else {
                                    failedCount++
                                }
                            }
                        }
                    }
                }
            }
            return failedCount
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            android.util.Log.e("PhotoUpload", "上傳照片時發生未預期錯誤: ${e.message}", e)
            return total - completedCount
        }
    }
}
