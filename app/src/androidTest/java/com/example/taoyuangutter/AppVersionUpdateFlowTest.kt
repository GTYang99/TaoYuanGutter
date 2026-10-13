package com.example.taoyuangutter

import android.app.Activity
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.taoyuangutter.api.AppVersionRecord
import com.example.taoyuangutter.api.AppVersionRepository
import com.example.taoyuangutter.api.AppVersionResponse
import com.example.taoyuangutter.api.GutterApiClient
import com.example.taoyuangutter.api.GutterApiService
import com.example.taoyuangutter.common.ApiBackendTarget
import com.example.taoyuangutter.common.AppUpdateFileDownloader
import kotlinx.coroutines.awaitCancellation
import okhttp3.HttpUrl
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class AppVersionUpdateFlowTest {
    @After fun resetHooks() {
        AppVersionUpdateTestHooks.repositoryFactory = null
        AppVersionUpdateTestHooks.downloaderFactory = null
        GutterApiClient.resetTargetForTests()
        // Allow the Application's process-background debounce to complete before the next case.
        Thread.sleep(900)
    }

    @Test fun foregroundChecksOnceAcrossActivityRecreationAndAgainAfterBackgroundReturn() {
        val calls = AtomicInteger()
        installRepository {
            calls.incrementAndGet()
            Response.success(currentVersionResponse())
        }
        val scenario = ActivityScenario.launch(com.example.taoyuangutter.login.LoginActivity::class.java)
        try {
            assertTrue(waitUntil(5_000) { calls.get() == 1 })
            scenario.recreate()
            Thread.sleep(350)
            assertEquals("Internal Activity recreation must not cause another foreground check", 1, calls.get())
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            Thread.sleep(1_000)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            assertTrue(waitUntil(5_000) { calls.get() == 2 })
        } finally {
            scenario.close()
        }
    }

    @Test fun continueDismissesPromptAndPreservesCurrentLoginScreenState() {
        val requestStarted = CountDownLatch(1)
        val allowResponse = CountDownLatch(1)
        installRepository {
            requestStarted.countDown()
            assertTrue(allowResponse.await(5, TimeUnit.SECONDS))
            Response.success(updateResponse("2.0.1"))
        }
        val scenario = ActivityScenario.launch(com.example.taoyuangutter.login.LoginActivity::class.java)
        try {
            assertTrue(requestStarted.await(5, TimeUnit.SECONDS))
            scenario.onActivity { activity ->
                activity.findViewById<EditText>(R.id.usernameEditText).setText("draft-user")
                activity.findViewById<EditText>(R.id.passwordEditText).setText("draft-password")
            }
            allowResponse.countDown()
            onView(withText("發現新版本 2.0.1")).check(matches(isDisplayed()))
            onView(withText("繼續使用")).perform(click())
            scenario.onActivity { activity ->
                assertEquals(com.example.taoyuangutter.login.LoginActivity::class.java, activity::class.java)
                assertEquals("draft-user", activity.findViewById<EditText>(R.id.usernameEditText).text.toString())
                assertEquals("draft-password", activity.findViewById<EditText>(R.id.passwordEditText).text.toString())
            }
        } finally {
            allowResponse.countDown()
            scenario.close()
        }
    }

    @Test fun cancellingDownloadReturnsToCurrentScreenWithoutFailurePrompt() {
        val downloadStarted = CountDownLatch(1)
        val downloadCancelled = CountDownLatch(1)
        installRepository { Response.success(updateResponse("2.0.1")) }
        AppVersionUpdateTestHooks.downloaderFactory = {
            object : AppUpdateFileDownloader {
                override suspend fun download(url: HttpUrl, destination: File, onProgress: (Long, Long) -> Unit): File {
                    downloadStarted.countDown()
                    onProgress(25, 100)
                    try {
                        awaitCancellation()
                    } finally {
                        downloadCancelled.countDown()
                    }
                }
            }
        }
        val scenario = ActivityScenario.launch(com.example.taoyuangutter.login.LoginActivity::class.java)
        try {
            onView(withText("發現新版本 2.0.1")).check(matches(isDisplayed()))
            onView(withText("前往更新")).perform(click())
            assertTrue(downloadStarted.await(5, TimeUnit.SECONDS))
            onView(withText("取消下載")).perform(click())
            assertTrue(downloadCancelled.await(5, TimeUnit.SECONDS))
            onView(withText("下載失敗")).check(doesNotExist())
            scenario.onActivity { activity -> assertEquals(com.example.taoyuangutter.login.LoginActivity::class.java, activity::class.java) }
        } finally {
            scenario.close()
        }
    }

    private fun installRepository(response: suspend () -> Response<AppVersionResponse>) {
        val delegate = GutterApiClient.serviceFor(ApiBackendTarget.BASE)
        val service = object : GutterApiService by delegate {
            override suspend fun getAppVersion(): Response<AppVersionResponse> = response()
        }
        AppVersionUpdateTestHooks.repositoryFactory = { AppVersionRepository { service } }
    }

    private fun currentVersionResponse() = AppVersionResponse(
        success = true,
        data = listOf(AppVersionRecord("android", BuildConfig.VERSION_NAME, "https://example.test/app.apk"))
    )

    private fun updateResponse(version: String) = AppVersionResponse(
        success = true,
        data = listOf(AppVersionRecord("android", version, "https://example.test/app.apk"))
    )

    private fun waitUntil(timeoutMs: Long, predicate: () -> Boolean): Boolean {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        while (System.nanoTime() < deadline) {
            if (predicate()) return true
            Thread.sleep(25)
        }
        return predicate()
    }
}
