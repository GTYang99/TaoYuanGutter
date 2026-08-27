package com.example.taoyuangutter

import android.view.LayoutInflater
import android.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainShellActivityTest {

    @Test
    fun shellLayoutInflatesBottomNavigation() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val themedContext = ContextThemeWrapper(context, R.style.Theme_TaoYuanGutter)
        val root = LayoutInflater.from(themedContext).inflate(R.layout.activity_main_shell, null, false)
        val bottomNav = root.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)

        assertNotNull(bottomNav)
        assertEquals(2, bottomNav.menu.size())
    }
}
