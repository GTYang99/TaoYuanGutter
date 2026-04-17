package com.example.taoyuangutter.gutter

import com.example.taoyuangutter.R
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
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.taoyuangutter.databinding.FragmentGutterPhotosBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GutterPhotosFragment : Fragment() {

    private var _binding: FragmentGutterPhotosBinding? = null
    private val binding get() = _binding!!
    var onDraftChanged: (() -> Unit)? = null

    private var photoUriSlot1: Uri? = null
    private var photoUriSlot2: Uri? = null
    private var photoUriSlot3: Uri? = null

    /** 目前正在等候拍照結果的照片欄位（1/2/3） */
    private var pendingSlot: Int = 0

    /** 防止多張照片同時載入失敗時重複彈出 Alert（每次 setEditable 重置） */
    private var hasShownLoadErrorAlert = false

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
            Toast.makeText(requireContext(), getString(R.string.msg_camera_permission_required), Toast.LENGTH_SHORT).show()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            // 系統重建 → 先在 onCreate 恢復，避免 ActivityResult 在 register 時立即回傳但 pendingSlot 尚未恢復
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
                        1 -> { photoUriSlot1 = uri; showPhoto(binding.ivPhotoSlot1, binding.placeholderSlot1, uri); binding.btnDeleteSlot1.visibility = View.VISIBLE }
                        2 -> { photoUriSlot2 = uri; showPhoto(binding.ivPhotoSlot2, binding.placeholderSlot2, uri); binding.btnDeleteSlot2.visibility = View.VISIBLE }
                        3 -> { photoUriSlot3 = uri; showPhoto(binding.ivPhotoSlot3, binding.placeholderSlot3, uri); binding.btnDeleteSlot3.visibility = View.VISIBLE }
                    }
                    onDraftChanged?.invoke()
                }
            }
            pendingSlot = 0
            pendingOutputPath = null
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
     * [enabled] = true → 編輯模式，可點選格子拍照，有照片時顯示刪除按鈕；
     * [enabled] = false → 檢視模式，只顯示既有照片，無法拍照，不顯示刪除按鈕
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
            // 有照片時顯示刪除按鈕
            if (photoUriSlot1 != null) binding.btnDeleteSlot1.visibility = View.VISIBLE
            if (photoUriSlot2 != null) binding.btnDeleteSlot2.visibility = View.VISIBLE
            if (photoUriSlot3 != null) binding.btnDeleteSlot3.visibility = View.VISIBLE
            // 刪除按鈕點擊邏輯
            binding.btnDeleteSlot1.setOnClickListener { deletePhoto(1) }
            binding.btnDeleteSlot2.setOnClickListener { deletePhoto(2) }
            binding.btnDeleteSlot3.setOnClickListener { deletePhoto(3) }
        } else {
            // 每次切換到唯讀模式時重置，確保每次開啟表單都能彈出 Alert
            hasShownLoadErrorAlert = false
            binding.photoSlot1.setOnClickListener(null)
            binding.photoSlot2.setOnClickListener(null)
            binding.photoSlot3.setOnClickListener(null)
            // 唯讀：沒有照片的格子隱藏 placeholder（不顯示相機圖示）
            if (photoUriSlot1 == null) binding.placeholderSlot1.visibility = View.INVISIBLE
            if (photoUriSlot2 == null) binding.placeholderSlot2.visibility = View.INVISIBLE
            if (photoUriSlot3 == null) binding.placeholderSlot3.visibility = View.INVISIBLE
            // 唯讀：不顯示刪除按鈕
            binding.btnDeleteSlot1.visibility = View.GONE
            binding.btnDeleteSlot2.visibility = View.GONE
            binding.btnDeleteSlot3.visibility = View.GONE
        }
    }

    /** 刪除指定 slot 的照片，先跳出確認 Dialog，使用者確認後才清除 */
    private fun deletePhoto(slot: Int) {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("確認刪除")
            .setMessage("確定要刪除這張照片嗎？")
            .setNegativeButton("取消", null)
            .setPositiveButton("刪除") { _, _ ->
                when (slot) {
                    1 -> {
                        photoUriSlot1 = null
                        showPhoto(binding.ivPhotoSlot1, binding.placeholderSlot1, null)
                        binding.btnDeleteSlot1.visibility = View.GONE
                    }
                    2 -> {
                        photoUriSlot2 = null
                        showPhoto(binding.ivPhotoSlot2, binding.placeholderSlot2, null)
                        binding.btnDeleteSlot2.visibility = View.GONE
                    }
                    3 -> {
                        photoUriSlot3 = null
                        showPhoto(binding.ivPhotoSlot3, binding.placeholderSlot3, null)
                        binding.btnDeleteSlot3.visibility = View.GONE
                    }
                }
                onDraftChanged?.invoke()
            }
            .show()
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            .setTextColor(android.graphics.Color.parseColor("#D32F2F"))
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
            Toast.makeText(requireContext(), getString(R.string.msg_photo_prepare_failed), Toast.LENGTH_SHORT).show()
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
            ?: File(requireContext().filesDir, "Pictures").apply { mkdirs() }
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
            photoView.visibility   = View.GONE
            Glide.with(this).clear(photoView)
            photoView.setImageDrawable(null)
            return
        }
        placeholder.visibility = View.GONE
        photoView.visibility   = View.VISIBLE

        // 統一用 Glide 載入（包含本機 content:// / file:// 以及遠端 http(s)://），避免 setImageURI 在部分機型不更新或解碼失敗
        val scheme = uri.scheme?.lowercase()
        val isRemote = scheme == "http" || scheme == "https"
        val builder = Glide.with(this).load(uri).centerCrop()
        if (isRemote) {
            // 唯讀模式下顯示伺服器照片：加入失敗 Alert（每次進入頁面只彈一次）
            builder.listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    if (_binding != null) {
                        photoView.visibility = View.GONE
                        placeholder.visibility = View.VISIBLE
                    }
                    showPhotoLoadErrorAlert()
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
        }
        builder.into(photoView)
    }

    /**
     * 顯示「資料加載不完整」Alert，每次進入唯讀模式只顯示一次（[hasShownLoadErrorAlert] 控制）。
     */
    private fun showPhotoLoadErrorAlert() {
        if (hasShownLoadErrorAlert) return
        hasShownLoadErrorAlert = true
        val act = activity ?: return
        act.runOnUiThread {
            if (act.isFinishing || act.isDestroyed) return@runOnUiThread
            MaterialAlertDialogBuilder(act)
                .setTitle("資料加載不完整")
                .setMessage("部分照片無法載入，資料可能不完整。\n\n請關閉後重新點選側溝線段。")
                .setPositiveButton("確定", null)
                .show()
        }
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

    /**
     * 匯入既有點位資料後，將下載完成的照片 URI 預填入三個欄位。
     * @param photo1-3 內容 URI（content:// 或 file://），null/空字串表示該欄位仍需補拍
     */
    fun prefillPhotos(photo1: String?, photo2: String?, photo3: String?) {
        if (_binding == null) return

        fun parse(s: String?): Uri? =
            s?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }

        photoUriSlot1 = parse(photo1)
        photoUriSlot2 = parse(photo2)
        photoUriSlot3 = parse(photo3)

        showPhoto(binding.ivPhotoSlot1, binding.placeholderSlot1, photoUriSlot1)
        showPhoto(binding.ivPhotoSlot2, binding.placeholderSlot2, photoUriSlot2)
        showPhoto(binding.ivPhotoSlot3, binding.placeholderSlot3, photoUriSlot3)

        // 依目前模式更新刪除按鈕狀態
        val isViewMode = arguments?.getBoolean(ARG_VIEW_MODE) ?: false
        setEditable(!isViewMode)
        onDraftChanged?.invoke()
    }
}
