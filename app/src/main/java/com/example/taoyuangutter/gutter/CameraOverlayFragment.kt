package com.example.taoyuangutter.gutter

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.hardware.camera2.CameraCharacteristics
import android.media.ExifInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.taoyuangutter.R
import com.example.taoyuangutter.databinding.FragmentCameraOverlayBinding
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 全螢幕相機 Overlay（嵌入 GutterFormActivity），避免 Activity 切換造成放大/旋轉卡頓。
 *
 * - 仍維持「需橫放才能拍」：未橫放顯示遮罩並禁用快門
 * - 兩指縮放：CameraX zoomRatio
 */
@ExperimentalCamera2Interop
class CameraOverlayFragment : Fragment() {

    private data class LensCandidate(
        val cameraInfo: CameraInfo,
        val score: Float,
        val cameraId: String?,
        val hasFlash: Boolean,
        val focalLengthMm: Float?,
        val relativeScale: Float
    )

    private var _binding: FragmentCameraOverlayBinding? = null
    private val binding get() = _binding!!

    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var outputFile: File? = null

    private lateinit var orientationListener: OrientationEventListener
    private var deviceIsLandscape = false
    private var lastSurfaceRotation: Int = Surface.ROTATION_0
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var minZoomRatio = 1f
    private var maxZoomRatio = 1f
    private var initialZoomSet = false
    private var updatingZoomSlider = false
    // 新增：保持 slider 值以便拍照後 post-process 使用
    private var brightnessAmount = 0.5f
    private var desiredFlashOn = false
    private var isFlashOn = false
    private var zoomStateInitialized = false
    private var selectedLensId: String? = null
    private var lensCandidates: List<LensCandidate> = emptyList()
    private var selectedLensIndex: Int = 0
    private var selectedLensRelativeScale: Float = 1f
    private var currentLensSupportsFlash: Boolean = false

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
        hideKeyboardIfShown()

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
        setupZoomSlider()
        setupColorControls()
        setupToggleButtons()
        applySystemBarInsets()
        setupOrientationListener()
        setupButtons(slot)
        startCamera()
    }

    private fun hideKeyboardIfShown() {
        val hostView = activity?.currentFocus ?: view ?: return
        activity?.currentFocus?.clearFocus()
        val imm = context?.getSystemService(InputMethodManager::class.java) ?: return
        imm.hideSoftInputFromWindow(hostView.windowToken, 0)
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
                    val targetRatio = (zoomState.zoomRatio * detector.scaleFactor)
                        .coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
                    cam.cameraControl.setZoomRatio(targetRatio)
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

    private fun setupZoomSlider() {
        binding.zoomSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser || updatingZoomSlider) return
                camera?.cameraControl?.setZoomRatio(progressToZoomRatio(progress))
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun progressToZoomRatio(progress: Int): Float {
        if (maxZoomRatio <= minZoomRatio) return minZoomRatio
        val fraction = progress.coerceIn(0, 100) / 100f
        return minZoomRatio + ((maxZoomRatio - minZoomRatio) * fraction)
    }

    private fun zoomRatioToProgress(zoomRatio: Float): Int {
        if (maxZoomRatio <= minZoomRatio) return 0
        return (((zoomRatio - minZoomRatio) / (maxZoomRatio - minZoomRatio)) * 100f)
            .roundToInt()
            .coerceIn(0, 100)
    }

    private fun setupToggleButtons() {
        binding.btnToggleColor.setOnClickListener {
            val visible = binding.colorSliders.isVisible
            binding.colorSliders.isVisible = !visible
            // 開啟色彩時，若變焦開啟則關閉變焦，保持畫面簡潔
            if (!visible) {
                binding.zoomSlider.isVisible = false
                binding.tvZoomLevel.isVisible = false
            }
            updateResetButtonVisibility()
        }
        binding.btnToggleZoom.setOnClickListener {
            val visible = binding.zoomSlider.isVisible
            binding.zoomSlider.isVisible = !visible
            binding.tvZoomLevel.isVisible = !visible
            // 開啟變焦時，若色彩開啟則關閉色彩
            if (!visible) binding.colorSliders.isVisible = false
            updateResetButtonVisibility()
        }
        binding.btnFlash.setOnClickListener {
            toggleFlash()
        }
        binding.btnToggleLens.setOnClickListener {
            switchLens()
        }
        binding.btnReset.setOnClickListener {
            resetAllToDefault()
        }
    }

    private fun toggleFlash() {
        val cam = camera ?: return
        val capture = imageCapture

        if (cam.cameraInfo.hasFlashUnit()) {
            desiredFlashOn = !desiredFlashOn
            applyFlashState(capture)
        } else {
            Toast.makeText(requireContext(), "此鏡頭不支援閃光燈", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateResetButtonVisibility() {
        binding.btnReset.isVisible =
            binding.colorSliders.isVisible || binding.zoomSlider.isVisible
    }

    private fun resetAllToDefault() {
        if (lensCandidates.isNotEmpty() && selectedLensIndex != 0) {
            selectedLensIndex = 0
            selectedLensId = lensCandidates.firstOrNull()?.cameraId
            binding.brightnessSlider.progress = 50
            brightnessAmount = 0.5f
            rebindCamera()
            return
        }

        // 重設變焦
        val targetInitialRatio = defaultZoomRatio()
        camera?.cameraControl?.setZoomRatio(targetInitialRatio)
        binding.zoomSlider.progress = zoomRatioToProgress(targetInitialRatio)
        binding.tvZoomLevel.text = formatDisplayedZoom(targetInitialRatio)

        // 重設色彩
        binding.brightnessSlider.progress = 50
        brightnessAmount = 0.5f
        applyColorCorrection()
    }

    private fun setupColorControls() {
        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                if (s?.id == R.id.brightnessSlider) {
                    brightnessAmount = p.coerceIn(0, 100) / 100f
                }
                if (fromUser) applyColorCorrection()
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        }
        binding.brightnessSlider.setOnSeekBarChangeListener(listener)
    }

    private fun applyColorCorrection() {
        val cam = camera ?: return
        val camControl = cam.cameraControl
        val camInfo = cam.cameraInfo

        // 處理亮度 (Exposure Compensation) — 使用 member brightnessAmount
        val brightness = brightnessAmount // 0..1
        val exposureState = camInfo.exposureState
        if (exposureState.isExposureCompensationSupported) {
            val range = exposureState.exposureCompensationRange
            // 讓 50% 對應到 0 (不補償)，0% 對應到 min，100% 對應到 max
            val index = if (brightness >= 0.5f) {
                val fraction = (brightness - 0.5f) * 2f
                (range.upper * fraction).roundToInt()
            } else {
                val fraction = (0.5f - brightness) * 2f
                (range.lower * fraction).roundToInt()
            }
            camControl.setExposureCompensationIndex(index)
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

        // 同步處理 Slider 與 Toggle 按鈕狀態
        val alpha = if (isLandscape) 1f else 0.2f
        binding.zoomSlider.isEnabled = isLandscape
        binding.brightnessSlider.isEnabled = isLandscape
        binding.btnToggleColor.isEnabled = isLandscape
        binding.btnToggleZoom.isEnabled = isLandscape
        binding.btnFlash.isEnabled = isLandscape
        binding.btnToggleLens.isEnabled = isLandscape && lensCandidates.size > 1
        
        binding.colorControlsLayout.alpha = alpha
        binding.zoomControlsLayout.alpha = alpha

        // 未橫放時收合滑桿
        if (!isLandscape) {
            binding.colorSliders.isVisible = false
            binding.zoomSlider.isVisible = false
            binding.tvZoomLevel.isVisible = false
            binding.btnReset.isVisible = false
        }
    }

    private fun updateLensToggleUi() {
        if (_binding == null) return
        if (lensCandidates.size <= 1) {
            binding.btnToggleLens.isVisible = false
            return
        }
        binding.btnToggleLens.isVisible = true
        binding.btnToggleLens.text = lensLabel(
            selectedLensIndex,
            lensCandidates.size,
            selectedLensRelativeScale
        )
        binding.btnToggleLens.isEnabled = deviceIsLandscape
        binding.btnToggleLens.alpha = if (deviceIsLandscape) 1f else 0.2f
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
            val provider = cameraProviderFuture.get()
            cameraProvider = provider
            bindUseCases(provider)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindUseCases(cameraProvider: ProcessCameraProvider) {
        val candidates = buildLensCandidates(cameraProvider)
        val selected = candidates.getOrNull(selectedLensIndex) ?: candidates.firstOrNull()
        if (selected != null) {
            selectedLensId = selected.cameraId
            selectedLensIndex = candidates.indexOfFirst { it.cameraId == selected.cameraId }.takeIf { it >= 0 }
                ?: selectedLensIndex
            selectedLensRelativeScale = selected.relativeScale
        }

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
            .setFlashMode(if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
            .build()

        try {
            camera?.cameraInfo?.zoomState?.removeObservers(viewLifecycleOwner)
            cameraProvider.unbindAll()
            val cameraSelector = selected?.let { cameraSelectorFor(it.cameraInfo) }
                ?: CameraSelector.DEFAULT_BACK_CAMERA
            val cam = cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            camera = cam
            currentLensSupportsFlash = cam.cameraInfo.hasFlashUnit()
            selectedLensRelativeScale = selected?.relativeScale ?: 1f

            updateLensToggleUi()
            applyFlashState(imageCapture)

            // 強制初始焦距為目前鏡頭的基準倍率
            if (!initialZoomSet) {
                val targetInitialRatio = defaultZoomRatio()
                cam.cameraControl.setZoomRatio(targetInitialRatio)
                initialZoomSet = true
                // Try to initialize UI from immediate zoomState if available
                val immediateState = cam.cameraInfo.zoomState.value
                if (immediateState != null && immediateState.minZoomRatio < immediateState.maxZoomRatio) {
                    minZoomRatio = immediateState.minZoomRatio
                    maxZoomRatio = immediateState.maxZoomRatio
                    val progress = zoomRatioToProgress(immediateState.zoomRatio)
                    updatingZoomSlider = true
                    binding.zoomSlider.progress = progress
                    updatingZoomSlider = false
                    binding.tvZoomLevel.text = formatDisplayedZoom(immediateState.zoomRatio)
                    binding.zoomSlider.isEnabled = true
                    zoomStateInitialized = true
                } else {
                    // Disable slider until observer provides real values
                    binding.zoomSlider.isEnabled = false
                    binding.tvZoomLevel.text = formatDisplayedZoom(targetInitialRatio)
                }
            }

            cam.cameraInfo.zoomState.observe(viewLifecycleOwner) { state ->
                minZoomRatio = state.minZoomRatio
                maxZoomRatio = state.maxZoomRatio
                val progress = zoomRatioToProgress(state.zoomRatio)
                if (binding.zoomSlider.progress != progress) {
                    updatingZoomSlider = true
                    binding.zoomSlider.progress = progress
                    updatingZoomSlider = false
                }
                // 更新倍率文字 (例如: 1.0x, 2.5x)
                val zoomRatio = state.zoomRatio
                binding.tvZoomLevel.text = formatDisplayedZoom(zoomRatio)
                if (!zoomStateInitialized) {
                    binding.zoomSlider.isEnabled = true
                    zoomStateInitialized = true
                }
            }
            applyColorCorrection()
            // 保持 setupOrientationListener 初始化狀態，等待實際方向事件更新
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.msg_camera_init_failed), Toast.LENGTH_SHORT).show()
            sendResult(Activity.RESULT_CANCELED, arguments?.getInt(ARG_SLOT, 0) ?: 0, null)
        }
    }

    private fun rebindCamera() {
        val provider = cameraProvider ?: return
        initialZoomSet = false
        zoomStateInitialized = false
        bindUseCases(provider)
    }

    private fun switchLens() {
        if (lensCandidates.size <= 1) return
        selectedLensIndex = (selectedLensIndex + 1) % lensCandidates.size
        selectedLensId = lensCandidates.getOrNull(selectedLensIndex)?.cameraId
        rebindCamera()
    }

    private fun buildLensCandidates(cameraProvider: ProcessCameraProvider): List<LensCandidate> {
        val availableInfos = cameraProvider.availableCameraInfos
        val backInfos = runCatching {
            CameraSelector.DEFAULT_BACK_CAMERA.filter(availableInfos)
        }.getOrElse {
            Log.w("CameraOverlay", "filter back cameras failed: ${it.message}")
            emptyList()
        }

        val rawCandidates = backInfos.mapNotNull { info ->
            val score = info.wideAngleScore() ?: 0f
            val cameraId = runCatching { Camera2CameraInfo.from(info).cameraId }.getOrNull()
            val focalLengthMm = info.primaryFocalLengthMm()
            LensCandidate(
                cameraInfo = info,
                score = score,
                cameraId = cameraId,
                hasFlash = info.hasFlashUnit(),
                focalLengthMm = focalLengthMm,
                relativeScale = 1f
            )
        }.sortedWith(
            compareByDescending<LensCandidate> { it.score }
                .thenByDescending { it.hasFlash }
                .thenBy { it.cameraId.orEmpty() }
        )

        val referenceFocalLengthMm = rawCandidates
            .mapNotNull { it.focalLengthMm }
            .sorted()
            .let { focalLengths -> referenceFocalLength(focalLengths) }

        val candidates = rawCandidates.map { candidate ->
            val relativeScale = candidate.focalLengthMm?.let { focalLength ->
                val reference = referenceFocalLengthMm
                if (reference != null && reference > 0f) {
                    focalLength / reference
                } else {
                    1f
                }
            } ?: 1f
            candidate.copy(relativeScale = relativeScale)
        }

        lensCandidates = candidates
        if (candidates.isEmpty()) {
            selectedLensIndex = 0
            selectedLensRelativeScale = 1f
        } else {
            val savedIndex: Int = selectedLensId?.let { id -> candidates.indexOfFirst { it.cameraId == id } } ?: -1
            selectedLensIndex = when {
                savedIndex >= 0 -> savedIndex
                selectedLensIndex in candidates.indices -> selectedLensIndex
                else -> 0
            }
            selectedLensRelativeScale = candidates.getOrNull(selectedLensIndex)?.relativeScale ?: 1f
        }
        updateLensToggleUi()
        return candidates
    }

    private fun referenceFocalLength(focalLengths: List<Float>): Float? {
        if (focalLengths.isEmpty()) return null
        val middle = focalLengths.size / 2
        return if (focalLengths.size % 2 == 0) {
            (focalLengths[middle - 1] + focalLengths[middle]) / 2f
        } else {
            focalLengths[middle]
        }
    }

    private fun cameraSelectorFor(cameraInfo: CameraInfo): CameraSelector {
        return CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .addCameraFilter { cameraInfos ->
                val selected = cameraInfos.firstOrNull { it == cameraInfo } ?: return@addCameraFilter cameraInfos
                listOf(selected)
            }
            .build()
    }

    private fun CameraInfo.wideAngleScore(): Float? {
        return runCatching {
            val camera2Info = Camera2CameraInfo.from(this)
            val lensFacing = camera2Info.getCameraCharacteristic(CameraCharacteristics.LENS_FACING)
            if (lensFacing != CameraCharacteristics.LENS_FACING_BACK) return null
            val focalLengths = camera2Info.getCameraCharacteristic(
                CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
            )
            val sensorSize = camera2Info.getCameraCharacteristic(
                CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE
            )
            val shortestFocalLength = focalLengths?.minOrNull() ?: return null
            val sensorWidth = sensorSize?.width ?: return null
            if (shortestFocalLength <= 0f || sensorWidth <= 0f) return null
            sensorWidth / shortestFocalLength
        }.getOrNull()
    }

    private fun CameraInfo.primaryFocalLengthMm(): Float? {
        return runCatching {
            val camera2Info = Camera2CameraInfo.from(this)
            val focalLengths = camera2Info.getCameraCharacteristic(
                CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
            )
            focalLengths?.minOrNull()
        }.getOrNull()
    }

    private fun defaultZoomRatio(): Float {
        return camera?.cameraInfo?.zoomState?.value?.minZoomRatio?.coerceAtLeast(1f) ?: 1f
    }

    private fun applyFlashState(capture: ImageCapture? = imageCapture) {
        val cam = camera ?: return
        currentLensSupportsFlash = cam.cameraInfo.hasFlashUnit()
        val actualFlashOn = desiredFlashOn && currentLensSupportsFlash
        isFlashOn = actualFlashOn
        capture?.flashMode = if (actualFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        binding.btnFlash.setImageResource(if (actualFlashOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off)
        binding.btnFlash.isEnabled = currentLensSupportsFlash
        binding.btnFlash.alpha = if (currentLensSupportsFlash) 1f else 0.4f
    }

    private fun lensLabel(index: Int, size: Int, relativeScale: Float): String {
        val lensName = when {
            size <= 1 -> "單鏡頭"
            index <= 0 -> "廣角"
            index >= size - 1 -> "望遠"
            else -> "一般"
        }
        return if (size <= 1) {
            lensName
        } else {
            "$lensName ${String.format(Locale.US, "%.1fx", relativeScale)}"
        }
    }

    private fun formatDisplayedZoom(cameraZoomRatio: Float): String {
        val relativeZoom = cameraZoomRatio * selectedLensRelativeScale
        return String.format(Locale.US, "%.1fx", relativeZoom)
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
            val shouldNormalizeOrientation = orientation != ExifInterface.ORIENTATION_NORMAL
            
            val shouldApplyColorMatrix = brightnessAmount != 0.5f
            if (!shouldNormalizeOrientation && !shouldApplyColorMatrix) return

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
            }

            val source = BitmapFactory.decodeFile(file.absolutePath) ?: return
            var output = if (shouldNormalizeOrientation) {
                Bitmap.createBitmap(
                    source,
                    0,
                    0,
                    source.width,
                    source.height,
                    matrix,
                    true
                ).also {
                    if (it != source) source.recycle()
                }
            } else {
                source
            }

            if (shouldApplyColorMatrix) {
                // Build combined ColorMatrix: brightness translate
                
                // Brightness translate: map brightnessAmount (0..1, 0.5 neutral) to +/- translate
                val brightnessDelta = (brightnessAmount - 0.5f) * 128f
                val translateMatrix = ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, brightnessDelta,
                        0f, 1f, 0f, 0f, brightnessDelta,
                        0f, 0f, 1f, 0f, brightnessDelta,
                        0f, 0f, 0f, 1f, 0f
                    )
                )

                val adjusted = Bitmap.createBitmap(output.width, output.height, Bitmap.Config.ARGB_8888)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    colorFilter = ColorMatrixColorFilter(translateMatrix)
                }
                Canvas(adjusted).drawBitmap(output, 0f, 0f, paint)
                output.recycle()
                output = adjusted
            }

            FileOutputStream(file).use { out ->
                output.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            output.recycle()

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
