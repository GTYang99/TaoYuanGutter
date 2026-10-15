package com.example.taoyuangutter

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.taoyuangutter.api.DitchDetails
import com.example.taoyuangutter.api.DitchNode
import com.example.taoyuangutter.api.DitchXyNum
import com.example.taoyuangutter.gutter.GutterInspectActivity
import org.hamcrest.CoreMatchers.not
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GutterInspectTieInUiTest {

    @Test
    fun tieInPointInspectionHidesExemptDetailsAndPhotos() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val scenario = ActivityScenario.launch<GutterInspectActivity>(
            GutterInspectActivity.newIntent(
                context = context,
                ditch = tieInDitch(),
                preloadedNodeDetailsJson = tieInNodeDetailsJson()
            )
        )

        scenario.use {
            onView(withText("點位資料（1）")).perform(click())
            onView(withId(R.id.layoutFields)).check(matches(not(hasDescendant(withText("溝蓋板厚度(公分)")))))
            onView(withId(R.id.layoutFields)).check(matches(not(hasDescendant(withText("側溝測量深度(公分)")))))
            onView(withId(R.id.layoutFields)).check(matches(not(hasDescendant(withText("側溝材質")))))
            onView(withId(R.id.layoutFields)).check(matches(not(hasDescendant(withText("連接管")))))
            onView(withId(R.id.layoutFields)).check(matches(not(hasDescendant(withText("側溝內徑寬度尺寸")))))
            onView(withId(R.id.layoutFields)).check(matches(not(hasDescendant(withText("側溝深度尺寸")))))
        }
    }

    private fun tieInDitch(): DitchDetails = DitchDetails(
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

    private fun tieInNodeDetailsJson(): String = """
        [{
          "node_id": 92011,
          "NODE_NUM": "1",
          "NODE_ATT": "1",
          "NODE_TYP": "2",
          "MAT_TYP": "1",
          "NODE_X": "303199.945",
          "NODE_Y": "2772661.352",
          "NODE_LE": "",
          "XY_NUM": "10362d0918pt006",
          "IS_CANTOPEN": "0",
          "IS_TIEINPOINT": "1",
          "COVER_DEP": 0,
          "NODE_DEP": 89,
          "NODE_WID": 78,
          "IS_BROKEN": "0",
          "IS_HANGING": "0",
          "IS_SILT": "0",
          "IS_CONNECTING": "0",
          "NOTE": "焊接",
          "is_pendingDeploy": "0",
          "is_virtual": "0",
          "node_img": []
        }]
    """.trimIndent()
}
