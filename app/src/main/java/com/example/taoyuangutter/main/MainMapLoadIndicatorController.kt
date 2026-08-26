package com.example.taoyuangutter.main

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.core.content.ContextCompat
import com.example.taoyuangutter.R
import com.example.taoyuangutter.databinding.ActivityMainBinding
import java.util.Locale

class MainMapLoadIndicatorController(
    private val context: Context,
    private val binding: ActivityMainBinding
) {
    private val stateMachine = MainMapLoadIndicatorStateMachine()
    private val baseBottomMarginPx by lazy {
        val lp = binding.mainMapLoadIndicatorContainer.layoutParams as? ViewGroup.MarginLayoutParams
        lp?.bottomMargin ?: 0
    }

    fun setBottomInset(bottomInsetPx: Int) {
        binding.mainMapLoadIndicatorContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = baseBottomMarginPx + bottomInsetPx.coerceAtLeast(0)
        }
    }

    fun syncZoom(zoom: Float) {
        render(stateMachine.syncZoom(zoom))
    }

    fun prepareForNewOperation(zoom: Float) {
        render(stateMachine.prepareForNewOperation(zoom))
    }

    fun beginLoading(zoom: Float) {
        render(stateMachine.beginLoading(zoom))
    }

    fun finishLoading(zoom: Float) {
        render(stateMachine.finishLoading(zoom))
    }

    fun failLoading(zoom: Float) {
        render(stateMachine.failLoading(zoom))
    }

    fun currentState(): MainMapLoadIndicatorState = stateMachine.currentState()

    private fun render(state: MainMapLoadIndicatorState) {
        val container = binding.mainMapLoadIndicatorContainer
        val icon = binding.ivMainMapLoadIndicatorIcon
        val text = binding.tvMainMapLoadIndicatorText
        val progress = binding.pbMainMapLoadIndicator

        when (state.mode) {
            MainMapLoadIndicatorMode.HIDDEN -> {
                container.visibility = View.GONE
                progress.visibility = View.GONE
            }
            MainMapLoadIndicatorMode.LOW_ZOOM -> {
                container.visibility = View.VISIBLE
                icon.setImageResource(R.drawable.ic_close)
                icon.imageTintList = ContextCompat.getColorStateList(context, R.color.colorOnCancel)
                progress.visibility = View.GONE
                text.text = context.getString(
                    R.string.msg_main_map_low_zoom,
                    formatZoomLevel(state.zoom)
                )
            }
            MainMapLoadIndicatorMode.LOADING -> {
                container.visibility = View.VISIBLE
                icon.setImageResource(R.drawable.ic_check)
                icon.imageTintList = ContextCompat.getColorStateList(context, R.color.colorPrimary)
                progress.visibility = View.VISIBLE
                text.text = context.getString(
                    R.string.msg_main_map_loading,
                    formatZoomLevel(state.zoom)
                )
            }
            MainMapLoadIndicatorMode.ERROR -> {
                container.visibility = View.VISIBLE
                icon.setImageResource(R.drawable.ic_exclamationmark_bubble)
                icon.imageTintList = ContextCompat.getColorStateList(context, R.color.colorOnCancel)
                progress.visibility = View.GONE
                text.text = context.getString(
                    R.string.msg_main_map_failed,
                    formatZoomLevel(state.zoom)
                )
            }
        }
    }

    private fun formatZoomLevel(zoom: Float): String {
        return String.format(Locale.getDefault(), "%.1f%s", zoom, context.getString(R.string.msg_zoom_suffix))
    }
}
