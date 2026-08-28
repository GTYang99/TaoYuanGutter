package com.example.taoyuangutter

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
import com.example.taoyuangutter.dashboard.DashboardFragment
import com.example.taoyuangutter.databinding.ActivityMainShellBinding
import com.example.taoyuangutter.map.MapWorkspaceFragment

class MainShellActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainShellBinding
    private var currentTabId: Int = R.id.nav_map

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainShellBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets()
        setupBottomNav()

        currentTabId = savedInstanceState?.getInt(KEY_SELECTED_TAB) ?: R.id.nav_map
        if (savedInstanceState == null || supportFragmentManager.fragments.isEmpty()) {
            showTab(currentTabId)
        } else {
            binding.bottomNav.selectedItemId = currentTabId
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (currentTabId != R.id.nav_map) {
                        binding.bottomNav.selectedItemId = R.id.nav_map
                    } else {
                        finish()
                    }
                }
            }
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_SELECTED_TAB, currentTabId)
        super.onSaveInstanceState(outState)
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == currentTabId) {
                true
            } else {
                showTab(item.itemId)
                true
            }
        }
    }

    private fun showTab(tabId: Int) {
        currentTabId = tabId
        val fragment = fragmentFactoryForTests?.invoke(tabId) ?: when (tabId) {
            R.id.nav_dashboard -> DashboardFragment.newInstance()
            else -> MapWorkspaceFragment.newInstance()
        }
        supportFragmentManager.commitNow {
            setReorderingAllowed(true)
            replace(R.id.shell_container, fragment, tabTag(tabId))
        }
        if (binding.bottomNav.selectedItemId != tabId) {
            binding.bottomNav.menu.findItem(tabId)?.isChecked = true
        }
    }

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            binding.bottomNav.setPadding(
                binding.bottomNav.paddingLeft,
                binding.bottomNav.paddingTop,
                binding.bottomNav.paddingRight,
                bottom
            )
            insets
        }
    }

    private fun tabTag(tabId: Int): String = "tab:$tabId"

    companion object {
        private const val KEY_SELECTED_TAB = "selected_tab"
        @Volatile
        var fragmentFactoryForTests: ((Int) -> Fragment)? = null
    }
}
