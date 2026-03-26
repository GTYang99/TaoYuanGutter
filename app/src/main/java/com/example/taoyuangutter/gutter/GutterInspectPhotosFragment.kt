package com.example.taoyuangutter.gutter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.taoyuangutter.api.DitchNode
import com.example.taoyuangutter.databinding.FragmentInspectPhotosBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * GutterInspectPhotosFragment
 *
 * 顯示整條側溝起點與終點的照片，全程唯讀。
 * 資料來源：DitchDetails.nodes 中 NODE_ATT=1（起點）、NODE_ATT=3（終點）的 url 列表。
 *
 * fileCategory 對應：
 *   1 → 測量位置及側溝概況（slot1）
 *   2 → 側溝內徑寬度尺寸（slot2）
 *   3 → 側溝深度尺寸（slot3）
 *
 * 圖片從 HTTP URL 以 Glide 載入。
 */
class GutterInspectPhotosFragment : Fragment() {

    private var _binding: FragmentInspectPhotosBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_NODES_JSON = "nodes_json"

        /**
         * 建立 Fragment 實例，傳入 DitchDetails.nodes 列表。
         */
        fun newInstance(nodes: List<DitchNode>): GutterInspectPhotosFragment {
            return GutterInspectPhotosFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_NODES_JSON, Gson().toJson(nodes))
                }
            }
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInspectPhotosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindPhotos()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── 照片顯示 ─────────────────────────────────────────────────────────

    private fun bindPhotos() {
        val json = arguments?.getString(ARG_NODES_JSON) ?: return
        val nodes: List<DitchNode> = try {
            val type = object : TypeToken<List<DitchNode>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        // NODE_ATT: "1"=起點、"3"=終點
        val startNode = nodes.firstOrNull { it.nodeAtt == "1" }
        val endNode   = nodes.firstOrNull { it.nodeAtt == "3" }

        // 依 fileCategory 取得對應 URL
        fun urlByCategory(node: DitchNode?, cat: String): String? =
            node?.url?.firstOrNull { it.fileCategory == cat }?.url

        // 起點照片
        loadPhoto(urlByCategory(startNode, "1"), binding.ivStrPhotoSlot1, binding.placeholderStrSlot1)
        loadPhoto(urlByCategory(startNode, "2"), binding.ivStrPhotoSlot2, binding.placeholderStrSlot2)
        loadPhoto(urlByCategory(startNode, "3"), binding.ivStrPhotoSlot3, binding.placeholderStrSlot3)

        // 終點照片
        loadPhoto(urlByCategory(endNode, "1"), binding.ivEndPhotoSlot1, binding.placeholderEndSlot1)
        loadPhoto(urlByCategory(endNode, "2"), binding.ivEndPhotoSlot2, binding.placeholderEndSlot2)
        loadPhoto(urlByCategory(endNode, "3"), binding.ivEndPhotoSlot3, binding.placeholderEndSlot3)
    }

    /**
     * 若 url 不為空，以 Glide 從 HTTP 載入圖片並顯示 ImageView；
     * 否則保留 placeholder 的「無照片」提示。
     */
    private fun loadPhoto(url: String?, imageView: ImageView, placeholder: View) {
        if (!url.isNullOrEmpty()) {
            Glide.with(this)
                .load(url)
                .into(imageView)
            imageView.visibility = View.VISIBLE
            placeholder.visibility = View.GONE
        } else {
            imageView.visibility = View.GONE
            placeholder.visibility = View.VISIBLE
        }
    }
}
