package com.example.taoyuangutter.gutter

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.taoyuangutter.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

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

    // ── Lifecycle ────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_curve)

        bindViews()
        setupToolbar()
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

            // ── 呼叫 API（目前為 placeholder）─────────────
            queryCurve(startXyNum, endXyNum)
        }
    }

    // ── API ──────────────────────────────────────────────

    /**
     * 打 API 查詢曲線資料。
     *
     * TODO: 替換為真實 API 呼叫
     *   1. 在 GutterApiService 新增端點定義
     *   2. 在 GutterRepository 新增 suspend fun queryCurve(...)
     *   3. 此處改為 lifecycleScope.launch { repository.queryCurve(...) }
     *
     * @param startXyNum 起點測量座標編號
     * @param endXyNum   終點測量座標編號
     */
    private fun queryCurve(startXyNum: String, endXyNum: String) {
        // 顯示 loading
        progressBar.visibility = View.VISIBLE
        cardResult.visibility = View.GONE
        btnQuery.isEnabled = false

        // ──────────────────────────────────────────────────
        // TODO: 替換以下 placeholder 為真實 API 呼叫
        //
        // lifecycleScope.launch {
        //     try {
        //         val result = gutterRepository.queryCurve(startXyNum, endXyNum)
        //         when (result) {
        //             is ApiResult.Success -> showResult(result.data)
        //             is ApiResult.Error   -> showError(result.message)
        //         }
        //     } catch (e: Exception) {
        //         showError(e.message ?: "未知錯誤")
        //     }
        // }
        // ──────────────────────────────────────────────────

        // Placeholder：模擬 API 延遲後顯示假資料
        btnQuery.postDelayed({
            progressBar.visibility = View.GONE
            btnQuery.isEnabled = true
            showResult("起點 XY_NUM: $startXyNum\n終點 XY_NUM: $endXyNum\n\n（API 尚未串接，此為預覽畫面）")
        }, 800)
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
}
