package com.example.taoyuangutter.gutter

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.taoyuangutter.api.DitchNode
import com.example.taoyuangutter.api.NodeDetails
import com.example.taoyuangutter.databinding.FragmentInspectPhotosBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class GutterInspectPhotosFragment : Fragment() {

    private data class PointViewData(
        val nodeId: Int,
        val label: String,
        val details: NodeDetails?,
        val photo1: String,
        val photo2: String,
        val photo3: String
    )

    private var _binding: FragmentInspectPhotosBinding? = null
    private val binding get() = _binding!!
    private var hasShownLoadErrorAlert = false
    private var pointDataList: List<PointViewData> = emptyList()

    companion object {
        private const val ARG_NODES_JSON = "nodes_json"
        private const val ARG_NODE_DETAILS_JSON = "node_details_json"
        private const val ARG_NODE_PHOTOS_JSON = "node_photos_json"

        fun newInstance(
            nodes: List<DitchNode>,
            preloadedNodeDetailsJson: String,
            preloadedPhotosJson: String
        ): GutterInspectPhotosFragment {
            return GutterInspectPhotosFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_NODES_JSON, Gson().toJson(nodes))
                    putString(ARG_NODE_DETAILS_JSON, preloadedNodeDetailsJson)
                    putString(ARG_NODE_PHOTOS_JSON, preloadedPhotosJson)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInspectPhotosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindPointSelectorAndContent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun bindPointSelectorAndContent() {
        val nodes = parseNodes(arguments?.getString(ARG_NODES_JSON))
        val detailsByNodeId = parseDetails(arguments?.getString(ARG_NODE_DETAILS_JSON))
            .mapNotNull { detail ->
                val id = detail.nodeId
                if (id == null) null else id to detail
            }
            .toMap()
        val photosByNodeId = mutableMapOf<Int, InspectPreloadedNodePhotos>()
        parsePhotos(arguments?.getString(ARG_NODE_PHOTOS_JSON)).forEach { photoItem ->
            photosByNodeId[photoItem.nodeId] = photoItem
        }

        val orderedNodes = nodes.sortedWith(
            compareBy<DitchNode>(
                { node: DitchNode -> when (node.nodeAtt) { "1" -> 0; "3" -> 2; else -> 1 } },
                { node: DitchNode -> node.nodeNum?.toIntOrNull() ?: Int.MAX_VALUE }
            )
        )

        pointDataList = orderedNodes.mapIndexed { index, node ->
            val detail = detailsByNodeId[node.nodeId]
            val p = photosByNodeId[node.nodeId]
            PointViewData(
                nodeId = node.nodeId,
                label = pointLabel(node, index),
                details = detail,
                photo1 = p?.photo1.orEmpty(),
                photo2 = p?.photo2.orEmpty(),
                photo3 = p?.photo3.orEmpty()
            )
        }

        if (pointDataList.isEmpty()) {
            showLoadErrorAlert()
            return
        }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            pointDataList.map { it.label }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerPointSelector.adapter = adapter
        binding.spinnerPointSelector.setSelection(0, false)
        
        // 設置 Spinner dropdown 位置偏移，防止蓋住選擇框
        binding.spinnerPointSelector.dropDownVerticalOffset = 120
        
        binding.spinnerPointSelector.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                renderPoint(pointDataList[position])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        })

        renderPoint(pointDataList.first())
    }

    private fun renderPoint(point: PointViewData) {
        renderFields(point.details)
        loadPhoto(point.photo1, binding.ivPhotoSlot1, binding.placeholderSlot1)
        loadPhoto(point.photo2, binding.ivPhotoSlot2, binding.placeholderSlot2)
        loadPhoto(point.photo3, binding.ivPhotoSlot3, binding.placeholderSlot3)

        binding.ivPhotoSlot1.setOnClickListener { showImageDetail(point.photo1) }
        binding.ivPhotoSlot2.setOnClickListener { showImageDetail(point.photo2) }
        binding.ivPhotoSlot3.setOnClickListener { showImageDetail(point.photo3) }
    }

    private fun showImageDetail(url: String) {
        if (url.isBlank()) return
        ImageDetailDialogFragment.newInstance(url)
            .show(childFragmentManager, "image_detail")
    }

    private fun renderFields(details: NodeDetails?) {
        binding.layoutFields.removeAllViews()

        val rows = listOf(
            "待架站" to mapBooleanCode(details?.isPendingDeploy),
            "側溝型式" to mapNodeType(details?.nodeTyP),
            "側溝X(E)座標" to details?.nodeX,
            "側溝Y(N)座標" to details?.nodeY,
            "側溝高程" to details?.nodeLe,
            "測量座標編號" to details?.xyNum,
            "溝蓋板厚度(cm)" to details?.coverDepAsString,
            "側溝頂寬度(cm)" to details?.nodeWidAsString,
            "側溝測量深度(cm)" to details?.nodeDepAsString,
            "側溝材質" to mapMaterialType(details?.matTyp),
            "淤積程度" to mapSilt(details?.isSilt),
            "溝體結構受損" to mapBoolean01(details?.isBroken == "1"),
            "附掛或過路管線" to mapBoolean01(details?.isHanging == "1"),
            "補充說明" to details?.note
        )

        rows.forEach { (label, rawValue) ->
            binding.layoutFields.addView(createFieldRow(label, normalizeDisplayValue(rawValue)))
        }
    }

    private fun createFieldRow(label: String, value: String): View {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = 16
            layoutParams = lp
        }

        val tvLabel = TextView(requireContext()).apply {
            text = label
            textSize = 13f
            setTextColor(resources.getColor(com.example.taoyuangutter.R.color.textColorSecondary, null))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = 4
            layoutParams = lp
        }
        val tvValue = TextView(requireContext()).apply {
            text = value
            minHeight = dp(32)
            gravity = android.view.Gravity.CENTER_VERTICAL
            textSize = 15f
            setTextColor(resources.getColor(com.example.taoyuangutter.R.color.textColorPrimary, null))
            setPadding(dp(12), dp(8), dp(12), dp(8))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        row.addView(tvLabel)
        row.addView(tvValue)
        return row
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun loadPhoto(url: String?, imageView: ImageView, placeholder: View) {
        if (!url.isNullOrBlank()) {
            imageView.visibility = View.VISIBLE
            placeholder.visibility = View.GONE
            Glide.with(this)
                .load(url)
                .thumbnail(0.25f)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (_binding != null) {
                            imageView.visibility = View.GONE
                            placeholder.visibility = View.VISIBLE
                        }
                        showLoadErrorAlert()
                        return true
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean = false
                })
                .into(imageView)
        } else {
            imageView.visibility = View.GONE
            placeholder.visibility = View.VISIBLE
        }
    }

    private fun parseNodes(json: String?): List<DitchNode> = try {
        if (json.isNullOrBlank()) emptyList() else Gson().fromJson(json, object : TypeToken<List<DitchNode>>() {}.type) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    private fun parseDetails(json: String?): List<NodeDetails> = try {
        if (json.isNullOrBlank()) emptyList() else Gson().fromJson(json, object : TypeToken<List<NodeDetails>>() {}.type) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    private fun parsePhotos(json: String?): List<InspectPreloadedNodePhotos> = try {
        if (json.isNullOrBlank()) emptyList() else Gson().fromJson(
            json,
            object : TypeToken<List<InspectPreloadedNodePhotos>>() {}.type
        ) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    private fun pointLabel(node: DitchNode, index: Int): String = when (node.nodeAtt) {
        "1" -> "起點"
        "3" -> "終點"
        else -> {
            val num = node.nodeNum?.trim().orEmpty().toIntOrNull() ?: (index + 1)
            "節點$num"
        }
    }

    private fun mapNodeType(code: String?): String = when (code) {
        "1" -> "U型溝（明溝）"
        "2" -> "U型溝（加蓋）"
        "3" -> "L型溝與暗溝渠併用"
        "4" -> "其他"
        else -> code.orEmpty()
    }

    private fun mapMaterialType(code: String?): String = when (code) {
        "1" -> "混凝土"
        "2" -> "卵礫石"
        "3" -> "紅磚"
        else -> code.orEmpty()
    }

    private fun mapBoolean01(value: Boolean?): String = when (value) {
        true -> "是"
        false -> "否"
        null -> ""
    }

    private fun mapBooleanCode(raw: String?): String = when (raw?.trim()?.lowercase()) {
        "1", "true", "y", "yes" -> "是"
        "0", "false", "n", "no" -> "否"
        else -> ""
    }

    private fun mapSilt(code: String?): String = when (code) {
        "0" -> "無"
        "1" -> "輕度"
        "2" -> "中度"
        "3" -> "嚴重"
        else -> code.orEmpty()
    }

    private fun normalizeDisplayValue(value: String?): String {
        val normalized = value?.trim().orEmpty()
        return if (normalized.isEmpty() || normalized.equals("null", ignoreCase = true)) "—" else normalized
    }

    private fun showLoadErrorAlert() {
        if (hasShownLoadErrorAlert) return
        hasShownLoadErrorAlert = true
        val act = activity ?: return
        act.runOnUiThread {
            if (act.isFinishing || act.isDestroyed) return@runOnUiThread
            AlertDialog.Builder(act)
                .setTitle("資料加載不完整")
                .setMessage("部分照片或點位欄位無法載入，資料可能不完整。")
                .setPositiveButton("確定", null)
                .show()
        }
    }
}
