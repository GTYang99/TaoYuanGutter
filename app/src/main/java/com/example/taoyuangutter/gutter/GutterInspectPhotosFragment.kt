package com.example.taoyuangutter.gutter

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.taoyuangutter.databinding.FragmentInspectPhotosBinding

/**
 * GutterInspectPhotosFragment
 *
 * 顯示整條側溝的起點與終點照片，全程唯讀。
 *
 * 對應文件 桃園側溝分析文件_檢視欄位：
 *   起點：STR_PHOTO_OV / STR_PHOTO_WID / STR_PHOTO_DEP（photo1/2/3 from START）
 *   終點：END_PHOTO_OV / END_PHOTO_WID / END_PHOTO_DEP（photo1/2/3 from END）
 */
class GutterInspectPhotosFragment : Fragment() {

    private var _binding: FragmentInspectPhotosBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_STR_PHOTO_1 = "str_photo_1"
        private const val ARG_STR_PHOTO_2 = "str_photo_2"
        private const val ARG_STR_PHOTO_3 = "str_photo_3"
        private const val ARG_END_PHOTO_1 = "end_photo_1"
        private const val ARG_END_PHOTO_2 = "end_photo_2"
        private const val ARG_END_PHOTO_3 = "end_photo_3"

        /**
         * 從 List<Waypoint> 取出起點與終點的照片路徑，建立 Fragment 實例。
         */
        fun newInstance(waypoints: List<Waypoint>): GutterInspectPhotosFragment {
            val start = waypoints.firstOrNull { it.type == WaypointType.START }
            val end   = waypoints.firstOrNull { it.type == WaypointType.END }

            return GutterInspectPhotosFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_STR_PHOTO_1, start?.basicData?.get("photo1"))
                    putString(ARG_STR_PHOTO_2, start?.basicData?.get("photo2"))
                    putString(ARG_STR_PHOTO_3, start?.basicData?.get("photo3"))
                    putString(ARG_END_PHOTO_1, end?.basicData?.get("photo1"))
                    putString(ARG_END_PHOTO_2, end?.basicData?.get("photo2"))
                    putString(ARG_END_PHOTO_3, end?.basicData?.get("photo3"))
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
        val a = arguments ?: return

        // 起點
        loadPhoto(a.getString(ARG_STR_PHOTO_1), binding.ivStrPhotoSlot1, binding.placeholderStrSlot1)
        loadPhoto(a.getString(ARG_STR_PHOTO_2), binding.ivStrPhotoSlot2, binding.placeholderStrSlot2)
        loadPhoto(a.getString(ARG_STR_PHOTO_3), binding.ivStrPhotoSlot3, binding.placeholderStrSlot3)

        // 終點
        loadPhoto(a.getString(ARG_END_PHOTO_1), binding.ivEndPhotoSlot1, binding.placeholderEndSlot1)
        loadPhoto(a.getString(ARG_END_PHOTO_2), binding.ivEndPhotoSlot2, binding.placeholderEndSlot2)
        loadPhoto(a.getString(ARG_END_PHOTO_3), binding.ivEndPhotoSlot3, binding.placeholderEndSlot3)
    }

    /**
     * 若 uriString 不為空，顯示圖片並隱藏佔位 placeholder；
     * 否則保留 placeholder 的「無照片」提示。
     */
    private fun loadPhoto(uriString: String?, imageView: ImageView, placeholder: View) {
        if (!uriString.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(uriString)
                imageView.setImageURI(null)   // 清除快取
                imageView.setImageURI(uri)
                imageView.visibility = View.VISIBLE
                placeholder.visibility = View.GONE
            } catch (e: Exception) {
                // URI 解析失敗：保留無照片佔位
                imageView.visibility = View.GONE
                placeholder.visibility = View.VISIBLE
            }
        } else {
            imageView.visibility = View.GONE
            placeholder.visibility = View.VISIBLE
        }
    }
}
