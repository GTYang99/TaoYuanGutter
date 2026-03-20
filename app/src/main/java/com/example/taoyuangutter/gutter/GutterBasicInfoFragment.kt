package com.example.taoyuangutter.gutter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.taoyuangutter.R
import com.example.taoyuangutter.databinding.FragmentGutterBasicInfoBinding

class GutterBasicInfoFragment : Fragment() {

    private var _binding: FragmentGutterBasicInfoBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_LAT       = "latitude"
        private const val ARG_LNG       = "longitude"
        private const val ARG_VIEW_MODE = "view_mode"

        // basicData 個別 key（避免傳 HashMap serializable 相容問題）
        private const val ARG_DATA_GUTTER_ID   = "d_gutterId"
        private const val ARG_DATA_GUTTER_TYPE = "d_gutterType"
        private const val ARG_DATA_COORD_X     = "d_coordX"
        private const val ARG_DATA_COORD_Y     = "d_coordY"
        private const val ARG_DATA_COORD_Z     = "d_coordZ"
        private const val ARG_DATA_MEASURE_ID  = "d_measureId"
        private const val ARG_DATA_DEPTH       = "d_depth"
        private const val ARG_DATA_TOP_WIDTH   = "d_topWidth"
        private const val ARG_DATA_REMARKS     = "d_remarks"

        /** 側溝形式選項 */
        val GUTTER_TYPES = listOf(
            "U形溝（明溝）",
            "U形溝（加蓋）",
            "L形溝與暗溝渠併用",
            "其他"
        )

        fun newInstance(
            latitude: Double,
            longitude: Double,
            viewMode: Boolean = false,
            basicData: HashMap<String, String> = hashMapOf()
        ) = GutterBasicInfoFragment().apply {
            arguments = Bundle().apply {
                putDouble(ARG_LAT, latitude)
                putDouble(ARG_LNG, longitude)
                putBoolean(ARG_VIEW_MODE, viewMode)
                putString(ARG_DATA_GUTTER_ID,   basicData["gutterId"]   ?: "")
                putString(ARG_DATA_GUTTER_TYPE, basicData["gutterType"] ?: "")
                putString(ARG_DATA_COORD_X,     basicData["coordX"]     ?: "")
                putString(ARG_DATA_COORD_Y,     basicData["coordY"]     ?: "")
                putString(ARG_DATA_COORD_Z,     basicData["coordZ"]     ?: "")
                putString(ARG_DATA_MEASURE_ID,  basicData["measureId"]  ?: "")
                putString(ARG_DATA_DEPTH,       basicData["depth"]      ?: "")
                putString(ARG_DATA_TOP_WIDTH,   basicData["topWidth"]   ?: "")
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val isViewMode = arguments?.getBoolean(ARG_VIEW_MODE) ?: false
        setupDropdown()
        prefillData()
        setupReadOnlyCoordinates()
        setEditable(!isViewMode)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /** 側溝形式下拉選單 */
    private fun setupDropdown() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            GUTTER_TYPES
        )
        binding.actvGutterType.setAdapter(adapter)
    }

    /** 從 args 預填既有資料（有則填，無則走座標預填） */
    private fun prefillData() {
        val args = arguments ?: return

        val gutterId   = args.getString(ARG_DATA_GUTTER_ID,   "")
        val gutterType = args.getString(ARG_DATA_GUTTER_TYPE, "")
        val coordX     = args.getString(ARG_DATA_COORD_X,     "")
        val coordY     = args.getString(ARG_DATA_COORD_Y,     "")
        val coordZ     = args.getString(ARG_DATA_COORD_Z,     "")
        val measureId  = args.getString(ARG_DATA_MEASURE_ID,  "")
        val depth      = args.getString(ARG_DATA_DEPTH,       "")
        val topWidth   = args.getString(ARG_DATA_TOP_WIDTH,   "")
        val remarks    = args.getString(ARG_DATA_REMARKS,     "")

        // 只要任一欄位有值就代表是既有資料，不能單靠 gutterId 判斷
        val hasAnyData = listOf(gutterId, gutterType, coordX, coordY, coordZ,
                                measureId, depth, topWidth, remarks).any { it.isNotEmpty() }

        if (hasAnyData) {
            binding.etGutterId.setText(gutterId)
            binding.actvGutterType.setText(gutterType, false)
            binding.etCoordX.setText(coordX)
            binding.etCoordY.setText(coordY)
            binding.etCoordZ.setText(coordZ)
            binding.etMeasureId.setText(measureId)
            binding.etDepth.setText(depth)
            binding.etTopWidth.setText(topWidth)
            binding.etRemarks.setText(remarks)
            // 若座標欄位仍空（例如僅填文字欄位），補上 GPS 預填
            if (coordX.isEmpty() && coordY.isEmpty()) prefillCoordinates()
        } else {
            // 全空 → 用 GPS 座標預填 X/Y
            prefillCoordinates()
        }
    }

    /**
     * 將 coordX / coordY 設為永久唯讀，字體顯示灰色。
     * 無論 viewMode 為何，這兩個欄位都只能由地圖 pin 帶入，不能手動輸入。
     */
    private fun setupReadOnlyCoordinates() {
        val grayColor = ContextCompat.getColor(requireContext(), R.color.inputFieldHint)
        listOf(binding.etCoordX, binding.etCoordY).forEach { et ->
            et.isEnabled = false
            et.isFocusable = false
            et.isFocusableInTouchMode = false
            et.setTextColor(grayColor)
        }
        // TextInputLayout 維持正常不透明（只有文字變灰，邊框不需要暗化）
        binding.tilCoordX.alpha = 1f
        binding.tilCoordY.alpha = 1f
    }

    /** 將 GPS 座標預填至 X/Y 欄位 */
    private fun prefillCoordinates() {
        val lat = arguments?.getDouble(ARG_LAT) ?: return
        val lng = arguments?.getDouble(ARG_LNG) ?: return
        if (lat != 0.0 || lng != 0.0) {
            // TWD97 轉換為 TODO；目前先直接填 WGS84 經緯度
            binding.etCoordX.setText("%.6f".format(lng))  // E = 經度
            binding.etCoordY.setText("%.6f".format(lat))  // N = 緯度
        }
    }

    /**
     * 切換所有輸入欄位的可編輯狀態。
     * [enabled] = true → 編輯模式；false → 檢視模式（唯讀）
     */
    fun setEditable(enabled: Boolean) {
        val fields = listOf(
            binding.etGutterId,
            // etCoordX / etCoordY 永久唯讀，不參與 setEditable 切換
            binding.etCoordZ,
            binding.etMeasureId,
            binding.etDepth,
            binding.etTopWidth,
            binding.etRemarks
        )
        fields.forEach { et ->
            et.isEnabled = enabled
            et.isFocusable = enabled
            et.isFocusableInTouchMode = enabled
        }

        // 下拉選單
        binding.actvGutterType.isEnabled = enabled
        binding.actvGutterType.isFocusable = enabled
        binding.actvGutterType.isFocusableInTouchMode = enabled
        if (enabled) {
            binding.actvGutterType.setOnClickListener { binding.actvGutterType.showDropDown() }
        } else {
            binding.actvGutterType.setOnClickListener(null)
        }

        // TextInputLayout stroke 顏色提示（唯讀時變灰）
        // tilCoordX / tilCoordY 不在此列，由 setupReadOnlyCoordinates() 固定處理
        val alpha = if (enabled) 1f else 0.5f
        listOf(
            binding.tilGutterId,
            binding.tilGutterType,
            binding.tilCoordZ,
            binding.tilMeasureId,
            binding.tilDepth,
            binding.tilTopWidth,
            binding.tilRemarks
        ).forEach { it.alpha = alpha }
    }

    /**
     * 驗證必填欄位（備註為選填，不檢查）。
     * @return 第一個未填欄位的顯示名稱；全部填妥則回傳 null。
     */
    fun validateRequiredFields(): String? {
        val d = collectData()
        if (d["gutterId"].isNullOrEmpty())   return "側溝編號"
        if (d["gutterType"].isNullOrEmpty())  return "側溝形式"
        if (d["coordX"].isNullOrEmpty())      return "側溝X（E）座標"
        if (d["coordY"].isNullOrEmpty())      return "側溝Y（N）座標"
        if (d["coordZ"].isNullOrEmpty())      return "側溝Z座標"
        if (d["measureId"].isNullOrEmpty())   return "測量座標編號"
        if (d["depth"].isNullOrEmpty())       return "側溝測量深度"
        if (d["topWidth"].isNullOrEmpty())    return "側溝頂寬度"
        return null
    }

    /** 收集表單資料（供 GutterFormActivity 提交用） */
    fun collectData(): Map<String, String> = mapOf(
        "gutterId"   to (binding.etGutterId.text?.toString() ?: ""),
        "gutterType" to (binding.actvGutterType.text?.toString() ?: ""),
        "coordX"     to (binding.etCoordX.text?.toString() ?: ""),
        "coordY"     to (binding.etCoordY.text?.toString() ?: ""),
        "coordZ"     to (binding.etCoordZ.text?.toString() ?: ""),
        "measureId"  to (binding.etMeasureId.text?.toString() ?: ""),
        "depth"      to (binding.etDepth.text?.toString() ?: ""),
        "topWidth"   to (binding.etTopWidth.text?.toString() ?: ""),
        "remarks"    to (binding.etRemarks.text?.toString() ?: "")
    )
}
