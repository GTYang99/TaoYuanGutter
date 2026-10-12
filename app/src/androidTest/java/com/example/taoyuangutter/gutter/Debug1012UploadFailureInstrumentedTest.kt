package com.example.taoyuangutter.gutter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.taoyuangutter.MainShellActivity
import com.example.taoyuangutter.R
import com.example.taoyuangutter.api.DitchNode
import com.example.taoyuangutter.api.GutterApiService
import com.example.taoyuangutter.api.GutterRepository
import com.example.taoyuangutter.api.StoreDitchResponse
import com.example.taoyuangutter.databinding.ActivityMainBinding
import com.example.taoyuangutter.main.MainBlockingUiController
import com.google.android.gms.maps.model.LatLng
import androidx.cardview.widget.CardView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentContainerView
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import java.lang.reflect.Proxy
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class Debug1012UploadFailureInstrumentedTest {

    @Test
    fun forbiddenEditStopsSpinnerAndRetryRestartsIt() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("taoyuan_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("auth_token", "debug-1012-test-token").commit()

        val hostRef = AtomicReference<Debug1012UploadHostFragment?>()
        MainShellActivity.fragmentFactoryForTests = { _ ->
            Debug1012UploadHostFragment().also { hostRef.set(it) }
        }
        val apiCalls = AtomicInteger()
        val failureDialogRef = AtomicReference<androidx.appcompat.app.AlertDialog?>()
        val scenario = ActivityScenario.launch(MainShellActivity::class.java)
        scenario.moveToState(Lifecycle.State.RESUMED)
        val hostReady = AtomicReference(false)
        repeat(50) {
            scenario.onActivity {
                val host = hostRef.get()
                hostReady.set(host?.isResumed == true && !host.childFragmentManager.isStateSaved)
            }
            if (hostReady.get()) return@repeat
            Thread.sleep(100)
        }
        assertTrue("Test host FragmentManager did not resume", hostReady.get())
        val sheetRef = AtomicReference<AddGutterBottomSheet?>()

        try {
            scenario.onActivity {
                val host = checkNotNull(hostRef.get())
                val fakeApi = forbiddenStoreDitchApi(apiCalls)
                val sheet = AddGutterBottomSheet.newInstanceForEdit(
                    waypoints = testWaypoints(),
                    spiNum = "DEBUG-1012",
                    spiTyp = "1"
                ).apply {
                    repositoryFactoryForTests = { GutterRepository(fakeApi) }
                    onFailureDialogShownForTests = { failureDialogRef.set(it) }
                }
                host.activeSheet = sheet
                sheetRef.set(sheet)
                sheet.showNow(host.childFragmentManager, "debug-1012-edit")
                sheet.performEditSubmit()
            }

            waitFor("first forbidden storeDitch response") {
                apiCalls.get() == 1 && (hostRef.get()?.failedSubmissionCount ?: 0) == 1
            }
            assertLoadingStopped(scenario, hostRef.get())
            val firstFailureDialog = failureDialog(failureDialogRef)
            assertTrue("Failure dialog is not showing", firstFailureDialog.isShowing)
            assertEquals("重傳", firstFailureDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).text)
            assertEquals("存入草稿", firstFailureDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).text)

            scenario.onActivity {
                firstFailureDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).performClick()
            }
            waitFor("retry forbidden storeDitch response") {
                apiCalls.get() == 2 && (hostRef.get()?.failedSubmissionCount ?: 0) == 2
            }

            val host = checkNotNull(hostRef.get())
            assertEquals(2, host.submittingCount)
            assertEquals(2, host.failedSubmissionCount)
            assertEquals(listOf(true, true), host.loadingAtRequest.toList())
            assertLoadingStopped(scenario, host)

            val secondFailureDialog = failureDialog(failureDialogRef)
            assertTrue("Retry failure dialog is not showing", secondFailureDialog.isShowing)
            scenario.onActivity {
                secondFailureDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).performClick()
            }
            waitFor("save-draft dismissal") {
                val isAdded = AtomicReference(true)
                scenario.onActivity { isAdded.set(sheetRef.get()?.isAdded == true) }
                !isAdded.get()
            }
            assertLoadingStopped(scenario, host)
        } finally {
            MainShellActivity.fragmentFactoryForTests = null
            prefs.edit().remove("auth_token").commit()
            scenario.close()
        }
    }

    private fun failureDialog(
        dialog: AtomicReference<androidx.appcompat.app.AlertDialog?>
    ): androidx.appcompat.app.AlertDialog {
        return checkNotNull(dialog.get()) { "Failure dialog was not captured" }
    }

    private fun assertLoadingStopped(
        scenario: ActivityScenario<MainShellActivity>,
        host: Debug1012UploadHostFragment?
    ) {
        assertTrue("Host fragment was not created", host != null)
        scenario.onActivity {
            val controller = checkNotNull(host?.blockingUiController)
            val binding = checkNotNull(host.mainBinding)
            assertFalse("Host still reports a blocking upload", controller.isBusyBlocking())
            assertEquals(View.GONE, binding.inspectLoadingOverlay.visibility)
            assertEquals(0f, binding.pbInspectLoading.rotation, 0f)
        }
    }

    private fun waitFor(description: String, condition: () -> Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (System.nanoTime() < deadline) {
            instrumentation.waitForIdleSync()
            if (condition()) return
            Thread.sleep(50)
        }
        throw AssertionError("Timed out waiting for " + description)
    }

    private fun forbiddenStoreDitchApi(callCount: AtomicInteger): GutterApiService {
        return Proxy.newProxyInstance(
            GutterApiService::class.java.classLoader,
            arrayOf(GutterApiService::class.java)
        ) { _, method, _ ->
            if (method.name == "storeDitch") {
                callCount.incrementAndGet()
                Response.error<StoreDitchResponse>(
                    403,
                    "{\"message\":\"Forbidden test response\"}"
                        .toResponseBody("application/json".toMediaType())
                )
            } else {
                error("Unexpected API call: " + method.name)
            }
        } as GutterApiService
    }

    private fun testWaypoints(): List<Waypoint> = listOf(
        waypoint(WaypointType.START, "起點", LatLng(24.99, 121.30)),
        waypoint(WaypointType.END, "終點", LatLng(24.991, 121.301))
    )

    private fun waypoint(type: WaypointType, label: String, location: LatLng): Waypoint =
        Waypoint(
            type = type,
            label = label,
            latLng = location,
            basicData = hashMapOf(
                "NODE_TYP" to "2",
                "NODE_X" to location.longitude.toString(),
                "NODE_Y" to location.latitude.toString(),
                "IS_CANTOPEN" to "1",
                "photo1" to "https://example.invalid/photo.jpg",
                "photo1ImgId" to "12"
            )
        )
}

class Debug1012UploadHostFragment : Fragment(), AddGutterBottomSheet.LocationPickerHost {
    var mainBinding: ActivityMainBinding? = null
        private set
    var blockingUiController: MainBlockingUiController? = null
        private set
    var activeSheet: AddGutterBottomSheet? = null
    var submittingCount = 0
        private set
    @Volatile
    var failedSubmissionCount = 0
        private set
    val loadingAtRequest = CopyOnWriteArrayList<Boolean>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ): View {
        val themedContext = android.view.ContextThemeWrapper(
            requireContext(),
            R.style.Theme_TaoYuanGutter
        )
        val root = ConstraintLayout(themedContext)
        fun <T : View> add(parent: ViewGroup, view: T, id: Int): T {
            view.id = id
            val params = when (parent) {
                is LinearLayout -> LinearLayout.LayoutParams(1, 1)
                is ConstraintLayout -> ConstraintLayout.LayoutParams(1, 1)
                else -> FrameLayout.LayoutParams(1, 1)
            }
            parent.addView(view, params)
            return view
        }

        add(root, FloatingActionButton(themedContext), R.id.btnAddGutter)
        add(root, FloatingActionButton(themedContext), R.id.btnLayers)
        add(root, FloatingActionButton(themedContext), R.id.btnLegend)
        add(root, TextView(themedContext), R.id.btnLogout)
        add(root, FloatingActionButton(themedContext), R.id.btnMeasureDistance)
        add(root, FloatingActionButton(themedContext), R.id.btnMyLocation)
        add(root, FloatingActionButton(themedContext), R.id.btnReportNoDitch)
        add(root, FloatingActionButton(themedContext), R.id.btnViewDrafts)
        add(root, ConstraintLayout(themedContext), R.id.inspectLoadingOverlay)
        add(root, ImageView(themedContext), R.id.ivMainMapLoadIndicatorIcon)
        add(root, ImageView(themedContext), R.id.ivMeasureCrosshair)
        add(root, MaterialCardView(themedContext), R.id.mainMapLoadIndicatorContainer)
        add(root, FragmentContainerView(themedContext), R.id.map)
        add(root, LinearLayout(themedContext), R.id.mapButtonsContainer)
        add(root, CircularProgressIndicator(themedContext), R.id.pbInspectLoading)
        add(root, ProgressBar(themedContext), R.id.pbMainMapLoadIndicator)
        add(root, TextView(themedContext), R.id.tvInspectLoading)
        add(root, TextView(themedContext), R.id.tvMainMapLoadIndicatorText)
        add(root, TextView(themedContext), R.id.tvPhotoUploadProgress)

        val pickerOverlay = add(root, ConstraintLayout(themedContext), R.id.locationPickerOverlay)
        add(pickerOverlay, LinearLayout(themedContext), R.id.topPickerBar)
        add(pickerOverlay, LinearLayout(themedContext), R.id.bottomPickerBar)
        add(pickerOverlay, MaterialButton(themedContext), R.id.btnCancelPick)
        add(pickerOverlay, MaterialButton(themedContext), R.id.btnConfirmPick)
        add(pickerOverlay, ExtendedFloatingActionButton(themedContext), R.id.fabMyLocation)
        add(pickerOverlay, ImageView(themedContext), R.id.ivCrosshair)

        val measurePanel = add(root, CardView(themedContext), R.id.measurePanel)
        add(measurePanel, TextView(themedContext), R.id.btnMeasureClose)
        add(measurePanel, TextView(themedContext), R.id.btnMeasureReset)
        add(measurePanel, TextView(themedContext), R.id.tvMeasureDistance)

        val noDitchPanel = add(root, MaterialCardView(themedContext), R.id.noDitchPanel)
        add(noDitchPanel, TextView(themedContext), R.id.btnNoDitchLeft)
        add(noDitchPanel, TextView(themedContext), R.id.btnNoDitchRight)
        add(noDitchPanel, TextView(themedContext), R.id.tvNoDitchLatLng)
        add(noDitchPanel, TextView(themedContext), R.id.tvNoDitchSubtitle)
        add(noDitchPanel, TextView(themedContext), R.id.tvNoDitchTitle)
        val noteLayout = add(noDitchPanel, TextInputLayout(themedContext), R.id.tilNoDitchNote)
        add(noteLayout, TextInputEditText(themedContext), R.id.etNoDitchNote)

        val binding = ActivityMainBinding.bind(root)
        mainBinding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        blockingUiController = MainBlockingUiController(
            context = requireContext(),
            binding = checkNotNull(mainBinding),
            isMeasuring = { false },
            isSheetActive = { activeSheet != null }
        )
    }

    override fun onDestroyView() {
        blockingUiController = null
        mainBinding = null
        super.onDestroyView()
    }

    override fun startLocationPick(sheet: AddGutterBottomSheet, waypointIndex: Int) = Unit
    override fun openWaypointForEdit(
        sheet: AddGutterBottomSheet,
        waypointIndex: Int,
        submittedDraftReadOnly: Boolean
    ) = Unit
    override fun onGutterSubmitted(waypoints: List<Waypoint>) = Unit
    override fun onStoreDitchRequestEntered(waypoints: List<Waypoint>) {
        loadingAtRequest += blockingUiController?.isBusyBlocking() == true
    }
    override fun onPendingPhotoUploadStarted(totalCount: Int) {
        blockingUiController?.beginPhotoUpload(totalCount)
    }
    override fun onPendingPhotoUploadProgress(success: Boolean) {
        blockingUiController?.recordPhotoUploadResult(success)
    }
    override fun onPendingPhotoUploadFinished() {
        blockingUiController?.endPhotoUpload()
    }
    override fun getInspectWaypoints(): List<Waypoint> = emptyList()
    override fun openWaypointForInspect(sheet: AddGutterBottomSheet, waypointIndex: Int) = Unit
    override fun onUpdateGutter(waypoints: List<Waypoint>, spiNum: String) = Unit
    override fun onDeleteGutter(spiNum: String) = Unit
    override fun onGutterSaved(
        spiNum: String?,
        waypoints: List<Waypoint>,
        nodes: List<DitchNode>
    ) = Unit
    override fun onGutterSaveFailed(waypoints: List<Waypoint>) = Unit
    override fun onGutterSubmitFailed() {
        failedSubmissionCount += 1
        blockingUiController?.setInspectLoading(false)
    }
    override fun onSheetViewportInsetChanged(bottomInsetPx: Int) = Unit
    override fun onGutterRetry() {
        blockingUiController?.setInspectLoading(true, "test retry")
    }
    override fun onGutterSubmitting() {
        submittingCount += 1
        activeSheet?.hideSelf()
        blockingUiController?.setInspectLoading(true, "test submitting")
    }
    override fun onStoreDitchNetworkClosed(spiNum: String?, waypoints: List<Waypoint>) {
        blockingUiController?.setInspectLoading(false)
    }
}
