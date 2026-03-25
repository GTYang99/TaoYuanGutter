package com.example.taoyuangutter.gutter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.taoyuangutter.databinding.FragmentInspectBasicBinding
import kotlin.math.sqrt

/**
 * GutterInspectBasicFragment
 *
 * 顯示整條側溝（多點位）的彙整基本資料，全程唯讀。
 * 資料來源：由 [GutterInspectActivity] 將 List<Waypoint> 轉換後以 arguments 傳入。
 *
 * 欄位對應文件 桃園側溝分析文件_檢視欄位：
 *   SPI_NUM  → 側溝編號（起點 gutterId）
 *   SPI_TYP  → 側溝形式（起點 gutterType，轉為中文）
 *   STR_X/Y  → 起點 X(E) / Y(N) 座標
 *   STR_LE   → 起點 Z 座標
 *   END_X/Y  → 終點 X(E) / Y(N) 座標
 *   END_LE   → 終點 Z 座標
 *   NODE_XY  → 所有節點座標，格式 "x,y_x,y_…"
 *   STR_DEP  → 起點深度
 *   END_DEP  → 終點深度
 *   STR_WID  → 起點頂寬
 *   END_WID  → 終點頂寬
 *   LENG     → 側溝長度（GPS 直線距離，m）
 *   SLOP     → 坡度（%）
 *   NOTE     → 補充說明（各點位 remarks 合併）
 */
class GutterInspectBasicFragment : Fragment() {

    private var _binding: FragmentInspectBasicBinding? = null
    private val binding get() = _binding!!

    companion object {
        // ── argument keys ──────────────────────────────────────────────
        private const val ARG_SPI_NUM  = "spi_num"
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

        /**
         * 從 List<Waypoint> 計算所有彙整欄位後，建立 Fragment 實例。
         */
        fun newInstance(waypoints: List<Waypoint>): GutterInspectBasicFragment {
            val start = waypoints.firstOrNull { it.type == WaypointType.START }
            val end   = waypoints.firstOrNull { it.type == WaypointType.END }
            val nodes = waypoints.filter   { it.type == WaypointType.NODE }

            // ── 彙整各欄位 ────────────────────────────────────────
            val spiNum  = start?.basicData?.get("gutterId")  ?: ""
            val spiTyp  = start?.basicData?.get("gutterType") ?: ""
            val strX    = start?.basicData?.get("coordX")    ?: ""
            val strY    = start?.basicData?.get("coordY")    ?: ""
            val strLe   = start?.basicData?.get("coordZ")    ?: ""
            val endX    = end?.basicData?.get("coordX")      ?: ""
            val endY    = end?.basicData?.get("coordY")      ?: ""
            val endLe   = end?.basicData?.get("coordZ")      ?: ""
            val strDep  = start?.basicData?.get("depth")     ?: ""
            val endDep  = end?.basicData?.get("depth")       ?: ""
            val strWid  = start?.basicData?.get("topWidth")  ?: ""
            val endWid  = end?.basicData?.get("topWidth")    ?: ""

            // NODE_XY：節點座標以 "x,y" 格式，多個節點以底線分隔
            val nodeXy = if (nodes.isEmpty()) {
                ""
            } else {
                nodes.joinToString("_") { wp ->
                    val x = wp.basicData["coordX"] ?: ""
                    val y = wp.basicData["coordY"] ?: ""
                    "$x,$y"
                }
            }

            // LENG：以起點/終點 GPS LatLng 計算平面直線距離（m）
            val leng = calcLength(start, end)

            // SLOP：(STR_LE − END_LE) / LENG × 100（%）
            val slop = calcSlope(strLe, endLe, leng)

            // NOTE：所有點位 remarks 過濾空值後合併（換行分隔）
            val note = waypoints.mapNotNull { wp ->
                wp.basicData["remarks"]?.takeIf { it.isNotBlank() }
            }.joinToString("\n")

            return GutterInspectBasicFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SPI_NUM, spiNum)
                    putString(ARG_SPI_TYP, spiTyp)
                    putString(ARG_STR_X,   strX)
                    putString(ARG_STR_Y,   strY)
                    putString(ARG_STR_LE,  strLe)
                    putString(ARG_END_X,   endX)
                    putString(ARG_END_Y,   endY)
                    putString(ARG_END_LE,  endLe)
                    putString(ARG_NODE_XY, nodeXy)
                    putString(ARG_STR_DEP, strDep)
                    putString(ARG_END_DEP, endDep)
                    putString(ARG_STR_WID, strWid)
                    putString(ARG_END_WID, endWid)
                    putString(ARG_LENG,    leng)
                    putString(ARG_SLOP,    slop)
                    putString(ARG_NOTE,    note)
                }
            }
        }

        // ── 計算輔助函式 ───────────────────────────────────────────

        /** 以 LatLng 計算起點到終點 Haversine 距離（公尺），格式化為兩位小數字串。 */
        private fun calcLength(start: Waypoint?, end: Waypoint?): String {
            val sLat = start?.latLng?.latitude  ?: return ""
            val sLng = start.latLng?.longitude  ?: return ""
            val eLat = end?.latLng?.latitude    ?: return ""
            val eLng = end?.latLng?.longitude   ?: return ""
            val R = 6371000.0   // 地球半徑（公尺）
            val dLat = Math.toRadians(eLat - sLat)
            val dLng = Math.toRadians(eLng - sLng)
            val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(sLat)) * Math.cos(Math.toRadians(eLat)) *
                    Math.sin(dLng / 2) * Math.sin(dLng / 2)
            val c = 2 * Math.atan2(sqrt(a), sqrt(1 - a))
            val dist = R * c
            return "%.2f".format(dist)
        }

        /**
         * 坡度 = (STR_LE − END_LE) / LENG × 100
         * 若任一值為空或 LENG 為 0 則回傳空字串。
         */
        private fun calcSlope(strLe: String, endLe: String, leng: String): String {
            val sZ = strLe.toDoubleOrNull() ?: return ""
            val eZ = endLe.toDoubleOrNull() ?: return ""
            val l  = leng.toDoubleOrNull()  ?: return ""
            if (l == 0.0) return ""
            val slope = (sZ - eZ) / l * 100.0
            return "%.4f".format(slope)
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

        binding.tvSpiNum.text = get(ARG_SPI_NUM)
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
