package com.example.taoyuangutter.gutter

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.taoyuangutter.R
import com.example.taoyuangutter.databinding.FragmentCameraOverlayBinding
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 全螢幕相機 Overlay（嵌入 GutterFormActivity），避免 Activity 切換造成放大/旋轉卡頓。
 *
 * - 仍維持「需橫放才能拍」：未橫放顯示遮罩並禁用快門
 * - 兩指縮放：CameraX zoomRatio
 */
class CameraOverlayFragment : Fragment() {

    private var _binding: FragmentCameraOverlayBinding? = null
    private val binding get() = _binding!!

    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private var camera: Camera? = null
    private var outputFile: File? = null

    private lateinit var orientationListener: OrientationEventListener
    private var deviceIsLandscape = false
    private var lastSurfaceRotation: Int = Surface.ROTATION_0
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    companion object {
        private const val ARG_OUTPUT_PATH = "output_path"
        private const val ARG_SLOT = "slot"

        const val RESULT_KEY = "camera_overlay_result"
        const val RESULT_SLOT = "result_slot"
        const val RESULT_PATH = "result_path"
        const val RESULT_CODE = "result_code"

        fun newInstance(slot: Int, outputPath: String): CameraOverlayFragment =
            CameraOverlayFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SLOT, slot)
                    putString(ARG_OUTPUT_PATH, outputPath)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraOverlayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val path = arguments?.getString(ARG_OUTPUT_PATH)
        val slot = arguments?.getInt(ARG_SLOT, 0) ?: 0
        if (path.isNullOrBlank() || slot !in 1..3) {
            sendResult(Activity.RESULT_CANCELED, slot, null)
            return
        }
        outputFile = File(path)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    sendResult(Activity.RESULT_CANCELED, slot, null)
                }
            }
        )

        setupZoomGesture()
        applySystemBarInsets()
        setupOrientationListener()
        setupButtons(slot)
        startCamera()
    }

    override fun onResume() {
        super.onResume()
        orientationListener.enable()
    }

    override fun onPause() {
        super.onPause()
        orientationListener.disable()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupButtons(slot: Int) {
        binding.btnCancel.setOnClickListener {
            sendResult(Activity.RESULT_CANCELED, slot, null)
        }
        binding.btnCapture.setOnClickListener {
            if (!deviceIsLandscape) return@setOnClickListener
            capturePhoto(slot)
        }
    }

    private fun setupZoomGesture() {
        scaleGestureDetector = ScaleGestureDetector(
            requireContext(),
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val cam = camera ?: return false
                    val zoomState = cam.cameraInfo.zoomState.value ?: return false
                    val current = zoomState.zoomRatio
                    val target = (current * detector.scaleFactor)
                        .coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
                    cam.cameraControl.setZoomRatio(target)
                    return true
                }
            }
        )

        binding.viewFinder.setOnTouchListener { _, event ->
            val handled = scaleGestureDetector.onTouchEvent(event)
            when {
                event.pointerCount > 1 -> true
                handled -> true
                event.actionMasked == MotionEvent.ACTION_MOVE && scaleGestureDetector.isInProgress -> true
                else -> false
            }
        }
    }

    private fun applySystemBarInsets() {
        if (_binding == null) return
        val baseBottom = binding.controlsContainer.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            binding.controlsContainer.setPadding(
                binding.controlsContainer.paddingLeft,
                binding.controlsContainer.paddingTop,
                binding.controlsContainer.paddingRight,
                baseBottom + systemBottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun setupOrientationListener() {
        orientationListener = object : OrientationEventListener(requireContext()) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                // 強制橫向模式：只有在 60~120 或 240~300 範圍內才認為是「橫放」
                val landscape = orientation in 60..120 || orientation in 240..300
                if (landscape != deviceIsLandscape) {
                    deviceIsLandscape = landscape
                    updateOrientationUi(landscape)
                }

                // 依感測器方向更新 targetRotation：
                // - Activity 固定直立不旋轉，但使用者必須橫放手機拍照
                // - 透過 targetRotation 讓 ImageCapture 的照片上下方向與實際拍攝方向一致
                // OrientationEventListener 的角度定義下：
                // 90° 代表裝置左側朝上（應對應 ROTATION_270）
                // 270° 代表裝置右側朝上（應對應 ROTATION_90）
                val rotation = when {
                    orientation in 60..120 -> Surface.ROTATION_270
                    orientation in 240..300 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                if (rotation != lastSurfaceRotation) {
                    lastSurfaceRotation = rotation
                    updateControlsPosition(rotation)
                    preview?.targetRotation = rotation
                    imageCapture?.targetRotation = rotation
                }
            }
        }

        // 初始狀態：先禁用快門，等待 orientation listener 回報實際方向
        deviceIsLandscape = false
        updateOrientationUi(false)
        lastSurfaceRotation = Surface.ROTATION_0
        updateControlsPosition(Surface.ROTATION_0)
    }

    private fun updateOrientationUi(isLandscape: Boolean) {
        if (_binding == null) return
        binding.orientationWarning.visibility = if (isLandscape) View.GONE else View.VISIBLE
        binding.btnCapture.isEnabled = isLandscape
        binding.btnCapture.alpha = if (isLandscape) 1f else 0.4f
    }

    /**
     * 快門固定底部，保留方法作為日後擴充點。
     */
    private fun updateControlsPosition(surfaceRotation: Int) {
        if (_binding == null) return
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindUseCases(cameraProvider)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindUseCases(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }
        this.preview = preview

        // 以目前 listener 已知的 rotation 作為初始值，避免先拍第一張方向錯誤
        preview.targetRotation = lastSurfaceRotation

        imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(lastSurfaceRotation)
            .build()

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
            // 保持 setupOrientationListener 初始化狀態，等待實際方向事件更新
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.msg_camera_init_failed), Toast.LENGTH_SHORT).show()
            sendResult(Activity.RESULT_CANCELED, arguments?.getInt(ARG_SLOT, 0) ?: 0, null)
        }
    }

    private fun capturePhoto(slot: Int) {
        val capture = imageCapture ?: return
        val file = outputFile ?: return

        binding.btnCapture.isEnabled = false
        val options = ImageCapture.OutputFileOptions.Builder(file).build()

        capture.takePicture(
            options,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            normalizeCapturedPhotoOrientation(file)
                        }
                        sendResult(Activity.RESULT_OK, slot, file.absolutePath)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    binding.btnCapture.isEnabled = deviceIsLandscape
                    binding.btnCapture.alpha = if (deviceIsLandscape) 1f else 0.4f
                    Toast.makeText(requireContext(), getString(R.string.msg_camera_capture_failed), Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun sendResult(resultCode: Int, slot: Int, path: String?) {
        parentFragmentManager.setFragmentResult(
            RESULT_KEY,
            Bundle().apply {
                putInt(RESULT_CODE, resultCode)
                putInt(RESULT_SLOT, slot)
                if (!path.isNullOrBlank()) putString(RESULT_PATH, path)
            }
        )
        (activity as? GutterFormActivity)?.hideCameraOverlay()
    }

    private fun normalizeCapturedPhotoOrientation(file: File) {
        runCatching {
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.postScale(-1f, 1f)
                }
                else -> return
            }

            val source = BitmapFactory.decodeFile(file.absolutePath) ?: return
            val fixed = Bitmap.createBitmap(
                source,
                0,
                0,
                source.width,
                source.height,
                matrix,
                true
            )
            if (fixed != source) source.recycle()

            FileOutputStream(file).use { out ->
                fixed.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            fixed.recycle()

            val fixedExif = ExifInterface(file.absolutePath)
            fixedExif.setAttribute(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL.toString()
            )
            fixedExif.saveAttributes()
        }.onFailure { e ->
            android.util.Log.w("CameraOverlay", "normalize orientation failed: ${e.message}")
        }
    }
}
