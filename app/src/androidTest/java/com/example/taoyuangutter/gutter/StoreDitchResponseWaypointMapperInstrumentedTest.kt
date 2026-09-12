package com.example.taoyuangutter.gutter

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.taoyuangutter.api.StoreDitchResponse
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoreDitchResponseWaypointMapperInstrumentedTest {
    @Test
    fun emulatorParsesStoreDitchPhotoIdsAndPersistsDraftSlots() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val response = Gson().fromJson(
            context.assets.open("store_ditch_success_response.json").bufferedReader().use { it.readText() },
            StoreDitchResponse::class.java
        )
        val node = response.data?.nodes?.single()
        assertNotNull(node)

        val waypoint = Waypoint(
            type = WaypointType.START,
            label = "起點",
            latLng = LatLng(25.0, 121.0)
        )
        val mapped = StoreDitchResponseWaypointMapper.apply(listOf(waypoint), listOf(node!!)).single()

        assertEquals("10888", mapped.basicData["_nodeId"])
        assertEquals("12135", mapped.basicData["photo1ImgId"])
        assertEquals("12134", mapped.basicData["photo2ImgId"])
        assertEquals("12133", mapped.basicData["photo3ImgId"])
    }
}
