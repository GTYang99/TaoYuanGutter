package com.example.taoyuangutter.gutter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taoyuangutter.R
import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.api.GutterRepository
import com.example.taoyuangutter.api.NodeDetails
import com.example.taoyuangutter.databinding.ActivityImportExistingWaypointBinding
import com.example.taoyuangutter.login.LoginActivity
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ImportExistingWaypointActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImportExistingWaypointBinding
    private val gutterRepository = GutterRepository()
    private lateinit var adapter: ImportWaypointAdapter
    private var selectedWaypoint: NodeDetails? = null
    private var searchJob: Job? = null
    private var searchSeq: Int = 0

    companion object {
        const val EXTRA_NODE_DETAILS_JSON = "node_details_json"

        fun newIntent(context: Context): Intent {
            return Intent(context, ImportExistingWaypointActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImportExistingWaypointBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        showHintState()
    }

    private fun setupUI() {
        // 返回按鈕 → 取消，不帶資料
        binding.btnBack.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        // 搜尋欄：按鍵盤確認鍵（🔍）才觸發篩選
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                performSearch(binding.etSearch.text.toString())
                true
            } else {
                false
            }
        }

        // 初始化 adapter：進頁不打 API，先顯示提示 item
        adapter = ImportWaypointAdapter(
            rows = listOf(ImportWaypointAdapter.Row.State("請輸入座標編號 (XY_NUM) 後按搜尋")),
            onItemSelected = { waypoint -> onWaypointSelected(waypoint) }
        )
        binding.rvWaypoints.layoutManager = LinearLayoutManager(this)
        binding.rvWaypoints.adapter = adapter

        // 浮動「匯入」按鈕：初始禁用，選中點位後才啟用
        binding.fabImport.isEnabled = false
        binding.fabImport.setOnClickListener {
            val waypoint = selectedWaypoint ?: return@setOnClickListener
            val json = Gson().toJson(waypoint)
            val intent = Intent().apply {
                putExtra(EXTRA_NODE_DETAILS_JSON, json)
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    // 使用者選中某一筆點位
    private fun onWaypointSelected(waypoint: NodeDetails) {
        selectedWaypoint = waypoint
        // 啟用「匯入」按鈕，改為主色
        binding.fabImport.isEnabled = true
        binding.fabImport.backgroundTintList =
            android.content.res.ColorStateList.valueOf(getColor(R.color.colorPrimary))
    }

    private fun resetImportButton() {
        selectedWaypoint = null
        binding.fabImport.isEnabled = false
        binding.fabImport.backgroundTintList =
            android.content.res.ColorStateList.valueOf(getColor(R.color.border_grey))
    }

    private fun showHintState() {
        resetImportButton()
        // 不使用空狀態容器，改用 RecyclerView 的單一提示 item
        binding.emptyStateContainer.visibility = View.GONE
        adapter.updateRows(
            listOf(ImportWaypointAdapter.Row.State("請輸入座標編號 (XY_NUM) 後按搜尋"))
        )
    }

    private fun showEmptyState() {
        resetImportButton()
        binding.emptyStateContainer.visibility = View.GONE
        adapter.updateRows(listOf(ImportWaypointAdapter.Row.State("無資料")))
    }

    private fun showErrorState(message: String) {
        resetImportButton()
        binding.emptyStateContainer.visibility = View.GONE
        adapter.updateRows(listOf(ImportWaypointAdapter.Row.State(message)))
    }

    private fun updateResultRows(results: List<NodeDetails>) {
        resetImportButton()
        binding.emptyStateContainer.visibility = View.GONE
        adapter.updateRows(results.map { ImportWaypointAdapter.Row.Waypoint(it) })
    }

    private fun performSearch(rawQuery: String) {
        val query = rawQuery.trim()
        if (query.isEmpty()) {
            showHintState()
            return
        }

        val token = LoginActivity.getSavedToken(this)
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.msg_login_first), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 取消上一次搜尋，避免快速連點造成 UI 被舊結果覆蓋
        searchJob?.cancel()
        resetImportButton()
        binding.loadingOverlay.visibility = View.VISIBLE
        val seq = ++searchSeq

        searchJob = lifecycleScope.launch {
            try {
                // 以 XY_NUM 查詢：GET /api/v1/node/nodeDetails?XY_NUM=...
                val result = gutterRepository.getNodeDetailsByXyNum(query, token)
                if (seq != searchSeq) return@launch
                when (result) {
                    is ApiResult.Success -> {
                        val list = result.data.data ?: emptyList()
                        if (list.isEmpty()) showEmptyState() else updateResultRows(list)
                    }
                    is ApiResult.Error -> {
                        Toast.makeText(
                            this@ImportExistingWaypointActivity,
                            "載入失敗：${result.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        showErrorState("載入失敗：${result.message}")
                    }
                }
            } catch (e: CancellationException) {
                // Ignore: user started another search or left the page.
            } catch (e: Exception) {
                Toast.makeText(
                    this@ImportExistingWaypointActivity,
                    "發生錯誤：${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                showErrorState("發生錯誤：${e.message ?: "未知錯誤"}")
            } finally {
                if (seq == searchSeq) {
                    binding.loadingOverlay.visibility = View.GONE
                }
            }
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }

    // 按返回鍵 = 取消，不帶資料回上一頁
    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        super.onBackPressed()
    }
}
