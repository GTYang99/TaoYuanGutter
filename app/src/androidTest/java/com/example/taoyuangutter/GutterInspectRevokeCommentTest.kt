package com.example.taoyuangutter

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.taoyuangutter.api.DitchDetails
import com.example.taoyuangutter.api.DitchXyNum
import com.example.taoyuangutter.gutter.GutterInspectActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GutterInspectRevokeCommentTest {

    @Test
    fun showsRevokeCommentAboveXyNumAndKeepsItAfterRecreate() {
        val comment = "終點，若現場淤泥、土石或雜物垃圾厚度未達溝體淨深一半，\n僅生長大量雜草或樹枝堆積，請協助調整"
        val scenario = launchInspect(createDitchDetails(spiState = "2", revokeComment = comment))

        scenario.use {
            assertRevokeCommentVisible(it, comment)
            it.recreate()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertRevokeCommentVisible(it, comment)
        }
    }

    @Test
    fun hidesRevokeCommentWhenStateOrCommentDoesNotQualify() {
        listOf(
            createDitchDetails(spiState = "2", revokeComment = ""),
            createDitchDetails(spiState = "2", revokeComment = "   "),
            createDitchDetails(spiState = "1", revokeComment = "需調整"),
            createDitchDetails(spiState = "3", revokeComment = "需調整"),
            createDitchDetails(spiState = null, revokeComment = "需調整"),
            createDitchDetails(spiState = "unknown", revokeComment = "需調整")
        ).forEach { ditch ->
            launchInspect(ditch).use { scenario ->
                scenario.onActivity { activity ->
                    assertEquals(View.GONE, activity.findViewById<View>(R.id.revokeCommentContainer).visibility)
                }
            }
        }
    }

    private fun assertRevokeCommentVisible(
        scenario: ActivityScenario<GutterInspectActivity>,
        comment: String
    ) {
        scenario.onActivity { activity ->
            val container = activity.findViewById<LinearLayout>(R.id.revokeCommentContainer)
            val title = activity.findViewById<TextView>(R.id.tvRevokeCommentTitle)
            val body = activity.findViewById<TextView>(R.id.tvRevokeComment)
            val xyNum = activity.findViewById<TextView>(R.id.tvSpiNum)
            val expectedRed = ContextCompat.getColor(activity, R.color.revoke_comment_red)

            assertEquals(View.VISIBLE, container.visibility)
            assertEquals("退回原因", title.text.toString())
            assertEquals(comment, body.text.toString())
            assertEquals(expectedRed, title.currentTextColor)
            assertTrue(title.typeface.isBold)
            assertTrue(container.top < xyNum.top)
        }
    }

    private fun launchInspect(ditch: DitchDetails): ActivityScenario<GutterInspectActivity> {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = GutterInspectActivity.newIntent(context, ditch)
        return ActivityScenario.launch(intent)
    }

    private fun createDitchDetails(spiState: String?, revokeComment: String?): DitchDetails {
        return DitchDetails(
            ditchId = 1306,
            spiNum = "3202-ZZ-00979",
            spiState = spiState,
            isCurve = "0",
            isPendingDeploy = "0",
            isVirtual = "0",
            spiTyp = "1",
            xyNum = DitchXyNum(
                start = "A0624pt46",
                nodes = listOf("A0624pt45", "A0624pt44"),
                end = "A0624pt43"
            ),
            strX = "268081.743",
            strY = "2766981.756",
            endX = "267985.071",
            endY = "2767042.770",
            strLe = "",
            endLe = "",
            nodeXy = "268051.061,2766980.608_268023.674,2767006.712",
            strDep = 90,
            endDep = 63,
            strWid = 59,
            endWid = 62,
            leng = "121.36",
            slop = "0.00000",
            note = "",
            nodes = emptyList(),
            revokeComment = revokeComment
        )
    }
}
