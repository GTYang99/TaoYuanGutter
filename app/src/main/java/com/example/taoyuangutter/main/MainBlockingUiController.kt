package com.example.taoyuangutter.main

import android.content.Context
import android.view.View
import com.example.taoyuangutter.R
import com.example.taoyuangutter.databinding.ActivityMainBinding

class MainBlockingUiController(
    private val context: Context,
    private val binding: ActivityMainBinding,
    private val isMeasuring: () -> Boolean
) {
    private var inspectLoadingVisible = false
    private var inspectLoadingMessage: String? = null
    private var photoUploadBlockingVisible = false
    private var photoUploadTotal = 0
    private var photoUploadCompleted = 0
    private var photoUploadFailed = 0

    fun setInspectLoading(visible: Boolean, message: String? = null) {
        inspectLoadingVisible = visible
        if (!message.isNullOrBlank()) inspectLoadingMessage = message
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

    fun setMainButtonsEnabled(enabled: Boolean) {
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
        setFabEnabled(binding.btnMeasureDistance, enabled)
        setFabEnabled(binding.btnReportNoDitch, enabled)
        binding.btnLogout.isEnabled = enabled
        binding.btnLogout.isClickable = enabled
        binding.btnLogout.alpha = if (enabled) 1f else 0.35f
    }

    private fun setPhotoUploadBlocking(visible: Boolean) {
        photoUploadBlockingVisible = visible
        if (!visible) {
            photoUploadTotal = 0
            photoUploadCompleted = 0
            photoUploadFailed = 0
        }
        if (!isMeasuring()) {
            setMainButtonsEnabled(!visible)
        }
        applyBlockingOverlay()
    }

    private fun applyBlockingOverlay() {
        val visible = inspectLoadingVisible || photoUploadBlockingVisible
        binding.inspectLoadingOverlay.visibility = if (visible) View.VISIBLE else View.GONE
        binding.tvPhotoUploadProgress.visibility = View.GONE
        binding.tvInspectLoading.visibility = View.VISIBLE
        if (photoUploadBlockingVisible) {
            binding.tvInspectLoading.text = context.getString(
                R.string.msg_photo_upload_overlay_progress,
                photoUploadCompleted,
                photoUploadTotal,
                photoUploadFailed
            )
        } else if (!inspectLoadingMessage.isNullOrBlank()) {
            binding.tvInspectLoading.text = inspectLoadingMessage
        }
    }
}
