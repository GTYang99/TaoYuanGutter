package com.example.taoyuangutter.main

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.example.taoyuangutter.R
import com.example.taoyuangutter.databinding.ActivityMainBinding
import com.example.taoyuangutter.map.DistanceMeasureManager
import com.example.taoyuangutter.map.MeasureConfig

class MeasureModeUiController(
    private val context: Context,
    private val binding: ActivityMainBinding,
    private val measureConfig: MeasureConfig,
    private val onMainButtonsEnabledChanged: (Boolean) -> Unit
) {
    private var measurePanelBaseBottomMarginPx: Int? = null

    fun setupPanelInsets() {
        val panel = binding.measurePanel.root
        if (measurePanelBaseBottomMarginPx == null) {
            val lp = panel.layoutParams as? ViewGroup.MarginLayoutParams
            measurePanelBaseBottomMarginPx = lp?.bottomMargin ?: 0
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val navBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val bottomInset = maxOf(navBottom, imeBottom)
            val base = measurePanelBaseBottomMarginPx ?: 0
            panel.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = base + bottomInset
            }
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    fun enter(mgr: DistanceMeasureManager?) {
        if (mgr == null) return
        mgr.enter()
        binding.ivMeasureCrosshair.setImageResource(measureConfig.crosshairResId)
        binding.ivMeasureCrosshair.visibility = View.GONE
        binding.measurePanel.root.visibility = View.VISIBLE
        binding.btnMeasureDistance.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary))
        binding.btnMeasureDistance.imageTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.white))
        onMainButtonsEnabledChanged(false)
    }

    fun exit(mgr: DistanceMeasureManager?) {
        if (mgr == null) return
        mgr.exit()
        binding.ivMeasureCrosshair.visibility = View.GONE
        binding.measurePanel.root.visibility = View.GONE
        binding.measurePanel.tvMeasureDistance.text = context.getString(R.string.measure_tap_hint)
        binding.btnMeasureDistance.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
        binding.btnMeasureDistance.imageTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary))
        onMainButtonsEnabledChanged(true)
    }

    fun updateDistanceDisplay(meters: Double?) {
        if (meters == null) {
            binding.ivMeasureCrosshair.visibility = View.GONE
            binding.measurePanel.tvMeasureDistance.text = context.getString(R.string.measure_tap_hint)
        } else {
            binding.ivMeasureCrosshair.visibility = View.VISIBLE
            binding.measurePanel.tvMeasureDistance.text = DistanceMeasureManager.formatDistance(meters)
        }
    }
}
