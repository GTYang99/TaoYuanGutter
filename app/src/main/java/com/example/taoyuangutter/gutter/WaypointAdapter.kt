package com.example.taoyuangutter.gutter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.taoyuangutter.databinding.ItemWaypointBinding

class WaypointAdapter(
    private val items: MutableList<Waypoint>,
    private val onItemClick: (position: Int) -> Unit
) : RecyclerView.Adapter<WaypointAdapter.ViewHolder>() {

    /** 由 AddGutterBottomSheet 注入，用於啟動拖曳 */
    var startDragListener: ((RecyclerView.ViewHolder) -> Unit)? = null

    inner class ViewHolder(val binding: ItemWaypointBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWaypointBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val isFirst = position == 0
        val isLast  = position == items.size - 1

        // 節點標籤
        holder.binding.tvWaypointLabel.text = item.label

        // 座標提示文字
        holder.binding.tvWaypointHint.text = item.latLng?.let {
            "%.5f, %.5f".format(it.latitude, it.longitude)
        } ?: "點選以設定位置"

        // 連接線：第一個隱藏上半，最後一個隱藏下半
        holder.binding.viewLineTop.visibility    = if (isFirst) View.INVISIBLE else View.VISIBLE
        holder.binding.viewLineBottom.visibility = if (isLast)  View.INVISIBLE else View.VISIBLE

        // 彩色圓點
        val dotColor = when (item.type) {
            WaypointType.START -> Color.parseColor("#4CAF50") // 綠色
            WaypointType.NODE  -> Color.parseColor("#2196F3") // 藍色
            WaypointType.END   -> Color.parseColor("#F44336") // 紅色
        }
        holder.binding.viewDot.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(dotColor)
        }

        // 拖曳把手：僅節點顯示，觸碰時啟動拖曳
        if (item.type == WaypointType.NODE) {
            holder.binding.ivDragHandle.visibility = View.VISIBLE
            holder.binding.ivDragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    startDragListener?.invoke(holder)
                }
                false
            }
        } else {
            holder.binding.ivDragHandle.visibility = View.GONE
            holder.binding.ivDragHandle.setOnTouchListener(null)
        }

        // 點選整行 → 請求選點
        holder.binding.root.setOnClickListener {
            onItemClick(holder.adapterPosition)
        }
    }

    override fun getItemCount(): Int = items.size
}
