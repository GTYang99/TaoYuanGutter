package com.example.taoyuangutter.gutter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.taoyuangutter.databinding.ItemAddGutterListBinding
import com.example.taoyuangutter.pending.GutterSessionDraft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddGutterListAdapter(
    private val onClick: (GutterSessionDraft) -> Unit
) : RecyclerView.Adapter<AddGutterListAdapter.ViewHolder>() {
    private val items = mutableListOf<GutterSessionDraft>()
    private val timeFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())

    fun submitList(drafts: List<GutterSessionDraft>) {
        items.clear()
        items.addAll(drafts)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemAddGutterListBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val draft = items[position]
        holder.binding.tvAddGutterListTitle.text = "側溝草稿 ${position + 1}"
        holder.binding.tvAddGutterListTime.text = "建立時間：${timeFormat.format(Date(draft.createdAt))}"
        val count = draft.waypoints.count { waypoint ->
            (waypoint.latitude != null && waypoint.longitude != null) ||
                waypoint.basicData.any { (_, value) -> value.isNotBlank() }
        }
        holder.binding.tvAddGutterListNodes.text = "已存節點：$count"
        holder.itemView.setOnClickListener { onClick(draft) }
    }

    class ViewHolder(val binding: ItemAddGutterListBinding) : RecyclerView.ViewHolder(binding.root)
}
