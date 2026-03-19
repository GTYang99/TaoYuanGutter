package com.example.taoyuangutter.gutter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
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
        val savedGutterId = args.getString(ARG_DATA_GUTTER_ID, "")
        if (savedGutterId.isNotEmpty()) {
            // 有既有資料 → 全部從 args 填入
            binding.etGutterId.setText(savedGutterId)
            binding.actvGutterType.setText(args.getString(ARG_DATA_GUTTER_TYPE, ""), false)
            binding.etCoordX.setText(args.getString(ARG_DATA_COORD_X, ""))
            binding.etCoordY.setText(args.getString(ARG_DATA_COORD_Y, ""))
            binding.etCoordZ.setText(args.getString(ARG_DATA_COORD_Z, ""))
            binding.etMeasureId.setText(args.getString(ARG_DATA_MEASURE_ID, ""))
            binding.etDepth.setText(args.getString(ARG_DATA_DEPTH, ""))
            binding.etTopWidth.setText(args.getString(ARG_DATA_TOP_WIDTH, ""))
            binding.etRemarks.setText(args.getString(ARG_DATA_REMARKS, ""))
        } else {
            // 沒有既有資料 → 用 GPS 座標預填 X/Y
            prefillCoordinates()
        }
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
            binding.etCoordX,
            binding.etCoordY,
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
        val alpha = if (enabled) 1f else 0.5f
        listOf(
            binding.tilGutterId,
            binding.tilGutterType,
            binding.tilCoordX,
            binding.tilCoordY,
            binding.tilCoordZ,
            binding.tilMeasureId,
            binding.tilDepth,
            binding.tilTopWidth,
            binding.tilRemarks
        ).forEach { it.alpha = alpha }
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
