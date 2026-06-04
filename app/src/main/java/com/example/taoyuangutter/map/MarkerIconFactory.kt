package com.example.taoyuangutter.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.example.taoyuangutter.R
import com.example.taoyuangutter.gutter.WaypointType
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

class MarkerIconFactory(
    private val context: Context
) {
    companion object {
        fun normal(
            context: Context,
            type: WaypointType,
            isPendingDeploy: Boolean = false,
            isVirtual: Boolean = false
        ): BitmapDescriptor {
            return MarkerIconFactory(context).icon(type, isPendingDeploy, isVirtual)
        }

        fun enlarged(
            context: Context,
            type: WaypointType,
            isPendingDeploy: Boolean = false,
            isVirtual: Boolean = false
        ): BitmapDescriptor {
            return MarkerIconFactory(context).enlargedIcon(type, isPendingDeploy, isVirtual)
        }
    }

    fun icon(
        type: WaypointType,
        isPendingDeploy: Boolean = false,
        isVirtual: Boolean = false
    ): BitmapDescriptor {
        val resId = markerResId(type, isPendingDeploy, isVirtual)
        val drawable = ContextCompat.getDrawable(context, resId)
            ?: return BitmapDescriptorFactory.defaultMarker()

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    fun enlargedIcon(
        type: WaypointType,
        isPendingDeploy: Boolean = false,
        isVirtual: Boolean = false
    ): BitmapDescriptor {
        val resId = markerResId(type, isPendingDeploy, isVirtual)
        val drawable = ContextCompat.getDrawable(context, resId)
            ?: return BitmapDescriptorFactory.defaultMarker()

        val scale = 1.5f
        val width = (drawable.intrinsicWidth * scale).toInt()
        val height = (drawable.intrinsicHeight * scale).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun markerResId(
        type: WaypointType,
        isPendingDeploy: Boolean,
        isVirtual: Boolean
    ): Int = when (type) {
        WaypointType.START -> if (isPendingDeploy) R.drawable.ic_legend_start_pending else R.drawable.ic_legend_start
        WaypointType.NODE -> when {
            isPendingDeploy -> R.drawable.ic_legend_node_pending
            isVirtual -> R.drawable.ic_legend_node_virtual
            else -> R.drawable.ic_legend_node
        }
        WaypointType.END -> if (isPendingDeploy) R.drawable.ic_legend_end_pending else R.drawable.ic_legend_end
    }
}
