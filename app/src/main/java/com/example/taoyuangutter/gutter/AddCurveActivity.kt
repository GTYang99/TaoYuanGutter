package com.example.taoyuangutter.gutter

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.taoyuangutter.R
import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.api.GutterRepository
import com.example.taoyuangutter.login.LoginActivity
import com.example.taoyuangutter.pending.GutterSessionDraft
import com.example.taoyuangutter.pending.GutterSessionRepository
import com.example.taoyuangutter.pending.KIND_CURVE
import com.example.taoyuangutter.pending.WaypointSnapshot
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * 新增曲線 Activity
 *
 * 使用者輸入起點與終點的 XY_NUM（測量座標編號），
 * 打 API 查詢對應的曲線資料並顯示結果。
 *
 * TODO: 接入真實 API 端點
 */
class AddCurveActivity : AppCompatActivity() {

    // ── Views ────────────────────────────────────────────
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tilStartXyNum: TextInputLayout
    private lateinit var etStartXyNum: TextInputEditText
    private lateinit var tilEndXyNum: TextInputLayout
    private lateinit var etEndXyNum: TextInputEditText
    private lateinit var btnQuery: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var cardResult: CardView
    private lateinit var tvResultTitle: TextView
    private lateinit var tvResultContent: TextView

    private val repository = GutterRepository()
    private val draftRepo by lazy { GutterSessionRepository(this) }

    private var forceOfflineMode: Boolean = false
    private var draftId: Long = 0L

    // ── Lifecycle ────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_curve)

        bindViews()
        setupToolbar()
        readIntentAndPrefill()
        setupQueryButton()
    }

    // ── Init ─────────────────────────────────────────────
    private fun bindViews() {
        toolbar = findViewById(R.id.toolbar)
        tilStartXyNum = findViewById(R.id.tilStartXyNum)
        etStartXyNum = findViewById(R.id.etStartXyNum)
        tilEndXyNum = findViewById(R.id.tilEndXyNum)
        etEndXyNum = findViewById(R.id.etEndXyNum)
        btnQuery = findViewById(R.id.btnQuery)
        progressBar = findViewById(R.id.progressBar)
        cardResult = findViewById(R.id.cardResult)
        tvResultTitle = findViewById(R.id.tvResultTitle)
        tvResultContent = findViewById(R.id.tvResultContent)
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun readIntentAndPrefill() {
        forceOfflineMode = intent.getBooleanExtra(EXTRA_FORCE_OFFLINE_MODE, false)
        draftId = intent.getLongExtra(EXTRA_DRAFT_ID, 0L)

        val prefillStart = intent.getStringExtra(EXTRA_PREFILL_START_XY_NUM)?.trim().orEmpty()
        val prefillEnd = intent.getStringExtra(EXTRA_PREFILL_END_XY_NUM)?.trim().orEmpty()
        if (prefillStart.isNotEmpty()) etStartXyNum.setText(prefillStart)
        if (prefillEnd.isNotEmpty()) etEndXyNum.setText(prefillEnd)
    }

    private fun setupQueryButton() {
        btnQuery.setOnClickListener {
            // 清除舊的錯誤提示
            tilStartXyNum.error = null
            tilEndXyNum.error = null

            val startXyNum = etStartXyNum.text?.toString()?.trim().orEmpty()
            val endXyNum = etEndXyNum.text?.toString()?.trim().orEmpty()

            // ── 驗證 ────────────────────────────────────
            if (startXyNum.isEmpty()) {
                tilStartXyNum.error = getString(R.string.curve_xy_num_required)
                return@setOnClickListener
            }
            if (endXyNum.isEmpty()) {
                tilEndXyNum.error = getString(R.string.curve_xy_num_required)
                return@setOnClickListener
            }

            // 離線模式：不打 API，直接存草稿
            if (forceOfflineMode) {
                saveCurveDraft(startXyNum, endXyNum, isOffline = true)
                Toast.makeText(this, getString(R.string.msg_draft_saved), Toast.LENGTH_SHORT).show()
                finish()
                return@setOnClickListener
            }

            val token = LoginActivity.getSavedToken(this)
            if (token.isNullOrEmpty()) {
                showError(getString(R.string.msg_login_first))
                return@setOnClickListener
            }

            // ── 呼叫 API ───────────────────────────────
            storeCurveDitch(startXyNum, endXyNum, token)
        }
    }

    // ── API ──────────────────────────────────────────────

    /**
     * 新增曲線側溝：
     * POST /api/v1/ditch/storeCurveDitch
     */
    private fun storeCurveDitch(startXyNum: String, endXyNum: String, token: String) {
        // 顯示 loading
        progressBar.visibility = View.VISIBLE
        cardResult.visibility = View.GONE
        btnQuery.isEnabled = false

        lifecycleScope.launch {
            try {
                val xyNums = listOf(startXyNum, endXyNum)
                when (val result = repository.storeCurveDitch(xyNums, token)) {
                    is ApiResult.Success -> {
                        progressBar.visibility = View.GONE
                        btnQuery.isEnabled = true

                        // 若是從草稿進來（或先前失敗存草稿），成功後刪除該草稿
                        if (draftId > 0L) {
                            draftRepo.delete(draftId)
                        }

                        val ditch = result.data.data
                        val spiNum = ditch?.spiNum ?: ""

                        Toast.makeText(this@AddCurveActivity, "新增成功：$spiNum", Toast.LENGTH_LONG).show()
                        setResult(RESULT_OK, android.content.Intent().putExtra(EXTRA_RESULT_SPI_NUM, spiNum))
                        // 回地圖刷新列表（MainActivity 會接 result）
                        finish()
                    }
                    is ApiResult.Error -> {
                        // 網路/後端失敗：存成待上傳草稿（弧線草稿）
                        saveCurveDraft(startXyNum, endXyNum, isOffline = false)
                        showError(result.message)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 任何例外：也存草稿，避免閃退/資料流失
                saveCurveDraft(startXyNum, endXyNum, isOffline = false)
                showError(e.message ?: "網路連線失敗")
            }
        }
    }

    private fun saveCurveDraft(startXyNum: String, endXyNum: String, isOffline: Boolean) {
        val now = System.currentTimeMillis()
        val resolvedId = if (draftId > 0L) draftId else now
        draftId = resolvedId

        val snapshots = listOf(
            WaypointSnapshot(
                type = WaypointType.START.name,
                label = "起點",
                latitude = null,
                longitude = null,
                basicData = hashMapOf("XY_NUM" to startXyNum)
            ),
            WaypointSnapshot(
                type = WaypointType.END.name,
                label = "終點",
                latitude = null,
                longitude = null,
                basicData = hashMapOf("XY_NUM" to endXyNum)
            )
        )
        draftRepo.save(
            GutterSessionDraft(
                id = resolvedId,
                savedAt = now,
                kind = KIND_CURVE,
                isOffline = isOffline,
                isSinglePoint = false,
                waypoints = snapshots
            )
        )
    }

    // ── UI Helpers ───────────────────────────────────────

    /** 顯示查詢成功結果 */
    private fun showResult(content: String) {
        cardResult.visibility = View.VISIBLE
        tvResultTitle.text = getString(R.string.curve_result_title)
        tvResultContent.text = content
    }

    /** 顯示錯誤訊息 */
    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        btnQuery.isEnabled = true
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_RESULT_SPI_NUM = "ex_result_spi_num"
        const val EXTRA_FORCE_OFFLINE_MODE = "ex_force_offline_mode"
        const val EXTRA_DRAFT_ID = "ex_draft_id"
        const val EXTRA_PREFILL_START_XY_NUM = "ex_prefill_start_xy_num"
        const val EXTRA_PREFILL_END_XY_NUM = "ex_prefill_end_xy_num"
    }
}
