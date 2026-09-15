package com.example.taoyuangutter.api

import com.example.taoyuangutter.common.PhotoCapturedAtResolver
import okio.Buffer
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NodeImageMultipartBodyTest {

    @Test
    fun includesCapturedAtForOnePhotoRequest() {
        val image = File.createTempFile("node-image", ".jpg")
        try {
            image.writeBytes(byteArrayOf(1, 2, 3))
            val body = buildNodeImageMultipartBody(
                file = image,
                nodeId = 42,
                fileCategory = 2,
                capturedAt = "2026-09-15 10:11:12"
            )
            val wire = Buffer().use { buffer ->
                body.writeTo(buffer)
                buffer.readUtf8()
            }

            assertTrue(wire.contains("name=\"node_id\""))
            assertTrue(wire.contains("\r\n42\r\n"))
            assertTrue(wire.contains("name=\"fileCategory\""))
            assertTrue(wire.contains("\r\n2\r\n"))
            assertTrue(wire.contains("name=\"captured_at\""))
            assertTrue(wire.contains("2026-09-15 10:11:12"))
        } finally {
            image.delete()
        }
    }

    @Test
    fun currentTimeUsesExistingApiFormat() {
        assertTrue(PhotoCapturedAtResolver.currentTime().matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")))
    }
}
