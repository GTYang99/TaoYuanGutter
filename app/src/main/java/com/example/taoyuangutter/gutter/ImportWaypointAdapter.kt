package com.example.taoyuangutter.gutter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.taoyuangutter.api.NodeDetails
import com.example.taoyuangutter.databinding.ItemImportWaypointBinding
import com.example.taoyuangutter.databinding.ItemImportWaypointStateBinding

class ImportWaypointAdapter(
    private var rows: List<Row>,
    private val onItemSelected: (NodeDetails) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var selectedIndex = -1

    sealed class Row {
        data class Waypoint(val item: NodeDetails) : Row()
        data class State(val message: String) : Row()
    }

    inner class WaypointVH(private val binding: ItemImportWaypointBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: NodeDetails, isSelected: Boolean) {
            binding.apply {
                // 設置基本資訊
                tvXyNum.text = item.xyNum ?: "---"

                // 設置選擇狀態
                rbSelect.isChecked = isSelected

                // 點擊事件
                root.setOnClickListener {
                    val previousIndex = selectedIndex
                    selectedIndex = bindingAdapterPosition
                    notifyItemChanged(previousIndex)
                    notifyItemChanged(selectedIndex)
                    onItemSelected(item)
                }
            }
        }

        private fun getNodeTypeName(nodeTyp: String?): String {
            return when (nodeTyp?.toIntOrNull()) {
                1 -> "U型溝（明溝）"
                2 -> "U型溝（加蓋）"
                3 -> "L型溝與暗溝渠併用"
                4 -> "其他"
                else -> "---"
            }
        }

        private fun getMatTypName(matTyp: String?): String {
            return when (matTyp?.toIntOrNull()) {
                1 -> "混凝土"
                2 -> "卵礫石"
                3 -> "紅磚"
                else -> "---"
            }
        }
    }

    inner class StateVH(private val binding: ItemImportWaypointStateBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: String) {
            binding.tvMessage.text = message
            // state row 不可選
            binding.root.setOnClickListener(null)
        }
    }

    override fun getItemViewType(position: Int): Int = when (rows[position]) {
        is Row.Waypoint -> 1
        is Row.State -> 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> {
                val binding = ItemImportWaypointBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                WaypointVH(binding)
            }
            else -> {
                val binding = ItemImportWaypointStateBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                StateVH(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Waypoint -> (holder as WaypointVH).bind(row.item, position == selectedIndex)
            is Row.State -> (holder as StateVH).bind(row.message)
        }
    }

    override fun getItemCount(): Int = rows.size

    fun updateRows(newRows: List<Row>) {
        rows = newRows
        selectedIndex = -1
        notifyDataSetChanged()
    }

    fun getSelectedWaypoint(): NodeDetails? {
        val row = rows.getOrNull(selectedIndex) as? Row.Waypoint
        return row?.item
    }
}
