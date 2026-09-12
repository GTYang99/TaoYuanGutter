package com.example.taoyuangutter.api

import com.google.gson.Gson
import org.junit.Assert.assertNull
import org.junit.Test

class NodeDetailsExtensionsTest {
    @Test
    fun safeCapturedAtTreatsExplicitNullAsMissingTimestamp() {
        val node = Gson().fromJson(
            """{"node_id":45,"captured_at":null}""",
            NodeDetails::class.java
        )

        assertNull(node.safeCapturedAt(0))
    }
}
