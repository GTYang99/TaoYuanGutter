package com.example.taoyuangutter.pending

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.taoyuangutter.databinding.ItemPendingDraftBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PendingDraftAdapter(
    val items: MutableList<GutterSessionDraft>,
    private val onItemClick: (GutterSessionDraft) -> Unit,
    private val onItemLongClick: (GutterSessionDraft) -> Unit = {}
) : RecyclerView.Adapter<PendingDraftAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())

    inner class ViewHolder(val binding: ItemPendingDraftBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPendingDraftBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val draft = items[position]
        with(holder.binding) {
            // 標題：優先顯示 START waypoint 的 SPI_NUM；沒有值時顯示固定文案
            val startWp = draft.waypoints.firstOrNull { it.type == "START" }
            val gutterId = startWp?.basicData?.get("SPI_NUM")?.takeIf { it.isNotEmpty() }
            tvPendingDraftTitle.text = gutterId ?: "側溝草稿"

            // 副標題1：建立時間（精確到分）
            tvPendingDraftTime.text = "建立時間：${dateFormat.format(Date(draft.savedAt))}"

            // 副標題2：已存節點數量（不含 START/END）
            val nodeCount = draft.waypoints.count { it.type == "NODE" } + 2
            tvPendingDraftNodes.text = "已存節點數量：$nodeCount 個"

            root.setOnClickListener { onItemClick(draft) }
            root.setOnLongClickListener {
                onItemLongClick(draft)
                true
            }
        }
    }

    fun removeItem(draft: GutterSessionDraft) {
        val idx = items.indexOfFirst { it.id == draft.id }
        if (idx >= 0) {
            items.removeAt(idx)
            notifyItemRemoved(idx)
        }
    }
}
