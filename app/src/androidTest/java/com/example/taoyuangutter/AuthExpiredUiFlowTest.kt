package com.example.taoyuangutter

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.login.AuthExpiredHandler
import com.example.taoyuangutter.login.LoginActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class AuthExpiredUiFlowTest {
    @Test
    fun editScreen401SavesDraftShowsDialogAndReturnsToLogin() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val saveCount = AtomicInteger(0)
        val handlerRef = AtomicReference<AuthExpiredHandler>()
        val loginResumed = AtomicReference(false)
        val app = ApplicationProvider.getApplicationContext<Application>()
        val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) {
                if (activity is LoginActivity) loginResumed.set(true)
            }
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        }
        app.registerActivityLifecycleCallbacks(lifecycleCallbacks)

        val scenario = ActivityScenario.launch(MainShellActivity::class.java)
        try {
            val handledRef = AtomicReference<Boolean>()
            scenario.onActivity { activity ->
                val handler = AuthExpiredHandler(activity)
                handlerRef.set(handler)
                handledRef.set(
                    handler.handleIfAuthExpired(
                        error = ApiResult.Error(message = "Unauthorized", code = 401),
                        onSaveDraft = { saveCount.incrementAndGet() }
                    )
                )
            }
            instrumentation.waitForIdleSync()

            assertTrue(handledRef.get())
            assertEquals(1, saveCount.get())
            waitUntilDialogDisplayed(handlerRef)
            instrumentation.runOnMainSync {
                handlerRef.get().confirmDialogForTests()
            }

            waitUntilLoginActivityObserved(loginResumed)
        } finally {
            app.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
            scenario.close()
        }
    }

    private fun waitUntilDialogDisplayed(handlerRef: AtomicReference<AuthExpiredHandler>) {
        repeat(30) {
            val isDialogShowing = AtomicReference(false)
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                isDialogShowing.set(handlerRef.get().isDialogShowingForTests())
            }
            if (isDialogShowing.get()) return
            Thread.sleep(100)
        }
        throw AssertionError("Auth-expired dialog was not displayed")
    }

    private fun waitUntilLoginActivityObserved(loginResumed: AtomicReference<Boolean>) {
        repeat(30) {
            if (loginResumed.get()) return
            Thread.sleep(100)
        }
        assertTrue(loginResumed.get())
    }
}
