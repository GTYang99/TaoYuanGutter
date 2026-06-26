package com.example.taoyuangutter.gutter

import com.example.taoyuangutter.R
import android.app.Activity
import android.os.Build
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.example.taoyuangutter.common.PhotoUploadValidator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GutterPhotosFragment : Fragment() {

    interface DraftChangeHost {
        fun onPhotosDraftChanged(photo1: String?, photo2: String?, photo3: String?)
        fun onPendingPhotoDraftChanged(slot: Int, pendingOutputPath: String?)
    }

    private var _binding: FragmentGutterPhotosBinding? = null
    private val binding get() = _binding!!
    var onDraftChanged: (() -> Unit)? = null
    private var draftChangeHost: DraftChangeHost? = null

    private var photoUriSlot1: Uri? = null
    private var photoUriSlot2: Uri? = null
    private var photoUriSlot3: Uri? = null
    private var isImportLocked: Boolean = false

    /** 目前正在等候拍照結果的照片欄位（1/2/3） */
    private var pendingSlot: Int = 0

    /** 防止多張照片同時載入失敗時重複彈出 Alert（每次 setEditable 重置） */
    private var hasShownLoadErrorAlert = false

    /**
     * 內部同步已儲存照片時會短暫開啟。
     * 這段期間不應回報草稿變更，否則會把「UI 回寫」誤當成「使用者操作」。
     */
    private var suppressDraftChangeCallback = false

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
            maybeRequestLegacyWritePermissionThenLaunch()
        } else if (!granted) {
            val canceledSlot = pendingSlot
            pendingSlot = 0
            if (canceledSlot > 0) {
                draftChangeHost?.onPendingPhotoDraftChanged(canceledSlot, null)
            }
            Toast.makeText(requireContext(), getString(R.string.msg_camera_permission_required), Toast.LENGTH_SHORT).show()
        }
    }

    private val legacyWritePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            val canceledSlot = pendingSlot
            Toast.makeText(
                requireContext(),
                "需要儲存權限才能同時寫入系統相簿",
                Toast.LENGTH_SHORT
            ).show()
            pendingSlot = 0
            if (canceledSlot > 0) {
                draftChangeHost?.onPendingPhotoDraftChanged(canceledSlot, null)
            }
            return@registerForActivityResult
        }
        if (pendingSlot > 0) {
            launchCameraOverlay(pendingSlot)
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────────

    companion object {
        private const val ARG_VIEW_MODE  = "view_mode"
        private const val ARG_IS_IMPORTED = "is_imported" // 新增：匯入點位旗標
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
            photo3: String? = null,
            isImported: Boolean = false // 新增：傳入匯入旗標
        ) = GutterPhotosFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_VIEW_MODE, viewMode)
                putBoolean(ARG_IS_IMPORTED, isImported)
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

    override fun onAttach(context: android.content.Context) {
        super.onAttach(context)
        draftChangeHost = context as? DraftChangeHost
    }

    override fun onDetach() {
        draftChangeHost = null
        super.onDetach()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 修正：如果是由系統重建，UI 會由 showPhoto 更新（在 onCreate 已恢復 URI）
        // 且 FragmentResultListener 會處理進行中的拍照結果

        // ── 註冊相機 Overlay 結果接收（避免 Activity 切換造成放大/旋轉卡頓） ──
        parentFragmentManager.setFragmentResultListener(
            CameraOverlayFragment.RESULT_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val code = bundle.getInt(CameraOverlayFragment.RESULT_CODE, Activity.RESULT_CANCELED)
            val slot = bundle.getInt(CameraOverlayFragment.RESULT_SLOT, 0)
            val path = bundle.getString(CameraOverlayFragment.RESULT_PATH) ?: pendingOutputPath
            if (code == Activity.RESULT_OK && !path.isNullOrBlank() && slot in 1..3) {
                (activity as? PhotoLoadingHost)?.setPhotoLoading(true)
                val file = File(path)
                val uri = try {
                    FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        file
                    )
                } catch (_: Exception) {
                    Uri.fromFile(file)
                }
                applyPhotoToSlot(slot, uri, notifyDraftChanged = true)
            }
            if (slot in 1..3) {
                draftChangeHost?.onPendingPhotoDraftChanged(slot, null)
            }
            pendingSlot = 0
            pendingOutputPath = null
        }

        // 根據恢復的 URI 更新 UI
        renderStoredPhotoSlots()

        val isViewMode = arguments?.getBoolean(ARG_VIEW_MODE) ?: false
        val isImported = arguments?.getBoolean(ARG_IS_IMPORTED) ?: false
        
        if (isImported) {
            setImportLocked(true)
        } else {
            applyImportLockUi()
            setEditable(!isViewMode)
        }
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
        val actualEnabled = enabled && !isImportLocked
        binding.importLockOverlay.visibility = View.GONE
        if (actualEnabled) {
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

        // 匯入鎖定或檢視模式均套用半透明，確保視覺一致性
        binding.root.alpha = if (actualEnabled) 1.0f else 0.6f
    }

    fun setImportLocked(locked: Boolean) {
        isImportLocked = locked
        applyImportLockUi()
        val isViewMode = arguments?.getBoolean(ARG_VIEW_MODE) ?: false
        setEditable(!isViewMode)
    }

    private fun applyImportLockUi() {
        if (_binding == null) return
        binding.importLockOverlay.visibility = View.GONE
        binding.root.foreground = if (isImportLocked) {
            ContextCompat.getDrawable(requireContext(), R.drawable.bg_import_lock_scrim)
        } else {
            null
        }
    }

    /** 刪除指定 slot 的照片，先跳出確認 Dialog，使用者確認後才清除 */
    private fun deletePhoto(slot: Int) {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("確認刪除")
            .setMessage("確定要刪除這張照片嗎？")
            .setNegativeButton("取消", null)
            .setPositiveButton("刪除") { _, _ ->
                clearPhotoSlot(slot, notifyDraftChanged = true)
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
            maybeRequestLegacyWritePermissionThenLaunch()
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun maybeRequestLegacyWritePermissionThenLaunch() {
        val slot = pendingSlot
        if (slot <= 0) return

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            legacyWritePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }

        launchCameraOverlay(slot)
    }

    /**
     * 建立輸出檔案後啟動 [LandscapeCameraActivity]。
     * Activity 強制鎖定橫向，且未橫放時會顯示「請轉為橫向拍照」遮罩並禁用快門。
     */
    private fun launchCameraOverlay(slot: Int) {
        val outputFile = createOutputFile(slot)
        if (outputFile == null) {
            val failedSlot = pendingSlot
            pendingSlot = 0
            if (failedSlot > 0) {
                draftChangeHost?.onPendingPhotoDraftChanged(failedSlot, null)
            }
            Toast.makeText(requireContext(), getString(R.string.msg_photo_prepare_failed), Toast.LENGTH_SHORT).show()
            return
        }
        pendingOutputPath = outputFile.absolutePath
        draftChangeHost?.onPendingPhotoDraftChanged(slot, outputFile.absolutePath)
        (activity as? GutterFormActivity)?.showCameraOverlay(slot, outputFile.absolutePath)
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

    private fun showPhoto(
        slot: Int,
        photoView: android.widget.ImageView,
        placeholder: View,
        loading: View,
        uri: Uri?
    ) {
        if (uri == null) {
            loading.visibility = View.GONE
            placeholder.visibility = View.VISIBLE
            photoView.visibility   = View.GONE
            Glide.with(this).clear(photoView)
            photoView.setImageDrawable(null)
            return
        }
        placeholder.visibility = View.GONE
        photoView.visibility   = View.VISIBLE
        val startMs = SystemClock.uptimeMillis()
        loading.visibility = View.VISIBLE

        // 統一用 Glide 載入（包含本機 content:// / file:// 以及遠端 http(s)://），避免 setImageURI 在部分機型不更新或解碼失敗
        val scheme = uri.scheme?.lowercase()
        val isRemote = scheme == "http" || scheme == "https"
        // 限制 decode 尺寸為縮圖大小，加快「拍照回來」顯示速度
        val targetW = photoView.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val targetH = (targetW * 3) / 4
        val builder = Glide.with(this).load(uri).override(targetW, targetH).centerCrop()
        builder.listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>,
                isFirstResource: Boolean
            ): Boolean {
                if (_binding != null) {
                    val elapsed = SystemClock.uptimeMillis() - startMs
                    val delayMs = (250L - elapsed).coerceAtLeast(0L)
                    loading.postDelayed({ if (_binding != null) loading.visibility = View.GONE }, delayMs)
                }
                (activity as? PhotoLoadingHost)?.setPhotoLoading(false)
                if (isRemote) {
                    photoView.visibility = View.GONE
                    placeholder.visibility = View.VISIBLE
                    showPhotoLoadErrorAlert()
                    // remote: 自己處理（顯示 placeholder），避免 Glide 再設錯誤圖
                    return true
                }

                photoView.visibility = View.GONE
                placeholder.visibility = View.VISIBLE
                // local：保留 slot 的資料，不因暫時載入失敗而把草稿內容清掉
                // 讓 Activity 的草稿同步與下一次重建還能接住同一張照片。
                return true
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable>?,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean {
                if (_binding != null) {
                    val elapsed = SystemClock.uptimeMillis() - startMs
                    val delayMs = (250L - elapsed).coerceAtLeast(0L)
                    loading.postDelayed({ if (_binding != null) loading.visibility = View.GONE }, delayMs)
                }
                (activity as? PhotoLoadingHost)?.setPhotoLoading(false)
                return false
            }
        })
        builder.into(photoView)
    }

    private fun renderStoredPhotoSlots() {
        applyPhotoToSlot(1, photoUriSlot1, notifyDraftChanged = false)
        applyPhotoToSlot(2, photoUriSlot2, notifyDraftChanged = false)
        applyPhotoToSlot(3, photoUriSlot3, notifyDraftChanged = false)
    }

    private fun applyPhotoToSlot(slot: Int, uri: Uri?, notifyDraftChanged: Boolean) {
        if (uri == null) {
            clearPhotoSlot(slot, notifyDraftChanged)
            return
        }

        val context = context
        if (context != null && !PhotoUploadValidator.isUsableForUpload(context, uri.toString())) {
            if (notifyDraftChanged) {
                clearPhotoSlot(slot, notifyDraftChanged)
            } else {
                when (slot) {
                    1 -> {
                        photoUriSlot1 = uri
                        showPhoto(1, binding.ivPhotoSlot1, binding.placeholderSlot1, binding.pbPhotoLoading1, null)
                    }
                    2 -> {
                        photoUriSlot2 = uri
                        showPhoto(2, binding.ivPhotoSlot2, binding.placeholderSlot2, binding.pbPhotoLoading2, null)
                    }
                    3 -> {
                        photoUriSlot3 = uri
                        showPhoto(3, binding.ivPhotoSlot3, binding.placeholderSlot3, binding.pbPhotoLoading3, null)
                    }
                }
                (activity as? PhotoLoadingHost)?.setPhotoLoading(false)
            }
            return
        }

        when (slot) {
            1 -> {
                photoUriSlot1 = uri
                showPhoto(1, binding.ivPhotoSlot1, binding.placeholderSlot1, binding.pbPhotoLoading1, uri)
                binding.btnDeleteSlot1.visibility = View.VISIBLE
            }
            2 -> {
                photoUriSlot2 = uri
                showPhoto(2, binding.ivPhotoSlot2, binding.placeholderSlot2, binding.pbPhotoLoading2, uri)
                binding.btnDeleteSlot2.visibility = View.VISIBLE
            }
            3 -> {
                photoUriSlot3 = uri
                showPhoto(3, binding.ivPhotoSlot3, binding.placeholderSlot3, binding.pbPhotoLoading3, uri)
                binding.btnDeleteSlot3.visibility = View.VISIBLE
            }
        }
        if (notifyDraftChanged) notifyDraftChanged()
    }

    private fun clearPhotoSlot(slot: Int, notifyDraftChanged: Boolean) {
        when (slot) {
            1 -> {
                photoUriSlot1 = null
                showPhoto(1, binding.ivPhotoSlot1, binding.placeholderSlot1, binding.pbPhotoLoading1, null)
                binding.btnDeleteSlot1.visibility = View.GONE
            }
            2 -> {
                photoUriSlot2 = null
                showPhoto(2, binding.ivPhotoSlot2, binding.placeholderSlot2, binding.pbPhotoLoading2, null)
                binding.btnDeleteSlot2.visibility = View.GONE
            }
            3 -> {
                photoUriSlot3 = null
                showPhoto(3, binding.ivPhotoSlot3, binding.placeholderSlot3, binding.pbPhotoLoading3, null)
                binding.btnDeleteSlot3.visibility = View.GONE
            }
        }
        (activity as? PhotoLoadingHost)?.setPhotoLoading(false)
        if (!suppressDraftChangeCallback) {
            draftChangeHost?.onPendingPhotoDraftChanged(slot, null)
        }
        if (notifyDraftChanged) notifyDraftChanged()
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
        val context = context ?: return "照片頁尚未準備完成"
        if (!PhotoUploadValidator.isUsableForUpload(context, photoUriSlot1?.toString())) {
            return "測量位置及側溝概況（第1張）"
        }
        if (!PhotoUploadValidator.isUsableForUpload(context, photoUriSlot2?.toString())) {
            return "側溝內徑寬度尺寸（第2張）"
        }
        if (!PhotoUploadValidator.isUsableForUpload(context, photoUriSlot3?.toString())) {
            return "側溝深度尺寸（第3張）"
        }
        return null
    }

    /** 傳回三個照片的 URI 字串（無照片或檔案不存在則為 null）。 */
    fun getPhotoPaths(): Triple<String?, String?, String?> = Triple(
        photoUriSlot1?.toString(),
        photoUriSlot2?.toString(),
        photoUriSlot3?.toString()
    )

    /**
     * 將已正規化後的草稿照片路徑回寫到 Fragment 狀態。
     * 不再次觸發草稿回寫，避免 Activity 在同步草稿後形成遞迴。
     */
    fun syncPersistedPhotoPaths(photo1: String?, photo2: String?, photo3: String?) {
        suppressDraftChangeCallback = true
        try {
            photoUriSlot1 = parseUriString(photo1)
            photoUriSlot2 = parseUriString(photo2)
            photoUriSlot3 = parseUriString(photo3)
            if (_binding != null) {
                renderStoredPhotoSlots()
            }
        } finally {
            suppressDraftChangeCallback = false
        }
    }

    /**
     * 匯入既有點位資料後，將下載完成的照片 URI 預填入三個欄位。
     * @param photo1-3 內容 URI（content:// 或 file://），null/空字串表示該欄位仍需補拍
     */
    fun prefillPhotos(photo1: String?, photo2: String?, photo3: String?) {
        photoUriSlot1 = parseUriString(photo1)
        photoUriSlot2 = parseUriString(photo2)
        photoUriSlot3 = parseUriString(photo3)
        if (_binding != null) {
            renderStoredPhotoSlots()

            // 依目前模式更新刪除按鈕狀態
            val isViewMode = arguments?.getBoolean(ARG_VIEW_MODE) ?: false
            setEditable(!isViewMode)
        }
        notifyDraftChanged()
    }

    private fun notifyDraftChanged() {
        onDraftChanged?.invoke()
        draftChangeHost?.onPhotosDraftChanged(
            photoUriSlot1?.toString(),
            photoUriSlot2?.toString(),
            photoUriSlot3?.toString()
        )
    }

    private fun parseUriString(uriString: String?): Uri? =
        uriString?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
}
