package com.example.taoyuangutter.gutter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import android.os.Bundle

class GutterFormPagerAdapter(
    activity: FragmentActivity,
    private val latitude: Double,
    private val longitude: Double
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> GutterBasicInfoFragment.newInstance(latitude, longitude)
        1 -> GutterPhotosFragment.newInstance()
        else -> throw IllegalArgumentException("Unknown page $position")
    }
}
