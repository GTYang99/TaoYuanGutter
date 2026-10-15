package com.example.taoyuangutter

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.taoyuangutter.api.NodeDetails
import com.example.taoyuangutter.gutter.GutterFormActivity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GutterImportExistingWaypointUiTest {

    @Test
    fun importingWithoutLocalPhotosDoesNotShowMissingPhotoToast() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scenario = ActivityScenario.launch<GutterFormActivity>(
            GutterFormActivity.newIntent(
                context = context,
                labels = arrayListOf("起點"),
                lats = doubleArrayOf(24.0),
                lngs = doubleArrayOf(121.0),
                basicData = hashMapOf("NODE_TYP" to "1"),
                sessionDraftId = 0L,
                sessionIsOffline = true
            )
        )

        try {
            scenario.onActivity { activity ->
                val handler = GutterFormActivity::class.java
                    .getDeclaredMethod("handleImportedNodeDetails", NodeDetails::class.java)
                    .apply { isAccessible = true }
                handler.invoke(activity, importedNodeWithoutPhotos())
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            onView(
                withText("匯入完成，但第1張、第2張、第3張照片未取得，請至照片頁補拍")
            ).check(doesNotExist())
        } finally {
            scenario.close()
        }
    }

    private fun importedNodeWithoutPhotos(): NodeDetails = NodeDetails(
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
