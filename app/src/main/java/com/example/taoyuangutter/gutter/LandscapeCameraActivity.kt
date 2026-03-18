package com.example.taoyuangutter.gutter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
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
                if (orientation == ORIENTATION_UNKNOWN) return
                val landscape = orientation in 60..120 || orientation in 240..300
                if (landscape != deviceIsLandscape) {
                    deviceIsLandscape = landscape
                    updateOrientationUI(landscape)
                }
            }
        }
        updateOrientationUI(false)
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
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        }

        val rotation = binding.viewFinder.display?.rotation ?: Surface.ROTATION_0
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(rotation)
            .build()

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
        } catch (e: Exception) {
            Toast.makeText(this, "相機初始化失敗", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this@LandscapeCameraActivity, "拍照失敗", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}
