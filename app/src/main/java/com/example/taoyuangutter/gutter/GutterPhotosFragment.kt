package com.example.taoyuangutter.gutter

import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    private var photoFileSlot1: File? = null
    private var photoFileSlot2: File? = null
    private var photoFileSlot3: File? = null

    private var pendingSlot: Int = 0
    private var pendingFile: File? = null

    // ── ActivityResultLaunchers ──────────────────────────────────────────

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openLandscapeCamera(pendingSlot)
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val path = result.data?.getStringExtra(LandscapeCameraActivity.EXTRA_RESULT_PATH)
            val file = if (path != null) File(path) else pendingFile

            if (file != null && file.exists()) {
                when (pendingSlot) {
                    1 -> { photoFileSlot1 = file; showPhoto(binding.ivPhotoSlot1, binding.placeholderSlot1, file) }
                    2 -> { photoFileSlot2 = file; showPhoto(binding.ivPhotoSlot2, binding.placeholderSlot2, file) }
                    3 -> { photoFileSlot3 = file; showPhoto(binding.ivPhotoSlot3, binding.placeholderSlot3, file) }
                }
            }
        }
        pendingFile = null
        pendingSlot = 0
    }

    // ── Lifecycle ────────────────────────────────────────────────────────

    companion object {
        private const val ARG_VIEW_MODE  = "view_mode"
        private const val KEY_PHOTO_1    = "photo_1"
        private const val KEY_PHOTO_2    = "photo_2"
        private const val KEY_PHOTO_3    = "photo_3"
        private const val KEY_PENDING_SLOT = "pending_slot"
        private const val KEY_PENDING_FILE = "pending_file"

        fun newInstance(viewMode: Boolean = false) = GutterPhotosFragment().apply {
            arguments = Bundle().apply { putBoolean(ARG_VIEW_MODE, viewMode) }
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

        // 恢復狀態
        savedInstanceState?.let { bundle ->
            bundle.getString(KEY_PHOTO_1)?.let { photoFileSlot1 = File(it) }
            bundle.getString(KEY_PHOTO_2)?.let { photoFileSlot2 = File(it) }
            bundle.getString(KEY_PHOTO_3)?.let { photoFileSlot3 = File(it) }
            pendingSlot = bundle.getInt(KEY_PENDING_SLOT)
            bundle.getString(KEY_PENDING_FILE)?.let { pendingFile = File(it) }
        }

        // 根據恢復的檔案更新 UI
        photoFileSlot1?.let { showPhoto(binding.ivPhotoSlot1, binding.placeholderSlot1, it) }
        photoFileSlot2?.let { showPhoto(binding.ivPhotoSlot2, binding.placeholderSlot2, it) }
        photoFileSlot3?.let { showPhoto(binding.ivPhotoSlot3, binding.placeholderSlot3, it) }

        val isViewMode = arguments?.getBoolean(ARG_VIEW_MODE) ?: false
        setEditable(!isViewMode)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        photoFileSlot1?.let { outState.putString(KEY_PHOTO_1, it.absolutePath) }
        photoFileSlot2?.let { outState.putString(KEY_PHOTO_2, it.absolutePath) }
        photoFileSlot3?.let { outState.putString(KEY_PHOTO_3, it.absolutePath) }
        outState.putInt(KEY_PENDING_SLOT, pendingSlot)
        pendingFile?.let { outState.putString(KEY_PENDING_FILE, it.absolutePath) }
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
            if (photoFileSlot1 == null) binding.placeholderSlot1.visibility = View.VISIBLE
            if (photoFileSlot2 == null) binding.placeholderSlot2.visibility = View.VISIBLE
            if (photoFileSlot3 == null) binding.placeholderSlot3.visibility = View.VISIBLE
        } else {
            binding.photoSlot1.setOnClickListener(null)
            binding.photoSlot2.setOnClickListener(null)
            binding.photoSlot3.setOnClickListener(null)
            // 唯讀：沒有照片的格子隱藏 placeholder（不顯示相機圖示）
            if (photoFileSlot1 == null) binding.placeholderSlot1.visibility = View.INVISIBLE
            if (photoFileSlot2 == null) binding.placeholderSlot2.visibility = View.INVISIBLE
            if (photoFileSlot3 == null) binding.placeholderSlot3.visibility = View.INVISIBLE
        }
    }

    // ── 相機流程 ─────────────────────────────────────────────────────────

    private fun requestCameraForSlot(slot: Int) {
        pendingSlot = slot
        if (ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openLandscapeCamera(slot)
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun openLandscapeCamera(slot: Int) {
        val file = createImageFile()
        pendingFile = file
        pendingSlot = slot
        val intent = LandscapeCameraActivity.newIntent(requireContext(), file)
        cameraLauncher.launch(intent)
    }

    private fun createImageFile(): File {
        val ts  = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dir = requireContext().getExternalFilesDir("Pictures") ?: requireContext().filesDir
        dir.mkdirs()
        return File(dir, "GUTTER_$ts.jpg")
    }

    // ── UI 更新 ──────────────────────────────────────────────────────────

    private fun showPhoto(photoView: android.widget.ImageView, placeholder: View, file: File) {
        if (!file.exists()) return
        placeholder.visibility = View.GONE
        photoView.visibility   = View.VISIBLE
        photoView.setImageURI(null) // 清除快取
        photoView.setImageURI(Uri.fromFile(file))
    }

    // ── 對外 API ─────────────────────────────────────────────────────────

    fun getPhotoUris(): Triple<Uri?, Uri?, Uri?> {
        fun fileToUri(file: File?): Uri? = file?.takeIf { it.exists() }?.let {
            FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                it
            )
        }
        return Triple(fileToUri(photoFileSlot1), fileToUri(photoFileSlot2), fileToUri(photoFileSlot3))
    }
}
