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

    /** false = 隱藏拖曳把手（檢視模式） */
    var showDragHandle: Boolean = true

    /** 點擊「刪除」按鈕的回呼 */
    var onSwipeDeleteClick: ((position: Int) -> Unit)? = null

    inner class ViewHolder(val binding: ItemWaypointBinding) :
        RecyclerView.ViewHolder(binding.root) {
        /** 供 ItemTouchHelper 直接操作前景平移 */
        val foreground: View get() = binding.layoutForeground
    }

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

        // 拖曳把手：依 showDragHandle 顯示或隱藏
        holder.binding.ivDragHandle.visibility = if (showDragHandle) View.VISIBLE else View.GONE
        holder.binding.ivDragHandle.setOnTouchListener { _, event ->
            if (showDragHandle && event.actionMasked == MotionEvent.ACTION_DOWN) {
                startDragListener?.invoke(holder)
            }
            false
        }

        // 刪除按鈕：NODE 型別設 VISIBLE（白色前景覆蓋，左滑時自然曝光）；其他型別 GONE
        if (item.type == WaypointType.NODE) {
            holder.binding.tvDeleteAction.visibility = View.VISIBLE
            // ── 關鍵修正：ItemTouchHelper 會在 ACTION_DOWN 時搶走觸摸事件，
            //   導致 click listener 永遠收不到。先呼叫 requestDisallowInterceptTouchEvent
            //   告知 RecyclerView 及其所有父層（如 BottomSheet）本次觸摸不屬於滑動手勢，讓事件正常派發給按鈕。
            holder.binding.tvDeleteAction.setOnTouchListener { v, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                }
                false  // 不消費事件，繼續傳遞給 onClick
            }
            holder.binding.tvDeleteAction.setOnClickListener {
                val pos = holder.adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onSwipeDeleteClick?.invoke(pos)
                }
            }
        } else {
            holder.binding.tvDeleteAction.visibility = View.GONE
            holder.binding.tvDeleteAction.setOnTouchListener(null)
            holder.binding.tvDeleteAction.setOnClickListener(null)
        }

        // 重設前景位移（避免 RecyclerView 重用時殘留位移）
        holder.binding.layoutForeground.translationX = 0f

        // 點選整行 → 請求選點
        holder.binding.root.setOnClickListener {
            onItemClick(holder.adapterPosition)
        }
    }

    override fun getItemCount(): Int = items.size
}
