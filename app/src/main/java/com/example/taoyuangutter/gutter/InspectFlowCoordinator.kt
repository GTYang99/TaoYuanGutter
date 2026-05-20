package com.example.taoyuangutter.gutter

import android.content.Context
import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.api.DitchNode
import com.example.taoyuangutter.api.GutterRepository
import com.example.taoyuangutter.api.NodeDetails
import com.example.taoyuangutter.login.LoginActivity
import com.google.android.gms.maps.model.Polyline
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class InspectFlowCoordinator(
    private val context: Context,
    private val repository: GutterRepository
) {
    data class InspectStart(
        val spiNum: String,
        val canEdit: Boolean,
        val latitudes: DoubleArray,
        val longitudes: DoubleArray,
        val routeWaypoints: List<Waypoint>
    )

    data class LoadedInspectData(
        val ditch: com.example.taoyuangutter.api.DitchDetails,
        val pendingByNodeId: Map<Int, Boolean>,
        val nodeDetailsList: List<NodeDetails>,
        val intent: android.content.Intent
    )

    fun prepareStart(polyline: Polyline): InspectStart? {
        val tag = polyline.tag as? Pair<*, *> ?: return null
        val spiNum = tag.first as? String ?: return null
        val groupId = tag.second as? String ?: ""
        val savedGroupId = LoginActivity.getSavedGroupId(context)
        val canEdit = savedGroupId != -1 && (
            savedGroupId == 1 || (groupId.isNotEmpty() && groupId.toIntOrNull() == savedGroupId)
        )
        return InspectStart(
            spiNum = spiNum,
            canEdit = canEdit,
            latitudes = polyline.points.map { it.latitude }.toDoubleArray(),
            longitudes = polyline.points.map { it.longitude }.toDoubleArray(),
            routeWaypoints = polyline.points.map { Waypoint(WaypointType.NODE, "", it, hashMapOf()) }
        )
    }

    suspend fun load(start: InspectStart, token: String): ApiResult<LoadedInspectData> {
        return when (val result = repository.getDitchDetails(start.spiNum, token)) {
            is ApiResult.Success -> {
                val ditch = result.data.data
                if (ditch == null) {
                    ApiResult.Error(message = "查無側溝資料", code = 404)
                } else {
                    val pendingByNodeId = ditch.nodes.associate { node ->
                        node.nodeId to parseLooseBoolean(node.isPendingDeploy)
                    }
                    val nodeDetailsList = repository.getNodeDetailsForNodes(ditch.nodes, token)
                    val downloaded = downloadAllNodePhotos(ditch.nodes)
                    if (downloaded == null) {
                        ApiResult.Error(
                            message = "圖片下載失敗，請重新點選側溝線段再試",
                            code = 500
                        )
                    } else {
                        val intent = GutterInspectActivity.newIntent(
                            context = context,
                            ditch = ditch,
                            canEdit = start.canEdit,
                            latitudes = start.latitudes,
                            longitudes = start.longitudes,
                            preloadedNodeDetailsJson = com.google.gson.Gson().toJson(nodeDetailsList),
                            preloadedNodePhotosJson = downloaded
                        )
                        ApiResult.Success(
                            LoadedInspectData(
                                ditch = ditch,
                                pendingByNodeId = pendingByNodeId,
                                nodeDetailsList = nodeDetailsList,
                                intent = intent
                            )
                        )
                    }
                }
            }
            is ApiResult.Error -> result
        }
    }

    private suspend fun downloadAllNodePhotos(nodes: List<DitchNode>): String? = coroutineScope {
        val orderedNodes = nodes.sortedWith(
            compareBy(
                { when (it.nodeAtt) { "1" -> 0; "3" -> 2; else -> 1 } },
                { it.nodeNum?.toIntOrNull() ?: Int.MAX_VALUE }
            )
        )

        val payloads = mutableListOf<InspectPreloadedNodePhotos>()
        for (node in orderedNodes) {
            fun urlByCategory(cat: String): String? =
                node.url.firstOrNull { it.fileCategory == cat }?.url

            suspend fun download(url: String?, prefix: String): String {
                if (url.isNullOrBlank()) return ""
                return repository.downloadImageToLocalContentUri(context, url, prefix = prefix)?.toString()
                    ?: return "__FAILED__"
            }

            val downloads = awaitAll(
                async { download(urlByCategory("1"), "INSPECT_${node.nodeId}_1_") },
                async { download(urlByCategory("2"), "INSPECT_${node.nodeId}_2_") },
                async { download(urlByCategory("3"), "INSPECT_${node.nodeId}_3_") }
            )
            if (downloads.any { it == "__FAILED__" }) return@coroutineScope null

            payloads.add(
                InspectPreloadedNodePhotos(
                    nodeId = node.nodeId,
                    photo1 = downloads[0],
                    photo2 = downloads[1],
                    photo3 = downloads[2]
                )
            )
        }
        com.google.gson.Gson().toJson(payloads)
    }

    private fun parseLooseBoolean(raw: String?): Boolean {
        return when (raw?.trim()?.lowercase()) {
            "1", "true", "y", "yes" -> true
            else -> false
        }
    }
}
