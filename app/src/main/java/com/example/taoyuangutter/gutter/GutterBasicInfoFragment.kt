package com.example.taoyuangutter.gutter

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.taoyuangutter.R
import com.example.taoyuangutter.databinding.FragmentGutterBasicInfoBinding

class GutterBasicInfoFragment : Fragment() {

    private var _binding: FragmentGutterBasicInfoBinding? = null
    private val binding get() = _binding!!
    var onDraftChanged: (() -> Unit)? = null
    var onRequestLocationPick: (() -> Unit)? = null
    private var isFormEditable: Boolean = true

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
        private const val ARG_DATA_NODE_DEP    = "d_node_dep"
        private const val ARG_DATA_NODE_WID    = "d_node_wid"
        private const val ARG_DATA_IS_BROKEN   = "d_is_broken"
        private const val ARG_DATA_IS_HANGING  = "d_is_hanging"
        private const val ARG_DATA_IS_SILT     = "d_is_silt"
        private const val ARG_DATA_IS_CANTOPEN = "d_is_cantopen"
        private const val ARG_DATA_NODE_NOTE   = "d_node_note"

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
        val SILT_OPTIONS = listOf("無", "輕度", "中度", "嚴重")

        fun newInstance(
            latitude: Double,
            longitude: Double,
            viewMode: Boolean = false,
            basicData: HashMap<String, String> = hashMapOf(),
            isOfflineMode: Boolean = false,
            isEditMode: Boolean = false // 新增：是否為編輯模式
        ) = GutterBasicInfoFragment().apply {
            arguments = Bundle().apply {
                putDouble(ARG_LAT, latitude)
                putDouble(ARG_LNG, longitude)
                putBoolean(ARG_VIEW_MODE, viewMode)
                putBoolean(ARG_OFFLINE_MODE, isOfflineMode)
                putBoolean(ARG_IS_EDIT_MODE, isEditMode) // 傳入編輯模式旗標
                putString(ARG_DATA_SPI_NUM,     basicData["SPI_NUM"]     ?: basicData["gutterId"] ?: "")
                putString(ARG_DATA_NODE_TYP,    basicData["NODE_TYP"]    ?: basicData["gutterType"] ?: "")
                putString(ARG_DATA_MAT_TYP,     basicData["MAT_TYP"]     ?: basicData["matTyp"] ?: "")
                putString(ARG_DATA_NODE_X,      basicData["NODE_X"]      ?: basicData["coordX"] ?: "")
                putString(ARG_DATA_NODE_Y,      basicData["NODE_Y"]      ?: basicData["coordY"] ?: "")
                putString(ARG_DATA_NODE_LE,     basicData["NODE_LE"]     ?: basicData["coordZ"] ?: "")
                putString(ARG_DATA_XY_NUM,      basicData["XY_NUM"]      ?: basicData["xyNum"] ?: "")
                putString(ARG_DATA_NODE_DEP,    basicData["NODE_DEP"]    ?: basicData["depth"] ?: "")
                putString(ARG_DATA_NODE_WID,    basicData["NODE_WID"]    ?: basicData["topWidth"] ?: "")
                putString(ARG_DATA_IS_BROKEN,   basicData["IS_BROKEN"]   ?: basicData["isBroken"] ?: "")
                putString(ARG_DATA_IS_HANGING,  basicData["IS_HANGING"]  ?: basicData["isHanging"] ?: "")
                putString(ARG_DATA_IS_SILT,     basicData["IS_SILT"]     ?: basicData["isSilt"] ?: "")
                putString(ARG_DATA_IS_CANTOPEN, basicData["IS_CANTOPEN"] ?: basicData["isCantOpen"] ?: "")
                putString(ARG_DATA_NODE_NOTE,   basicData["NODE_NOTE"]   ?: basicData["remarks"] ?: "")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGutterBasicInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val isViewMode   = arguments?.getBoolean(ARG_VIEW_MODE)   ?: false
        val isEditMode   = arguments?.getBoolean(ARG_IS_EDIT_MODE) ?: false
        
        val dropdowns = listOf(
            binding.actvGutterType,
            binding.actvMatType,
            binding.actvIsBroken,
            binding.actvIsHanging,
            binding.actvIsSilt
        )
        // 針對所有下拉選單，強制不跳出鍵盤
        dropdowns.forEach { it.showSoftInputOnFocus = false }

        setupDropdowns()
        prefillData()
        setupReadOnlyCoordinates()
        setEditable(!isViewMode)
        setupCantOpen()
        setupRangeWatchers()
        setupDraftWatchers()
        setupCoordinatePickers()

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

    /** 設定所有下拉選單的 Adapter */
    private fun setupDropdowns() {
        fun adapter(options: List<String>) = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            options
        )
        binding.actvGutterType.setAdapter(adapter(GUTTER_TYPES))
        binding.actvMatType.setAdapter(adapter(MAT_TYPES))
        binding.actvIsBroken.setAdapter(adapter(BROKEN_OPTIONS))
        binding.actvIsHanging.setAdapter(adapter(HANGING_OPTIONS))
        binding.actvIsSilt.setAdapter(adapter(SILT_OPTIONS))
    }

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
        val nodeDep    = args.getString(ARG_DATA_NODE_DEP,    "")
        val nodeWid    = args.getString(ARG_DATA_NODE_WID,    "")
        val isBroken   = args.getString(ARG_DATA_IS_BROKEN,   "")
        val isHanging  = args.getString(ARG_DATA_IS_HANGING,  "")
        val isSilt     = args.getString(ARG_DATA_IS_SILT,     "")
        val isCantOpen = args.getString(ARG_DATA_IS_CANTOPEN, "")
        val nodeNote   = args.getString(ARG_DATA_NODE_NOTE,   "")

        val hasAnyData = listOf(
            spiNum, nodeTyp, matTyp, nodeX, nodeY, nodeLe,
            xyNum, nodeDep, nodeWid, isBroken, isHanging, isSilt, isCantOpen, nodeNote
        ).any { it.isNotEmpty() }

        if (hasAnyData) {
            binding.etGutterId.setText(spiNum)
            binding.actvGutterType.setText(nodeTypCodeToText(nodeTyp), false)
            binding.actvMatType.setText(matTypCodeToText(matTyp), false)
            binding.etCoordX.setText(nodeX)
            binding.etCoordY.setText(nodeY)
            binding.etCoordZ.setText(nodeLe)
            binding.etMeasureId.setText(xyNum)
            binding.etDepth.setText(nodeDep)
            binding.etTopWidth.setText(nodeWid)
            binding.actvIsBroken.setText(isBrokenCodeToText(isBroken), false)
            binding.actvIsHanging.setText(isHangingCodeToText(isHanging), false)
            binding.actvIsSilt.setText(isSiltCodeToText(isSilt), false)
            binding.cbCantOpen.isChecked = parseLooseBoolean(isCantOpen)
            binding.etRemarks.setText(nodeNote)
            if (nodeX.isEmpty() && nodeY.isEmpty()) prefillCoordinates()
        } else {
            prefillCoordinates()
        }
    }

    private fun setupCantOpen() {
        // 檢視模式：僅展示，不允許互動
        val isViewMode = arguments?.getBoolean(ARG_VIEW_MODE) ?: false
        binding.cbCantOpen.isEnabled = !isViewMode

        // 依照目前狀態套用一次
        applyCantOpenUi(binding.cbCantOpen.isChecked)

        // 之後才開始監聽，避免 prefill 時觸發清空
        binding.cbCantOpen.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                // 不可開蓋：下方欄位不必填，直接清空避免誤送舊值
                binding.etDepth.setText("")
                binding.etTopWidth.setText("")
                binding.actvMatType.setText("", false)
                binding.actvIsBroken.setText("", false)
                binding.actvIsHanging.setText("", false)
                binding.actvIsSilt.setText("", false)
                binding.tilDepth.error = null
                binding.tilTopWidth.error = null
            }
            applyCantOpenUi(checked)
            onDraftChanged?.invoke()
        }
    }

    private fun applyCantOpenUi(isCantOpen: Boolean) {
        // 若整個表單不可編輯（檢視模式），就不要額外干預 enable 狀態
        if (!isFormEditable) {
            setCantOpenFieldsEnabled(false)
            return
        }
        setCantOpenFieldsEnabled(!isCantOpen)
    }

    private fun setCantOpenFieldsEnabled(enabled: Boolean) {
        // 需要被 disable 的欄位：深度、頂寬、材質、受損、附掛、淤積
        listOf(binding.etDepth, binding.etTopWidth).forEach { et ->
            et.isEnabled = enabled
            et.isFocusable = enabled
            et.isFocusableInTouchMode = enabled
        }
        listOf(
            binding.actvMatType,
            binding.actvIsBroken,
            binding.actvIsHanging,
            binding.actvIsSilt
        ).forEach { actv ->
            actv.isEnabled = enabled
        }

        val alpha = if (enabled) 1f else 0.5f
        listOf(
            binding.tilDepth,
            binding.tilTopWidth,
            binding.tilMatType,
            binding.tilIsBroken,
            binding.tilIsHanging,
            binding.tilIsSilt
        ).forEach { it.alpha = alpha }
    }

    /**
     * 將 coordX / coordY 設為永久唯讀，字體顯示灰色。
     */
    private fun setupReadOnlyCoordinates() {
        val grayColor = ContextCompat.getColor(requireContext(), R.color.inputFieldHint)
        listOf(binding.etCoordX, binding.etCoordY).forEach { et ->
            et.isEnabled = true
            et.isFocusable = false
            et.isFocusableInTouchMode = false
            et.isCursorVisible = false
            et.isLongClickable = false
            et.keyListener = null
            et.setTextIsSelectable(false)
            et.isClickable = true
            et.setTextColor(grayColor)
        }
        binding.tilCoordX.isClickable = true
        binding.tilCoordY.isClickable = true
        binding.tilCoordX.alpha = 1f
        binding.tilCoordY.alpha = 1f
    }

    private fun setupCoordinatePickers() {
        val listener = View.OnClickListener {
            // 檢視模式不允許變更座標
            val isViewMode = arguments?.getBoolean(ARG_VIEW_MODE) ?: false
            if (isViewMode) return@OnClickListener
            onRequestLocationPick?.invoke()
        }
        binding.tilCoordX.setOnClickListener(listener)
        binding.tilCoordY.setOnClickListener(listener)
        binding.etCoordX.setOnClickListener(listener)
        binding.etCoordY.setOnClickListener(listener)
    }

    /** 將 GPS 座標預填至 X/Y 欄位 */
    private fun prefillCoordinates() {
        val lat           = arguments?.getDouble(ARG_LAT)                    ?: return
        val lng           = arguments?.getDouble(ARG_LNG)                    ?: return
        val isOfflineMode = arguments?.getBoolean(ARG_OFFLINE_MODE, false)  ?: false

        when {
            isOfflineMode -> {
                binding.etCoordX.setText("0.0")
                binding.etCoordY.setText("0.0")
            }
            lat != 0.0 || lng != 0.0 -> {
                binding.etCoordX.setText("%.6f".format(lng))
                binding.etCoordY.setText("%.6f".format(lat))
            }
        }
    }

    /**
     * 切換所有輸入欄位的可編輯狀態。
     */
    fun setEditable(enabled: Boolean) {
        isFormEditable = enabled
        val textFields = listOf(
            binding.etGutterId,
            binding.etMeasureId,
            binding.etDepth,
            binding.etTopWidth,
            binding.etRemarks
        )
        textFields.forEach { et ->
            et.isEnabled = enabled
            et.isFocusable = enabled
            et.isFocusableInTouchMode = enabled
        }

        val dropdowns = listOf(
            binding.actvGutterType,
            binding.actvMatType,
            binding.actvIsBroken,
            binding.actvIsHanging,
            binding.actvIsSilt
        )
        dropdowns.forEach { actv ->
            actv.isEnabled = enabled
            // 下拉選單不應該可以輸入文字，故設為不可聚焦
            actv.isFocusable = false
            actv.isFocusableInTouchMode = false
            
            if (enabled) {
                actv.setOnClickListener {
                    hideKeyboard()
                    actv.showDropDown()
                }
            } else {
                actv.setOnClickListener(null)
            }
        }

        val alpha = if (enabled) 1f else 0.5f
        listOf(
            binding.tilGutterId,
            binding.tilGutterType,
            binding.tilMatType,
            binding.tilMeasureId,
            binding.tilDepth,
            binding.tilTopWidth,
            binding.tilIsBroken,
            binding.tilIsHanging,
            binding.tilIsSilt,
            binding.tilRemarks
        ).forEach { it.alpha = alpha }

        binding.cbCantOpen.isEnabled = enabled

        // Z 座標（NODE_LE）由後端提供，可檢視但不可修改
        binding.etCoordZ.isEnabled = false
        binding.etCoordZ.isFocusable = false
        binding.etCoordZ.isFocusableInTouchMode = false
        binding.tilCoordZ.alpha = 1f

        // 重新套用「無法開蓋」狀態（例如從檢視進入編輯）
        applyCantOpenUi(binding.cbCantOpen.isChecked)
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
            binding.etDepth,
            binding.etTopWidth,
            binding.etRemarks
        ).forEach { it.clearFocus() }
    }

    /**
     * 驗證必填欄位，以及深度與頂寬的合理區間（防呆）。
     *
     * 規則來源：桃園側溝分析文件_防呆
     *   NODE_DEP：35 < 值 < 110（公分）
     *   NODE_WID：值 > 25（公分）
     *
     * @return 第一個未填或超出範圍的欄位提示字串；全部通過則回傳 null。
     */
    fun validateRequiredFields(): String? {
        val d = collectData()
        val isCantOpen = parseLooseBoolean(d["IS_CANTOPEN"])

        // 新增與編輯模式均不驗證側溝編號（欄位已隱藏）
        if (d["NODE_TYP"].isNullOrEmpty())    return "側溝形式"
        if (d["NODE_X"].isNullOrEmpty())      return "側溝X（E）座標"
        if (d["NODE_Y"].isNullOrEmpty())      return "側溝Y（N）座標"
        if (d["XY_NUM"].isNullOrEmpty())      return "測量座標編號"

        // 不可開蓋：下方欄位可不填，直接通過
        if (isCantOpen) return null

        if (d["MAT_TYP"].isNullOrEmpty())     return "側溝材質"

        // ── NODE_DEP 深度必填 + 區間防呆 ─────────────────────────
        if (d["NODE_DEP"].isNullOrEmpty()) return "側溝測量深度"
        val depth = d["NODE_DEP"]!!.toDoubleOrNull()
        if (depth == null || depth <= 35.0 || depth >= 110.0) {
            binding.tilDepth.error = "合理區間：35～110 公分"
            return "側溝測量深度（合理區間：35～110 公分）"
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
                    v <= 35.0 || v >= 110.0 -> "合理區間：35～110 公分"
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
                onDraftChanged?.invoke()
            }
        }

        listOf(
            binding.etGutterId,
            binding.etCoordZ,
            binding.etMeasureId,
            binding.etDepth,
            binding.etTopWidth,
            binding.etRemarks,
            binding.actvGutterType,
            binding.actvMatType,
            binding.actvIsBroken,
            binding.actvIsHanging,
            binding.actvIsSilt
        ).forEach { it.addTextChangedListener(watcher) }
    }

    /** 收集表單資料（供 GutterFormActivity 提交用） */
    fun collectData(): Map<String, String> = mapOf(
        "SPI_NUM"     to (binding.etGutterId.text?.toString()      ?: ""),
        "NODE_TYP"    to gutterTypeTextToCode(binding.actvGutterType.text?.toString()),
        "MAT_TYP"     to matTypeTextToCode(binding.actvMatType.text?.toString()),
        "NODE_X"      to (binding.etCoordX.text?.toString()        ?: ""),
        "NODE_Y"      to (binding.etCoordY.text?.toString()        ?: ""),
        "NODE_LE"     to (binding.etCoordZ.text?.toString()        ?: ""),
        "XY_NUM"      to (binding.etMeasureId.text?.toString()     ?: ""),
        "NODE_DEP"    to (binding.etDepth.text?.toString()         ?: ""),
        "NODE_WID"    to (binding.etTopWidth.text?.toString()      ?: ""),
        "IS_BROKEN"   to brokenTextToCode(binding.actvIsBroken.text?.toString()),
        "IS_HANGING"  to hangingTextToCode(binding.actvIsHanging.text?.toString()),
        "IS_SILT"     to siltTextToCode(binding.actvIsSilt.text?.toString()),
        // 以 "1"/"" 形式存入 basicData（送出 API 時再轉為 JSON boolean）
        "IS_CANTOPEN" to (if (binding.cbCantOpen.isChecked) "1" else ""),
        "NODE_NOTE"   to (binding.etRemarks.text?.toString()       ?: "")
    )

    fun updateCoordinates(longitude: Double, latitude: Double) {
        binding.etCoordX.setText("%.6f".format(longitude))
        binding.etCoordY.setText("%.6f".format(latitude))
        onDraftChanged?.invoke()
    }

    fun prefillDataFromImport(nodeDetails: com.example.taoyuangutter.api.NodeDetails) {
        binding.apply {
            // 基本資訊
            etMeasureId.setText(nodeDetails.xyNum ?: "")

            // 溝型（API key 為 NODE_TYP，值為字串）
            val nodeTypText = when (nodeDetails.nodeTyP?.toIntOrNull()) {
                1 -> GUTTER_TYPES[0]  // U型溝（明溝）
                2 -> GUTTER_TYPES[1]  // U型溝（加蓋）
                else -> ""
            }
            actvGutterType.setText(nodeTypText)

            // 材質（API key 為 MAT_TYP，值為字串）
            val matTypText = when (nodeDetails.matTyp?.toIntOrNull()) {
                1 -> MAT_TYPES[0]  // 混凝土
                2 -> MAT_TYPES[1]  // 卵礫石
                else -> ""
            }
            actvMatType.setText(matTypText)

            // 深度、寬度（API 回傳 Double，使用 AsString 方法自動轉換）
            etDepth.setText(nodeDetails.nodeDepAsString)
            etTopWidth.setText(nodeDetails.nodeWidAsString)

            // 破損狀態（API key 為 IS_BROKEN，值為字串）
            val brokenText = when (nodeDetails.isBroken?.toIntOrNull()) {
                0 -> BROKEN_OPTIONS[0]  // 無破損
                1 -> BROKEN_OPTIONS[1]  // 有破損
                else -> ""
            }
            actvIsBroken.setText(brokenText)

            // 懸掛狀態（API key 為 IS_HANGING，值為字串）
            val hangingText = when (nodeDetails.isHanging?.toIntOrNull()) {
                0 -> HANGING_OPTIONS[0]  // 無懸掛
                1 -> HANGING_OPTIONS[1]  // 有懸掛
                else -> ""
            }
            actvIsHanging.setText(hangingText)

            // 淤積狀態（API key 為 IS_SILT，值為字串）
            val siltText = when (nodeDetails.isSilt?.toIntOrNull()) {
                0 -> SILT_OPTIONS[0]  // 無
                1 -> SILT_OPTIONS[1]  // 輕度
                2 -> SILT_OPTIONS[2]  // 中度
                3 -> SILT_OPTIONS[3]  // 嚴重
                else -> ""
            }
            actvIsSilt.setText(siltText)

            // 無法開蓋狀態（使用 isCantOpenAsBoolean 方法處理型別轉換）
            cbCantOpen.isChecked = nodeDetails.isCantOpenAsBoolean

            // 備註（API key 為 NOTE）
            etRemarks.setText(nodeDetails.note ?: "")

            // 坐標資訊（WGS84 經緯度，由 API 回傳為字串）
            if (!nodeDetails.latitude.isNullOrEmpty() && !nodeDetails.longitude.isNullOrEmpty()) {
                etCoordX.setText(nodeDetails.longitude)
                etCoordY.setText(nodeDetails.latitude)
            }

            // Z 坐標（標高，API key 為 NODE_LE，回傳字串）
            if (!nodeDetails.nodeLe.isNullOrEmpty()) {
                etCoordZ.setText(nodeDetails.nodeLe)
            }
        }
        onDraftChanged?.invoke()
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
        "3" -> SILT_OPTIONS[3]
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
        SILT_OPTIONS[3] -> "3"
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
}
