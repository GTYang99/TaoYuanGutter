package com.example.taoyuangutter.gutter

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.taoyuangutter.databinding.FragmentGutterPhotosBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GutterPhotosFragment : Fragment() {

    private var _binding: FragmentGutterPhotosBinding? = null
    private val binding get() = _binding!!

    private var photoUriSlot1: Uri? = null
    private var photoUriSlot2: Uri? = null
    private var photoUriSlot3: Uri? = null

    /** 目前正在等候拍照結果的照片欄位（1/2/3） */
    private var pendingSlot: Int = 0

    /** LandscapeCameraActivity 輸出檔案的絕對路徑（Activity 重建後恢復用） */
    private var pendingOutputPath: String? = null

    // ── ActivityResultLaunchers ──────────────────────────────────────────

    /**
     * 相機權限請求。
     * 取得權限後自動以 pendingSlot 開啟 LandscapeCameraActivity。
     */
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingSlot > 0) {
            launchLandscapeCamera(pendingSlot)
        } else if (!granted) {
            pendingSlot = 0
            Toast.makeText(requireContext(), "需要相機權限才能拍照", Toast.LENGTH_SHORT).show()
        }
    }

    /** LandscapeCameraActivity 結果接收器 */
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>

    // ── Lifecycle ────────────────────────────────────────────────────────

    companion object {
        private const val ARG_VIEW_MODE  = "view_mode"
        // 既有照片路徑（重新開啟表單時帶入）
        const val ARG_PHOTO_1      = "arg_photo_1"
        const val ARG_PHOTO_2      = "arg_photo_2"
        const val ARG_PHOTO_3      = "arg_photo_3"
        // savedInstanceState keys
        private const val KEY_PHOTO_1        = "photo_1"
        private const val KEY_PHOTO_2        = "photo_2"
        private const val KEY_PHOTO_3        = "photo_3"
        private const val KEY_PENDING_SLOT   = "pending_slot"
        private const val KEY_PENDING_PATH   = "pending_path"

        fun newInstance(
            viewMode: Boolean = false,
            photo1: String? = null,
            photo2: String? = null,
            photo3: String? = null
        ) = GutterPhotosFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_VIEW_MODE, viewMode)
                if (!photo1.isNullOrEmpty()) putString(ARG_PHOTO_1, photo1)
                if (!photo2.isNullOrEmpty()) putString(ARG_PHOTO_2, photo2)
                if (!photo3.isNullOrEmpty()) putString(ARG_PHOTO_3, photo3)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGutterPhotosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── 註冊 LandscapeCameraActivity 結果接收 ──────────────────────
        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // 取回 LandscapeCameraActivity 已儲存的檔案路徑
                val path = result.data?.getStringExtra(LandscapeCameraActivity.EXTRA_RESULT_PATH)
                    ?: pendingOutputPath
                if (path != null) {
                    val file = File(path)
                    // 轉為 FileProvider content URI，與其他照片儲存格式一致
                    val uri = try {
                        FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.fileprovider",
                            file
                        )
                    } catch (e: Exception) {
                        Uri.fromFile(file)  // fallback
                    }
                    when (pendingSlot) {
                        1 -> { photoUriSlot1 = uri; showPhoto(binding.ivPhotoSlot1, binding.placeholderSlot1, uri) }
                        2 -> { photoUriSlot2 = uri; showPhoto(binding.ivPhotoSlot2, binding.placeholderSlot2, uri) }
                        3 -> { photoUriSlot3 = uri; showPhoto(binding.ivPhotoSlot3, binding.placeholderSlot3, uri) }
                    }
                }
            }
            pendingSlot = 0
            pendingOutputPath = null
        }

        if (savedInstanceState != null) {
            // 系統重建 → 從 savedInstanceState 恢復
            savedInstanceState.getString(KEY_PHOTO_1)?.let { photoUriSlot1 = Uri.parse(it) }
            savedInstanceState.getString(KEY_PHOTO_2)?.let { photoUriSlot2 = Uri.parse(it) }
            savedInstanceState.getString(KEY_PHOTO_3)?.let { photoUriSlot3 = Uri.parse(it) }
            pendingSlot       = savedInstanceState.getInt(KEY_PENDING_SLOT, 0)
            pendingOutputPath = savedInstanceState.getString(KEY_PENDING_PATH)
        } else {
            // 首次建立 → 從 arguments 帶入既有照片（重新開啟表單時）
            fun tryLoad(uriString: String?): Uri? =
                uriString?.takeIf { it.isNotEmpty() }?.let { Uri.parse(it) }
            photoUriSlot1 = tryLoad(arguments?.getString(ARG_PHOTO_1))
            photoUriSlot2 = tryLoad(arguments?.getString(ARG_PHOTO_2))
            photoUriSlot3 = tryLoad(arguments?.getString(ARG_PHOTO_3))
        }

        // 根據恢復的 URI 更新 UI
        photoUriSlot1?.let { showPhoto(binding.ivPhotoSlot1, binding.placeholderSlot1, it) }
        photoUriSlot2?.let { showPhoto(binding.ivPhotoSlot2, binding.placeholderSlot2, it) }
        photoUriSlot3?.let { showPhoto(binding.ivPhotoSlot3, binding.placeholderSlot3, it) }

        val isViewMode = arguments?.getBoolean(ARG_VIEW_MODE) ?: false
        setEditable(!isViewMode)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        photoUriSlot1?.let { outState.putString(KEY_PHOTO_1, it.toString()) }
        photoUriSlot2?.let { outState.putString(KEY_PHOTO_2, it.toString()) }
        photoUriSlot3?.let { outState.putString(KEY_PHOTO_3, it.toString()) }
        outState.putInt(KEY_PENDING_SLOT, pendingSlot)
        pendingOutputPath?.let { outState.putString(KEY_PENDING_PATH, it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── 可編輯狀態切換 ────────────────────────────────────────────────────

    /**
     * [enabled] = true → 編輯模式，可點選格子拍照；
     * [enabled] = false → 檢視模式，只顯示既有照片，無法拍照
     */
    fun setEditable(enabled: Boolean) {
        if (enabled) {
            binding.photoSlot1.setOnClickListener { requestCameraForSlot(1) }
            binding.photoSlot2.setOnClickListener { requestCameraForSlot(2) }
            binding.photoSlot3.setOnClickListener { requestCameraForSlot(3) }
            // 無照片時顯示 placeholder（相機 icon）
            if (photoUriSlot1 == null) binding.placeholderSlot1.visibility = View.VISIBLE
            if (photoUriSlot2 == null) binding.placeholderSlot2.visibility = View.VISIBLE
            if (photoUriSlot3 == null) binding.placeholderSlot3.visibility = View.VISIBLE
        } else {
            binding.photoSlot1.setOnClickListener(null)
            binding.photoSlot2.setOnClickListener(null)
            binding.photoSlot3.setOnClickListener(null)
            // 唯讀：沒有照片的格子隱藏 placeholder（不顯示相機圖示）
            if (photoUriSlot1 == null) binding.placeholderSlot1.visibility = View.INVISIBLE
            if (photoUriSlot2 == null) binding.placeholderSlot2.visibility = View.INVISIBLE
            if (photoUriSlot3 == null) binding.placeholderSlot3.visibility = View.INVISIBLE
        }
    }

    // ── 相機流程 ─────────────────────────────────────────────────────────

    /**
     * 點擊照片格後呼叫。
     * 先確認相機權限，有權限則直接開啟 [LandscapeCameraActivity]（強制橫向）；
     * 無權限則請求後，授權回呼中再自動開啟。
     */
    private fun requestCameraForSlot(slot: Int) {
        pendingSlot = slot
        if (ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            launchLandscapeCamera(slot)
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    /**
     * 建立輸出檔案後啟動 [LandscapeCameraActivity]。
     * Activity 強制鎖定橫向，且未橫放時會顯示「請轉為橫向拍照」遮罩並禁用快門。
     */
    private fun launchLandscapeCamera(slot: Int) {
        val outputFile = createOutputFile(slot)
        if (outputFile == null) {
            pendingSlot = 0
            Toast.makeText(requireContext(), "無法準備拍照檔案", Toast.LENGTH_SHORT).show()
            return
        }
        pendingOutputPath = outputFile.absolutePath
        val intent = LandscapeCameraActivity.newIntent(requireContext(), outputFile)
        cameraLauncher.launch(intent)
    }

    /**
     * 在 external pictures 目錄建立暫存輸出檔案。
     * 命名格式：GUTTER_{slot}_{timestamp}.jpg
     */
    private fun createOutputFile(slot: Int): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return try {
            File.createTempFile("GUTTER_${slot}_${timeStamp}_", ".jpg", storageDir)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ── UI 更新 ──────────────────────────────────────────────────────────

    private fun showPhoto(photoView: android.widget.ImageView, placeholder: View, uri: Uri?) {
        if (uri == null) {
            placeholder.visibility = View.VISIBLE
            photoView.visibility = View.GONE
            photoView.setImageURI(null)
            return
        }
        placeholder.visibility = View.GONE
        photoView.visibility   = View.VISIBLE
        photoView.setImageURI(null) // 清除快取
        photoView.setImageURI(uri)
    }

    // ── 對外 API ─────────────────────────────────────────────────────────

    /**
     * 驗證三個照片格是否全部拍攝完畢。
     * @return 第一個尚未拍攝的照片格說明；全部完成則回傳 null。
     */
    fun validateAllPhotos(): String? {
        if (photoUriSlot1 == null) return "測量位置及側溝概況（第1張）"
        if (photoUriSlot2 == null) return "側溝內徑寬度尺寸（第2張）"
        if (photoUriSlot3 == null) return "側溝深度尺寸（第3張）"
        return null
    }

    /** 傳回三個照片的 URI 字串（無照片或檔案不存在則為 null）。 */
    fun getPhotoPaths(): Triple<String?, String?, String?> = Triple(
        photoUriSlot1?.toString(),
        photoUriSlot2?.toString(),
        photoUriSlot3?.toString()
    )
}
