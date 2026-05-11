package com.example.taoyuangutter.main

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import com.example.taoyuangutter.R
import com.example.taoyuangutter.databinding.ActivityMainBinding
import com.google.android.gms.maps.model.LatLng

class NoDitchModeUiController(
    private val context: Context,
    private val binding: ActivityMainBinding,
    private val onMainButtonsEnabledChanged: (Boolean) -> Unit,
    private val onExitRequested: () -> Unit,
    private val onResetRequested: () -> Unit,
    private val onSubmitRequested: (note: String) -> Unit
) {
    private var panelBaseBottomMarginPx: Int? = null

    fun setupPanelInsets() {
        val panel = binding.noDitchPanel.root
        if (panelBaseBottomMarginPx == null) {
            val lp = panel.layoutParams as? ViewGroup.MarginLayoutParams
            panelBaseBottomMarginPx = lp?.bottomMargin ?: 0
        }
        ViewCompat.setOnApplyWindowInsetsListener(panel) { _, insets ->
            val navBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val bottomInset = maxOf(navBottom, imeBottom)
            val base = panelBaseBottomMarginPx ?: 0
            panel.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = base + bottomInset
            }
            insets
        }
        ViewCompat.requestApplyInsets(panel)
    }

    fun bind() {
        binding.noDitchPanel.btnNoDitchLeft.setOnClickListener { onLeftClicked() }
        binding.noDitchPanel.btnNoDitchRight.setOnClickListener { onRightClicked() }
        binding.noDitchPanel.etNoDitchNote.doAfterTextChanged { updateSubmitEnabled() }
    }

    fun enter() {
        binding.noDitchPanel.root.visibility = View.VISIBLE
        binding.btnReportNoDitch.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary))
        binding.btnReportNoDitch.imageTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.white))
        onMainButtonsEnabledChanged(false)
        renderNoPickState()
    }

    fun exit() {
        binding.noDitchPanel.root.visibility = View.GONE
        binding.btnReportNoDitch.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
        binding.btnReportNoDitch.imageTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary))
        onMainButtonsEnabledChanged(true)
        clearInput()
    }

    fun setPickedLatLng(latLng: LatLng?) {
        if (latLng == null) {
            renderNoPickState()
        } else {
            renderPickedState(latLng)
        }
    }

    fun setSubmitting(submitting: Boolean) {
        binding.noDitchPanel.btnNoDitchLeft.isEnabled = !submitting
        binding.noDitchPanel.btnNoDitchRight.isEnabled = !submitting && isSubmitEnabled()
        binding.noDitchPanel.etNoDitchNote.isEnabled = !submitting
    }

    private fun onLeftClicked() {
        if (binding.noDitchPanel.tilNoDitchNote.visibility != View.VISIBLE) {
            onExitRequested()
        } else {
            onResetRequested()
        }
    }

    private fun onRightClicked() {
        val note = binding.noDitchPanel.etNoDitchNote.text?.toString().orEmpty().trim()
        if (note.isEmpty()) {
            binding.noDitchPanel.tilNoDitchNote.error = context.getString(R.string.no_ditch_note_hint)
            return
        }
        binding.noDitchPanel.tilNoDitchNote.error = null
        onSubmitRequested(note)
    }

    private fun renderNoPickState() {
        binding.noDitchPanel.tvNoDitchTitle.text = context.getString(R.string.no_ditch_mode_title)
        binding.noDitchPanel.tvNoDitchSubtitle.text = context.getString(R.string.no_ditch_pick_hint)
        binding.noDitchPanel.tvNoDitchLatLng.visibility = View.GONE
        binding.noDitchPanel.tilNoDitchNote.visibility = View.GONE
        binding.noDitchPanel.btnNoDitchRight.visibility = View.GONE
        binding.noDitchPanel.btnNoDitchLeft.text = context.getString(R.string.no_ditch_back)
        clearInput()
    }

    private fun renderPickedState(latLng: LatLng) {
        binding.noDitchPanel.tvNoDitchTitle.text = context.getString(R.string.no_ditch_input_title)
        binding.noDitchPanel.tvNoDitchSubtitle.text = context.getString(R.string.no_ditch_note_subtitle)
        binding.noDitchPanel.tvNoDitchLatLng.visibility = View.VISIBLE
        binding.noDitchPanel.tvNoDitchLatLng.text = context.getString(
            R.string.no_ditch_latlng_format,
            latLng.latitude,
            latLng.longitude
        )
        binding.noDitchPanel.tilNoDitchNote.visibility = View.VISIBLE
        binding.noDitchPanel.btnNoDitchRight.visibility = View.VISIBLE
        binding.noDitchPanel.btnNoDitchLeft.text = context.getString(R.string.no_ditch_reset)
        updateSubmitEnabled()
    }

    private fun clearInput() {
        binding.noDitchPanel.etNoDitchNote.setText("")
        binding.noDitchPanel.tilNoDitchNote.error = null
        updateSubmitEnabled()
    }

    private fun isSubmitEnabled(): Boolean {
        val note = binding.noDitchPanel.etNoDitchNote.text?.toString().orEmpty().trim()
        return note.isNotEmpty()
    }

    private fun updateSubmitEnabled() {
        val enabled = isSubmitEnabled()
        binding.noDitchPanel.btnNoDitchRight.isEnabled = enabled
        binding.noDitchPanel.btnNoDitchRight.alpha = if (enabled) 1f else 0.5f
        binding.noDitchPanel.btnNoDitchRight.setTextColor(
            ContextCompat.getColor(
                context,
                if (enabled) R.color.colorPrimary else R.color.textColorSecondary
            )
        )
    }
}
