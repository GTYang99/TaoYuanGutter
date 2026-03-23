package com.example.taoyuangutter.offline

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taoyuangutter.databinding.ActivityOfflineDraftsBinding
import com.example.taoyuangutter.gutter.GutterFormActivity

/**
 * 顯示所有已儲存的離線草稿列表。
 * 點選某筆草稿 → 在 GutterFormActivity（離線模式）開啟以檢視 / 編輯。
 */
class OfflineDraftsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOfflineDraftsBinding
    private lateinit var repo:    OfflineDraftRepository
    private lateinit var adapter: OfflineDraftAdapter

    private val editDraftLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // 編輯完回來後重新整理列表
        refreshList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOfflineDraftsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = OfflineDraftRepository(this)

        binding.btnDraftsBack.setOnClickListener { finish() }

        setupRecyclerView()
        refreshList()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun setupRecyclerView() {
        adapter = OfflineDraftAdapter(
            items        = mutableListOf(),
            onItemClick  = { draft -> openDraft(draft) },
            onDeleteClick = { draft -> confirmDelete(draft) }
        )
        binding.rvDrafts.apply {
            layoutManager = LinearLayoutManager(this@OfflineDraftsActivity)
            adapter = this@OfflineDraftsActivity.adapter
        }
    }

    private fun refreshList() {
        val drafts = repo.getAll()
        adapter.items.clear()
        adapter.items.addAll(drafts)
        adapter.notifyDataSetChanged()

        binding.tvEmptyDrafts.visibility = if (drafts.isEmpty()) View.VISIBLE else View.GONE
        binding.rvDrafts.visibility      = if (drafts.isEmpty()) View.GONE    else View.VISIBLE
    }

    private fun openDraft(draft: OfflineDraft) {
        val intent = GutterFormActivity.newOfflineIntent(this, draft.id)
        editDraftLauncher.launch(intent)
    }

    private fun confirmDelete(draft: OfflineDraft) {
        AlertDialog.Builder(this)
            .setTitle("刪除草稿")
            .setMessage("確定刪除「${draft.gutterId.ifEmpty { "此草稿" }}」？此動作無法復原。")
            .setPositiveButton("刪除") { _, _ ->
                repo.delete(draft.id)
                adapter.removeItem(draft)
                if (adapter.itemCount == 0) {
                    binding.tvEmptyDrafts.visibility = View.VISIBLE
                    binding.rvDrafts.visibility      = View.GONE
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
