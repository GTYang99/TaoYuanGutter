package com.example.taoyuangutter.gutter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.taoyuangutter.api.DitchDetails
import com.example.taoyuangutter.databinding.FragmentInspectBasicBinding

/**
 * GutterInspectBasicFragment
 *
 * 顯示整條側溝的基本資料，全程唯讀。
 * 資料來源：由 [GutterInspectActivity] 將 [DitchDetails] 以 arguments 傳入。
 *
 * 欄位對應文件 桃園側溝分析文件_側溝檢視欄位：
 *   XY_NUM   → 側溝座標編號（起點/節點/終點）
 *   SPI_TYP  → 側溝形式（代碼轉中文）
 *   STR_X/Y  → 起點 X(E) / Y(N) 座標
 *   STR_LE   → 起點高程
 *   END_X/Y  → 終點 X(E) / Y(N) 座標
 *   END_LE   → 終點高程
 *   NODE_XY  → 所有節點座標
 *   STR_DEP  → 起點深度（公分）
 *   END_DEP  → 終點深度（公分）
 *   STR_WID  → 起點頂寬（公分）
 *   END_WID  → 終點頂寬（公分）
 *   LENG     → 側溝長度（公尺）
 *   SLOP     → 坡度
 *   NOTE     → 補充說明
 */
class GutterInspectBasicFragment : Fragment() {

    private var _binding: FragmentInspectBasicBinding? = null
    private val binding get() = _binding!!

    companion object {
        // ── argument keys ──────────────────────────────────────────────
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

        /** SPI_TYP 代碼 → 中文對照 */
        private val SPI_TYP_MAP = mapOf(
            "1" to "U形溝（明溝）",
            "2" to "U形溝（加蓋）",
            "3" to "L形溝與暗溝渠併用",
            "4" to "其他"
        )

        private fun formatXyNum(ditch: DitchDetails?): String {
            val xy = ditch?.xyNum
            val lines = mutableListOf<String>()
            val start = xy?.start?.trim().orEmpty()
            if (start.isNotEmpty()) lines.add("起點: $start")
            val nodes = xy?.nodes.orEmpty().map { it.trim() }.filter { it.isNotEmpty() }
            if (nodes.isNotEmpty()) lines.add("節點: ${nodes.joinToString("、")}")
            val end = xy?.end?.trim().orEmpty()
            if (end.isNotEmpty()) lines.add("終點: $end")
            return lines.joinToString("\n")
        }

        /**
         * 從 [DitchDetails] 取出所有欄位，建立 Fragment 實例。
         */
        fun newInstance(ditch: DitchDetails?): GutterInspectBasicFragment {
            return GutterInspectBasicFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_XY_NUM, formatXyNum(ditch))
                    // SPI_TYP：先查對照表，查不到則原樣顯示
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
                }
            }
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────────

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

    // ── 資料繫結 ─────────────────────────────────────────────────────────

    private fun bindFields() {
        val a = arguments ?: return

        fun get(key: String) = a.getString(key, "").takeIf { it.isNotEmpty() } ?: "—"

        binding.tvSpiNum.text = get(ARG_XY_NUM)
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
