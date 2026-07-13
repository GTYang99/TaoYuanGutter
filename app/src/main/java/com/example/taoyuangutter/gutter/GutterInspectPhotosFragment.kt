package com.example.taoyuangutter.gutter

import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.taoyuangutter.R
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
        val photo3: String,
        val capturedAt1: String?,
        val capturedAt2: String?,
        val capturedAt3: String?
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
                label = pointLabel(node, detail, index),
                details = detail,
                photo1 = p?.photo1.orEmpty(),
                photo2 = p?.photo2.orEmpty(),
                photo3 = p?.photo3.orEmpty(),
                capturedAt1 = detail?.capturedAt?.getOrNull(0)?.takeIf { it.isNotBlank() },
                capturedAt2 = detail?.capturedAt?.getOrNull(1)?.takeIf { it.isNotBlank() },
                capturedAt3 = detail?.capturedAt?.getOrNull(2)?.takeIf { it.isNotBlank() }
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
        val isVirtual = point.details?.isVirtual == "1" || point.details?.isVirtual?.lowercase() == "true"
        renderFields(point, isVirtual)
    }

    private fun showImageDetail(url: String) {
        if (url.isBlank()) return
        ImageDetailDialogFragment.newInstance(url)
            .show(childFragmentManager, "image_detail")
    }

    private fun renderFields(point: PointViewData, isVirtual: Boolean) {
        val details = point.details
        val isCantOpen = details?.isCantOpenAsBoolean == true
        binding.layoutFields.removeAllViews()
        binding.layoutFields.addView(createFieldRow("待架站", normalizeDisplayValue(mapBooleanCode(details?.isPendingDeploy))))
        if (!isVirtual) {
            binding.layoutFields.addView(createFieldRow("側溝型式", normalizeDisplayValue(mapNodeType(details?.nodeTyP))))
        }
        binding.layoutFields.addView(createFieldRow("側溝X(E)座標", normalizeDisplayValue(details?.nodeX)))
        binding.layoutFields.addView(createFieldRow("側溝Y(N)座標", normalizeDisplayValue(details?.nodeY)))
        if (!isVirtual) {
            binding.layoutFields.addView(createFieldRow("側溝高程", normalizeDisplayValue(details?.nodeLe)))
            binding.layoutFields.addView(
                createPhotoSection(
                    title = getString(R.string.photo_slot_overview),
                    url = point.photo1,
                    capturedAt = point.capturedAt1,
                    onClick = { showImageDetail(point.photo1) }
                )
            )
        }
        if (!isVirtual && !isCantOpen) {
            binding.layoutFields.addView(createFieldRow("溝蓋板厚度(公分)", normalizeDisplayValue(details?.coverDepAsString)))
            binding.layoutFields.addView(
                createPhotoSection(
                    title = getString(R.string.label_photo_title_width),
                    url = point.photo2,
                    capturedAt = point.capturedAt2,
                    onClick = { showImageDetail(point.photo2) }
                )
            )
            binding.layoutFields.addView(createFieldRow("側溝頂寬度(公分)", normalizeDisplayValue(details?.nodeWidAsString)))
            binding.layoutFields.addView(
                createPhotoSection(
                    title = getString(R.string.label_photo_title_depth),
                    url = point.photo3,
                    capturedAt = point.capturedAt3,
                    onClick = { showImageDetail(point.photo3) }
                )
            )
            binding.layoutFields.addView(createFieldRow("側溝測量深度(公分)", normalizeDisplayValue(details?.nodeDepAsString)))
            binding.layoutFields.addView(createFieldRow("側溝材質", normalizeDisplayValue(mapMaterialType(details?.matTyp))))
            binding.layoutFields.addView(createFieldRow("淤積程度", normalizeDisplayValue(mapSilt(details?.isSilt))))
            binding.layoutFields.addView(createFieldRow("溝體結構受損", normalizeDisplayValue(mapBoolean01(details?.isBroken == "1"))))
            binding.layoutFields.addView(createFieldRow("附掛或過路管線", normalizeDisplayValue(mapBoolean01(details?.isHanging == "1"))))
        }
        binding.layoutFields.addView(createFieldRow("補充說明", normalizeDisplayValue(details?.note)))
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

    private fun createPhotoSection(
        title: String,
        url: String?,
        capturedAt: String?,
        onClick: () -> Unit
    ): View {
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(16)
            }
        }
        root.addView(LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
            addView(TextView(requireContext()).apply {
                text = title
                textSize = 13f
                setTypeface(typeface, Typeface.NORMAL)
                setTextColor(resources.getColor(R.color.textColorSecondary, null))
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            })
            addView(TextView(requireContext()).apply {
                text = capturedAt.orEmpty()
                textSize = 12f
                setTextColor(resources.getColor(R.color.textColorSecondary, null))
                visibility = if (capturedAt.isNullOrBlank()) View.GONE else View.VISIBLE
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            })
        })

        val card = ConstraintLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_photo_placeholder)
            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND
        }
        val ratioAnchor = View(requireContext()).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(0, 0).apply {
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                dimensionRatio = "H,4:3"
            }
        }
        val placeholder = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToTop = ratioAnchor.id
                bottomToBottom = ratioAnchor.id
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            }
            addView(ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
                setImageResource(R.drawable.ic_camera)
                alpha = 0.3f
            })
            addView(TextView(requireContext()).apply {
                text = getString(R.string.no_photo)
                textSize = 12f
                setTextColor(resources.getColor(R.color.inputFieldHint, null))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(6)
                }
            })
        }
        val imageView = ImageView(requireContext()).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = View.GONE
            layoutParams = ConstraintLayout.LayoutParams(0, 0).apply {
                topToTop = ratioAnchor.id
                bottomToBottom = ratioAnchor.id
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            }
            setOnClickListener { onClick() }
        }
        card.addView(ratioAnchor)
        card.addView(placeholder)
        card.addView(imageView)
        root.addView(card)
        loadPhoto(url, imageView, placeholder)
        return root
    }

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

    private fun pointLabel(node: DitchNode, details: NodeDetails?, index: Int): String {
        val baseLabel = when (node.nodeAtt) {
            "1" -> "起點"
            "3" -> "終點"
            else -> {
                val num = node.nodeNum?.trim().orEmpty().toIntOrNull() ?: (index + 1)
                "節點$num"
            }
        }
        val xyNum = details?.xyNum?.trim().orEmpty().takeIf { it.isNotEmpty() } ?: "---"
        return "$baseLabel ($xyNum)"
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
