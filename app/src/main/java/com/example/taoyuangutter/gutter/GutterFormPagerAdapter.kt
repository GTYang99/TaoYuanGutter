package com.example.taoyuangutter.gutter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class GutterFormPagerAdapter(
    private val fragmentActivity: FragmentActivity,
    private val latitude: Double,
    private val longitude: Double,
    private val viewMode: Boolean = false,
    private val basicData: HashMap<String, String> = hashMapOf()
) : FragmentStateAdapter(fragmentActivity) {

    // 保留 Fragment 引用，供外部切換編輯模式時直接呼叫
    private val fragments = arrayOfNulls<Fragment>(2)

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        val frag: Fragment = when (position) {
            0 -> GutterBasicInfoFragment.newInstance(latitude, longitude, viewMode, basicData)
            1 -> GutterPhotosFragment.newInstance(
                    viewMode,
                    photo1 = basicData["photo1"],
                    photo2 = basicData["photo2"],
                    photo3 = basicData["photo3"]
                 )
            else -> throw IllegalArgumentException("Unknown page $position")
        }
        fragments[position] = frag
        return frag
    }

    /**
     * FragmentStateAdapter 在系統重建 Fragment 時不會呼叫 createFragment()，
     * 直接從 FragmentManager 恢復。此時 fragments[0] 為 null，
     * 所以加上 findFragmentByTag("f{itemId}") 作為 fallback。
     */
    fun getBasicInfoFragment(): GutterBasicInfoFragment? =
        (fragments[0] as? GutterBasicInfoFragment)
            ?: fragmentActivity.supportFragmentManager
                   .findFragmentByTag("f${getItemId(0)}") as? GutterBasicInfoFragment

    fun getPhotosFragment(): GutterPhotosFragment? =
        (fragments[1] as? GutterPhotosFragment)
            ?: fragmentActivity.supportFragmentManager
                   .findFragmentByTag("f${getItemId(1)}") as? GutterPhotosFragment
}
