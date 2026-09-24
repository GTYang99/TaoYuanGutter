package com.example.taoyuangutter.pending

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.taoyuangutter.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class PendingDraftAdapterUiTest {
    private val context = ContextThemeWrapper(
        ApplicationProvider.getApplicationContext(),
        R.style.Theme_TaoYuanGutter
    )

    @Test
    fun submittedAndUnsubmittedTagsUseRequiredStylesAndCallbacks() {
        val clicked = mutableListOf<Long>()
        val longClicked = mutableListOf<Long>()
        val submitted = draft(hasSubmittedStoreDitch = true)
        val unsubmitted = draft(hasSubmittedStoreDitch = false, id = 924L)
        val adapter = PendingDraftAdapter(
            items = mutableListOf(submitted, unsubmitted),
            onItemClick = { clicked += it.id },
            onItemLongClick = { longClicked += it.id }
        )
        val parent = FrameLayout(context)

        val submittedHolder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(submittedHolder, 0)
        val submittedTag = submittedHolder.binding.tvPendingDraftSubmissionTag
        assertEquals("曾提交過上傳", submittedTag.text.toString())
        assertEquals(View.VISIBLE, submittedTag.visibility)
        assertEquals(Color.WHITE, submittedTag.currentTextColor)
        assertEquals(Color.rgb(0x62, 0x36, 0xFF), (submittedTag.background as ColorDrawable).color)

        val unsubmittedHolder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(unsubmittedHolder, 1)
        val unsubmittedTag = unsubmittedHolder.binding.tvPendingDraftSubmissionTag
        assertEquals("未提交上傳", unsubmittedTag.text.toString())
        assertEquals(View.VISIBLE, unsubmittedTag.visibility)
        assertEquals(Color.rgb(0x62, 0x36, 0xFF), unsubmittedTag.currentTextColor)
        val outline = unsubmittedTag.background as? GradientDrawable
        assertNotNull(outline)
        assertEquals(dp(6), unsubmittedTag.paddingLeft)
        assertEquals(dp(2), unsubmittedTag.paddingTop)

        assertTrue(submittedHolder.binding.root.performClick())
        assertTrue(unsubmittedHolder.binding.root.performLongClick())
        assertEquals(listOf(923L), clicked)
        assertEquals(listOf(924L), longClicked)
    }

    @Test
    fun existingGutterDraftHidesSubmissionTag() {
        val adapter = PendingDraftAdapter(
            items = mutableListOf(
                draft(
                    hasSubmittedStoreDitch = true,
                    waypoints = listOf(
                        WaypointSnapshot(
                            type = "START",
                            basicData = hashMapOf("SPI_NUM" to "TYG-EXISTING")
                        )
                    )
                )
            ),
            onItemClick = {},
            onItemLongClick = {}
        )
        val holder = adapter.onCreateViewHolder(FrameLayout(context), 0)

        adapter.onBindViewHolder(holder, 0)

        assertEquals(View.GONE, holder.binding.tvPendingDraftSubmissionTag.visibility)
    }

    @Test
    fun draftItemPlacesSubtitlesBelowTitleRow() {
        val adapter = PendingDraftAdapter(
            items = mutableListOf(draft(hasSubmittedStoreDitch = true)),
            onItemClick = {},
            onItemLongClick = {}
        )
        val parent = FrameLayout(context)
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        parent.addView(holder.itemView)

        val width = dp(360)
        parent.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        parent.layout(0, 0, width, parent.measuredHeight)

        assertTrue(
            "建立時間副標題不可與標題列重疊",
            holder.binding.tvPendingDraftTime.top >= holder.binding.layoutPendingDraftTitle.bottom
        )
        assertTrue(
            "節點數量副標題不可與建立時間重疊",
            holder.binding.tvPendingDraftNodes.top >= holder.binding.tvPendingDraftTime.bottom
        )
    }

    private fun draft(
        hasSubmittedStoreDitch: Boolean,
        id: Long = 923L,
        waypoints: List<WaypointSnapshot> = emptyList()
    ) = GutterSessionDraft(
        id = id,
        savedAt = 2L,
        hasSubmittedStoreDitch = hasSubmittedStoreDitch,
        waypoints = waypoints
    )

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).roundToInt()
}
