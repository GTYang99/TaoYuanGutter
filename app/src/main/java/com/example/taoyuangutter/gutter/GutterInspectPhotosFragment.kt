package com.example.taoyuangutter.gutter

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
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

    /** 防止重複彈出 Alert（多張圖片同時失敗時只顯示一次） */
    private var hasShownLoadErrorAlert = false

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
        val json = arguments?.getString(ARG_NODES_JSON) ?: run {
            showLoadErrorAlert()
            return
        }
        val nodes: List<DitchNode> = try {
            val type = object : TypeToken<List<DitchNode>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        if (nodes.isEmpty()) {
            showLoadErrorAlert()
            return
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
     * 若 Glide 載入失敗，顯示「資料加載不完整」Alert。
     * 否則保留 placeholder 的「無照片」提示。
     */
    private fun loadPhoto(url: String?, imageView: ImageView, placeholder: View) {
        if (!url.isNullOrEmpty()) {
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
                        // 只有 View 仍然存在時才更新 UI
                        if (_binding != null) {
                            imageView.visibility = View.GONE
                            placeholder.visibility = View.VISIBLE
                        }
                        // Alert 透過 activity.runOnUiThread 顯示，不受 binding 影響
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

    /**
     * 顯示「資料加載不完整」Alert，每次進入照片頁只顯示一次。
     * 透過 activity.runOnUiThread 確保在主執行緒彈出，
     * 即使 Glide 的 onLoadFailed 從背景執行緒回呼也能正確運作。
     */
    private fun showLoadErrorAlert() {
        if (hasShownLoadErrorAlert) return
        hasShownLoadErrorAlert = true
        val act = activity ?: return
        act.runOnUiThread {
            if (act.isFinishing || act.isDestroyed) return@runOnUiThread
            AlertDialog.Builder(act)
                .setTitle("資料加載不完整")
                .setMessage("部分照片無法載入，資料可能不完整。\n\n請點選側溝線段重新下載資料。")
                .setPositiveButton("確定", null)
                .show()
        }
    }
}
