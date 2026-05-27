package com.example.taoyuangutter.gutter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.taoyuangutter.api.DitchDetails
import com.example.taoyuangutter.api.DitchXyNum
import com.example.taoyuangutter.api.NodeDetails
import com.example.taoyuangutter.databinding.FragmentInspectBasicBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * GutterInspectBasicFragment
 *
 * 顯示整條側溝的基本資料，全程唯讀。
 */
class GutterInspectBasicFragment : Fragment() {

    private var _binding: FragmentInspectBasicBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_XY_NUM   = "xy_num"
        private const val ARG_SPI_TYP  = "spi_typ"
        private const val ARG_STR_X    = "str_x"
        private const val ARG_STR_Y    = "str_y"
        private const val ARG_STR_LE   = "str_le"
        private const val ARG_END_X    = "end_x"
        private const val ARG_END_Y    = "end_y"
        private const val ARG_END_LE   = "end_le"
        private const val ARG_NODE_XY  = "node_xy"
        private const val ARG_STR_DEP  = "str_dep"
        private const val ARG_END_DEP  = "end_dep"
        private const val ARG_STR_WID  = "str_wid"
        private const val ARG_END_WID  = "end_wid"
        private const val ARG_LENG     = "leng"
        private const val ARG_SLOP     = "slop"
        private const val ARG_NOTE     = "note"
        private const val ARG_NODE_DETAILS_JSON = "node_details_json"

        private val VIRTUAL_COLOR = android.graphics.Color.parseColor("#B7B7C2")

        private val SPI_TYP_MAP = mapOf(
            "1" to "U形溝（明溝）",
            "2" to "U形溝（加蓋）",
            "3" to "L形溝與暗溝渠併用",
            "4" to "其他"
        )

        /**
         * 從 [DitchDetails] 取出所有欄位，建立 Fragment 實例。
         */
        fun newInstance(ditch: DitchDetails?, nodeDetailsJson: String = "[]"): GutterInspectBasicFragment {
            return GutterInspectBasicFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_XY_NUM,  Gson().toJson(ditch?.xyNum))
                    putString(ARG_SPI_TYP, SPI_TYP_MAP[ditch?.spiTyp] ?: (ditch?.spiTyp ?: ""))
                    putString(ARG_STR_X,   ditch?.strX  ?: "")
                    putString(ARG_STR_Y,   ditch?.strY  ?: "")
                    putString(ARG_STR_LE,  ditch?.strLe ?: "")
                    putString(ARG_END_X,   ditch?.endX  ?: "")
                    putString(ARG_END_Y,   ditch?.endY  ?: "")
                    putString(ARG_END_LE,  ditch?.endLe ?: "")
                    putString(ARG_NODE_XY, ditch?.nodeXy ?: "")
                    putString(ARG_STR_DEP, ditch?.strDep?.toString() ?: "")
                    putString(ARG_END_DEP, ditch?.endDep?.toString() ?: "")
                    putString(ARG_STR_WID, ditch?.strWid?.toString() ?: "")
                    putString(ARG_END_WID, ditch?.endWid?.toString() ?: "")
                    putString(ARG_LENG,    ditch?.leng  ?: "")
                    putString(ARG_SLOP,    ditch?.slop  ?: "")
                    putString(ARG_NOTE,    ditch?.note  ?: "")
                    putString(ARG_NODE_DETAILS_JSON, nodeDetailsJson)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInspectBasicBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindFields()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun bindFields() {
        val a = arguments ?: return
        val gson = Gson()

        val nodeDetailsJson = a.getString(ARG_NODE_DETAILS_JSON, "[]")
        val nodeDetailsList = runCatching {
            val type = object : TypeToken<List<NodeDetails>>() {}.type
            gson.fromJson<List<NodeDetails>>(nodeDetailsJson, type)
        }.getOrNull() ?: emptyList()

        fun isVirtual(xyNum: String?): Boolean {
            if (xyNum.isNullOrEmpty()) return false
            return nodeDetailsList.any { it.xyNum == xyNum && (it.isVirtual == "1" || it.isVirtual?.lowercase() == "true") }
        }

        val xyNumJson = a.getString(ARG_XY_NUM, "")
        val xyNumObj = runCatching { gson.fromJson(xyNumJson, DitchXyNum::class.java) }.getOrNull()
        
        val ssb = android.text.SpannableStringBuilder()
        fun appendColored(label: String, value: String?) {
            if (value.isNullOrEmpty()) return
            if (ssb.isNotEmpty()) ssb.append("\n")
            val start = ssb.length
            ssb.append("$label: $value")
            if (isVirtual(value)) {
                ssb.setSpan(
                    android.text.style.ForegroundColorSpan(VIRTUAL_COLOR),
                    start + label.length + 2,
                    ssb.length,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        xyNumObj?.let {
            appendColored("起點", it.start)
            it.nodes?.forEach { node -> appendColored("節點", node) }
            appendColored("終點", it.end)
        }

        fun get(key: String) = a.getString(key, "").takeIf { it.isNotEmpty() } ?: "—"

        binding.tvSpiNum.text = if (ssb.isEmpty()) "—" else ssb
        binding.tvSpiTyp.text = get(ARG_SPI_TYP)
        binding.tvStrX.text   = get(ARG_STR_X)
        binding.tvStrY.text   = get(ARG_STR_Y)
        binding.tvStrLe.text  = get(ARG_STR_LE)
        binding.tvEndX.text   = get(ARG_END_X)
        binding.tvEndY.text   = get(ARG_END_Y)
        binding.tvEndLe.text  = get(ARG_END_LE)
        binding.tvNodeXy.text = get(ARG_NODE_XY)
        binding.tvStrDep.text = get(ARG_STR_DEP)
        binding.tvEndDep.text = get(ARG_END_DEP)
        binding.tvStrWid.text = get(ARG_STR_WID)
        binding.tvEndWid.text = get(ARG_END_WID)
        binding.tvLeng.text   = get(ARG_LENG)
        binding.tvSlop.text   = get(ARG_SLOP)
        binding.tvNote.text   = a.getString(ARG_NOTE, "").takeIf { it.isNotEmpty() } ?: "—"
    }
}
