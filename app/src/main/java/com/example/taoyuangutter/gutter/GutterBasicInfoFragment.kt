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
        private const val ARG_LAT = "latitude"
        private const val ARG_LNG = "longitude"

        /** 側溝形式選項 */
        val GUTTER_TYPES = listOf(
            "U形溝（明溝）",
            "U形溝（加蓋）",
            "L形溝與暗溝渠併用",
            "其他"
        )

        fun newInstance(latitude: Double, longitude: Double) = GutterBasicInfoFragment().apply {
            arguments = Bundle().apply {
                putDouble(ARG_LAT, latitude)
                putDouble(ARG_LNG, longitude)
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
        setupDropdown()
        prefillCoordinates()
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
        // 禁止鍵盤彈出，只允許下拉
        binding.actvGutterType.isFocusable = false
        binding.actvGutterType.isFocusableInTouchMode = false
        binding.actvGutterType.setOnClickListener {
            binding.actvGutterType.showDropDown()
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
