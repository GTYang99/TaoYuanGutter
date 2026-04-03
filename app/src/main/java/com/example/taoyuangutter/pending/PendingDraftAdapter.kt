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
            // 標題：
            // - 離線草稿：固定顯示「離線草稿」
            // - 其他：優先顯示 START waypoint 的 SPI_NUM；沒有值時顯示「側溝草稿」
            if (draft.isOffline) {
                tvPendingDraftTitle.text = "離線草稿"
            } else {
                val startWp = draft.waypoints.firstOrNull { it.type == "START" }
                val gutterId = startWp?.basicData?.get("SPI_NUM")?.takeIf { it.isNotEmpty() }
                tvPendingDraftTitle.text = gutterId ?: "側溝草稿"
            }

            // 副標題1：建立時間（精確到分）
            tvPendingDraftTime.text = "建立時間：${dateFormat.format(Date(draft.savedAt))}"

            // 副標題2：已存節點數量（以「非空資料」點位數計算）
            // 規則：任一點位有座標，或 basicData 任一欄位有值，就視為非空。
            val nonEmptyCount = draft.waypoints.count { wp ->
                val hasLatLng = wp.latitude != null && wp.longitude != null
                val hasAnyBasic = wp.basicData.any { (_, v) -> v.isNotBlank() }
                hasLatLng || hasAnyBasic
            }
            tvPendingDraftNodes.text = "已存節點數量：$nonEmptyCount 個"

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
