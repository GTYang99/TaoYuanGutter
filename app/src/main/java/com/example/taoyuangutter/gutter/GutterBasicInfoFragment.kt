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

    companion object {
        private const val ARG_LAT          = "latitude"
        private const val ARG_LNG          = "longitude"
        private const val ARG_VIEW_MODE     = "view_mode"
        private const val ARG_OFFLINE_MODE  = "offline_mode"
        private const val ARG_IS_EDIT_MODE  = "is_edit_mode" // 新增：是否為編輯模式

        // basicData 個別 key
        private const val ARG_DATA_GUTTER_ID   = "d_gutterId"
        private const val ARG_DATA_GUTTER_TYPE = "d_gutterType"
        private const val ARG_DATA_MAT_TYP     = "d_matTyp"
        private const val ARG_DATA_COORD_X     = "d_coordX"
        private const val ARG_DATA_COORD_Y     = "d_coordY"
        private const val ARG_DATA_COORD_Z     = "d_coordZ"
        private const val ARG_DATA_MEASURE_ID  = "d_measureId"
        private const val ARG_DATA_DEPTH       = "d_depth"
        private const val ARG_DATA_TOP_WIDTH   = "d_topWidth"
        private const val ARG_DATA_IS_BROKEN   = "d_isBroken"
        private const val ARG_DATA_IS_HANGING  = "d_isHanging"
        private const val ARG_DATA_IS_SILT     = "d_isSilt"
        private const val ARG_DATA_REMARKS     = "d_remarks"

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
                putString(ARG_DATA_GUTTER_ID,   basicData["gutterId"]   ?: "")
                putString(ARG_DATA_GUTTER_TYPE, basicData["gutterType"] ?: "")
                putString(ARG_DATA_MAT_TYP,     basicData["matTyp"]     ?: "")
                putString(ARG_DATA_COORD_X,     basicData["coordX"]     ?: "")
                putString(ARG_DATA_COORD_Y,     basicData["coordY"]     ?: "")
                putString(ARG_DATA_COORD_Z,     basicData["coordZ"]     ?: "")
                putString(ARG_DATA_MEASURE_ID,  basicData["xyNum"]      ?: "")
                putString(ARG_DATA_DEPTH,       basicData["depth"]      ?: "")
                putString(ARG_DATA_TOP_WIDTH,   basicData["topWidth"]   ?: "")
                putString(ARG_DATA_IS_BROKEN,   basicData["isBroken"]   ?: "")
                putString(ARG_DATA_IS_HANGING,  basicData["isHanging"]  ?: "")
                putString(ARG_DATA_IS_SILT,     basicData["isSilt"]     ?: "")
                putString(ARG_DATA_REMARKS,     basicData["remarks"]    ?: "")
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
        setupRangeWatchers()

        // 新增與編輯模式下均隱藏側溝編號欄位（僅檢視模式顯示）
        if (!isViewMode) {
            binding.tilGutterTitle.visibility = View.GONE
            binding.tilGutterId.visibility = View.GONE
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

        val gutterId   = args.getString(ARG_DATA_GUTTER_ID,   "")
        val gutterType = args.getString(ARG_DATA_GUTTER_TYPE, "")
        val matTyp     = args.getString(ARG_DATA_MAT_TYP,     "")
        val coordX     = args.getString(ARG_DATA_COORD_X,     "")
        val coordY     = args.getString(ARG_DATA_COORD_Y,     "")
        val coordZ     = args.getString(ARG_DATA_COORD_Z,     "")
        val xyNum      = args.getString(ARG_DATA_MEASURE_ID,  "")
        val depth      = args.getString(ARG_DATA_DEPTH,       "")
        val topWidth   = args.getString(ARG_DATA_TOP_WIDTH,   "")
        val isBroken   = args.getString(ARG_DATA_IS_BROKEN,   "")
        val isHanging  = args.getString(ARG_DATA_IS_HANGING,  "")
        val isSilt     = args.getString(ARG_DATA_IS_SILT,     "")
        val remarks    = args.getString(ARG_DATA_REMARKS,     "")

        val hasAnyData = listOf(
            gutterId, gutterType, matTyp, coordX, coordY, coordZ,
            xyNum, depth, topWidth, isBroken, isHanging, isSilt, remarks
        ).any { it.isNotEmpty() }

        if (hasAnyData) {
            binding.etGutterId.setText(gutterId)
            binding.actvGutterType.setText(gutterType, false)
            binding.actvMatType.setText(matTyp, false)
            binding.etCoordX.setText(coordX)
            binding.etCoordY.setText(coordY)
            binding.etCoordZ.setText(coordZ)
            binding.etMeasureId.setText(xyNum)
            binding.etDepth.setText(depth)
            binding.etTopWidth.setText(topWidth)
            binding.actvIsBroken.setText(isBroken, false)
            binding.actvIsHanging.setText(isHanging, false)
            binding.actvIsSilt.setText(isSilt, false)
            binding.etRemarks.setText(remarks)
            if (coordX.isEmpty() && coordY.isEmpty()) prefillCoordinates()
        } else {
            prefillCoordinates()
        }
    }

    /**
     * 將 coordX / coordY 設為永久唯讀，字體顯示灰色。
     */
    private fun setupReadOnlyCoordinates() {
        val grayColor = ContextCompat.getColor(requireContext(), R.color.inputFieldHint)
        listOf(binding.etCoordX, binding.etCoordY).forEach { et ->
            et.isEnabled = false
            et.isFocusable = false
            et.isFocusableInTouchMode = false
            et.setTextColor(grayColor)
        }
        binding.tilCoordX.alpha = 1f
        binding.tilCoordY.alpha = 1f
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
        val textFields = listOf(
            binding.etGutterId,
            binding.etCoordZ,
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
            binding.tilCoordZ,
            binding.tilMeasureId,
            binding.tilDepth,
            binding.tilTopWidth,
            binding.tilIsBroken,
            binding.tilIsHanging,
            binding.tilIsSilt,
            binding.tilRemarks
        ).forEach { it.alpha = alpha }
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
        val args       = arguments ?: return null
        val isEditMode = args.getBoolean(ARG_IS_EDIT_MODE)
        val d = collectData()

        // 新增與編輯模式均不驗證側溝編號（欄位已隱藏）
        if (isEditMode && d["gutterId"].isNullOrEmpty())    return "側溝編號"
        if (d["gutterType"].isNullOrEmpty())  return "側溝形式"
        if (d["matTyp"].isNullOrEmpty())      return "側溝材質"
        if (d["coordX"].isNullOrEmpty())      return "側溝X（E）座標"
        if (d["coordY"].isNullOrEmpty())      return "側溝Y（N）座標"
        if (d["coordZ"].isNullOrEmpty())      return "側溝Z座標"
        if (d["xyNum"].isNullOrEmpty())        return "測量座標編號"

        // ── NODE_DEP 深度必填 + 區間防呆 ─────────────────────────
        if (d["depth"].isNullOrEmpty()) return "側溝測量深度"
        val depth = d["depth"]!!.toDoubleOrNull()
        if (depth == null || depth <= 35.0 || depth >= 110.0) {
            binding.tilDepth.error = "合理區間：35～110 公分"
            return "側溝測量深度（合理區間：35～110 公分）"
        }

        // ── NODE_WID 頂寬必填 + 區間防呆 ─────────────────────────
        if (d["topWidth"].isNullOrEmpty()) return "側溝頂寬度"
        val topWidth = d["topWidth"]!!.toDoubleOrNull()
        if (topWidth == null || topWidth <= 25.0) {
            binding.tilTopWidth.error = "需大於 25 公分"
            return "側溝頂寬度（需大於 25 公分）"
        }

        if (d["isBroken"].isNullOrEmpty())    return "溝體結構受損"
        if (d["isHanging"].isNullOrEmpty())   return "附掛或過路管線"
        if (d["isSilt"].isNullOrEmpty())      return "淤積程度"
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

    /** 收集表單資料（供 GutterFormActivity 提交用） */
    fun collectData(): Map<String, String> = mapOf(
        "gutterId"   to (binding.etGutterId.text?.toString()        ?: ""),
        "gutterType" to (binding.actvGutterType.text?.toString()    ?: ""),
        "matTyp"     to (binding.actvMatType.text?.toString()       ?: ""),
        "coordX"     to (binding.etCoordX.text?.toString()          ?: ""),
        "coordY"     to (binding.etCoordY.text?.toString()          ?: ""),
        "coordZ"     to (binding.etCoordZ.text?.toString()          ?: ""),
        "xyNum"      to (binding.etMeasureId.text?.toString()        ?: ""),
        "depth"      to (binding.etDepth.text?.toString()           ?: ""),
        "topWidth"   to (binding.etTopWidth.text?.toString()        ?: ""),
        "isBroken"   to (binding.actvIsBroken.text?.toString()      ?: ""),
        "isHanging"  to (binding.actvIsHanging.text?.toString()     ?: ""),
        "isSilt"     to (binding.actvIsSilt.text?.toString()        ?: ""),
        "remarks"    to (binding.etRemarks.text?.toString()         ?: "")
    )
}
