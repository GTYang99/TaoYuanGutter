package com.example.taoyuangutter.gutter

import android.content.Context
import androidx.camera.core.CameraUnavailableException
import androidx.camera.core.CameraXConfig
import androidx.camera.core.InitializationException
import androidx.camera.core.RetryPolicy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraProviderInitializationUiTest {

    @Test
    fun providerInitializationFailureDoesNotTerminateFormActivity() {
        ProcessCameraProvider.configureInstance(
            CameraXConfig.Builder()
                .setCameraFactoryProvider { _, _, _, _ ->
                    throw InitializationException(
                        "Injected CameraX provider initialization failure",
                        CameraUnavailableException(CameraUnavailableException.CAMERA_ERROR)
                    )
                }
                .setCameraProviderInitRetryPolicy(RetryPolicy.NEVER)
                .build()
        )

        val context = ApplicationProvider.getApplicationContext<Context>()
        val outputFile = File(context.cacheDir, "camera-provider-failure.jpg")
        val intent = GutterFormActivity.newIntent(
            context,
            arrayListOf("test"),
            doubleArrayOf(0.0),
            doubleArrayOf(0.0),
            basicData = hashMapOf("NODE_TYP" to "1", "IS_CANTOPEN" to ""),
            sessionDraftId = 0L,
            sessionIsOffline = true
        )

        ActivityScenario.launch<GutterFormActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                activity.showCameraOverlay(1, outputFile.absolutePath)
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertEquals(androidx.lifecycle.Lifecycle.State.RESUMED, scenario.state)
        }

        outputFile.delete()
    }
}
