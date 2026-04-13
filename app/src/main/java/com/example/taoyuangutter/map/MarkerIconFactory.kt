package com.example.taoyuangutter.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.example.taoyuangutter.R
import com.example.taoyuangutter.gutter.WaypointType
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

object MarkerIconFactory {
    fun normal(context: Context, type: WaypointType): BitmapDescriptor =
        fromVector(context, resIdFor(type))

    fun enlarged(context: Context, type: WaypointType, scale: Float = 1.5f): BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(context, resIdFor(type))
            ?: return BitmapDescriptorFactory.defaultMarker()
        val width = (drawable.intrinsicWidth * scale).toInt().coerceAtLeast(1)
        val height = (drawable.intrinsicHeight * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun resIdFor(type: WaypointType): Int = when (type) {
        WaypointType.START -> R.drawable.ic_legend_start
        WaypointType.NODE -> R.drawable.ic_legend_node
        WaypointType.END -> R.drawable.ic_legend_end
    }

    private fun fromVector(context: Context, resId: Int): BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(context, resId)
            ?: return BitmapDescriptorFactory.defaultMarker()
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}

