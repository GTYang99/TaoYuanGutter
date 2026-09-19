package com.example.taoyuangutter.gutter

import android.content.ContextWrapper
import com.example.taoyuangutter.api.DitchNode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoUploadManagerPendingPhotosTest {
    @Test
    fun successfulPhotosRemainOutOfPendingUploadsAfterWaypointReversal() = runBlocking {
        val first = waypoint("uid-first", "101")
        val second = waypoint("uid-second", "202")
        val manager = PhotoUploadManager(ContextWrapper(null))

        val pendingCount = manager.countPendingPhotos(
            waypoints = listOf(second, first),
            nodes = listOf(node(2), node(1))
        )

        assertEquals(0, pendingCount)
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
