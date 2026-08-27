package com.example.taoyuangutter

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainShellActivityTest {

    @Test
    fun shellActivityInflatesBottomNavigation() {
        ActivityScenario.launch(MainShellActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity.findViewById<android.view.View>(R.id.bottomNav))
            }
        }
    }
}
