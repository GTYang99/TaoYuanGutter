package com.example.taoyuangutter.gutter

import android.content.ContextWrapper
import com.example.taoyuangutter.api.DitchNode
import com.example.taoyuangutter.api.GutterApiService
import com.example.taoyuangutter.api.GutterRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicInteger

class PhotoUploadManagerPendingPhotosTest {
    @Test
    fun successfulPhotosRemainOutOfPendingUploadsAfterWaypointReversal() = runBlocking {
        val first = waypoint("uid-first", "101")
        val second = waypoint("uid-second", "202")
        val uploadCalls = AtomicInteger()
        val manager = managerCountingUploadCalls(uploadCalls)

        val result = manager.uploadWaypointPhotos(
            waypoints = listOf(second, first),
            nodes = listOf(node(2), node(1)),
            token = "test-token"
        )

        assertEquals(0, uploadCalls.get())
        assertEquals(0, (result as PhotoUploadManager.UploadBatchResult.Completed).failCount)
    }

    @Test
    fun mergedSuccessfulReplacementDoesNotInvokeUploadAgain() = runBlocking {
        val merged = PhotoResultMetadataMerger.merge(
            existing = mapOf(
                "photo1" to "content://old-photo",
                "photo1ImgId" to "101",
                "photo1UploadState" to "success"
            ),
            incoming = mapOf(
                "photo1" to "content://new-photo",
                "photo1ImgId" to "202",
                "photo1UploadState" to "success"
            )
        )
        val uploadCalls = AtomicInteger()
        val manager = managerCountingUploadCalls(uploadCalls)
        val waypoint = Waypoint(
            type = WaypointType.NODE,
            label = "節點1",
            basicData = HashMap(merged),
            uid = "uid-replacement"
        )

        manager.uploadWaypointPhotos(
            waypoints = listOf(waypoint),
            nodes = listOf(node(1)),
            token = "test-token"
        )

        assertEquals("202", merged["photo1ImgId"])
        assertEquals(0, uploadCalls.get())
    }

    private fun managerCountingUploadCalls(uploadCalls: AtomicInteger): PhotoUploadManager {
        val api = Proxy.newProxyInstance(
            GutterApiService::class.java.classLoader,
            arrayOf(GutterApiService::class.java)
        ) { _, method, _ ->
            if (method.name == "uploadNodeImage") uploadCalls.incrementAndGet()
            error("Unexpected API call: ${method.name}")
        } as GutterApiService
        return PhotoUploadManager(ContextWrapper(null), GutterRepository(api))
    }

    private fun waypoint(uid: String, imgId: String) = Waypoint(
        type = WaypointType.NODE,
        label = uid,
        basicData = hashMapOf(
            "photo1" to "https://example.test/$uid.jpg",
            "photo1ImgId" to imgId,
            "photo1UploadState" to "success"
        ),
        uid = uid
    )

    private fun node(nodeId: Int) = DitchNode(
        nodeId = nodeId,
        nodeAtt = "2",
        nodeNum = null,
        url = emptyList()
    )
}
