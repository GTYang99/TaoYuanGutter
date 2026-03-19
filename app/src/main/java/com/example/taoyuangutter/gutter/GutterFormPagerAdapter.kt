package com.example.taoyuangutter.gutter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class GutterFormPagerAdapter(
    activity: FragmentActivity,
    private val latitude: Double,
    private val longitude: Double,
    private val viewMode: Boolean = false,
    private val basicData: HashMap<String, String> = hashMapOf()
) : FragmentStateAdapter(activity) {

    // 保留 Fragment 引用，供外部切換編輯模式時直接呼叫
    private val fragments = arrayOfNulls<Fragment>(2)

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        val frag: Fragment = when (position) {
            0 -> GutterBasicInfoFragment.newInstance(latitude, longitude, viewMode, basicData)
            1 -> GutterPhotosFragment.newInstance(viewMode)
            else -> throw IllegalArgumentException("Unknown page $position")
        }
        fragments[position] = frag
        return frag
    }

    fun getBasicInfoFragment(): GutterBasicInfoFragment? =
        fragments[0] as? GutterBasicInfoFragment

    fun getPhotosFragment(): GutterPhotosFragment? =
        fragments[1] as? GutterPhotosFragment
}
