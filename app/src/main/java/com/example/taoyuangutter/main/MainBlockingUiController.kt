package com.example.taoyuangutter.main

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.view.animation.LinearInterpolator
import com.example.taoyuangutter.R
import com.example.taoyuangutter.databinding.ActivityMainBinding

class MainBlockingUiController(
    private val context: Context,
    private val binding: ActivityMainBinding,
    private val isMeasuring: () -> Boolean,
    private val isSheetActive: () -> Boolean = { false }
) {
    private var inspectLoadingVisible = false
    private var inspectLoadingMessage: String? = null
    private var photoUploadBlockingVisible = false
    private var photoUploadTotal = 0
    private var photoUploadCompleted = 0
    private var photoUploadFailed = 0
    private var targetPanelVisible = false
    private var spinnerAnimator: ObjectAnimator? = null

    fun setInspectLoading(visible: Boolean, message: String? = null) {
        inspectLoadingVisible = visible
        if (!message.isNullOrBlank()) inspectLoadingMessage = message
        // 進入 loading 時一律虛化背景
        if (visible) {
            setMainButtonsEnabled(false)
        } else {
            // loading 結束時，若目前沒有開啟表單且非測距模式，才還原按鈕
            if (!isSheetActive() && !isMeasuring()) {
                setMainButtonsEnabled(true)
            }
        }
        applyBlockingOverlay()
    }

    fun beginPhotoUpload(total: Int) {
        photoUploadTotal = total
        photoUploadCompleted = 0
        photoUploadFailed = 0
        setPhotoUploadBlocking(true)
    }

    fun recordPhotoUploadResult(success: Boolean) {
        photoUploadCompleted += 1
        if (!success) {
            photoUploadFailed += 1
        }
        applyBlockingOverlay()
    }

    fun endPhotoUpload() {
        setPhotoUploadBlocking(false)
    }

    fun isBusyBlocking(): Boolean {
        return inspectLoadingVisible || photoUploadBlockingVisible
    }

    fun setTargetPanelVisible(visible: Boolean) {
        targetPanelVisible = visible
        if (visible && !isBusyBlocking()) {
            applyTargetPanelPolicy()
        } else if (!visible) {
            setMainButtonsVisible(true)
        }
    }

    private fun setMainButtonsVisible(visible: Boolean) {
        val value = if (visible) View.VISIBLE else View.GONE
        binding.btnLogout.visibility = value
        binding.btnAddGutter.visibility = value
        binding.btnLegend.visibility = value
        binding.btnLayers.visibility = value
        binding.btnViewDrafts.visibility = value
        binding.btnMyLocation.visibility = value
        binding.btnReportNoDitch.visibility = value
        binding.btnMeasureDistance.visibility = value
    }

    private fun applyTargetPanelPolicy() {
        setMainButtonsVisible(false)
        binding.btnMeasureDistance.visibility = View.VISIBLE
        binding.btnMeasureDistance.isEnabled = true
        binding.btnMeasureDistance.isClickable = true
        binding.btnMeasureDistance.alpha = 1f
    }

    fun setMainButtonsEnabled(enabled: Boolean) {
        if (targetPanelVisible && !isBusyBlocking()) {
            applyTargetPanelPolicy()
            return
        }
        fun setFabEnabled(view: View, value: Boolean) {
            view.isEnabled = value
            view.isClickable = value
            view.alpha = if (value) 1f else 0.35f
        }

        setFabEnabled(binding.btnAddGutter, enabled)
        setFabEnabled(binding.btnLegend, enabled)
        setFabEnabled(binding.btnLayers, enabled)
        setFabEnabled(binding.btnViewDrafts, enabled)
        setFabEnabled(binding.btnMyLocation, enabled)
        setFabEnabled(binding.btnReportNoDitch, enabled)
        setFabEnabled(binding.btnMeasureDistance, enabled)
        binding.btnLogout.isEnabled = enabled
        binding.btnLogout.isClickable = enabled
        binding.btnLogout.alpha = if (enabled) 1f else 0.35f
    }

    fun setMainButtonsEnabledDuringMeasureMode(enabled: Boolean) {
        // 測距模式需要保留 btnMeasureDistance 作為「模式切換」入口，因此不在此控制該按鈕。
        setMainButtonsEnabled(enabled)
        // 其他模式入口（回報無側溝）在測距中應暫時不可操作。
        binding.btnReportNoDitch.isEnabled = enabled
        binding.btnReportNoDitch.isClickable = enabled
        binding.btnReportNoDitch.alpha = if (enabled) 1f else 0.35f
        binding.btnMeasureDistance.isEnabled = true
        binding.btnMeasureDistance.isClickable = true
        binding.btnMeasureDistance.alpha = 1f
    }

    fun setMainButtonsEnabledDuringNoDitchMode(enabled: Boolean) {
        // 回報無側溝模式需要保留 btnReportNoDitch 作為「模式入口/指示」，因此不在此控制該按鈕。
        setMainButtonsEnabled(enabled)
        // 其他模式入口（例如測距）在回報模式中應暫時不可操作。
        binding.btnMeasureDistance.isEnabled = enabled
        binding.btnMeasureDistance.isClickable = enabled
        binding.btnMeasureDistance.alpha = if (enabled) 1f else 0.35f
        binding.btnReportNoDitch.isEnabled = true
        binding.btnReportNoDitch.isClickable = true
        binding.btnReportNoDitch.alpha = 1f
    }

    private fun setPhotoUploadBlocking(visible: Boolean) {
        photoUploadBlockingVisible = visible
        if (!visible) {
            photoUploadTotal = 0
            photoUploadCompleted = 0
            photoUploadFailed = 0
        }
        
        if (visible) {
            setMainButtonsEnabled(false)
        } else {
            // 上傳結束時，若目前沒有開啟表單且非測距模式，才還原按鈕
            if (!isSheetActive() && !isMeasuring()) {
                setMainButtonsEnabled(true)
            }
        }
        applyBlockingOverlay()
    }

    private fun applyBlockingOverlay() {
        val visible = inspectLoadingVisible || photoUploadBlockingVisible
        binding.inspectLoadingOverlay.visibility = if (visible) View.VISIBLE else View.GONE
        // Keep the shared submission/photo indicator explicitly indeterminate on every state update.
        binding.pbInspectLoading.isIndeterminate = true
        if (visible) {
            // Explicitly start the drawable: show() alone does not guarantee a running state.
            binding.pbInspectLoading.show()
            binding.pbInspectLoading.indeterminateDrawable?.start()
            startSpinnerAnimation()
        } else {
            binding.pbInspectLoading.indeterminateDrawable?.stop()
            stopSpinnerAnimation()
            binding.pbInspectLoading.hide()
        }
        // Show progress once, in the shared overlay centre. The separate
        // top-level label is retained only for layout compatibility.
        binding.tvPhotoUploadProgress.visibility = View.GONE
        binding.tvInspectLoading.visibility = View.VISIBLE
        if (photoUploadBlockingVisible) {
            val progressText = context.getString(
                R.string.msg_photo_upload_overlay_progress,
                photoUploadCompleted,
                photoUploadTotal,
                photoUploadFailed
            )
            binding.tvInspectLoading.text = progressText
            binding.tvPhotoUploadProgress.text = progressText
        } else if (!inspectLoadingMessage.isNullOrBlank()) {
            binding.tvInspectLoading.text = inspectLoadingMessage
        }
    }

    private fun startSpinnerAnimation() {
        val animator = spinnerAnimator ?: createSpinnerAnimator(binding.pbInspectLoading).also {
            spinnerAnimator = it
        }
        if (!animator.isStarted) animator.start()
    }

    private fun stopSpinnerAnimation() {
        spinnerAnimator?.cancel()
        binding.pbInspectLoading.rotation = 0f
    }

    companion object {
        internal fun createSpinnerAnimator(target: View): ObjectAnimator =
            ObjectAnimator.ofFloat(target, View.ROTATION, 0f, 360f).apply {
                duration = 900L
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
            }
    }
}
