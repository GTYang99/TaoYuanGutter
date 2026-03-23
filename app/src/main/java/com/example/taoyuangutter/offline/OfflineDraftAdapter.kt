package com.example.taoyuangutter.offline

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.taoyuangutter.databinding.ItemOfflineDraftBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OfflineDraftAdapter(
    val items: MutableList<OfflineDraft>,
    private val onItemClick: (OfflineDraft) -> Unit,
    private val onDeleteClick: (OfflineDraft) -> Unit
) : RecyclerView.Adapter<OfflineDraftAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())

    inner class ViewHolder(val binding: ItemOfflineDraftBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOfflineDraftBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val draft = items[position]
        with(holder.binding) {
            tvDraftGutterId.text  = draft.gutterId.ifEmpty { "（未填側溝編號）" }
            tvDraftGutterType.text = draft.gutterType.ifEmpty { "（未填側溝形式）" }
            tvDraftDate.text = dateFormat.format(Date(draft.savedAt))

            root.setOnClickListener { onItemClick(draft) }
            btnDeleteDraft.setOnClickListener { onDeleteClick(draft) }
        }
    }

    /** 從列表移除指定 draft 並更新 RecyclerView。 */
    fun removeItem(draft: OfflineDraft) {
        val idx = items.indexOfFirst { it.id == draft.id }
        if (idx >= 0) {
            items.removeAt(idx)
            notifyItemRemoved(idx)
        }
    }
}
