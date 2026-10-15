package com.example.taoyuangutter.api

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionModelsTest {
    @Test fun comparesNumericComponentsRatherThanText() {
        assertTrue(AppVersion.parse("1.10.0")!! > AppVersion.parse("1.9.9")!!)
        assertTrue(AppVersion.parse("2.0.0")!! > AppVersion.parse("1.99.99")!!)
        assertEquals(0, AppVersion.parse("1.02.3")!!.compareTo(AppVersion.parse("1.2.3")!!))
    }

    @Test fun rejectsMalformedOrOverflowingVersionComponents() {
        listOf("1.2", "1.2.3.4", "1.x.3", "1.2.3-beta", " 1.2.3", "1.2.2147483648").forEach {
            assertNull(AppVersion.parse(it))
        }
    }

    @Test fun acceptsExactlyOneAndroidRecordWithHttpsUrl() {
        val body = Gson().fromJson(
            """{"success":true,"data":[{"type":"android","version":"2.0.1","url":"https://updates.example/app.apk"}]}""",
            AppVersionResponse::class.java
        )
        val result = body.toAvailableAndroidUpdateOrNull()!!
        assertEquals("2.0.1", result.version.toString())
        assertEquals("https://updates.example/app.apk", result.apkUrl.toString())
    }

    @Test fun rejectsBadEnvelopeRecordsVersionAndUrl() {
        val gson = Gson()
        val samples = listOf(
            """{"success":false,"data":[{"type":"android","version":"2.0.1","url":"https://a.test/x.apk"}]}""",
            """{"success":true,"data":[]}""",
            """{"success":true,"data":[{"type":"ios","version":"2.0.1","url":"https://a.test/x.apk"}]}""",
            """{"success":true,"data":[{"type":"android","version":"2.0","url":"https://a.test/x.apk"}]}""",
            """{"success":true,"data":[{"type":"android","version":"2.0.1","url":"http://a.test/x.apk"}]}""",
            """{"success":true,"data":[{"type":"android","version":"2.0.1","url":"not a url"}]}""",
            """{"success":true,"data":[{"type":"android","version":"2.0.1","url":"https://a.test/x.apk"},{"type":"android","version":"2.0.2","url":"https://a.test/y.apk"}]}"""
        )
        samples.forEach { assertNull(gson.fromJson(it, AppVersionResponse::class.java).toAvailableAndroidUpdateOrNull()) }
    }
}
