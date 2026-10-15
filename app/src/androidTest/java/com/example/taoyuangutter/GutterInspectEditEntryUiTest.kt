package com.example.taoyuangutter

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.Lifecycle
import com.example.taoyuangutter.api.DitchDetails
import com.example.taoyuangutter.api.DitchNode
import com.example.taoyuangutter.api.DitchXyNum
import com.example.taoyuangutter.api.NodeDetails
import com.example.taoyuangutter.gutter.GutterInspectActivity
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GutterInspectEditEntryUiTest {

    @Test
    fun noServerPhotosEnterEditWithoutConfirmationDialog() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("taoyuan_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("auth_token", "ui-test-token").commit()

        val scenario = ActivityScenario.launch<GutterInspectActivity>(
            GutterInspectActivity.newIntent(
                context = context,
                ditch = noPhotoDitch(),
                canEdit = true,
                preloadedNodeDetailsJson = Gson().toJson(listOf(noPhotoNodeDetails()))
            )
        )

        try {
            onView(withId(R.id.btnEdit)).perform(click())
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        } finally {
            prefs.edit().remove("auth_token").commit()
            scenario.close()
        }
    }

    private fun noPhotoDitch(): DitchDetails = DitchDetails(
        ditchId = 9201,
        spiNum = "TEST-0920-1",
        spiState = "1",
        isCurve = "0",
        isPendingDeploy = "0",
        isVirtual = "0",
        spiTyp = "1",
        xyNum = DitchXyNum(start = "S-1", end = "E-1"),
        strX = "268081.743",
        strY = "2766981.756",
        endX = "267985.071",
        endY = "2767042.770",
        strLe = "",
        endLe = "",
        nodeXy = "",
        strDep = 90,
        endDep = 63,
        strWid = 59,
        endWid = 62,
        leng = "10",
        slop = "0",
        note = "",
        nodes = listOf(
            DitchNode(
                nodeId = 92011,
                nodeAtt = "1",
                nodeNum = "1",
                url = emptyList()
            )
        )
    )

    private fun noPhotoNodeDetails(): NodeDetails = NodeDetails(
        ditchId = "9201",
        nodeId = 92011,
        nodeNum = "1",
        nodeAttr = "1",
        nodeTyP = "2",
        matTyp = "1",
        nodeX = "121.000000",
        nodeY = "24.000000",
        nodeLe = "0",
        xyNum = "S-1",
        coverDep = "3",
        nodeDep = "20",
        nodeWid = "30",
        isCantOpen = "0",
        isTieInPoint = "0",
        isConnecting = "0",
        isBroken = "0",
        isHanging = "0",
        isSilt = "0",
        note = "",
        latitude = "24.000000",
        longitude = "121.000000",
        isPendingDeploy = "0",
        isVirtual = "0"
    )
}
