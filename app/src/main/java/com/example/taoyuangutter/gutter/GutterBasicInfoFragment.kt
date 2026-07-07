package com.example.taoyuangutter.gutter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.ColorStateList
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.util.Log
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.taoyuangutter.R
import com.example.taoyuangutter.common.PhotoCapturedAtResolver
import com.example.taoyuangutter.common.PhotoUploadValidator
import com.example.taoyuangutter.databinding.FragmentGutterBasicInfoBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GutterBasicInfoFragment : Fragment() {

    interface DraftChangeHost {
        fun onBasicInfoDraftChanged(data: Map<String, String>)
    }

    private var _binding: FragmentGutterBasicInfoBinding? = null
    private val binding get() = _binding!!
    var onDraftChanged: (() -> Unit)? = null
    var onRequestLocationPick: (() -> Unit)? = null
    private var draftChangeHost: DraftChangeHost? = null
    private var isFormEditable: Boolean = true
    private var isImportLocked: Boolean = false
    private var isVirtualMode: Boolean = false // 新增：是否為虛擬點模式
    // Keep using request keys NODE_X/NODE_Y; just change UI presentation.
    private var coordXValue: String = ""
    private var coordYValue: String = ""
    private var photoDraftChangeHost: GutterPhotosFragment.DraftChangeHost? = null
    private var photoUriSlot1: Uri? = null
    private var photoUriSlot2: Uri? = null
    private var photoUriSlot3: Uri? = null
    private var photoCapturedAtSlot1: String? = null
    private var photoCapturedAtSlot2: String? = null
    private var photoCapturedAtSlot3: String? = null
    private var pendingSlot: Int = 0
    private var pendingOutputPath: String? = null
    private var hasShownPhotoLoadErrorAlert = false
    private var suppressPhotoDraftCallbacks = false
    private var selectedGutterType: String? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingSlot > 0) {
            maybeRequestLegacyWritePermissionThenLaunch()
        } else if (!granted) {
            val canceledSlot = pendingSlot
            pendingSlot = 0
            if (canceledSlot > 0) {
                photoDraftChangeHost?.onPendingPhotoDraftChanged(canceledSlot, null)
            }
            Toast.makeText(requireContext(), getString(R.string.msg_camera_permission_required), Toast.LENGTH_SHORT).show()
        }
    }

    private val legacyWritePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            val canceledSlot = pendingSlot
            pendingSlot = 0
            if (canceledSlot > 0) {
                photoDraftChangeHost?.onPendingPhotoDraftChanged(canceledSlot, null)
            }
            Toast.makeText(requireContext(), "需要儲存權限才能同時寫入系統相簿", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        if (pendingSlot > 0) {
            launchCameraOverlay(pendingSlot)
        }
    }

    companion object {
        private const val ARG_LAT          = "latitude"
        private const val ARG_LNG          = "longitude"
        private const val ARG_VIEW_MODE     = "view_mode"
        private const val ARG_OFFLINE_MODE  = "offline_mode"
        private const val ARG_IS_EDIT_MODE  = "is_edit_mode" // 新增：是否為編輯模式

        // basicData 個別 key
        private const val ARG_DATA_SPI_NUM     = "d_spi_num"
        private const val ARG_DATA_NODE_TYP    = "d_node_typ"
        private const val ARG_DATA_MAT_TYP     = "d_mat_typ"
        private const val ARG_DATA_NODE_X      = "d_node_x"
        private const val ARG_DATA_NODE_Y      = "d_node_y"
        private const val ARG_DATA_NODE_LE     = "d_node_le"
        private const val ARG_DATA_XY_NUM      = "d_xy_num"
        private const val ARG_DATA_COVER_DEP = "d_cover_dep"
        private const val ARG_DATA_NODE_DEP    = "d_node_dep"
        private const val ARG_DATA_NODE_WID    = "d_node_wid"
        private const val ARG_DATA_IS_BROKEN   = "d_is_broken"
        private const val ARG_DATA_IS_HANGING  = "d_is_hanging"
        private const val ARG_DATA_IS_SILT     = "d_is_silt"
        private const val ARG_DATA_IS_CANTOPEN = "d_is_cantopen"
        private const val ARG_DATA_NODE_NOTE   = "d_node_note"
        private const val ARG_DATA_IS_PENDING_DEPLOY = "d_is_pending_deploy"
        private const val ARG_DATA_IS_VIRTUAL  = "d_is_virtual" // 新增：虛擬點欄位
        private const val ARG_DATA_IS_IMPORTED = "d_is_imported" // 新增：匯入點位旗標
        private const val ARG_PHOTO_1 = "arg_photo_1"
        private const val ARG_PHOTO_2 = "arg_photo_2"
        private const val ARG_PHOTO_3 = "arg_photo_3"
        private const val ARG_PHOTO_1_CAPTURED_AT = "arg_photo_1_captured_at"
        private const val ARG_PHOTO_2_CAPTURED_AT = "arg_photo_2_captured_at"
        private const val ARG_PHOTO_3_CAPTURED_AT = "arg_photo_3_captured_at"
        private const val KEY_PHOTO_1 = "photo_1"
        private const val KEY_PHOTO_2 = "photo_2"
        private const val KEY_PHOTO_3 = "photo_3"
        private const val KEY_PHOTO_1_CAPTURED_AT = "photo_1_captured_at"
        private const val KEY_PHOTO_2_CAPTURED_AT = "photo_2_captured_at"
        private const val KEY_PHOTO_3_CAPTURED_AT = "photo_3_captured_at"
        private const val KEY_PENDING_SLOT = "pending_slot"
        private const val KEY_PENDING_PATH = "pending_path"

        /** 側溝形式選項（NODE_TYP）*/
        val GUTTER_TYPES = listOf(
            "U形溝（明溝）",
            "U形溝（加蓋）",
            "L形溝與暗溝渠併用",
            "其他"
        )

        /** 側溝材質選項（MAT_TYP）*/
        val MAT_TYPES = listOf("混凝土", "卵礫石", "紅磚")

        /** 溝體結構受損選項（IS_BROKEN）*/
        val BROKEN_OPTIONS = listOf("否", "是")

        /** 附掛或過路管線選項（IS_HANGING）*/
        val HANGING_OPTIONS = listOf("無", "有")

        /** 淤積程度選項（IS_SILT）*/
        // 0=無、1=輕度、2=嚴重（中度已移除；若收到舊資料 3 也一律顯示為嚴重作為緩衝）
        val SILT_OPTIONS = listOf("無", "輕度", "嚴重")

        fun newInstance(
            latitude: Double,
            longitude: Double,
            viewMode: Boolean = false,
            basicData: HashMap<String, String> = hashMapOf(),
            isOfflineMode: Boolean = false,
            isEditMode: Boolean = false, // 新增：是否為編輯模式
            isVirtual: String = "0" // 新增：是否為虛擬點
        ) = GutterBasicInfoFragment().apply {
            arguments = Bundle().apply {
                putDouble(ARG_LAT, latitude)
                putDouble(ARG_LNG, longitude)
                putBoolean(ARG_VIEW_MODE, viewMode)
                putBoolean(ARG_OFFLINE_MODE, isOfflineMode)
                putBoolean(ARG_IS_EDIT_MODE, isEditMode) // 傳入編輯模式旗標
                putString(ARG_DATA_IS_VIRTUAL, isVirtual) // 傳入虛擬點旗標
                putString(ARG_DATA_IS_IMPORTED, basicData["_isImported"] ?: "") // 傳入匯入旗標
                putString(ARG_DATA_SPI_NUM,     basicData["SPI_NUM"]     ?: basicData["gutterId"] ?: "")
                putString(ARG_DATA_NODE_TYP,    basicData["NODE_TYP"]    ?: basicData["gutterType"] ?: "")
                putString(ARG_DATA_MAT_TYP,     basicData["MAT_TYP"]     ?: basicData["matTyp"] ?: "")
                putString(ARG_DATA_NODE_X,      basicData["NODE_X"]      ?: basicData["coordX"] ?: "")
                putString(ARG_DATA_NODE_Y,      basicData["NODE_Y"]      ?: basicData["coordY"] ?: "")
                putString(ARG_DATA_NODE_LE,     basicData["NODE_LE"]     ?: basicData["coordZ"] ?: "")
                putString(ARG_DATA_XY_NUM,      basicData["XY_NUM"]      ?: basicData["xyNum"] ?: "")
                putString(ARG_DATA_COVER_DEP, basicData["COVER_DEP"] ?: "")
                putString(ARG_DATA_NODE_DEP,    basicData["NODE_DEP"]    ?: basicData["depth"] ?: "")
                putString(ARG_DATA_NODE_WID,    basicData["NODE_WID"]    ?: basicData["topWidth"] ?: "")
                putString(ARG_DATA_IS_BROKEN,   basicData["IS_BROKEN"]   ?: basicData["isBroken"] ?: "")
                putString(ARG_DATA_IS_HANGING,  basicData["IS_HANGING"]  ?: basicData["isHanging"] ?: "")
                putString(ARG_DATA_IS_SILT,     basicData["IS_SILT"]     ?: basicData["isSilt"] ?: "")
                putString(ARG_DATA_IS_CANTOPEN, basicData["IS_CANTOPEN"] ?: basicData["isCantOpen"] ?: "")
                putString(ARG_DATA_NODE_NOTE,   basicData["NODE_NOTE"]   ?: basicData["remarks"] ?: "")
                putString(ARG_PHOTO_1, basicData["photo1"] ?: "")
                putString(ARG_PHOTO_2, basicData["photo2"] ?: "")
                putString(ARG_PHOTO_3, basicData["photo3"] ?: "")
                putString(ARG_PHOTO_1_CAPTURED_AT, basicData["photo1CapturedAt"] ?: "")
                putString(ARG_PHOTO_2_CAPTURED_AT, basicData["photo2CapturedAt"] ?: "")
                putString(ARG_PHOTO_3_CAPTURED_AT, basicData["photo3CapturedAt"] ?: "")
                putString(
                    ARG_DATA_IS_PENDING_DEPLOY,
                    basicData["IS_PENDING_DEPLOY"]
                        ?: basicData["is_pendingDeploy"]
                        ?: basicData["isPendingDeploy"]
                        ?: ""
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGutterBasicInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            savedInstanceState.getString(KEY_PHOTO_1)?.let { photoUriSlot1 = Uri.parse(it) }
            savedInstanceState.getString(KEY_PHOTO_2)?.let { photoUriSlot2 = Uri.parse(it) }
            savedInstanceState.getString(KEY_PHOTO_3)?.let { photoUriSlot3 = Uri.parse(it) }
            photoCapturedAtSlot1 = savedInstanceState.getString(KEY_PHOTO_1_CAPTURED_AT)
            photoCapturedAtSlot2 = savedInstanceState.getString(KEY_PHOTO_2_CAPTURED_AT)
            photoCapturedAtSlot3 = savedInstanceState.getString(KEY_PHOTO_3_CAPTURED_AT)
            pendingSlot = savedInstanceState.getInt(KEY_PENDING_SLOT, 0)
            pendingOutputPath = savedInstanceState.getString(KEY_PENDING_PATH)
        } else {
            photoUriSlot1 = parseUriString(arguments?.getString(ARG_PHOTO_1))
            photoUriSlot2 = parseUriString(arguments?.getString(ARG_PHOTO_2))
            photoUriSlot3 = parseUriString(arguments?.getString(ARG_PHOTO_3))
            photoCapturedAtSlot1 = arguments?.getString(ARG_PHOTO_1_CAPTURED_AT)
            photoCapturedAtSlot2 = arguments?.getString(ARG_PHOTO_2_CAPTURED_AT)
            photoCapturedAtSlot3 = arguments?.getString(ARG_PHOTO_3_CAPTURED_AT)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        draftChangeHost = context as? DraftChangeHost
        photoDraftChangeHost = context as? GutterPhotosFragment.DraftChangeHost
    }

    override fun onDetach() {
        draftChangeHost = null
        photoDraftChangeHost = null
        super.onDetach()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        photoUriSlot1?.let { outState.putString(KEY_PHOTO_1, it.toString()) }
        photoUriSlot2?.let { outState.putString(KEY_PHOTO_2, it.toString()) }
        photoUriSlot3?.let { outState.putString(KEY_PHOTO_3, it.toString()) }
        photoCapturedAtSlot1?.let { outState.putString(KEY_PHOTO_1_CAPTURED_AT, it) }
        photoCapturedAtSlot2?.let { outState.putString(KEY_PHOTO_2_CAPTURED_AT, it) }
        photoCapturedAtSlot3?.let { outState.putString(KEY_PHOTO_3_CAPTURED_AT, it) }
        outState.putInt(KEY_PENDING_SLOT, pendingSlot)
        pendingOutputPath?.let { outState.putString(KEY_PENDING_PATH, it) }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val isViewMode   = arguments?.getBoolean(ARG_VIEW_MODE)   ?: false
        val isEditMode   = arguments?.getBoolean(ARG_IS_EDIT_MODE) ?: false
        setupGutterTypeSelector()

        // 修正：僅在初次建立（無 savedInstanceState）時預填資料
        // 系統重建時，EditText 與 CheckBox 會由系統自動還原其 state
        if (savedInstanceState == null) {
            prefillData()
        } else {
            // 重建時，仍需從 arguments 取得 X/Y 基底值（避免 collectData 時為空）
            coordXValue = arguments?.getString(ARG_DATA_NODE_X) ?: ""
            coordYValue = arguments?.getString(ARG_DATA_NODE_Y) ?: ""
        }
        parentFragmentManager.setFragmentResultListener(
            CameraOverlayFragment.RESULT_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val code = bundle.getInt(CameraOverlayFragment.RESULT_CODE, Activity.RESULT_CANCELED)
            val slot = bundle.getInt(CameraOverlayFragment.RESULT_SLOT, 0)
            val path = bundle.getString(CameraOverlayFragment.RESULT_PATH) ?: pendingOutputPath
            if (code == Activity.RESULT_OK && !path.isNullOrBlank() && slot in 1..3) {
                (activity as? PhotoLoadingHost)?.setPhotoLoading(true)
                val file = File(path)
                setCapturedAtForSlot(
                    slot = slot,
                    capturedAt = PhotoCapturedAtResolver.resolveBestEffort(requireContext(), file.absolutePath),
                    notifyDraftChanged = true
                )
                val uri = try {
                    FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        file
                    )
                } catch (_: Exception) {
                    Uri.fromFile(file)
                }
                applyPhotoToSlot(slot, uri, notifyDraftChanged = true)
            }
            if (slot in 1..3) {
                photoDraftChangeHost?.onPendingPhotoDraftChanged(slot, null)
            }
            pendingSlot = 0
            pendingOutputPath = null
        }
        renderStoredPhotoSlots()
        setEditable(!isViewMode)
        setupCantOpen()
        
        // 確保在 View 建立後，立即根據目前的「側溝形式」、「無法開蓋」與「虛擬點」狀態更新 UI
        applyGutterTypeUi()
        applyCantOpenUi(binding.cbCantOpen.isChecked)
        setVirtualMode(isVirtualMode)
        
        setupPendingDeployButton(isViewMode)
        setupRangeWatchers()
        setupDraftWatchers()
        setupLocationPickerButton()

        // 新增與編輯模式下均隱藏側溝編號欄位（僅檢視模式顯示）
        if (!isViewMode) {
            binding.tilGutterTitle.visibility = View.GONE
            binding.tilGutterId.visibility = View.GONE
        }

        // 新增/編輯模式隱藏 Z 座標欄位；僅檢視模式顯示（後端提供、不可修改）
        if (!isViewMode) {
            binding.tvCoordZTitle.visibility = View.GONE
            binding.tilCoordZ.visibility = View.GONE
        }

        binding.btnPickLocation.isEnabled = !isViewMode
        applyImportLockUi()

        // 點擊空白處關閉鍵盤
        view.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                hideKeyboard()
                clearAllFocus()
            }
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── RadioGroup 工具函式 ───────────────────────────────────────────────

    /** 依文字找到對應 RadioButton 並勾選；找不到則清除選取。 */
    private fun RadioGroup.setCheckedByText(text: String?) {
        if (text.isNullOrEmpty()) { clearCheck(); return }
        for (i in 0 until childCount) {
            val rb = getChildAt(i) as? RadioButton ?: continue
            if (rb.text == text) { check(rb.id); return }
        }
        clearCheck()
    }

    /** 取得目前勾選 RadioButton 的文字；無勾選回傳空字串。 */
    private fun RadioGroup.getCheckedText(): String {
        val id = checkedRadioButtonId
        if (id == -1) return ""
        return (findViewById<RadioButton>(id))?.text?.toString() ?: ""
    }

    /** 設定 RadioGroup 所有子 RadioButton 的 isEnabled。 */
    private fun RadioGroup.setChildrenEnabled(enabled: Boolean) {
        for (i in 0 until childCount) { getChildAt(i)?.isEnabled = enabled }
    }

    private fun setupGutterTypeSelector() {
        binding.layoutGutterTypeSelector.setOnClickListener {
            if (!binding.layoutGutterTypeSelector.isEnabled) return@setOnClickListener
            MaterialAlertDialogBuilder(requireContext())
                .setItems(GUTTER_TYPES.toTypedArray()) { _, which ->
                    setGutterTypeSelection(GUTTER_TYPES[which], notifyDraftChanged = true)
                }
                .show()
        }
        renderGutterTypeDisplay()
    }

    private fun setGutterTypeSelection(text: String?, notifyDraftChanged: Boolean) {
        selectedGutterType = text?.takeIf { it in GUTTER_TYPES }
        renderGutterTypeDisplay()
        applyGutterTypeUi()
        if (notifyDraftChanged) notifyDraftChanged()
    }

    private fun renderGutterTypeDisplay() {
        val text = selectedGutterType
        binding.tvGutterTypeSelector.text = text ?: "請選擇"
        val colorRes = if (text.isNullOrBlank()) R.color.inputFieldHint else R.color.textColorPrimary
        binding.tvGutterTypeSelector.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
    }

    private fun selectedGutterTypeText(): String = selectedGutterType.orEmpty()

    /** 從 args 預填既有資料（有則填，無則走座標預填） */
    private fun prefillData() {
        val args = arguments ?: return

        val spiNum     = args.getString(ARG_DATA_SPI_NUM,     "")
        val nodeTyp    = args.getString(ARG_DATA_NODE_TYP,    "")
        val matTyp     = args.getString(ARG_DATA_MAT_TYP,     "")
        val nodeX      = args.getString(ARG_DATA_NODE_X,      "")
        val nodeY      = args.getString(ARG_DATA_NODE_Y,      "")
        val nodeLe     = args.getString(ARG_DATA_NODE_LE,     "")
        val xyNum      = args.getString(ARG_DATA_XY_NUM,      "")
        val coverDep = args.getString(ARG_DATA_COVER_DEP, "")
        val nodeDep    = args.getString(ARG_DATA_NODE_DEP,    "")
        val nodeWid    = args.getString(ARG_DATA_NODE_WID,    "")
        val isBroken   = args.getString(ARG_DATA_IS_BROKEN,   "")
        val isHanging  = args.getString(ARG_DATA_IS_HANGING,  "")
        val isSilt     = args.getString(ARG_DATA_IS_SILT,     "")
        val isCantOpen = args.getString(ARG_DATA_IS_CANTOPEN, "")
        val nodeNote   = args.getString(ARG_DATA_NODE_NOTE,   "")
        val photo1 = args.getString(ARG_PHOTO_1, "")
        val photo2 = args.getString(ARG_PHOTO_2, "")
        val photo3 = args.getString(ARG_PHOTO_3, "")
        val photo1CapturedAt = args.getString(ARG_PHOTO_1_CAPTURED_AT, "")
        val photo2CapturedAt = args.getString(ARG_PHOTO_2_CAPTURED_AT, "")
        val photo3CapturedAt = args.getString(ARG_PHOTO_3_CAPTURED_AT, "")
        val isPendingDeploy = args.getString(ARG_DATA_IS_PENDING_DEPLOY, "")
        val isVirtualArg = args.getString(ARG_DATA_IS_VIRTUAL, "0")
        val isImportedArg = args.getString(ARG_DATA_IS_IMPORTED, "")

        val hasAnyData = listOf(
            spiNum, nodeTyp, matTyp, nodeX, nodeY, nodeLe,
            xyNum, coverDep, nodeDep, nodeWid, isBroken, isHanging, isSilt, isCantOpen, nodeNote,
            isPendingDeploy, isVirtualArg, isImportedArg,
            photo1, photo2, photo3, photo1CapturedAt, photo2CapturedAt, photo3CapturedAt
        ).any { it.isNotEmpty() && it != "0" && it != "false" }

        if (hasAnyData) {
            setVirtualMode(parseLooseBoolean(isVirtualArg))
            setImportLocked(parseLooseBoolean(isImportedArg))
            binding.etGutterId.setText(spiNum)
            setGutterTypeSelection(nodeTypCodeToText(nodeTyp).takeIf { it.isNotBlank() }, notifyDraftChanged = false)
            binding.rgMatType.setCheckedByText(matTypCodeToText(matTyp))
            coordXValue = nodeX
            coordYValue = nodeY
            binding.etCoordZ.setText(nodeLe)
            binding.etMeasureId.setText(xyNum)
            setPendingDeploySelected(parseLooseBoolean(isPendingDeploy))
            binding.etCoverThickness.setText(coverDep)
            binding.etDepth.setText(nodeDep)
            binding.etTopWidth.setText(nodeWid)
            photoUriSlot1 = parseUriString(photo1)
            photoUriSlot2 = parseUriString(photo2)
            photoUriSlot3 = parseUriString(photo3)
            photoCapturedAtSlot1 = photo1CapturedAt.takeIf { it.isNotBlank() }
            photoCapturedAtSlot2 = photo2CapturedAt.takeIf { it.isNotBlank() }
            photoCapturedAtSlot3 = photo3CapturedAt.takeIf { it.isNotBlank() }
            binding.rgIsBroken.setCheckedByText(isBrokenCodeToText(isBroken))
            binding.rgIsHanging.setCheckedByText(isHangingCodeToText(isHanging))
            binding.rgIsSilt.setCheckedByText(isSiltCodeToText(isSilt))
            
            val cantOpenBool = parseLooseBoolean(isCantOpen)
            binding.cbCantOpen.isChecked = cantOpenBool
            applyCantOpenUi(cantOpenBool)

            binding.etRemarks.setText(nodeNote)
            if (nodeX.isEmpty() && nodeY.isEmpty()) prefillCoordinates()
        } else {
            setGutterTypeSelection(null, notifyDraftChanged = false)
            prefillCoordinates()
        }
    }

    private fun setupPendingDeployButton(isViewMode: Boolean) {
        // View mode: disabled until user enters edit mode (GutterFormActivity.enterEditMode -> setEditable(true))
        binding.btnPendingDeploy.isEnabled = !isViewMode && isFormEditable
        // Avoid stale listeners when toggling enabled state / rebinding view.
        binding.btnPendingDeploy.setOnCheckedChangeListener(null)
        binding.btnPendingDeploy.setOnCheckedChangeListener { _, _ ->
            if (binding.btnPendingDeploy.isEnabled) notifyDraftChanged()
        }
    }

    private fun setPendingDeploySelected(selected: Boolean) {
        // UI spec: checkbox only; text color unchanged; checkbox uses theme primary via XML buttonTint.
        binding.btnPendingDeploy.isChecked = selected
    }

    private fun setupCantOpen() {
        // 檢視模式：僅展示，不允許互動
        val isViewMode = arguments?.getBoolean(ARG_VIEW_MODE) ?: false
        binding.cbCantOpen.isEnabled = !isViewMode

        // 依照目前狀態套用一次
        applyCantOpenUi(binding.cbCantOpen.isChecked)
        if (!isViewMode && binding.cbCantOpen.isChecked) {
            clearMeasurementPhotosForCantOpen(notifyDraftChanged = false)
        }

        // 之後才開始監聽，避免 prefill 時觸發清空
        binding.cbCantOpen.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                // 不可開蓋：下方欄位不必填，直接清空避免誤送舊值
                binding.etCoverThickness.setText("")
                binding.etDepth.setText("")
                binding.etTopWidth.setText("")
                binding.rgMatType.clearCheck()
                binding.rgIsBroken.clearCheck()
                binding.rgIsHanging.clearCheck()
                binding.rgIsSilt.clearCheck()
                binding.tilCoverThickness.error = null
                binding.tilDepth.error = null
                binding.tilTopWidth.error = null
                clearMeasurementPhotosForCantOpen(notifyDraftChanged = false)
            }
            applyCantOpenUi(checked)
            notifyDraftChanged()
        }
    }

    private fun isUOpenGutter(): Boolean {
        return _binding != null && selectedGutterTypeText() == GUTTER_TYPES[0]
    }

    private fun applyCantOpenUi(isCantOpen: Boolean) {
        // 若整個表單不可編輯（檢視模式）或處於匯入鎖定狀態，一律禁用
        if (!isFormEditable || isImportLocked) {
            setCantOpenFieldsEnabled(false)
            return
        }

        val isUOpen = isUOpenGutter()
        // 「無法開蓋」只控制其自身的必填/編輯狀態；
        // 「明溝」只影響溝蓋板厚度欄位，不應牽動待架站或 cbCantOpen。
        val enableFields = !isCantOpen
        setCantOpenFieldsEnabled(enableFields, forceCoverDisabled = isUOpen)

        // 核心修正：當「明溝」或「無法開蓋」時，僅針對「厚度輸入框」顯示白色遮罩
        // 確保 cbCantOpen 勾選框不在遮罩範圍內
        binding.vCoverThicknessOverlay.visibility = if (isUOpen || isCantOpen) View.VISIBLE else View.GONE
        if (isUOpen || isCantOpen) {
            binding.vCoverThicknessOverlay.bringToFront()
        }

        // Log for debug
        Log.d(
            "GutterBasicInfo",
            "applyCantOpenUi isCantOpen=$isCantOpen isUOpen=$isUOpen enableFields=$enableFields"
        )

        binding.cbCantOpen.isEnabled = true
    }

    private fun applyGutterTypeUi() {
        val isUOpen = isUOpenGutter()

        // 確保「溝蓋板厚度」區塊在畫面上保持顯示
        binding.llCoverThicknessWrapper.visibility = View.VISIBLE

        // Log for debug
        Log.d("GutterBasicInfo", "applyGutterTypeUi isUOpen=$isUOpen")

        if (isUOpen) {
            binding.tilCoverThickness.error = null
            binding.etCoverThickness.clearFocus()
            hideKeyboard()
            // 明溝時強制填入 0
            binding.etCoverThickness.setText("0")
        }

        // 重新評估下方細節欄位與厚度遮罩
        applyCantOpenUi(binding.cbCantOpen.isChecked)
    }

    private fun setCantOpenFieldsEnabled(enabled: Boolean, forceCoverDisabled: Boolean = false) {
        // 需要被 disable 的欄位：溝蓋板厚度、深度、頂寬、材質、受損、附掛、淤積
        val viewsToToggle = listOf(
            binding.etCoverThickness, binding.etDepth, binding.etTopWidth,
            binding.rgMatType, binding.rgIsBroken, binding.rgIsHanging, binding.rgIsSilt
        )
        
        viewsToToggle.forEach { v ->
            val shouldEnable = when (v) {
                binding.etCoverThickness -> enabled && !forceCoverDisabled
                else -> enabled
            }
            v.isEnabled = shouldEnable
            if (v is android.widget.EditText) {
                v.isFocusable = shouldEnable
                v.isFocusableInTouchMode = shouldEnable
                v.isCursorVisible = shouldEnable
            } else if (v is android.widget.RadioGroup) {
                v.setChildrenEnabled(shouldEnable)
            }
        }

        val coverAlpha = if (enabled && !forceCoverDisabled) 1f else 0.5f
        binding.tilCoverThickness.alpha = coverAlpha
        binding.vCoverThicknessOverlay.isClickable = forceCoverDisabled
        binding.vCoverThicknessOverlay.isFocusable = forceCoverDisabled

        val alpha = if (enabled) 1f else 0.5f
        listOf(
            binding.tilDepth,
            binding.tilTopWidth,
            binding.rgMatType,
            binding.rgIsBroken,
            binding.rgIsHanging,
            binding.rgIsSilt
        ).forEach { it.alpha = alpha }
    }

    private fun setupLocationPickerButton() {
        binding.btnPickLocation.setOnClickListener {
            // 檢視模式不允許變更座標
            val isViewMode = arguments?.getBoolean(ARG_VIEW_MODE) ?: false
            if (isViewMode) return@setOnClickListener
            onRequestLocationPick?.invoke()
        }
    }

    /** 將 GPS 座標預填至 X/Y 欄位 */
    private fun prefillCoordinates() {
        val lat           = arguments?.getDouble(ARG_LAT)                    ?: return
        val lng           = arguments?.getDouble(ARG_LNG)                    ?: return
        val isOfflineMode = arguments?.getBoolean(ARG_OFFLINE_MODE, false)  ?: false

        when {
            isOfflineMode -> {
                coordXValue = "0.0"
                coordYValue = "0.0"
            }
            lat != 0.0 || lng != 0.0 -> {
                coordXValue = "%.6f".format(lng)
                coordYValue = "%.6f".format(lat)
            }
        }
    }

    /**
     * 切換所有輸入欄位的可編輯狀態。
     */
    fun setEditable(enabled: Boolean) {
        isFormEditable = enabled
        val actualEnabled = enabled && !isImportLocked
        val textFields = listOf(
            binding.etGutterId,
            binding.etMeasureId,
            binding.etCoverThickness,
            binding.etDepth,
            binding.etTopWidth,
            binding.etRemarks
        )
        textFields.forEach { et ->
            et.isEnabled = actualEnabled
            et.isFocusable = actualEnabled
            et.isFocusableInTouchMode = actualEnabled
        }

        listOf(
            binding.rgMatType,
            binding.rgIsBroken,
            binding.rgIsHanging,
            binding.rgIsSilt
        ).forEach { rg -> rg.setChildrenEnabled(actualEnabled) }
        binding.layoutGutterTypeSelector.isEnabled = actualEnabled
        binding.tvGutterTypeSelector.isEnabled = actualEnabled

        // 匯入鎖定與檢視模式均使用 0.5 半透明，提供一致的「不可修改」視覺暗示
        val alpha = if (actualEnabled) 1f else 0.5f
        listOf(
            binding.tilGutterId,
            binding.layoutGutterTypeSelector,
            binding.rgMatType,
            binding.tilMeasureId,
            binding.tilCoverThickness,
            binding.tilDepth,
            binding.tilTopWidth,
            binding.rgIsBroken,
            binding.rgIsHanging,
            binding.rgIsSilt,
            binding.tilRemarks
        ).forEach { it.alpha = alpha }

        binding.cbCantOpen.isEnabled = actualEnabled
        binding.btnPendingDeploy.isEnabled = actualEnabled
        // Re-apply style (so view->edit mode transitions update colors correctly)
        setPendingDeploySelected(binding.btnPendingDeploy.isChecked)

        // Z 座標（NODE_LE）由後端提供，可檢視但不可修改
        binding.etCoordZ.isEnabled = false
        binding.etCoordZ.isFocusable = false
        binding.etCoordZ.isFocusableInTouchMode = false
        binding.tilCoordZ.alpha = 1f

        // 重新套用「無法開蓋」狀態（例如從檢視進入編輯）
        applyCantOpenUi(binding.cbCantOpen.isChecked)
        applyImportLockUi()
        setPhotoEditable(actualEnabled, isViewMode = !enabled)
    }

    /** 隱藏虛擬鍵盤 */
    private fun hideKeyboard() {
        val view = activity?.currentFocus ?: return
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /** 清除所有輸入框的焦點 */
    private fun clearAllFocus() {
        listOf(
            binding.etGutterId,
            binding.etCoordZ,
            binding.etMeasureId,
            binding.etCoverThickness,
            binding.etDepth,
            binding.etTopWidth,
            binding.etRemarks
        ).forEach { it.clearFocus() }
    }

    /**
     * 驗證必填欄位，以及深度與頂寬的合理區間（防呆）。
     *
     * 規則來源：桃園側溝分析文件_防呆
     *   NODE_DEP：9 < 值 （公分）
     *   NODE_WID：值 > 25（公分）
     *
     * @return 第一個未填或超出範圍的欄位提示字串；全部通過則回傳 null。
     */
    fun validateRequiredFields(): String? {
        val d = collectData()
        val isVirtual = parseLooseBoolean(d["is_virtual"])
        val isCantOpen = parseLooseBoolean(d["IS_CANTOPEN"])
        val isUOpen = isUOpenGutter()

        // 虛擬模式下，僅驗證位置與座標編號
        if (isVirtual) {
            if (d["NODE_X"].isNullOrEmpty())      return "側溝位置"
            if (d["NODE_Y"].isNullOrEmpty())      return "側溝位置"
            if (d["XY_NUM"].isNullOrEmpty())      return "測量座標編號"
            return null
        }

        // 點選「明溝」或「無法開蓋」時，下方細節欄位不用填寫（跳過驗證）
        if (isUOpen || isCantOpen) return null

        if (d["MAT_TYP"].isNullOrEmpty())     return "側溝材質"

        // ── NODE_DEP 深度必填 + 區間防呆 ─────────────────────────
        if (d["NODE_DEP"].isNullOrEmpty()) return "側溝測量深度"
        val depth = d["NODE_DEP"]!!.toDoubleOrNull()
        if (depth == null || depth <= 9) {
            binding.tilDepth.error = "合理區間：大於 9 公分"
            return "側溝測量深度（合理區間：大於 9 公分）"
        }

        // ── NODE_WID 頂寬必填 + 區間防呆 ─────────────────────────
        if (d["NODE_WID"].isNullOrEmpty()) return "側溝頂寬度"
        val topWidth = d["NODE_WID"]!!.toDoubleOrNull()
        if (topWidth == null || topWidth <= 25.0) {
            binding.tilTopWidth.error = "需大於 25 公分"
            return "側溝頂寬度（需大於 25 公分）"
        }

        if (d["IS_BROKEN"].isNullOrEmpty())   return "溝體結構受損"
        if (d["IS_HANGING"].isNullOrEmpty())  return "附掛或過路管線"
        if (d["IS_SILT"].isNullOrEmpty())     return "淤積程度"
        return null
    }

    /**
     * 監聽深度與頂寬輸入，使用者開始修改時自動清除錯誤提示，
     * 並在離開焦點時即時顯示範圍錯誤。
     */
    private fun setupRangeWatchers() {
        // 清除錯誤的通用 TextWatcher
        fun clearErrorWatcher(clearAction: () -> Unit) = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) = clearAction()
            override fun afterTextChanged(s: Editable?) = Unit
        }

        binding.etDepth.addTextChangedListener(clearErrorWatcher {
            binding.tilDepth.error = null
        })
        binding.etTopWidth.addTextChangedListener(clearErrorWatcher {
            binding.tilTopWidth.error = null
        })

        // 離開焦點時即時驗證
        binding.etDepth.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val v = binding.etDepth.text?.toString()?.toDoubleOrNull()
                binding.tilDepth.error = when {
                    v == null           -> null   // 空值留給 validateRequiredFields 處理
                    v <= 9 -> "合理區間：大於 9 公分"
                    else                -> null
                }
            }
        }
        binding.etTopWidth.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val v = binding.etTopWidth.text?.toString()?.toDoubleOrNull()
                binding.tilTopWidth.error = when {
                    v == null      -> null
                    v <= 25.0      -> "需大於 25 公分"
                    else           -> null
                }
            }
        }
    }

    private fun setupDraftWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                notifyDraftChanged()
            }
        }

        listOf(
            binding.etGutterId,
            binding.etCoordZ,
            binding.etMeasureId,
            binding.etCoverThickness,
            binding.etDepth,
            binding.etTopWidth,
            binding.etRemarks
        ).forEach { it.addTextChangedListener(watcher) }

        val radioListener = RadioGroup.OnCheckedChangeListener { _, _ -> notifyDraftChanged() }
        listOf(
            binding.rgMatType,
            binding.rgIsBroken,
            binding.rgIsHanging,
            binding.rgIsSilt
        ).forEach { it.setOnCheckedChangeListener(radioListener) }
    }

    /** 收集表單資料（供 GutterFormActivity 提交用） */
    fun collectData(): Map<String, String> {
        val gutterTypeText = selectedGutterTypeText()
        val isUOpen = gutterTypeText == GUTTER_TYPES[0]
        
        return mapOf(
            "is_virtual"  to (if (isVirtualMode) "1" else "0"),
            "_isImported" to (if (isImportLocked) "1" else "0"),
            "SPI_NUM"     to (binding.etGutterId.text?.toString()      ?: ""),
            "NODE_TYP"    to gutterTypeTextToCode(gutterTypeText),
            "MAT_TYP"     to matTypeTextToCode(binding.rgMatType.getCheckedText()),
            "NODE_X"      to coordXValue,
            "NODE_Y"      to coordYValue,
            "NODE_LE"     to (binding.etCoordZ.text?.toString()        ?: ""),
            "XY_NUM"      to (binding.etMeasureId.text?.toString()     ?: ""),
            // 待架站（點位層級）：以 "1"/"0" 形式存入 basicData
            "IS_PENDING_DEPLOY" to (if (binding.btnPendingDeploy.isChecked) "1" else "0"),
            // 主要 key：COVER_DEP（API 欄位名）。若是「明溝」，固定回傳 "0"
            "COVER_DEP" to if (isUOpen) "0" else (binding.etCoverThickness.text?.toString() ?: ""),
            "NODE_DEP"    to (binding.etDepth.text?.toString()         ?: ""),
            "NODE_WID"    to (binding.etTopWidth.text?.toString()      ?: ""),
            "IS_BROKEN"   to brokenTextToCode(binding.rgIsBroken.getCheckedText()),
            "IS_HANGING"  to hangingTextToCode(binding.rgIsHanging.getCheckedText()),
            "IS_SILT"     to siltTextToCode(binding.rgIsSilt.getCheckedText()),
            // 以 "1"/"" 形式存入 basicData（送出 API 時再轉為 JSON boolean）
            "IS_CANTOPEN" to (if (binding.cbCantOpen.isChecked) "1" else ""),
            "NODE_NOTE"   to (binding.etRemarks.text?.toString()       ?: ""),
            "photo1" to (photoUriSlot1?.toString() ?: ""),
            "photo2" to (photoUriSlot2?.toString() ?: ""),
            "photo3" to (photoUriSlot3?.toString() ?: ""),
            "photo1CapturedAt" to (photoCapturedAtSlot1 ?: ""),
            "photo2CapturedAt" to (photoCapturedAtSlot2 ?: ""),
            "photo3CapturedAt" to (photoCapturedAtSlot3 ?: "")
        )
    }

    fun updateCoordinates(longitude: Double, latitude: Double) {
        coordXValue = "%.6f".format(longitude)
        coordYValue = "%.6f".format(latitude)
        notifyDraftChanged()
    }

    /** 切換虛擬點模式 */
    fun setVirtualMode(isVirtual: Boolean) {
        isVirtualMode = isVirtual
        val visibility = if (isVirtual) View.GONE else View.VISIBLE
        binding.llVirtualHidden1.visibility = visibility
        binding.llVirtualHidden2.visibility = visibility
        // 根據需求保留「溝蓋板厚度」欄位，不隨虛擬模式隱藏
        binding.llVirtualHidden3.visibility = visibility
        binding.layoutOverviewPhotoSection.visibility = visibility
        binding.layoutWidthPhotoSection.visibility = visibility
        binding.layoutDepthPhotoSection.visibility = visibility
        notifyDraftChanged()
    }

    fun setImportLocked(locked: Boolean) {
        isImportLocked = locked
        applyImportLockUi()
        setEditable(isFormEditable)
    }

    private fun applyImportLockUi() {
        if (_binding == null) return
        binding.importLockOverlay.visibility = View.GONE
        binding.root.foreground =
            if (isImportLocked) ContextCompat.getDrawable(requireContext(), R.drawable.bg_import_lock_scrim) else null
    }

    fun prefillDataFromImport(nodeDetails: com.example.taoyuangutter.api.NodeDetails) {
        setImportLocked(true)
        binding.apply {
            // 基本資訊
            etMeasureId.setText(nodeDetails.xyNum ?: "")
            // 匯入既有點位時，待架站預設為否（可再由使用者自行切換）
            setPendingDeploySelected(false)

            // 溝型（API key 為 NODE_TYP，值為字串）
            val nodeTypText = when (nodeDetails.nodeTyP?.toIntOrNull()) {
                1 -> GUTTER_TYPES[0]  // U型溝（明溝）
                2 -> GUTTER_TYPES[1]  // U型溝（加蓋）
                else -> ""
            }
            setGutterTypeSelection(nodeTypText.takeIf { it.isNotBlank() }, notifyDraftChanged = false)

            // 材質（API key 為 MAT_TYP，值為字串）
            val matTypText = when (nodeDetails.matTyp?.toIntOrNull()) {
                1 -> MAT_TYPES[0]  // 混凝土
                2 -> MAT_TYPES[1]  // 卵礫石
                else -> ""
            }
            rgMatType.setCheckedByText(matTypText)

            // 深度、寬度（API 回傳 Double，使用 AsString 方法自動轉換）
            etCoverThickness.setText(nodeDetails.coverDepAsString)
            etDepth.setText(nodeDetails.nodeDepAsString)
            etTopWidth.setText(nodeDetails.nodeWidAsString)

            // 破損狀態（API key 為 IS_BROKEN，值為字串）
            val brokenText = when (nodeDetails.isBroken?.toIntOrNull()) {
                0 -> BROKEN_OPTIONS[0]  // 無破損
                1 -> BROKEN_OPTIONS[1]  // 有破損
                else -> ""
            }
            rgIsBroken.setCheckedByText(brokenText)

            // 懸掛狀態（API key 為 IS_HANGING，值為字串）
            val hangingText = when (nodeDetails.isHanging?.toIntOrNull()) {
                0 -> HANGING_OPTIONS[0]  // 無懸掛
                1 -> HANGING_OPTIONS[1]  // 有懸掛
                else -> ""
            }
            rgIsHanging.setCheckedByText(hangingText)

            // 淤積狀態（API key 為 IS_SILT，值為字串）
            val siltText = when (nodeDetails.isSilt?.toIntOrNull()) {
                0 -> SILT_OPTIONS[0]  // 無
                1 -> SILT_OPTIONS[1]  // 輕度
                2 -> SILT_OPTIONS[2]  // 嚴重
                3 -> SILT_OPTIONS[2]  // 舊資料緩衝：一律視為嚴重
                else -> ""
            }
            rgIsSilt.setCheckedByText(siltText)

            // 無法開蓋狀態（使用 isCantOpenAsBoolean 方法處理型別轉換）
            cbCantOpen.isChecked = nodeDetails.isCantOpenAsBoolean

            // 備註（API key 為 NOTE）
            etRemarks.setText(nodeDetails.note ?: "")

            // 坐標資訊（WGS84 經緯度，由 API 回傳為字串）
            if (!nodeDetails.latitude.isNullOrEmpty() && !nodeDetails.longitude.isNullOrEmpty()) {
                coordXValue = nodeDetails.longitude ?: coordXValue
                coordYValue = nodeDetails.latitude ?: coordYValue
            }

            // Z 坐標（標高，API key 為 NODE_LE，回傳字串）
            if (!nodeDetails.nodeLe.isNullOrEmpty()) {
                etCoordZ.setText(nodeDetails.nodeLe)
            }
        }
        applyGutterTypeUi()
        notifyDraftChanged()
    }

    fun validateAllPhotos(): String? {
        val context = context ?: return "照片尚未準備完成"
        if (!PhotoUploadValidator.isUsableForUpload(context, photoUriSlot1?.toString())) {
            return "測量位置及側溝概況"
        }
        val isCantOpen = binding.cbCantOpen.isChecked
        if (!isCantOpen && !PhotoUploadValidator.isUsableForUpload(context, photoUriSlot2?.toString())) {
            return "側溝頂寬度"
        }
        if (!isCantOpen && !PhotoUploadValidator.isUsableForUpload(context, photoUriSlot3?.toString())) {
            return "側溝測量深度"
        }
        return null
    }

    fun getPhotoPaths(): Triple<String?, String?, String?> = Triple(
        photoUriSlot1?.toString(),
        photoUriSlot2?.toString(),
        photoUriSlot3?.toString()
    )

    fun syncPersistedPhotoState(
        photo1: String?,
        photo2: String?,
        photo3: String?,
        capturedAt1: String?,
        capturedAt2: String?,
        capturedAt3: String?
    ) {
        suppressPhotoDraftCallbacks = true
        try {
            photoUriSlot1 = parseUriString(photo1)
            photoUriSlot2 = parseUriString(photo2)
            photoUriSlot3 = parseUriString(photo3)
            photoCapturedAtSlot1 = capturedAt1?.takeIf { it.isNotBlank() }
            photoCapturedAtSlot2 = capturedAt2?.takeIf { it.isNotBlank() }
            photoCapturedAtSlot3 = capturedAt3?.takeIf { it.isNotBlank() }
            if (_binding != null) {
                renderStoredPhotoSlots()
                setPhotoEditable(isFormEditable && !isImportLocked, isViewMode = !isFormEditable)
            }
        } finally {
            suppressPhotoDraftCallbacks = false
        }
    }

    fun prefillPhotos(
        photo1: String?,
        photo2: String?,
        photo3: String?,
        capturedAt1: String? = null,
        capturedAt2: String? = null,
        capturedAt3: String? = null
    ) {
        photoUriSlot1 = parseUriString(photo1)
        photoUriSlot2 = parseUriString(photo2)
        photoUriSlot3 = parseUriString(photo3)
        photoCapturedAtSlot1 = capturedAt1?.takeIf { it.isNotBlank() }
        photoCapturedAtSlot2 = capturedAt2?.takeIf { it.isNotBlank() }
        photoCapturedAtSlot3 = capturedAt3?.takeIf { it.isNotBlank() }
        if (_binding != null) {
            renderStoredPhotoSlots()
            setPhotoEditable(isFormEditable && !isImportLocked, isViewMode = !isFormEditable)
        }
        notifyPhotoDraftChanged()
    }

    private fun setPhotoEditable(enabled: Boolean, isViewMode: Boolean) {
        if (_binding == null) return
        if (enabled) {
            binding.btnTakePhotoSlot1.setOnClickListener { requestCameraForSlot(1) }
            binding.btnTakePhotoSlot2.setOnClickListener { requestCameraForSlot(2) }
            binding.btnTakePhotoSlot3.setOnClickListener { requestCameraForSlot(3) }
            binding.photoSlot1.setOnClickListener { requestCameraForSlot(1) }
            binding.photoSlot2.setOnClickListener { requestCameraForSlot(2) }
            binding.photoSlot3.setOnClickListener { requestCameraForSlot(3) }
            binding.btnDeleteSlot1.setOnClickListener { deletePhoto(1) }
            binding.btnDeleteSlot2.setOnClickListener { deletePhoto(2) }
            binding.btnDeleteSlot3.setOnClickListener { deletePhoto(3) }
        } else {
            hasShownPhotoLoadErrorAlert = false
            binding.btnTakePhotoSlot1.setOnClickListener(null)
            binding.btnTakePhotoSlot2.setOnClickListener(null)
            binding.btnTakePhotoSlot3.setOnClickListener(null)
            binding.photoSlot1.setOnClickListener(null)
            binding.photoSlot2.setOnClickListener(null)
            binding.photoSlot3.setOnClickListener(null)
            binding.btnDeleteSlot1.visibility = View.GONE
            binding.btnDeleteSlot2.visibility = View.GONE
            binding.btnDeleteSlot3.visibility = View.GONE
        }
        renderPhotoSectionState(1, enabled, isViewMode)
        renderPhotoSectionState(2, enabled, isViewMode)
        renderPhotoSectionState(3, enabled, isViewMode)
        val alpha = if (enabled) 1f else 0.6f
        binding.layoutOverviewPhotoSection.alpha = alpha
        binding.layoutWidthPhotoSection.alpha = alpha
        binding.layoutDepthPhotoSection.alpha = alpha
    }

    private fun renderPhotoSectionState(slot: Int, editable: Boolean, isViewMode: Boolean) {
        val hasPhoto = when (slot) {
            1 -> photoUriSlot1 != null
            2 -> photoUriSlot2 != null
            else -> photoUriSlot3 != null
        }
        val isCantOpenPhotoSlot = binding.cbCantOpen.isChecked && slot in 2..3
        val button = when (slot) {
            1 -> binding.btnTakePhotoSlot1
            2 -> binding.btnTakePhotoSlot2
            else -> binding.btnTakePhotoSlot3
        }
        val card = when (slot) {
            1 -> binding.photoSlot1
            2 -> binding.photoSlot2
            else -> binding.photoSlot3
        }
        val placeholder = when (slot) {
            1 -> binding.placeholderSlot1
            2 -> binding.placeholderSlot2
            else -> binding.placeholderSlot3
        }
        val delete = when (slot) {
            1 -> binding.btnDeleteSlot1
            2 -> binding.btnDeleteSlot2
            else -> binding.btnDeleteSlot3
        }

        if (hasPhoto) {
            button.visibility = View.GONE
            card.visibility = View.VISIBLE
            placeholder.visibility = View.GONE
            delete.visibility = if (editable) View.VISIBLE else View.GONE
        } else {
            button.visibility = if (editable) View.VISIBLE else View.GONE
            card.visibility = View.GONE
            placeholder.visibility = if (isViewMode) View.INVISIBLE else View.VISIBLE
            delete.visibility = View.GONE
        }
        applyTakePhotoButtonStyle(button, enabled = editable && !isCantOpenPhotoSlot, disabledByCantOpen = isCantOpenPhotoSlot)
    }

    private fun deletePhoto(slot: Int) {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("確認刪除")
            .setMessage("確定要刪除這張照片嗎？")
            .setNegativeButton("取消", null)
            .setPositiveButton("刪除") { _, _ ->
                clearPhotoSlot(slot, notifyDraftChanged = true)
            }
            .show()
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            .setTextColor(android.graphics.Color.parseColor("#D32F2F"))
    }

    private fun requestCameraForSlot(slot: Int) {
        if (binding.cbCantOpen.isChecked && slot in 2..3) return
        pendingSlot = slot
        if (ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            maybeRequestLegacyWritePermissionThenLaunch()
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun maybeRequestLegacyWritePermissionThenLaunch() {
        val slot = pendingSlot
        if (slot <= 0) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            legacyWritePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        launchCameraOverlay(slot)
    }

    private fun launchCameraOverlay(slot: Int) {
        val outputFile = createOutputFile(slot)
        if (outputFile == null) {
            val failedSlot = pendingSlot
            pendingSlot = 0
            if (failedSlot > 0) {
                photoDraftChangeHost?.onPendingPhotoDraftChanged(failedSlot, null)
            }
            Toast.makeText(requireContext(), getString(R.string.msg_photo_prepare_failed), Toast.LENGTH_SHORT).show()
            return
        }
        pendingOutputPath = outputFile.absolutePath
        photoDraftChangeHost?.onPendingPhotoDraftChanged(slot, outputFile.absolutePath)
        (activity as? GutterFormActivity)?.showCameraOverlay(slot, outputFile.absolutePath)
    }

    private fun createOutputFile(slot: Int): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: File(requireContext().filesDir, "Pictures").apply { mkdirs() }
        return try {
            File.createTempFile("GUTTER_${slot}_${timeStamp}_", ".jpg", storageDir)
        } catch (_: Exception) {
            null
        }
    }

    private fun renderStoredPhotoSlots() {
        applyPhotoToSlot(1, photoUriSlot1, notifyDraftChanged = false)
        applyPhotoToSlot(2, photoUriSlot2, notifyDraftChanged = false)
        applyPhotoToSlot(3, photoUriSlot3, notifyDraftChanged = false)
        renderCapturedAtLabels()
    }

    private fun applyPhotoToSlot(slot: Int, uri: Uri?, notifyDraftChanged: Boolean) {
        if (uri == null) {
            clearPhotoSlot(slot, notifyDraftChanged)
            return
        }
        val context = context
        if (context != null && !PhotoUploadValidator.isUsableForUpload(context, uri.toString())) {
            if (notifyDraftChanged) {
                clearPhotoSlot(slot, notifyDraftChanged)
            } else {
                when (slot) {
                    1 -> {
                        photoUriSlot1 = uri
                        showPhoto(binding.ivPhotoSlot1, binding.placeholderSlot1, binding.pbPhotoLoading1, null)
                    }
                    2 -> {
                        photoUriSlot2 = uri
                        showPhoto(binding.ivPhotoSlot2, binding.placeholderSlot2, binding.pbPhotoLoading2, null)
                    }
                    3 -> {
                        photoUriSlot3 = uri
                        showPhoto(binding.ivPhotoSlot3, binding.placeholderSlot3, binding.pbPhotoLoading3, null)
                    }
                }
                (activity as? PhotoLoadingHost)?.setPhotoLoading(false)
            }
            return
        }
        when (slot) {
            1 -> {
                photoUriSlot1 = uri
                showPhoto(binding.ivPhotoSlot1, binding.placeholderSlot1, binding.pbPhotoLoading1, uri)
            }
            2 -> {
                photoUriSlot2 = uri
                showPhoto(binding.ivPhotoSlot2, binding.placeholderSlot2, binding.pbPhotoLoading2, uri)
            }
            3 -> {
                photoUriSlot3 = uri
                showPhoto(binding.ivPhotoSlot3, binding.placeholderSlot3, binding.pbPhotoLoading3, uri)
            }
        }
        renderPhotoSectionState(slot, isFormEditable && !isImportLocked, isViewMode = !isFormEditable)
        if (notifyDraftChanged) notifyPhotoDraftChanged()
    }

    private fun clearPhotoSlot(slot: Int, notifyDraftChanged: Boolean) {
        (activity as? GutterFormActivity)?.beginPhotoDraftBatch()
        try {
            when (slot) {
                1 -> {
                    photoUriSlot1 = null
                    photoCapturedAtSlot1 = null
                    showPhoto(binding.ivPhotoSlot1, binding.placeholderSlot1, binding.pbPhotoLoading1, null)
                }
                2 -> {
                    photoUriSlot2 = null
                    photoCapturedAtSlot2 = null
                    showPhoto(binding.ivPhotoSlot2, binding.placeholderSlot2, binding.pbPhotoLoading2, null)
                }
                3 -> {
                    photoUriSlot3 = null
                    photoCapturedAtSlot3 = null
                    showPhoto(binding.ivPhotoSlot3, binding.placeholderSlot3, binding.pbPhotoLoading3, null)
                }
            }
            renderPhotoSectionState(slot, isFormEditable && !isImportLocked, isViewMode = !isFormEditable)
            renderCapturedAtLabels()
            (activity as? PhotoLoadingHost)?.setPhotoLoading(false)
            if (!suppressPhotoDraftCallbacks) {
                photoDraftChangeHost?.onPhotoCapturedAtDraftChanged(slot, null)
                photoDraftChangeHost?.onPendingPhotoDraftChanged(slot, null)
            }
            if (notifyDraftChanged) notifyPhotoDraftChanged()
        } finally {
            (activity as? GutterFormActivity)?.endPhotoDraftBatch()
        }
    }

    private fun clearMeasurementPhotosForCantOpen(notifyDraftChanged: Boolean) {
        if (_binding == null) return
        (activity as? GutterFormActivity)?.beginPhotoDraftBatch()
        try {
            clearPhotoSlot(2, notifyDraftChanged = false)
            clearPhotoSlot(3, notifyDraftChanged = false)
            if (notifyDraftChanged) notifyPhotoDraftChanged()
        } finally {
            (activity as? GutterFormActivity)?.endPhotoDraftBatch()
        }
    }

    private fun applyTakePhotoButtonStyle(
        button: MaterialButton,
        enabled: Boolean,
        disabledByCantOpen: Boolean
    ) {
        button.isEnabled = enabled
        button.alpha = 1f
        val context = button.context
        val background = if (disabledByCantOpen) {
            ContextCompat.getColor(context, R.color.photoButtonDisabled)
        } else {
            ContextCompat.getColor(context, R.color.colorBgTitle)
        }
        val stroke = if (disabledByCantOpen) {
            ContextCompat.getColor(context, R.color.photoButtonDisabled)
        } else {
            ContextCompat.getColor(context, R.color.colorPrimaryLight2)
        }
        val text = if (disabledByCantOpen) {
            ContextCompat.getColor(context, R.color.white)
        } else {
            ContextCompat.getColor(context, R.color.colorPrimary)
        }
        button.backgroundTintList = ColorStateList.valueOf(background)
        button.strokeColor = ColorStateList.valueOf(stroke)
        button.setTextColor(text)
    }

    private fun showPhoto(
        photoView: ImageView,
        placeholder: View,
        loading: View,
        uri: Uri?
    ) {
        if (uri == null) {
            loading.visibility = View.GONE
            placeholder.visibility = View.VISIBLE
            photoView.visibility = View.GONE
            Glide.with(this).clear(photoView)
            photoView.setImageDrawable(null)
            return
        }
        placeholder.visibility = View.GONE
        photoView.visibility = View.VISIBLE
        val startMs = SystemClock.uptimeMillis()
        loading.visibility = View.VISIBLE
        val scheme = uri.scheme?.lowercase()
        val isRemote = scheme == "http" || scheme == "https"
        val targetW = photoView.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val targetH = (targetW * 3) / 4
        Glide.with(this)
            .load(uri)
            .override(targetW, targetH)
            .centerCrop()
            .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<android.graphics.drawable.Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    if (_binding != null) {
                        val delayMs = (250L - (SystemClock.uptimeMillis() - startMs)).coerceAtLeast(0L)
                        loading.postDelayed({ if (_binding != null) loading.visibility = View.GONE }, delayMs)
                    }
                    (activity as? PhotoLoadingHost)?.setPhotoLoading(false)
                    photoView.visibility = View.GONE
                    placeholder.visibility = View.VISIBLE
                    if (isRemote) showPhotoLoadErrorAlert()
                    return true
                }

                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable,
                    model: Any,
                    target: Target<android.graphics.drawable.Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    if (_binding != null) {
                        val delayMs = (250L - (SystemClock.uptimeMillis() - startMs)).coerceAtLeast(0L)
                        loading.postDelayed({ if (_binding != null) loading.visibility = View.GONE }, delayMs)
                    }
                    (activity as? PhotoLoadingHost)?.setPhotoLoading(false)
                    return false
                }
            })
            .into(photoView)
    }

    private fun setCapturedAtForSlot(slot: Int, capturedAt: String?, notifyDraftChanged: Boolean) {
        val normalized = capturedAt?.takeIf { it.isNotBlank() }
        when (slot) {
            1 -> photoCapturedAtSlot1 = normalized
            2 -> photoCapturedAtSlot2 = normalized
            3 -> photoCapturedAtSlot3 = normalized
            else -> return
        }
        renderCapturedAtLabels()
        if (notifyDraftChanged && !suppressPhotoDraftCallbacks) {
            photoDraftChangeHost?.onPhotoCapturedAtDraftChanged(slot, normalized)
        }
    }

    private fun renderCapturedAtLabels() {
        bindCapturedAt(binding.tvPhotoTime1, photoUriSlot1 != null, photoCapturedAtSlot1)
        bindCapturedAt(binding.tvPhotoTime2, photoUriSlot2 != null, photoCapturedAtSlot2)
        bindCapturedAt(binding.tvPhotoTime3, photoUriSlot3 != null, photoCapturedAtSlot3)
    }

    private fun bindCapturedAt(view: TextView, hasPhoto: Boolean, capturedAt: String?) {
        if (!hasPhoto) {
            view.text = ""
            view.visibility = View.GONE
            return
        }
        val text = capturedAt?.takeIf { it.isNotBlank() }
        if (text.isNullOrBlank()) {
            view.text = ""
            view.visibility = View.GONE
            return
        }
        view.text = text
        view.visibility = View.VISIBLE
    }

    private fun showPhotoLoadErrorAlert() {
        if (hasShownPhotoLoadErrorAlert) return
        hasShownPhotoLoadErrorAlert = true
        val act = activity ?: return
        act.runOnUiThread {
            if (act.isFinishing || act.isDestroyed) return@runOnUiThread
            MaterialAlertDialogBuilder(act)
                .setTitle("資料加載不完整")
                .setMessage("部分照片無法載入，資料可能不完整。\n\n請關閉後重新點選側溝線段。")
                .setPositiveButton("確定", null)
                .show()
        }
    }

    private fun notifyPhotoDraftChanged() {
        if (suppressPhotoDraftCallbacks) return
        photoDraftChangeHost?.onPhotosDraftChanged(
            photoUriSlot1?.toString(),
            photoUriSlot2?.toString(),
            photoUriSlot3?.toString()
        )
    }

    private fun notifyDraftChanged() {
        onDraftChanged?.invoke()
        if (_binding != null) {
            draftChangeHost?.onBasicInfoDraftChanged(collectData())
        }
    }

    private fun nodeTypCodeToText(code: String?): String = when (code) {
        "1" -> GUTTER_TYPES[0]
        "2" -> GUTTER_TYPES[1]
        "3" -> GUTTER_TYPES[2]
        "4" -> GUTTER_TYPES[3]
        else -> code ?: ""
    }

    private fun matTypCodeToText(code: String?): String = when (code) {
        "1" -> MAT_TYPES[0]
        "2" -> MAT_TYPES[1]
        "3" -> MAT_TYPES[2]
        else -> code ?: ""
    }

    private fun isBrokenCodeToText(code: String?): String = when (code) {
        "0" -> BROKEN_OPTIONS[0]
        "1" -> BROKEN_OPTIONS[1]
        else -> code ?: ""
    }

    private fun isHangingCodeToText(code: String?): String = when (code) {
        "0" -> HANGING_OPTIONS[0]
        "1" -> HANGING_OPTIONS[1]
        else -> code ?: ""
    }

    private fun isSiltCodeToText(code: String?): String = when (code) {
        "0" -> SILT_OPTIONS[0]
        "1" -> SILT_OPTIONS[1]
        "2" -> SILT_OPTIONS[2]
        "3" -> SILT_OPTIONS[2]  // 舊資料緩衝：一律視為嚴重
        else -> code ?: ""
    }

    private fun gutterTypeTextToCode(text: String?): String = when (text) {
        GUTTER_TYPES[0] -> "1"
        GUTTER_TYPES[1] -> "2"
        GUTTER_TYPES[2] -> "3"
        GUTTER_TYPES[3] -> "4"
        else -> text ?: ""
    }

    private fun matTypeTextToCode(text: String?): String = when (text) {
        MAT_TYPES[0] -> "1"
        MAT_TYPES[1] -> "2"
        MAT_TYPES[2] -> "3"
        else -> text ?: ""
    }

    private fun brokenTextToCode(text: String?): String = when (text) {
        BROKEN_OPTIONS[0] -> "0"
        BROKEN_OPTIONS[1] -> "1"
        else -> text ?: ""
    }

    private fun hangingTextToCode(text: String?): String = when (text) {
        HANGING_OPTIONS[0] -> "0"
        HANGING_OPTIONS[1] -> "1"
        else -> text ?: ""
    }

    private fun siltTextToCode(text: String?): String = when (text) {
        SILT_OPTIONS[0] -> "0"
        SILT_OPTIONS[1] -> "1"
        SILT_OPTIONS[2] -> "2"
        else -> text ?: ""
    }

    private fun parseLooseBoolean(raw: String?): Boolean {
        val v = raw?.trim()?.lowercase()
        return when (v) {
            "1", "true", "t", "y", "yes" -> true
            "0", "false", "f", "n", "no", "", null -> false
            else -> false
        }
    }

    private fun parseUriString(uriString: String?): Uri? =
        uriString?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
}
