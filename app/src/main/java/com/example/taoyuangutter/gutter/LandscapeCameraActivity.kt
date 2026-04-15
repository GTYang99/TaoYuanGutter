package com.example.taoyuangutter.gutter

import com.example.taoyuangutter.R
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import com.example.taoyuangutter.databinding.ActivityLandscapeCameraBinding
import java.io.File

/**
 * 強制橫向拍照的自訂相機 Activity（CameraX）。
 */
class LandscapeCameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLandscapeCameraBinding
    private var imageCapture: ImageCapture? = null
    private var outputFile: File? = null

    private lateinit var orientationListener: OrientationEventListener
    private var deviceIsLandscape = false
    private var lastDisplayRotation: Int = Surface.ROTATION_0

    companion object {
        private const val EXTRA_OUTPUT_PATH = "output_path"
        const val EXTRA_RESULT_PATH = "result_path"

        fun newIntent(context: Context, outputFile: File): Intent =
            Intent(context, LandscapeCameraActivity::class.java).apply {
                putExtra(EXTRA_OUTPUT_PATH, outputFile.absolutePath)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        binding = ActivityLandscapeCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val path = intent.getStringExtra(EXTRA_OUTPUT_PATH)
        if (path == null) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        outputFile = File(path)

        setupOrientationListener()
        startCamera()
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        orientationListener.enable()
    }

    override fun onPause() {
        super.onPause()
        orientationListener.disable()
    }

    private fun setupOrientationListener() {
        orientationListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                // ORIENTATION_UNKNOWN = 感應器無法讀取（模擬器常見），保持現有狀態不做任何切換
                if (orientation == ORIENTATION_UNKNOWN) return

                // 旋轉 90 ↔ 270 仍屬於 landscape；但我們需要依 display rotation 調整控制列位置
                val rotation = getDisplayRotation()
                if (rotation != lastDisplayRotation) {
                    lastDisplayRotation = rotation
                    applyControlsSide(rotation)
                    imageCapture?.targetRotation = rotation
                }

                val landscape = orientation in 60..120 || orientation in 240..300
                if (landscape != deviceIsLandscape) {
                    deviceIsLandscape = landscape
                    updateOrientationUI(landscape)
                }
            }
        }

        // ── 用 display rotation 初始化（而非假設直立）──────────────────────
        // 因 manifest 宣告 sensorLandscape，Activity 啟動時 display 已是橫向；
        // 實體裝置與模擬器均可正確初始化，避免模擬器感應器不動時按鈕永遠 disabled。
        val rotation = getDisplayRotation()
        val isDisplayLandscape = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270
        deviceIsLandscape = isDisplayLandscape
        lastDisplayRotation = rotation
        applyControlsSide(rotation)
        updateOrientationUI(isDisplayLandscape)
    }

    private fun getDisplayRotation(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.rotation ?: Surface.ROTATION_0
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.rotation
        }

    /**
     * 讓快門固定在「手機右側短邊」：
     * - 使用者順時針旋轉 90°（ROTATION_90）橫拿時，快門在螢幕左側，左手可按
     * - 使用者逆時針旋轉 90°（ROTATION_270）橫拿時，快門在螢幕右側，右手可按
     *
     * 若現場測試左右相反，只要對調 ROTATION_90/270 的分支即可。
     */
    private fun applyControlsSide(rotation: Int) {
        val set = ConstraintSet()
        set.clone(binding.root)

        set.clear(R.id.controlsContainer, ConstraintSet.START)
        set.clear(R.id.controlsContainer, ConstraintSet.END)

        when (rotation) {
            Surface.ROTATION_90 -> {
                set.connect(
                    R.id.controlsContainer,
                    ConstraintSet.END,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.END
                )
            }
            Surface.ROTATION_270 -> {
                set.connect(
                    R.id.controlsContainer,
                    ConstraintSet.START,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.START
                )
            }
            else -> {
                set.connect(
                    R.id.controlsContainer,
                    ConstraintSet.END,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.END
                )
            }
        }

        set.applyTo(binding.root)
    }

    private fun updateOrientationUI(isLandscape: Boolean) {
        binding.orientationWarning.visibility = if (isLandscape) View.GONE else View.VISIBLE
        binding.btnCapture.isEnabled = isLandscape
        binding.btnCapture.alpha = if (isLandscape) 1f else 0.4f
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindUseCases(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindUseCases(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

        val rotation = binding.viewFinder.display?.rotation ?: Surface.ROTATION_0
        imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(rotation)
            .build()

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.msg_camera_init_failed), Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private fun setupButtons() {
        binding.btnCapture.setOnClickListener { capturePhoto() }
        binding.btnCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private fun capturePhoto() {
        val capture = imageCapture ?: return
        val file    = outputFile   ?: return

        binding.btnCapture.isEnabled = false

        val options = ImageCapture.OutputFileOptions.Builder(file).build()

        capture.takePicture(
            options,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val resultIntent = Intent().apply {
                        putExtra(EXTRA_RESULT_PATH, file.absolutePath)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }

                override fun onError(exception: ImageCaptureException) {
                    binding.btnCapture.isEnabled = deviceIsLandscape
                    binding.btnCapture.alpha = if (deviceIsLandscape) 1f else 0.4f
                    Toast.makeText(this@LandscapeCameraActivity, getString(R.string.msg_camera_capture_failed), Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}
