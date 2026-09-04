package com.example.taoyuangutter.map

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import android.net.Uri
import com.example.taoyuangutter.R
import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.api.DitchNode
import com.example.taoyuangutter.api.GutterApiClient
import com.example.taoyuangutter.api.GutterRepository
import com.example.taoyuangutter.api.NoDitchPoint
import com.example.taoyuangutter.common.LocationPickEvents
import com.example.taoyuangutter.common.PhotoImgIdTraceDebugger
import com.example.taoyuangutter.common.PhotoUriStore
import com.example.taoyuangutter.common.UploadFailureClassifier
import com.example.taoyuangutter.databinding.ActivityMainBinding
import com.example.taoyuangutter.gutter.AddGutterBottomSheet
import com.example.taoyuangutter.gutter.GutterFormActivity
import com.example.taoyuangutter.gutter.GutterFormContract
import com.example.taoyuangutter.gutter.GutterFormNavigator
import com.example.taoyuangutter.gutter.GutterInspectActivity
import com.example.taoyuangutter.gutter.GutterSheetSessionBinder
import com.example.taoyuangutter.gutter.GutterSessionFlowCoordinator
import com.example.taoyuangutter.gutter.GutterSessionUiCoordinator
import com.example.taoyuangutter.gutter.InspectFlowCoordinator
import com.example.taoyuangutter.gutter.PhotoUploadManager
import com.example.taoyuangutter.gutter.Waypoint
import com.example.taoyuangutter.gutter.WaypointType
import com.example.taoyuangutter.login.AuthExpiredHandler
import com.example.taoyuangutter.login.AuthNavigator
import com.example.taoyuangutter.login.LoginActivity
import com.example.taoyuangutter.main.MainBlockingUiController
import com.example.taoyuangutter.main.MAIN_MAP_SCOPE_SEARCH_MIN_ZOOM
import com.example.taoyuangutter.main.MainMapLoadIndicatorController
import com.example.taoyuangutter.main.MainViewModel
import com.example.taoyuangutter.main.MeasureModeUiController
import com.example.taoyuangutter.main.NoDitchModeUiController
import com.example.taoyuangutter.pending.GutterDraftCoordinator
import com.example.taoyuangutter.pending.GutterSessionDraft
import com.example.taoyuangutter.pending.GutterSessionRepository
import com.example.taoyuangutter.pending.PendingDraftSheetNavigator
import com.example.taoyuangutter.pending.WaypointSnapshot
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.max

class MapWorkspaceFragment : Fragment(),
    OnMapReadyCallback,
    AddGutterBottomSheet.LocationPickerHost,
    LayersBottomSheet.Host {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels()

    private var googleMap: GoogleMap? = null
    private var isOfflineMainMode: Boolean = false
    private var currentSheetBottomInsetPx: Int = 0
    private var addGutterBaseBottomMarginPx: Int? = null
    private var pickerBarBaseBottomMarginPx: Int? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var gutterFormLauncher: ActivityResultLauncher<Intent>
    private lateinit var inspectLauncher: ActivityResultLauncher<Intent>

    private lateinit var mainBlockingUiController: MainBlockingUiController
    private lateinit var mainMapLoadIndicatorController: MainMapLoadIndicatorController
    private lateinit var measureModeUiController: MeasureModeUiController
    private lateinit var noDitchModeUiController: NoDitchModeUiController

    private val gutterRepository = GutterRepository()
    private val photoUploadManager by lazy { PhotoUploadManager(requireContext(), gutterRepository) }
    private val sessionDraftRepository by lazy { GutterSessionRepository(requireContext()) }
    private val draftCoordinator by lazy { GutterDraftCoordinator(requireContext(), sessionDraftRepository) }
    private val pendingDraftSheetNavigator by lazy { PendingDraftSheetNavigator(childFragmentManager) }
    private val gutterSheetSessionBinder = GutterSheetSessionBinder()
    private val gutterSessionFlowCoordinator = GutterSessionFlowCoordinator()
    private val gutterSessionUiCoordinator by lazy {
        GutterSessionUiCoordinator(
            flowCoordinator = gutterSessionFlowCoordinator,
            pendingDraftSheetNavigator = pendingDraftSheetNavigator
        )
    }
    private val authNavigator by lazy { AuthNavigator(requireContext()) }
    private val authExpiredHandler by lazy(LazyThreadSafetyMode.NONE) { AuthExpiredHandler(requireActivity()) }
    private val gutterFormNavigator by lazy { GutterFormNavigator(requireContext()) }
    private val markerIconFactory by lazy { MarkerIconFactory(requireContext()) }
    private val inspectFlowCoordinator by lazy {
        InspectFlowCoordinator(
            context = requireContext(),
            repository = gutterRepository
        )
    }
    private val gutterMapController by lazy {
        GutterMapController(
            mapProvider = { googleMap },
            markerIconProvider = { type, isPending, isVirtual ->
                markerIconFactory.icon(type, isPending, isVirtual)
            },
            polylineColorProvider = { isCurve -> resolveGutterPolylineColor() },
            pendingFlagParser = ::parseLooseBoolean
        )
    }
    private val mapCameraController by lazy {
        MapCameraController(
            context = requireContext(),
            mapProvider = { googleMap }
        )
    }
    private val myLocationController by lazy {
        MyLocationController(
            context = requireContext(),
            fusedLocationClient = fusedLocationClient,
            mapProvider = { googleMap }
        )
    }
    private val inspectMarkerController by lazy {
        InspectMarkerController(
            mapProvider = { googleMap },
            markerIconProvider = { type, isPending, isVirtual ->
                markerIconFactory.icon(type, isPending, isVirtual)
            },
            enlargedMarkerIconProvider = { type, isPending, isVirtual ->
                markerIconFactory.enlargedIcon(type, isPending, isVirtual)
            },
            pendingFlagParser = ::parseLooseBoolean,
            virtualFlagParser = ::parseLooseBoolean
        )
    }
    private val scopeGutterPolylineController by lazy {
        ScopeGutterPolylineController(mapProvider = { googleMap })
    }
    private val scopeViewportLoader by lazy {
        ScopeViewportLoader(
            repository = gutterRepository,
            mapProvider = { googleMap }
        )
    }
    private val scopeMapCoordinator by lazy {
        ScopeMapCoordinator(
            loadViewport = scopeViewportLoader::load,
            drawFeatures = { features, savedGroupId ->
                scopeGutterPolylineController.drawFeatures(features, savedGroupId)
            },
            savedGroupIdProvider = { LoginActivity.getSavedGroupId(requireContext()) }
        )
    }
    private val mapOverlayController by lazy {
        MapOverlayController(
            mapProvider = { googleMap },
            onNoDitchPointsLayerChanged = { enabled ->
                if (!enabled) clearNoDitchPointsMarkers()
            }
        )
    }

    private var currentSessionDraftId: Long? = null
    private var pendingEditOriginalWaypoints: List<WaypointSnapshot>? = null
    private var currentSessionResumedFromDraft: Boolean = false
    private var currentSessionIsOffline: Boolean = false
    private var initialSpiState: String? = null
    private var activeSheet: AddGutterBottomSheet? = null
    private var pickingIndex: Int = -1
    private var pendingWaypointFormIndex: Int = -1
    private var inspectSheet: AddGutterBottomSheet? = null
    private var inspectWaypoints: List<Waypoint> = emptyList()
    private var inspectPreviewIntent: Intent? = null
    private var shouldReturnToInspectPreview = false
    private data class PendingInspectPreviewReload(
        val spiNum: String,
        val waypoints: List<Waypoint>,
        val token: String
    )
    private var pendingInspectPreviewReload: PendingInspectPreviewReload? = null
    private var isInspecting = false
    private var isInspectUiLocked = false
    private var isInEditingMode = false
    private var isMainMapGestureActive = false
    private var pendingUserLocationRecenter = false
    private var pendingLocationRecenterReload = false
    private data class PendingForceReload(
        val id: Long,
        val reason: String
    )
    private var pendingForceReload: PendingForceReload? = null
    private var forceReloadInFlightId: Long? = null
    private var nextForceReloadId = 0L
    private var isReferenceRouteActive = false
    private var referenceRoutePoints: List<LatLng> = emptyList()
    private var editLatLngSnapshot: List<Pair<Long, Long>>? = null
    private var hasShownEditPolyline: Boolean = false
    private val submittedPolylines = mutableListOf<Polyline>()
    private var currentWaypoints: List<Waypoint> = emptyList()
    private var measureManager: DistanceMeasureManager? = null
    private lateinit var measureConfig: MeasureConfig
    private var noDitchMarker: com.google.android.gms.maps.model.Marker? = null
    private var noDitchPickedLatLng: LatLng? = null
    private var isNoDitchPickMode: Boolean = false
    private var isNoDitchPickClickEnabled: Boolean = false
    private var noDitchPointsMarkers = mutableListOf<com.google.android.gms.maps.model.Marker>()
    private var noDitchPoints = mutableListOf<NoDitchPoint>()
    private var lastKnownLocation: Location? = null

    private val waypointLocationChangedReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            if (intent?.action != LocationPickEvents.ACTION_WAYPOINT_LOCATION_CHANGED) return
            val draftId = intent.getLongExtra(LocationPickEvents.EXTRA_SESSION_DRAFT_ID, 0L)
            val index = intent.getIntExtra(LocationPickEvents.EXTRA_WAYPOINT_INDEX, -1)
            val lat = intent.getDoubleExtra(LocationPickEvents.EXTRA_LATITUDE, Double.NaN)
            val lng = intent.getDoubleExtra(LocationPickEvents.EXTRA_LONGITUDE, Double.NaN)
            if (draftId <= 0L || draftId != currentSessionDraftId || index < 0 || lat.isNaN() || lng.isNaN()) return
            val sheet = activeSheet ?: return
            sheet.updateWaypointLocation(index, LatLng(lat, lng))
            mapCameraController.fitCameraToWaypoints(
                sheet.getWaypoints(),
                bottomOffsetRatio = 0.8,
                resetPaddingAfter = false,
                maxZoom = 19f,
                paddingDp = 24
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                myLocationController.enableMyLocationAndMove(
                    onLocationUpdated = ::handleMainMapLocationUpdated,
                    onCameraMoveFinished = ::handleMainMapLocationRecenterFinished
                )
            } else {
                pendingUserLocationRecenter = false
            }
        }

        gutterFormLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            binding.btnAddGutter.visibility = View.VISIBLE
            renderReferenceRouteIfActive()
            when {
                activeSheet != null -> handleAddSheetActivityResult(result)
                inspectSheet != null -> handleInspectSheetActivityResult(result)
            }
        }

        inspectLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            isInspecting = false
            if (result.resultCode == GutterInspectActivity.RESULT_EDIT_DITCH) {
                handleInspectEditResult(result.data)
            } else {
                isInEditingMode = false
                inspectPreviewIntent = null
                shouldReturnToInspectPreview = false
                unlockInspectUiIfIdle()
                clearReferenceRoute()
                gutterMapController.clearPreviewLayer()
                clearWorkingMarkers()
                loadGuttersByViewport(showFeedback = true)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        applySystemBarInsets()

        mainBlockingUiController = MainBlockingUiController(
            context = requireContext(),
            binding = binding,
            isMeasuring = { measureManager?.isMeasuring == true },
            isSheetActive = { activeSheet != null || inspectSheet != null || isInspecting || isInspectUiLocked }
        )
        mainMapLoadIndicatorController = MainMapLoadIndicatorController(requireContext(), binding)
        measureConfig = MeasureConfig()
        measureModeUiController = MeasureModeUiController(
            context = requireContext(),
            binding = binding,
            measureConfig = measureConfig,
            onMainButtonsEnabledChanged = { enabled ->
                mainBlockingUiController.setMainButtonsEnabledDuringMeasureMode(enabled)
            }
        )
        measureModeUiController.setupPanelInsets()
        noDitchModeUiController = NoDitchModeUiController(
            context = requireContext(),
            binding = binding,
            onMainButtonsEnabledChanged = { enabled ->
                mainBlockingUiController.setMainButtonsEnabledDuringNoDitchMode(enabled)
            },
            onExitRequested = { exitNoDitchMode() },
            onResetRequested = { resetNoDitchPick() },
            onSubmitRequested = { note -> submitNoDitch(note) }
        )
        noDitchModeUiController.setupPanelInsets()
        noDitchModeUiController.bind()
        mainMapLoadIndicatorController.setBottomInset(currentSheetBottomInsetPx)
        isOfflineMainMode = false

        ContextCompat.registerReceiver(
            requireContext(),
            waypointLocationChangedReceiver,
            android.content.IntentFilter(LocationPickEvents.ACTION_WAYPOINT_LOCATION_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        setupButtons()
        setupLocationPickerOverlay()
        initializeMap()
        if (savedInstanceState != null) {
            restoreStateAfterRecreation(savedInstanceState)
        }
    }

    override fun onDestroyView() {
        authExpiredHandler.reset()
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        runCatching { requireContext().unregisterReceiver(waypointLocationChangedReceiver) }
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PENDING_WP_INDEX, pendingWaypointFormIndex)
        currentSessionDraftId?.let { outState.putLong("saved_session_draft_id", it) }
        outState.putBoolean("saved_session_resumed_from_draft", currentSessionResumedFromDraft)
        outState.putBoolean("saved_session_is_offline", currentSessionIsOffline)
        outState.putBoolean("saved_is_inspecting", isInspecting)
        outState.putBoolean("saved_is_in_editing_mode", isInEditingMode)
        outState.putString("saved_initial_spi_state", initialSpiState)
        outState.putString("saved_current_waypoints_json", Gson().toJson(currentWaypoints.map { wp ->
            WaypointSnapshot(type = wp.type.name, label = wp.label, latitude = wp.latLng?.latitude, longitude = wp.latLng?.longitude, basicData = wp.basicData, uid = wp.uid)
        }))
        val inspectWpsSnapshots = inspectWaypoints.map { wp ->
            WaypointSnapshot(type = wp.type.name, label = wp.label, latitude = wp.latLng?.latitude, longitude = wp.latLng?.longitude, basicData = wp.basicData, uid = wp.uid)
        }
        outState.putString("saved_inspect_waypoints_json", Gson().toJson(inspectWpsSnapshots))
        outState.putBoolean("saved_is_reference_route_active", isReferenceRouteActive)
        if (referenceRoutePoints.isNotEmpty()) {
            outState.putDoubleArray("saved_reference_route_lats", referenceRoutePoints.map { it.latitude }.toDoubleArray())
            outState.putDoubleArray("saved_reference_route_lngs", referenceRoutePoints.map { it.longitude }.toDoubleArray())
        }
        outState.putBoolean("saved_has_shown_edit_polyline", hasShownEditPolyline)
        if (editLatLngSnapshot != null) {
            outState.putString("saved_edit_lat_lng_snapshot_json", Gson().toJson(editLatLngSnapshot))
        }
    }

    private fun initializeMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment ?: return
        mapFragment.getMapAsync(this)
    }

    private fun restoreStateAfterRecreation(savedState: Bundle) {
        pendingWaypointFormIndex = savedState.getInt(KEY_PENDING_WP_INDEX, -1)
        currentSessionDraftId = if (savedState.containsKey("saved_session_draft_id")) {
            savedState.getLong("saved_session_draft_id")
        } else {
            null
        }
        currentSessionResumedFromDraft = savedState.getBoolean("saved_session_resumed_from_draft", false)
        currentSessionIsOffline = savedState.getBoolean("saved_session_is_offline", false)
        isInspecting = savedState.getBoolean("saved_is_inspecting", false)
        isInEditingMode = savedState.getBoolean("saved_is_in_editing_mode", false)
        initialSpiState = savedState.getString("saved_initial_spi_state")

        savedState.getString("saved_current_waypoints_json")?.takeIf { it.isNotBlank() }?.let { json ->
            val type = object : com.google.gson.reflect.TypeToken<List<WaypointSnapshot>>() {}.type
            val snapshots: List<WaypointSnapshot> = try { Gson().fromJson(json, type) } catch (_: Exception) { emptyList() }
            currentWaypoints = snapshots.map { snap ->
                val wpType = WaypointType.entries.firstOrNull { it.name == snap.type } ?: WaypointType.NODE
                val latLng = if (snap.latitude != null && snap.longitude != null) LatLng(snap.latitude, snap.longitude) else null
                Waypoint(wpType, snap.label, latLng, snap.basicData, snap.uid.ifBlank { snap.basicData["_nodeId"] ?: "${wpType.name}_${snap.label}" })
            }
        }

        savedState.getString("saved_inspect_waypoints_json")?.takeIf { it.isNotBlank() }?.let { json ->
            val type = object : com.google.gson.reflect.TypeToken<List<WaypointSnapshot>>() {}.type
            val snapshots: List<WaypointSnapshot> = try { Gson().fromJson(json, type) } catch (_: Exception) { emptyList() }
            inspectWaypoints = snapshots.map { snap ->
                val wpType = WaypointType.entries.firstOrNull { it.name == snap.type } ?: WaypointType.NODE
                val latLng = if (snap.latitude != null && snap.longitude != null) LatLng(snap.latitude, snap.longitude) else null
                Waypoint(wpType, snap.label, latLng, snap.basicData, snap.uid.ifBlank { snap.basicData["_nodeId"] ?: "${wpType.name}_${snap.label}" })
            }
        }

        isReferenceRouteActive = savedState.getBoolean("saved_is_reference_route_active", false)
        val refLats = savedState.getDoubleArray("saved_reference_route_lats")
        val refLngs = savedState.getDoubleArray("saved_reference_route_lngs")
        if (refLats != null && refLngs != null && refLats.size == refLngs.size) {
            referenceRoutePoints = refLats.indices.map { LatLng(refLats[it], refLngs[it]) }
        }

        hasShownEditPolyline = savedState.getBoolean("saved_has_shown_edit_polyline", false)
        savedState.getString("saved_edit_lat_lng_snapshot_json")?.takeIf { it.isNotBlank() }?.let { json ->
            val type = object : com.google.gson.reflect.TypeToken<List<Pair<Long, Long>>>() {}.type
            editLatLngSnapshot = try { Gson().fromJson(json, type) } catch (_: Exception) { null }
        }

        val restoredSheet = childFragmentManager.findFragmentByTag(AddGutterBottomSheet.TAG) as? AddGutterBottomSheet ?: return
        if (restoredSheet.isAddMode()) {
            activeSheet = restoredSheet
            bindAddGutterSheet(restoredSheet)
        } else {
            inspectSheet = restoredSheet
            restoredSheet.onWaypointsChanged = {
                if (it == null) {
                    isInEditingMode = false
                    clearWorkingMarkers()
                    inspectSheet = null
                    loadGuttersByViewport(showFeedback = true)
                }
            }
        }
    }

    private fun applySystemBarInsets() {
        val addFab = binding.btnAddGutter
        if (addGutterBaseBottomMarginPx == null) {
            val lp = addFab.layoutParams as? ViewGroup.MarginLayoutParams
            addGutterBaseBottomMarginPx = lp?.bottomMargin ?: 0
        }
        val bottomPickerBar = binding.root.findViewById<View>(R.id.bottomPickerBar)
        if (pickerBarBaseBottomMarginPx == null) {
            val lp = bottomPickerBar?.layoutParams as? ViewGroup.MarginLayoutParams
            pickerBarBaseBottomMarginPx = lp?.bottomMargin ?: 0
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val bottomInset = max(systemBottom, imeBottom)

            val addBase = addGutterBaseBottomMarginPx ?: 0
            addFab.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = addBase + bottomInset
            }

            val pickerBase = pickerBarBaseBottomMarginPx ?: 0
            bottomPickerBar?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = pickerBase + bottomInset
            }
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.mapType = GoogleMap.MAP_TYPE_NONE
        mainViewModel.overlayState?.let { savedState ->
            mapOverlayController.applyState(savedState)
            scopeGutterPolylineController.setVisible(savedState.showPlan)
        } ?: run {
            mapOverlayController.setBaseLayer(LayersBottomSheet.LAYER_EMAP)
            mapOverlayController.applyWmsOverlays()
            scopeGutterPolylineController.setVisible(mapOverlayController.currentState().showPlan)
        }
        mapOverlayController.ensureMeasureLabelsOverlay()
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(24.9929, 121.3011), 16f))

        myLocationController.requestLocationAndMove(
            requestPermission = {
                locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
            },
            onLocationUpdated = ::handleMainMapLocationUpdated,
            onCameraMoveFinished = ::handleMainMapLocationRecenterFinished
        )

        googleMap?.uiSettings?.apply {
            isZoomControlsEnabled = false
            isCompassEnabled = true
            isMapToolbarEnabled = false
        }

        map.setOnCameraMoveStartedListener { reason -> handleMainMapCameraMoveStarted(reason) }
        map.setOnMarkerClickListener { marker ->
            if (measureManager?.isMeasuring == true) {
                measureManager?.setStartPoint(marker.position)
                return@setOnMarkerClickListener true
            }
            if (marker.tag is NoDitchPoint) {
                fetchAndShowNoDitchPointNote(marker)
                return@setOnMarkerClickListener true
            }
            val wpIndex = marker.tag as? Int ?: return@setOnMarkerClickListener false
            if (inspectSheet != null) {
                val wp = inspectWaypoints.getOrNull(wpIndex) ?: return@setOnMarkerClickListener false
                val latLng = wp.latLng ?: return@setOnMarkerClickListener false
                openInspectForm(wpIndex, wp, latLng)
            } else {
                val wp = currentWaypoints.getOrNull(wpIndex) ?: return@setOnMarkerClickListener false
                val latLng = wp.latLng ?: return@setOnMarkerClickListener false
                pendingWaypointFormIndex = wpIndex
                openAddForm(wpIndex, wp, latLng)
            }
            true
        }
        map.setOnPolylineClickListener { polyline ->
            if (measureManager?.isMeasuring == true) return@setOnPolylineClickListener
            openInspectBottomSheet(polyline)
        }
        map.setOnMapClickListener { latLng -> handleMainMapTap(latLng) }
        map.setOnCameraIdleListener {
            handleMainMapCameraIdle()
            if (mapOverlayController.currentState().showNoDitchPoints) {
                loadNoDitchPointsForVisibleArea()
            }
        }

        measureConfig = MeasureConfig(startMarkerIcon = BitmapDescriptorFactory.defaultMarker(256f))
        measureManager = DistanceMeasureManager(map, measureConfig) { meters ->
            updateMeasureDistanceDisplay(meters)
        }

        renderReferenceRouteIfActive()
        if (isInEditingMode) {
            if (activeSheet != null) {
                refreshWorkingLayer(currentWaypoints)
                mainBlockingUiController.setMainButtonsEnabled(false)
            } else if (inspectSheet != null) {
                refreshWorkingLayer(inspectWaypoints, inspectSheet?.isCurveMode() == true)
                mainBlockingUiController.setMainButtonsEnabled(false)
            }
        }

        requestBackgroundScopeRefresh()
    }

    private fun setupButtons() {
        binding.btnLogout.setOnClickListener {
            if (isOfflineMainMode) {
                authNavigator.clearAuthAndGoLogin()
                return@setOnClickListener
            }
            val token = LoginActivity.getSavedToken(requireContext())
            if (token.isNullOrEmpty()) {
                authNavigator.goToLogin()
                return@setOnClickListener
            }
            binding.btnLogout.isEnabled = false
            lifecycleScope.launch {
                when (val result = gutterRepository.logout(token)) {
                    is ApiResult.Success -> authNavigator.clearAuthAndGoLogin()
                    is ApiResult.Error -> {
                        if (result.code == 401) {
                            authNavigator.clearAuthAndGoLogin()
                            return@launch
                        }
                        binding.btnLogout.isEnabled = true
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        binding.btnViewDrafts.setOnClickListener { showPendingDraftsSheet() }
        binding.btnLegend.setOnClickListener { LegendBottomSheet().show(childFragmentManager, "LegendBottomSheet") }
        binding.btnLayers.setOnClickListener {
            val state = mapOverlayController.currentState()
            LayersBottomSheet.newInstance(
                selectedLayer = state.selectedLayer,
                showPlan = state.showPlan,
                showWaterOld = state.showWaterOld,
                showPossible = state.showPossible,
                showRegion = state.showRegion,
                showNoDitchPoints = state.showNoDitchPoints
            ).show(childFragmentManager, "LayersBottomSheet")
        }
        binding.btnMyLocation.setOnClickListener {
            pendingUserLocationRecenter = true
            myLocationController.requestLocationAndMove(
                requestPermission = {
                    locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                },
                onLocationUpdated = { location ->
                    handleMainMapLocationUpdated(location)
                },
                onCameraMoveFinished = ::handleMainMapLocationRecenterFinished
            )
        }
        binding.btnReportNoDitch.setOnClickListener { openNoDitchReport() }
        binding.btnMeasureDistance.setOnClickListener { toggleMeasureMode() }
        binding.measurePanel.btnMeasureReset.setOnClickListener { measureManager?.reset() }
        binding.measurePanel.btnMeasureClose.setOnClickListener { exitMeasureMode() }
        binding.btnAddGutter.setOnClickListener { openAddGutterFlow() }
    }

    private fun setupLocationPickerOverlay() {
        binding.locationPickerOverlay.btnConfirmPick.setOnClickListener {
            val latLng = googleMap?.cameraPosition?.target
            binding.locationPickerOverlay.root.visibility = View.GONE
            if (latLng != null && pickingIndex >= 0) {
                activeSheet?.updateWaypointLocation(pickingIndex, latLng)
                val wp = currentWaypoints.getOrNull(pickingIndex) ?: Waypoint(WaypointType.NODE, "點位")
                pendingWaypointFormIndex = pickingIndex
                openAddForm(pickingIndex, wp, latLng)
            } else {
                binding.btnAddGutter.visibility = View.VISIBLE
                activeSheet?.showSelf()
            }
            pickingIndex = -1
        }
        binding.locationPickerOverlay.btnCancelPick.setOnClickListener {
            binding.locationPickerOverlay.root.visibility = View.GONE
            binding.btnAddGutter.visibility = View.VISIBLE
            resetHighlightedMarker()
            activeSheet?.showSelf()
            pickingIndex = -1
        }
    }

    override fun onLayerSelected(layer: String) {
        mapOverlayController.setBaseLayer(layer)
        mainViewModel.overlayState = mapOverlayController.currentState()
    }

    override fun onOverlayTogglesChanged(
        showPlan: Boolean,
        showWaterOld: Boolean,
        showPossible: Boolean,
        showRegion: Boolean,
        showNoDitchPoints: Boolean
    ) {
        mapOverlayController.updateOverlayToggles(
            showPlan = showPlan,
            showWaterOld = showWaterOld,
            showPossible = showPossible,
            showRegion = showRegion,
            showNoDitchPoints = showNoDitchPoints
        )
        scopeGutterPolylineController.setVisible(showPlan)
        if (showNoDitchPoints) {
            loadNoDitchPointsForVisibleArea()
        }
        mainViewModel.overlayState = mapOverlayController.currentState()
    }

    override fun startLocationPick(sheet: AddGutterBottomSheet, waypointIndex: Int) {
        activeSheet = sheet
        pickingIndex = waypointIndex
        highlightMarker(waypointIndex)
        sheet.hideSelf()
        binding.btnAddGutter.visibility = View.GONE
        binding.locationPickerOverlay.root.visibility = View.VISIBLE
        mapCameraController.setPersistentBottomInset(0)
    }

    override fun openWaypointForEdit(sheet: AddGutterBottomSheet, waypointIndex: Int) {
        currentWaypoints = sheet.getWaypoints()
        val wp = currentWaypoints.getOrNull(waypointIndex) ?: return
        val initialLatLng = wp.latLng ?: googleMap?.cameraPosition?.target ?: LatLng(0.0, 0.0)
        pendingWaypointFormIndex = waypointIndex
        highlightMarker(waypointIndex)
        sheet.hideSelf()
        binding.btnAddGutter.visibility = View.GONE
        openAddForm(waypointIndex, wp, initialLatLng, isEditMode = sheet.isEditMode())
    }

    override fun openWaypointForInspect(sheet: AddGutterBottomSheet, waypointIndex: Int) {
        val wp = inspectWaypoints.getOrNull(waypointIndex) ?: return
        val latLng = wp.latLng ?: return
        highlightMarker(waypointIndex)
        sheet.hideSelf()
        binding.btnAddGutter.visibility = View.GONE
        openInspectForm(waypointIndex, wp, latLng)
    }

    override fun onGutterSubmitted(waypoints: List<Waypoint>) {
        activeSheet?.hideSelf()
        mainBlockingUiController.setInspectLoading(true, getString(R.string.msg_gutter_submitting))
    }

    override fun onPendingPhotoUploadStarted(totalCount: Int) {
        if (totalCount <= 0) return
        activeSheet?.hideSelf()
        mainBlockingUiController.beginPhotoUpload(totalCount)
    }

    override fun onPendingPhotoUploadProgress(success: Boolean) {
        mainBlockingUiController.recordPhotoUploadResult(success)
    }

    override fun onPendingPhotoUploadFinished() {
        mainBlockingUiController.endPhotoUpload()
    }

    override fun onGutterRetry() {
        mainBlockingUiController.setInspectLoading(true, getString(R.string.msg_gutter_submitting))
    }

    override fun onGutterSubmitting() {
        activeSheet?.hideSelf()
        mainBlockingUiController.setInspectLoading(true, getString(R.string.msg_gutter_submitting))
    }

    override fun getInspectWaypoints(): List<Waypoint> = inspectWaypoints

    override fun onSheetViewportInsetChanged(bottomInsetPx: Int) {
        currentSheetBottomInsetPx = bottomInsetPx.coerceAtLeast(0)
        mapCameraController.setPersistentBottomInset(currentSheetBottomInsetPx)
        mainMapLoadIndicatorController.setBottomInset(currentSheetBottomInsetPx)
    }

    override fun onUpdateGutter(waypoints: List<Waypoint>, spiNum: String) {
        shouldReturnToInspectPreview = false
        inspectPreviewIntent = null
        pendingEditOriginalWaypoints = activeSheet?.getOriginalWaypointSnapshots()
        activeSheet?.onWaypointsChanged = null
        gutterMapController.clearWorkingLayer()
        activeSheet = null
        clearWorkingMarkers()
        binding.btnAddGutter.visibility = View.VISIBLE
        mapCameraController.setPersistentBottomInset(0)
        setMainButtonsEnabledRespectingInspectLock(true)
        isInEditingMode = false
        loadGuttersByViewport(showFeedback = true)
    }

    override fun onDeleteGutter(spiNum: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("刪除側溝")
            .setMessage("確定要刪除側溝「$spiNum」及其所有點位嗎？此操作無法復原。")
            .setPositiveButton("確定刪除") { _, _ ->
                val token = LoginActivity.getSavedToken(requireContext())
                if (token.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.msg_login_first), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    when (val result = gutterRepository.deleteDitch(spiNum, token)) {
                        is ApiResult.Success -> {
                            shouldReturnToInspectPreview = false
                            inspectPreviewIntent = null
                            draftCoordinator.deleteDraftsBySpiNum(requireContext(), spiNum)
                            scopeGutterPolylineController.remove(spiNum)
                            activeSheet?.onWaypointsChanged = null
                            clearReferenceRoute()
                            gutterMapController.clearPreviewLayer()
                            activeSheet?.dismiss()
                            activeSheet = null
                            clearWorkingMarkers()
                            isInEditingMode = false
                            binding.btnAddGutter.visibility = View.VISIBLE
                            mapCameraController.setPersistentBottomInset(0)
                            restoreMainUiAfterSheetClosed()
                            Toast.makeText(requireContext(), String.format(getString(R.string.msg_delete_success), spiNum), Toast.LENGTH_SHORT).show()
                            loadGuttersByViewport()
                        }
                        is ApiResult.Error -> {
                            if (authExpiredHandler.handleIfAuthExpired(result)) return@launch
                            Toast.makeText(requireContext(), String.format(getString(R.string.msg_delete_failed), result.message), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onGutterSaved(spiNum: String?, waypoints: List<Waypoint>, nodes: List<DitchNode>) {
        shouldReturnToInspectPreview = false
        inspectPreviewIntent = null
        clearReferenceRoute()
        editLatLngSnapshot = null
        hasShownEditPolyline = false
        val (persistedWaypoints, pendingDraftId) = persistServerIdsIntoDraft(spiNum, waypoints, nodes)
        val token = LoginActivity.getSavedToken(requireContext()) ?: run {
            mainBlockingUiController.setInspectLoading(false)
            return
        }

        if (spiNum != null) {
            activeSheet?.onWaypointsChanged = null
            scopeGutterPolylineController.remove(spiNum)
            loadGuttersByViewport()
        } else {
            activeSheet?.onWaypointsChanged = null
            gutterMapController.clearPreviewLayer()
            clearWorkingMarkers()
            binding.btnAddGutter.visibility = View.VISIBLE
            isInEditingMode = false
            setMainButtonsEnabledRespectingInspectLock(true)
            drawSubmittedGutter(waypoints)
            mapCameraController.setPersistentBottomInset(0)
            loadGuttersByViewport()
        }
        finalizePhotoUploadFlow(
            spiNum = spiNum,
            persistedWaypoints = persistedWaypoints,
            nodes = nodes,
            pendingDraftId = pendingDraftId,
            token = token,
            originalWaypoints = pendingEditOriginalWaypoints
        )
        pendingEditOriginalWaypoints = null
    }

    override fun onGutterSaveFailed(waypoints: List<Waypoint>) {
        mainBlockingUiController.setInspectLoading(false)
        saveWaypointsAsPendingDraft(waypoints)
    }

    override fun onGutterSubmitFailed() {
        mainBlockingUiController.setInspectLoading(false)
    }

    override fun onStoreDitchNetworkClosed(spiNum: String?, waypoints: List<Waypoint>) {
        mainBlockingUiController.setInspectLoading(false)
        saveWaypointsAsPendingDraft(waypoints)
        activeSheet?.onWaypointsChanged = null
        activeSheet?.dismissAllowingStateLoss()
        activeSheet = null
        val resolvedSpiNum = spiNum?.takeIf { it.isNotBlank() }
            ?: waypoints.firstOrNull { it.type == WaypointType.START }
                ?.basicData?.get("SPI_NUM")
                ?.takeIf { it.isNotBlank() }
        if (resolvedSpiNum.isNullOrBlank()) {
            shouldReturnToInspectPreview = false
            inspectPreviewIntent = null
            pendingInspectPreviewReload = null
            inspectSheet?.onWaypointsChanged = null
            inspectSheet?.dismissAllowingStateLoss()
            inspectSheet = null
            isInEditingMode = false
            clearReferenceRoute()
            gutterMapController.clearPreviewLayer()
            clearWorkingMarkers()
            mapCameraController.setPersistentBottomInset(0)
            restoreMainUiAfterSheetClosed()
            loadGuttersByViewport(showFeedback = true)
            return
        }
        val token = LoginActivity.getSavedToken(requireContext())
        pendingInspectPreviewReload = token?.let {
            PendingInspectPreviewReload(
                spiNum = resolvedSpiNum,
                waypoints = waypoints.toList(),
                token = it
            )
        }
        shouldReturnToInspectPreview = false
        inspectPreviewIntent = null
        isInEditingMode = false
        clearReferenceRoute()
        gutterMapController.clearPreviewLayer()
        clearWorkingMarkers()
        mapCameraController.setPersistentBottomInset(0)
        restoreMainUiAfterSheetClosed()
        loadGuttersByViewport(showFeedback = true)
    }

    private fun openNoDitchReport() {
        if (measureManager?.isMeasuring == true) exitMeasureMode()
        if (isNoDitchPickMode) return
        enterNoDitchMode()
    }

    private fun openAddGutterFlow() {
        currentSessionResumedFromDraft = false
        gutterSessionUiCoordinator.startAddSession(
            isOfflineMainMode = isOfflineMainMode,
            hooks = buildSessionUiHooks()
        )
    }

    private fun buildSessionUiHooks(): GutterSessionUiCoordinator.Hooks {
        return GutterSessionUiCoordinator.Hooks(
            isHostFinishing = { false },
            prepareForNewMapSession = {
                gutterMapController.clearPreviewLayer()
                clearWorkingMarkers()
                fitCameraToAllGutters()
                isInEditingMode = true
                mainBlockingUiController.setMainButtonsEnabled(false)
                scopeGutterPolylineController.clear()
                submittedPolylines.forEach { it.remove() }
                submittedPolylines.clear()
            },
            prepareForResumedMapSession = {
                activeSheet?.onWaypointsChanged = null
                activeSheet?.dismissAllowingStateLoss()
                activeSheet = null
                gutterMapController.clearPreviewLayer()
                clearWorkingMarkers()
                isInEditingMode = true
                mainBlockingUiController.setMainButtonsEnabled(false)
                scopeGutterPolylineController.clear()
                submittedPolylines.forEach { it.remove() }
                submittedPolylines.clear()
            },
            bindSheet = { sheet, initialWaypointCount -> bindAddGutterSheet(sheet, initialWaypointCount) },
            showSheet = { sheet -> sheet.show(childFragmentManager, AddGutterBottomSheet.TAG) },
            onMapSessionReady = { draftId, isOffline, sheet ->
                currentSessionDraftId = draftId
                currentSessionIsOffline = isOffline
                activeSheet = sheet
                draftCoordinator.ensureDraftExists(
                    draftId = draftId,
                    waypoints = sheet.getWaypoints(),
                    spiTyp = sheet.getSelectedSpiTypCode(),
                    isOffline = isOffline,
                    isCurve = sheet.isCurveMode()
                )
            },
            onResumedWaypointsReady = { waypoints ->
                currentWaypoints = waypoints
                refreshWorkingLayer(waypoints)
            },
            onRefitRequested = { waypoints ->
                mapCameraController.fitCameraToWaypoints(waypoints, bottomOffsetRatio = 0.5, resetPaddingAfter = false)
            },
            onReloadRequested = { loadGuttersByViewport() }
        )
    }

    private fun bindAddGutterSheet(sheet: AddGutterBottomSheet, initialWaypointCount: Int = 0) {
        gutterSheetSessionBinder.bind(
            sheet = sheet,
            config = GutterSheetSessionBinder.Config(initialWaypointCount = initialWaypointCount, refitOnGrowth = true),
            hooks = GutterSheetSessionBinder.Hooks(
                onWaypointsUpdated = { waypoints ->
                    currentWaypoints = waypoints
                    refreshWorkingLayer(waypoints)
                    mainBlockingUiController.setMainButtonsEnabled(false)
                },
                onWaypointsCleared = {
                    currentSessionDraftId?.let { draftCoordinator.deleteDraftIfEffectivelyEmpty(it) }
                    currentSessionDraftId = null
                    currentSessionResumedFromDraft = false
                    isInEditingMode = false
                    mapCameraController.setPersistentBottomInset(0)
                    gutterMapController.clearPreviewLayer()
                    activeSheet = null
                    restoreMainUiAfterSheetClosed()
                    if (!isOfflineMainMode) loadGuttersByViewport(showFeedback = true) else refreshWorkingLayer(emptyList())
                },
                onAutoSaveRequested = { waypoints -> autoSaveSessionDraft(waypoints) },
                onRefitRequested = { waypoints ->
                    mapCameraController.fitCameraToWaypoints(waypoints, bottomOffsetRatio = 0.5, resetPaddingAfter = false)
                }
            )
        )
    }

    private fun openInspectForm(waypointIndex: Int, wp: Waypoint, latLng: LatLng) {
        mapCameraController.moveCameraToLatLngOffset(latLng, 0.75, mapCameraController.zoomForGutterSize(wp.basicData))
        val state = mapOverlayController.currentState()
        val intent = gutterFormNavigator.buildInspectIntent(
            waypointIndex = waypointIndex,
            waypoint = wp,
            latLng = latLng,
            wmtsLayer = currentWmtsLayer(),
            hostLastLocation = lastKnownLocation,
            referencePoints = if (isReferenceRouteActive) referenceRoutePoints else emptyList(),
            showPlan = state.showPlan,
            showWaterOld = state.showWaterOld,
            showPossible = state.showPossible,
            showRegion = state.showRegion
        )
        gutterFormLauncher.launch(intent)
    }

    private fun openAddForm(currentIndex: Int, wp: Waypoint, latLng: LatLng, isEditMode: Boolean = false) {
        mapCameraController.moveCameraToLatLngOffset(latLng, 0.75, mapCameraController.zoomForGutterSize(wp.basicData))
        val state = mapOverlayController.currentState()
        val sessionDraftId = ensureCurrentSessionDraftId()
        val launch = gutterFormNavigator.buildAddIntent(
            currentWaypoints = currentWaypoints,
            currentIndex = currentIndex,
            waypoint = wp,
            isEditMode = isEditMode,
            currentSessionDraftId = sessionDraftId,
            wmtsLayer = currentWmtsLayer(),
            sessionIsOffline = currentSessionIsOffline,
            hostLastLocation = lastKnownLocation,
            referencePoints = if (isReferenceRouteActive) referenceRoutePoints else emptyList(),
            showPlan = state.showPlan,
            showWaterOld = state.showWaterOld,
            showPossible = state.showPossible,
            showRegion = state.showRegion
        )
        currentSessionDraftId = launch.draftId
        gutterFormLauncher.launch(launch.intent)
    }

    private fun ensureCurrentSessionDraftId(): Long {
        currentSessionDraftId?.let { return it }
        return System.currentTimeMillis().also { currentSessionDraftId = it }
    }

    private fun currentWmtsLayer(): String = mapOverlayController.currentLayer()

    private fun openInspectBottomSheet(polyline: Polyline) {
        if (measureManager?.isMeasuring == true) return
        if (isInspecting) return
        val token = LoginActivity.getSavedToken(requireContext()) ?: return
        val start = inspectFlowCoordinator.prepareStart(polyline) ?: return
        isInspecting = true
        isInEditingMode = true
        lockInspectUi()
        mainBlockingUiController.setInspectLoading(true, "載入側溝資料中…")
        mapCameraController.fitCameraToWaypointsWithViewportFraction(start.routeWaypoints, viewportHeightFraction = 1.0 / 3.0)
        lifecycleScope.launch {
            try {
                when (val result = inspectFlowCoordinator.load(start, token)) {
                    is ApiResult.Success -> {
                        scopeGutterPolylineController.clear()
                        submittedPolylines.forEach { it.remove() }
                        submittedPolylines.clear()
                        inspectPreviewIntent = Intent(result.data.intent)
                        shouldReturnToInspectPreview = false
                        inspectWaypoints = buildInspectWaypoints(result.data.nodeDetailsList)
                        currentWaypoints = inspectWaypoints
                        val isCurve = result.data.ditch.isCurve?.trim() == "1" ||
                            result.data.ditch.isCurve?.trim()?.equals("true", true) == true
                        referenceRoutePoints = start.routeWaypoints.mapNotNull { it.latLng }
                        isReferenceRouteActive = false
                        refreshWorkingLayer(inspectWaypoints, isCurve)
                        activeSheet?.hideSelf()
                        mainBlockingUiController.setInspectLoading(false)
                        val launched = launchInspectSafely(result.data.intent)
                        if (!launched) {
                            isInspecting = false
                            isInEditingMode = false
                            inspectPreviewIntent = null
                            shouldReturnToInspectPreview = false
                            clearReferenceRoute()
                            gutterMapController.clearPreviewLayer()
                            clearWorkingMarkers()
                            unlockInspectUiIfIdle()
                            loadGuttersByViewport(showFeedback = true)
                        }
                    }
                    is ApiResult.Error -> {
                        isInspecting = false
                        isInEditingMode = false
                        inspectPreviewIntent = null
                        shouldReturnToInspectPreview = false
                        unlockInspectUiIfIdle()
                        if (authExpiredHandler.handleIfAuthExpired(result)) return@launch
                        Toast.makeText(requireContext(), if (result.message == "查無側溝資料") getString(R.string.msg_no_line_data) else "查詢失敗(${result.code}): ${result.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } finally {
                mainBlockingUiController.setInspectLoading(false)
            }
        }
    }

    private fun launchInspectSafely(intent: Intent): Boolean {
        if (!lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) return false
        return runCatching { inspectLauncher.launch(intent); true }.getOrElse { false }
    }

    private fun handleAddSheetActivityResult(result: androidx.activity.result.ActivityResult) {
        when {
            result.resultCode == Activity.RESULT_OK && pendingWaypointFormIndex >= 0 -> {
                val data = result.data
                val returnedDraftId = data?.getLongExtra(GutterFormActivity.EXTRA_SESSION_DRAFT_ID, 0L) ?: 0L
                if (returnedDraftId > 0L) currentSessionDraftId = returnedDraftId
                val resultLat = data?.getDoubleExtra(GutterFormActivity.RESULT_LATITUDE, Double.NaN) ?: Double.NaN
                val resultLng = data?.getDoubleExtra(GutterFormActivity.RESULT_LONGITUDE, Double.NaN) ?: Double.NaN
                val fallbackLatLng = currentWaypoints.getOrNull(pendingWaypointFormIndex)?.latLng
                val effectiveLat = if (!resultLat.isNaN()) resultLat else fallbackLatLng?.latitude ?: Double.NaN
                val effectiveLng = if (!resultLng.isNaN()) resultLng else fallbackLatLng?.longitude ?: Double.NaN
                if (!effectiveLat.isNaN() && !effectiveLng.isNaN()) {
                    activeSheet?.updateWaypointLocation(pendingWaypointFormIndex, LatLng(effectiveLat, effectiveLng))
                }
                val rawData = GutterFormContract.readResultData(result.data)
                lifecycleScope.launch {
                    val newData = PhotoUriStore.normalizeBasicDataPhotoUris(requireContext(), rawData, prefix = "GUTTER_EXT_")
                    activeSheet?.updateWaypointBasicData(pendingWaypointFormIndex, newData)
                    currentWaypoints = activeSheet?.getWaypoints() ?: currentWaypoints
                    refreshWorkingForEditFlow(currentWaypoints)
                    resetHighlightedMarker()
                    pendingWaypointFormIndex = -1
                    activeSheet?.showSelf()
                    if (currentWaypoints.isNotEmpty()) {
                        mapCameraController.fitCameraToWaypoints(currentWaypoints, bottomOffsetRatio = 0.52, resetPaddingAfter = false)
                    }
                }
            }
            result.resultCode == Activity.RESULT_CANCELED -> {
                resetHighlightedMarker()
                pendingWaypointFormIndex = -1
                activeSheet?.showSelf()
                currentWaypoints = activeSheet?.getWaypoints() ?: currentWaypoints
                refreshWorkingForEditFlow(currentWaypoints)
            }
            else -> {
                activeSheet?.showSelf()
            }
        }
    }

    private fun handleInspectSheetActivityResult(result: androidx.activity.result.ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val returnedDraftId = data?.getLongExtra(GutterFormActivity.EXTRA_SESSION_DRAFT_ID, 0L) ?: 0L
            if (returnedDraftId > 0L) currentSessionDraftId = returnedDraftId
            val idx = data?.getIntExtra(GutterFormActivity.RESULT_WAYPOINT_INDEX, -1) ?: -1
            if (idx >= 0) {
                val rawData = GutterFormContract.readResultData(data)
                lifecycleScope.launch {
                    val newData = PhotoUriStore.normalizeBasicDataPhotoUris(requireContext(), rawData, prefix = "GUTTER_EXT_")
                    inspectWaypoints.getOrNull(idx)?.basicData = newData
                }
            }
        }
        inspectSheet?.showSelf()
        mapCameraController.fitCameraToWaypointsWithViewportFraction(inspectWaypoints, viewportHeightFraction = 1.0 / 3.0)
    }

    private fun handleInspectEditResult(data: Intent?) {
        val json = data?.getStringExtra(GutterInspectActivity.EXTRA_RESULT_WAYPOINTS_JSON) ?: return
        val spiNum = data.getStringExtra(GutterInspectActivity.EXTRA_RESULT_SPI_NUM) ?: ""
        val spiTyp = data.getStringExtra(GutterInspectActivity.EXTRA_RESULT_SPI_TYP) ?: ""
        val spiState = data.getStringExtra(GutterInspectActivity.EXTRA_RESULT_SPI_STATE) ?: ""
        initialSpiState = spiState
        val isCurveRaw = data.getStringExtra(GutterInspectActivity.EXTRA_RESULT_IS_CURVE) ?: "0"
        val isCurve = isCurveRaw.trim() == "1" || isCurveRaw.trim().equals("true", true)
        val oldSheet = childFragmentManager.findFragmentByTag(AddGutterBottomSheet.TAG) as? AddGutterBottomSheet
        oldSheet?.onWaypointsChanged = null
        oldSheet?.dismissAllowingStateLoss()
        childFragmentManager.executePendingTransactions()
        activeSheet = null
        val wpsType = object : com.google.gson.reflect.TypeToken<List<WaypointSnapshot>>() {}.type
        val snapshots: List<WaypointSnapshot> = try { Gson().fromJson(json, wpsType) } catch (_: Exception) { emptyList() }
        val wps = snapshots.map {
            val wpType = WaypointType.entries.firstOrNull { t -> t.name == it.type } ?: WaypointType.NODE
            val latLng = if (it.latitude != null && it.longitude != null) LatLng(it.latitude, it.longitude) else null
            Waypoint(wpType, it.label, latLng, it.basicData, it.uid.ifBlank { it.basicData["_nodeId"] ?: "${wpType.name}_${it.label}" })
        }
        currentSessionDraftId = null
        currentSessionResumedFromDraft = false
        shouldReturnToInspectPreview = true
        isInEditingMode = true
        scopeGutterPolylineController.clear()
        submittedPolylines.forEach { it.remove() }
        submittedPolylines.clear()
        editLatLngSnapshot = buildLatLngSnapshot(wps)
        hasShownEditPolyline = false
        val sheet = AddGutterBottomSheet.newInstanceForEdit(wps, spiNum, isCurve, spiTyp)
        var lastWaypointsSize = wps.size
        sheet.onWaypointsChanged = { updated ->
            if (updated == null) {
                mapCameraController.setPersistentBottomInset(0)
                activeSheet = null
                shouldReturnToInspectPreview = false
                isInEditingMode = false
                clearReferenceRoute()
                gutterMapController.clearPreviewLayer()
                clearWorkingMarkers()
                unlockInspectUiIfIdle()
                loadGuttersByViewport(showFeedback = true)
            } else {
                val shouldRefit = updated.size > lastWaypointsSize
                lastWaypointsSize = updated.size
                currentWaypoints = updated.toMutableList()
                refreshWorkingForEditFlow(updated)
                autoSaveSessionDraft(updated)
                if (shouldRefit) {
                    mapCameraController.fitCameraToWaypoints(updated, bottomOffsetRatio = 0.5, resetPaddingAfter = false)
                }
            }
        }
        activeSheet = sheet
        sheet.show(childFragmentManager, AddGutterBottomSheet.TAG)
        currentWaypoints = wps.toMutableList()
        refreshWorkingMarkers(wps)
        mapCameraController.fitCameraToWaypointsWithViewportFraction(wps, viewportHeightFraction = 1.0 / 3.0)
    }

    private fun toggleMeasureMode() {
        val mgr = measureManager ?: return
        if (mgr.isMeasuring) exitMeasureMode() else enterMeasureMode()
    }

    private fun enterMeasureMode() { measureModeUiController.enter(measureManager) }
    private fun exitMeasureMode() { measureModeUiController.exit(measureManager) }
    private fun updateMeasureDistanceDisplay(meters: Double?) { measureModeUiController.updateDistanceDisplay(meters) }

    private fun enterNoDitchMode() {
        isNoDitchPickMode = true
        noDitchPickedLatLng = null
        clearNoDitchMarker()
        noDitchModeUiController.enter()
        noDitchModeUiController.setPickedLatLng(null)
        setNoDitchMapClickListenerEnabled(true)
    }

    private fun exitNoDitchMode() {
        isNoDitchPickMode = false
        setNoDitchMapClickListenerEnabled(false)
        noDitchPickedLatLng = null
        clearNoDitchMarker()
        noDitchModeUiController.exit()
    }

    private fun resetNoDitchPick() {
        noDitchPickedLatLng = null
        clearNoDitchMarker()
        noDitchModeUiController.setPickedLatLng(null)
        if (isNoDitchPickMode) setNoDitchMapClickListenerEnabled(true)
    }

    private fun submitNoDitch(note: String) {
        val latLng = noDitchPickedLatLng ?: return
        val token = LoginActivity.getSavedToken(requireContext())
        if (token.isNullOrEmpty()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.no_ditch_mode_title))
                .setMessage(getString(R.string.msg_not_logged_in))
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }
        noDitchModeUiController.setSubmitting(true)
        lifecycleScope.launch {
            val result = gutterRepository.storeNoDitch(latitude = latLng.latitude, longitude = latLng.longitude, note = note, token = token)
            noDitchModeUiController.setSubmitting(false)
            when (result) {
                is ApiResult.Success -> {
                    val message = result.data.message ?: getString(R.string.msg_submit_success)
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.no_ditch_mode_title))
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok) { _, _ -> exitNoDitchMode() }
                        .show()
                }
                is ApiResult.Error -> {
                    if (authExpiredHandler.handleIfAuthExpired(result)) return@launch
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.no_ditch_mode_title))
                        .setMessage(result.message)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            }
        }
    }

    private fun setNoDitchMapClickListenerEnabled(enabled: Boolean) { isNoDitchPickClickEnabled = enabled }

    private fun handleMainMapTap(latLng: LatLng) {
        if (isNoDitchPickClickEnabled && isNoDitchPickMode && noDitchPickedLatLng == null) {
            onNoDitchLatLngPicked(latLng)
            return
        }
        if (!isNoDitchPickMode && mapOverlayController.currentState().showNoDitchPoints) {
            fetchAndShowNoDitchPointNoteAt(latLng, showNoNoteToast = false)
        }
    }

    private fun onNoDitchLatLngPicked(latLng: LatLng) {
        val map = googleMap ?: return
        noDitchPickedLatLng = latLng
        clearNoDitchMarker()
        noDitchMarker = map.addMarker(
            com.google.android.gms.maps.model.MarkerOptions()
                .position(latLng)
                .icon(buildNoDitchMarkerIcon())
                .anchor(0.5f, 1.0f)
        )
        setNoDitchMapClickListenerEnabled(false)
        noDitchModeUiController.setPickedLatLng(latLng)
    }

    private fun clearNoDitchMarker() {
        noDitchMarker?.remove()
        noDitchMarker = null
    }

    private fun buildNoDitchMarkerIcon(): com.google.android.gms.maps.model.BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_noditch_pin)
            ?: return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
        val sizePx = (60f * resources.displayMetrics.density).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun loadNoDitchPointsForVisibleArea() {
        val map = googleMap ?: return
        val bounds = map.projection.visibleRegion.latLngBounds
        val bbox = buildNoDitchPointsWfsBbox(bounds)
        lifecycleScope.launch {
            try {
                val response = GutterApiClient.instance.getNoDitchPointsByBbox(bbox = bbox)
                if (response.isSuccessful) {
                    val features = response.body()?.features ?: emptyList()
                    updateNoDitchPointsMarkers(features.map {
                        val lat = if (it.properties.latitude != 0.0) it.properties.latitude else it.geometry.coordinates.getOrNull(1) ?: 0.0
                        val lng = if (it.properties.longitude != 0.0) it.properties.longitude else it.geometry.coordinates.getOrNull(0) ?: 0.0
                        NoDitchPoint(id = it.id, latitude = lat, longitude = lng, note = it.properties.note)
                    })
                }
            } catch (e: Exception) {
                android.util.Log.e("MapWorkspaceFragment", "Failed to load no ditch points", e)
            }
        }
    }

    private fun updateNoDitchPointsMarkers(points: List<NoDitchPoint>) {
        val map = googleMap ?: return
        noDitchPointsMarkers.forEach { it.remove() }
        noDitchPointsMarkers.clear()
        points.forEach { point ->
            val marker = map.addMarker(
                com.google.android.gms.maps.model.MarkerOptions()
                    .position(point.latLng)
                    .icon(buildNoDitchPointIcon())
                    .anchor(0.5f, 1.0f)
                    .title(point.note ?: "無側溝點位")
            )
            marker?.tag = point
            marker?.let { noDitchPointsMarkers.add(it) }
        }
        noDitchPoints = points.toMutableList()
    }

    private fun clearNoDitchPointsMarkers() {
        noDitchPointsMarkers.forEach { it.remove() }
        noDitchPointsMarkers.clear()
        noDitchPoints.clear()
    }

    private fun buildNoDitchPointIcon(): com.google.android.gms.maps.model.BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_noditch_point)
            ?: return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
        val sizePx = (20f * resources.displayMetrics.density).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun buildNoDitchPointsWfsBbox(bounds: com.google.android.gms.maps.model.LatLngBounds): String {
        val sw = bounds.southwest
        val ne = bounds.northeast
        return "${sw.longitude},${sw.latitude},${ne.longitude},${ne.latitude},EPSG:4326"
    }

    private fun buildNoDitchPointsWmsBbox(bounds: com.google.android.gms.maps.model.LatLngBounds): String {
        val sw = bounds.southwest
        val ne = bounds.northeast
        return "${sw.latitude},${sw.longitude},${ne.latitude},${ne.longitude}"
    }

    private fun fetchAndShowNoDitchPointNote(marker: com.google.android.gms.maps.model.Marker) {
        fetchAndShowNoDitchPointNoteAt(marker.position, showNoNoteToast = true)
    }

    private fun fetchAndShowNoDitchPointNoteAt(targetLatLng: LatLng, showNoNoteToast: Boolean) {
        val map = googleMap ?: return
        val bounds = map.projection.visibleRegion.latLngBounds
        val mapView = binding.map
        val width = mapView.width
        val height = mapView.height
        if (width <= 0 || height <= 0) return
        val bbox = buildNoDitchPointsWmsBbox(bounds)
        val screenPoint = map.projection.toScreenLocation(targetLatLng)
        lifecycleScope.launch {
            try {
                val response = GutterApiClient.instance.getNoDitchPointsFeatureInfo(
                    bbox = bbox,
                    width = width,
                    height = height,
                    i = screenPoint.x,
                    j = screenPoint.y,
                    featureCount = 10
                )
                if (!response.isSuccessful) return@launch
                val note = response.body()?.features?.asSequence()?.mapNotNull { it.properties.note }?.firstOrNull()
                if (note.isNullOrBlank()) {
                    if (showNoNoteToast) Toast.makeText(requireContext(), "此點位無備註", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("無側溝點位備註")
                    .setMessage(note)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            } catch (e: Exception) {
                android.util.Log.e("MapWorkspaceFragment", "Failed to fetch no ditch point note", e)
            }
        }
    }

    private fun lockInspectUi() {
        isInspectUiLocked = true
        mainBlockingUiController.setMainButtonsEnabled(false)
    }

    private fun unlockInspectUiIfIdle() {
        if (activeSheet != null || inspectSheet != null || isInspecting || measureManager?.isMeasuring == true) {
            mainBlockingUiController.setMainButtonsEnabled(false)
            return
        }
        isInspectUiLocked = false
        mainBlockingUiController.setMainButtonsEnabled(true)
        consumePendingForceReloadIfPossible()
    }

    private fun setMainButtonsEnabledRespectingInspectLock(enabled: Boolean) {
        if (enabled && isInspectUiLocked) {
            mainBlockingUiController.setMainButtonsEnabled(false)
        } else {
            mainBlockingUiController.setMainButtonsEnabled(enabled)
        }
    }

    private fun restoreMainUiAfterSheetClosed() {
        if (mainBlockingUiController.isBusyBlocking()) {
            mainBlockingUiController.setMainButtonsEnabled(false)
            return
        }
        if (activeSheet != null || inspectSheet != null || isInspecting || measureManager?.isMeasuring == true) {
            mainBlockingUiController.setMainButtonsEnabled(false)
            return
        }
        isInspectUiLocked = false
        mainBlockingUiController.setMainButtonsEnabled(true)
        consumePendingForceReloadIfPossible()
    }

    private fun showPendingDraftsSheet() {
        if (isInspectUiLocked || isInspecting || shouldReturnToInspectPreview) return
        gutterSessionUiCoordinator.showPendingDrafts { draft -> resumePendingDraft(draft) }
    }

    private fun resumePendingDraft(draft: GutterSessionDraft) {
        currentSessionResumedFromDraft = true
        gutterSessionUiCoordinator.resumeDraft(
            draft = draft,
            isOfflineMainMode = isOfflineMainMode,
            hooks = buildSessionUiHooks()
        )
    }

    private fun refreshWorkingLayer(waypoints: List<Waypoint>, isCurve: Boolean? = null) {
        gutterMapController.refreshWorkingLayer(waypoints = waypoints, isCurve = isCurve ?: activeSheet?.isCurveMode() == true)
    }

    private fun refreshWorkingMarkers(waypoints: List<Waypoint>) { gutterMapController.refreshWorkingMarkers(waypoints) }

    private fun refreshWorkingForEditFlow(waypoints: List<Waypoint>) {
        if (activeSheet == null || activeSheet?.isEditMode() != true) {
            refreshWorkingLayer(waypoints)
            return
        }
        val after = buildLatLngSnapshot(waypoints)
        if (!hasShownEditPolyline) {
            val before = editLatLngSnapshot
            val latLngChanged = before == null || after != before
            if (latLngChanged) {
                if (!isReferenceRouteActive && referenceRoutePoints.size >= 2) {
                    setReferenceRoute(referenceRoutePoints)
                }
                hasShownEditPolyline = true
            }
        }
        editLatLngSnapshot = after
        refreshWorkingLayer(waypoints)
    }

    private fun buildLatLngSnapshot(waypoints: List<Waypoint>): List<Pair<Long, Long>> {
        fun quantize(v: Double): Long = kotlin.math.round(v * 1_000_000.0).toLong()
        return waypoints.mapNotNull { it.latLng }.map { quantize(it.latitude) to quantize(it.longitude) }
    }

    private fun setReferenceRoute(points: List<LatLng>) {
        isReferenceRouteActive = true
        referenceRoutePoints = points
        gutterMapController.showReferenceRoute(points)
    }

    private fun clearReferenceRoute() {
        isReferenceRouteActive = false
        referenceRoutePoints = emptyList()
        gutterMapController.clearReferenceRoute()
    }

    private fun renderReferenceRouteIfActive() {
        if (!isReferenceRouteActive) return
        gutterMapController.showReferenceRoute(referenceRoutePoints)
    }

    private fun clearWorkingMarkers() {
        gutterMapController.clearWorkingMarkers()
        inspectMarkerController.clear()
    }

    private fun parseLooseBoolean(raw: String?): Boolean {
        val v = raw?.trim()?.lowercase()
        return when (v) {
            "1", "true", "t", "y", "yes" -> true
            else -> false
        }
    }

    private fun drawSubmittedGutter(waypoints: List<Waypoint>) {
        val map = googleMap ?: return
        val routePoints = waypoints.mapNotNull { it.latLng }
        if (routePoints.size < 2) return
        val polyline = map.addPolyline(
            PolylineOptions()
                .addAll(routePoints)
                .color(resolveGutterPolylineColor())
                .width(10f)
                .geodesic(true)
                .clickable(true)
        )
        polyline.tag = ArrayList(waypoints)
        submittedPolylines.add(polyline)
    }

    private fun buildInspectWaypoints(nodes: List<com.example.taoyuangutter.api.NodeDetails>): List<Waypoint> {
        return nodes.mapNotNull { node ->
            val lat = node.latitude?.toDoubleOrNull() ?: return@mapNotNull null
            val lng = node.longitude?.toDoubleOrNull() ?: return@mapNotNull null
            val type = when (node.nodeAttr) {
                "1" -> WaypointType.START
                "3" -> WaypointType.END
                else -> WaypointType.NODE
            }
            val label = when (type) {
                WaypointType.START -> "起點"
                WaypointType.END -> "終點"
                WaypointType.NODE -> "節點"
            }
            Waypoint(
                type = type,
                label = label,
                latLng = LatLng(lat, lng),
                basicData = hashMapOf(
                    "IS_PENDING_DEPLOY" to (if (parseLooseBoolean(node.isPendingDeploy)) "1" else "0"),
                    "is_virtual" to (if (parseLooseBoolean(node.isVirtual)) "1" else "0")
                ),
                uid = node.nodeId?.toString() ?: "${type.name}_${lat}_${lng}"
            )
        }
    }

    private fun resolveGutterPolylineColor(): Int = Color.parseColor("#562ECB")

    private fun highlightMarker(waypointIndex: Int) {
        val waypoints = if (inspectSheet != null) inspectWaypoints else currentWaypoints
        inspectMarkerController.highlightMarker(waypointIndex, waypoints)
    }

    private fun resetHighlightedMarker() {
        val waypoints = if (inspectSheet != null) inspectWaypoints else currentWaypoints
        inspectMarkerController.resetHighlightedMarker(waypoints)
    }

    private fun handleMainMapCameraMoveStarted(reason: Int) {
        when (reason) {
            GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE -> {
                isMainMapGestureActive = true
                mainMapLoadIndicatorController.prepareForNewOperation(currentMainMapZoom())
            }
            GoogleMap.OnCameraMoveStartedListener.REASON_API_ANIMATION,
            GoogleMap.OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION -> isMainMapGestureActive = false
            else -> isMainMapGestureActive = false
        }
    }

    private fun handleMainMapCameraIdle() {
        val zoom = currentMainMapZoom()
        mainMapLoadIndicatorController.syncZoom(zoom)
        if (pendingLocationRecenterReload) {
            pendingLocationRecenterReload = false
            requestForceScopeReload("location recenter")
            return
        }
        if (isMainMapGestureActive) {
            isMainMapGestureActive = false
            requestUserInteractionScopeSearch()
            return
        }
        consumePendingForceReloadIfPossible()
    }

    private fun handleMainMapLocationUpdated(location: Location) {
        lastKnownLocation = location
        pendingLocationRecenterReload = true
        pendingUserLocationRecenter = false
    }

    private fun handleMainMapLocationRecenterFinished() {
        if (!pendingLocationRecenterReload) return
        pendingLocationRecenterReload = false
        requestForceScopeReload("location recenter finished")
    }

    private fun currentMainMapZoom(): Float = googleMap?.cameraPosition?.zoom ?: 0f

    private fun requestUserInteractionScopeSearch() {
        if (isOfflineMainMode) return
        val zoom = currentMainMapZoom()
        mainMapLoadIndicatorController.syncZoom(zoom)
        if (zoom < MAIN_MAP_SCOPE_SEARCH_MIN_ZOOM) {
            mainMapLoadIndicatorController.prepareForNewOperation(zoom)
            return
        }
        if (!canRunMainMapScopeQuery()) return
        scopeMapCoordinator.loadDebounced(
            scope = lifecycleScope,
            config = buildScopeLoadConfig(showFeedback = true),
            hooks = buildScopeLoadHooks(),
            debounceMs = GUTTER_LOAD_DEBOUNCE_MS
        )
    }

    private fun requestBackgroundScopeRefresh() {
        if (isOfflineMainMode) return
        val zoom = currentMainMapZoom()
        mainMapLoadIndicatorController.syncZoom(zoom)
        if (zoom < MAIN_MAP_SCOPE_SEARCH_MIN_ZOOM) {
            mainMapLoadIndicatorController.prepareForNewOperation(zoom)
            return
        }
        if (!canRunMainMapScopeQuery()) return
        scopeMapCoordinator.load(
            scope = lifecycleScope,
            config = buildScopeLoadConfig(showFeedback = false),
            hooks = buildScopeLoadHooks()
        )
    }

    private fun requestForceScopeReload(reason: String) {
        if (isOfflineMainMode) return
        val zoom = currentMainMapZoom()
        mainMapLoadIndicatorController.syncZoom(zoom)
        if (zoom < MAIN_MAP_SCOPE_SEARCH_MIN_ZOOM) {
            pendingForceReload = null
            forceReloadInFlightId = null
            mainMapLoadIndicatorController.prepareForNewOperation(zoom)
            return
        }
        val token = LoginActivity.getSavedToken(requireContext())
        if (token.isNullOrBlank()) return
        val request = PendingForceReload(id = ++nextForceReloadId, reason = reason)
        if (!canRunMainMapScopeQuery() || forceReloadInFlightId != null) {
            pendingForceReload = request
            return
        }
        pendingForceReload = null
        executeForceScopeReload(request)
    }

    private fun executeForceScopeReload(request: PendingForceReload) {
        forceReloadInFlightId = request.id
        val zoom = currentMainMapZoom()
        if (zoom < MAIN_MAP_SCOPE_SEARCH_MIN_ZOOM) {
            forceReloadInFlightId = null
            mainMapLoadIndicatorController.prepareForNewOperation(zoom)
            return
        }
        mainMapLoadIndicatorController.beginLoading(zoom)
        scopeMapCoordinator.load(
            scope = lifecycleScope,
            config = buildScopeLoadConfig(showFeedback = true),
            hooks = buildScopeLoadHooks()
        )
    }

    private fun consumePendingForceReloadIfPossible() {
        if (forceReloadInFlightId != null) return
        val pending = pendingForceReload ?: return
        if (currentMainMapZoom() < MAIN_MAP_SCOPE_SEARCH_MIN_ZOOM) return
        if (!canRunMainMapScopeQuery()) return
        pendingForceReload = null
        executeForceScopeReload(pending)
    }

    private fun canRunMainMapScopeQuery(): Boolean {
        val token = LoginActivity.getSavedToken(requireContext())
        return lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED) &&
            googleMap != null &&
            !token.isNullOrBlank() &&
            !mainBlockingUiController.isBusyBlocking() &&
            activeSheet == null &&
            !isInEditingMode &&
            inspectSheet == null &&
            !isInspecting &&
            !isInspectUiLocked &&
            !shouldReturnToInspectPreview
    }

    private fun loadGuttersByViewportDebounced() { requestUserInteractionScopeSearch() }
    private fun loadGuttersByViewport(showFeedback: Boolean = true) {
        if (showFeedback) requestForceScopeReload("legacy-visible-load") else requestBackgroundScopeRefresh()
    }

    private fun buildScopeLoadConfig(showFeedback: Boolean): ScopeMapCoordinator.Config {
        return ScopeMapCoordinator.Config(
            isOfflineMode = isOfflineMainMode,
            isBlocked = isInEditingMode,
            token = LoginActivity.getSavedToken(requireContext()),
            showFeedback = showFeedback
        )
    }

    private fun buildScopeLoadHooks(): ScopeMapCoordinator.Hooks {
        return ScopeMapCoordinator.Hooks(
            onBeforeDraw = {
                submittedPolylines.forEach { it.remove() }
                submittedPolylines.clear()
            },
            onLoadingStarted = { mainMapLoadIndicatorController.beginLoading(currentMainMapZoom()) },
            onLoadingFinished = {
                mainMapLoadIndicatorController.finishLoading(currentMainMapZoom())
                forceReloadInFlightId = null
                pendingInspectPreviewReload?.let { pending ->
                    pendingInspectPreviewReload = null
                    reopenInspectPreviewAfterUpdate(pending.spiNum, pending.waypoints, pending.token)
                }
                consumePendingForceReloadIfPossible()
            },
            onLoadingFailed = { _, _ ->
                mainMapLoadIndicatorController.failLoading(currentMainMapZoom())
                forceReloadInFlightId = null
                consumePendingForceReloadIfPossible()
            },
            onAuthExpired = { _, message ->
                authExpiredHandler.handleIfAuthExpired(
                    ApiResult.Error(
                        message = message,
                        code = 401
                    )
                ) {
                    saveWaypointsAsPendingDraft(currentWaypoints)
                }
            },
            onSilentError = { _, message -> android.util.Log.w("ScopeSearch", "查詢失敗: $message") }
        )
    }

    private fun fitCameraToAllGutters() {
        mapCameraController.fitCameraToTaggedPolylines(submittedPolylines)
    }

    private fun persistServerIdsIntoDraft(
        spiNum: String?,
        waypoints: List<Waypoint>,
        nodes: List<DitchNode>
    ): Pair<List<Waypoint>, Long?> {
        val resolvedSpiNum = spiNum?.takeIf { it.isNotBlank() }
            ?: waypoints.firstOrNull { it.type == WaypointType.START }?.basicData?.get("SPI_NUM")
        val updatedWaypoints = waypoints.mapIndexed { index, waypoint ->
            val merged = HashMap(waypoint.basicData).apply {
                if (!resolvedSpiNum.isNullOrBlank()) put("SPI_NUM", resolvedSpiNum)
                nodes.getOrNull(index)?.let { node -> put("_nodeId", node.nodeId.toString()) }
            }
            waypoint.copy(basicData = merged)
        }
        val saveResult = draftCoordinator.autoSaveSessionDraft(
            waypoints = updatedWaypoints,
            currentSessionDraftId = currentSessionDraftId,
            spiTyp = resolveCurrentSessionSpiTyp(updatedWaypoints),
            isOffline = currentSessionIsOffline,
            isCurve = activeSheet?.isCurveMode() ?: false
        )
        val savedDraftId = saveResult?.draftId ?: currentSessionDraftId
        currentSessionDraftId = savedDraftId
        return updatedWaypoints to savedDraftId
    }

    private fun resolveCurrentSessionSpiTyp(waypoints: List<Waypoint>): String? {
        return activeSheet?.getSelectedSpiTypCode()
            ?: waypoints.firstOrNull { it.type == WaypointType.START }?.basicData?.get("SPI_TYP")
            ?: waypoints.firstOrNull { it.type == WaypointType.START }?.basicData?.get("NODE_TYP")
    }

    private fun autoSaveSessionDraft(waypoints: List<Waypoint>) {
        val result = draftCoordinator.autoSaveSessionDraft(
            waypoints = waypoints,
            currentSessionDraftId = currentSessionDraftId,
            spiTyp = resolveCurrentSessionSpiTyp(waypoints),
            isOffline = currentSessionIsOffline,
            isCurve = activeSheet?.isCurveMode() ?: false
        ) ?: return
        currentSessionDraftId = result.draftId
    }

    private fun saveWaypointsAsPendingDraft(waypoints: List<Waypoint>) {
        autoSaveSessionDraft(waypoints)
    }

    private fun finalizePhotoUploadFlow(
        spiNum: String?,
        persistedWaypoints: List<Waypoint>,
        nodes: List<DitchNode>,
        pendingDraftId: Long?,
        token: String,
        originalWaypoints: List<WaypointSnapshot>? = null
    ) {
        lifecycleScope.launch {
            try {
                val uploadWaypoints = if (currentSessionResumedFromDraft) {
                    persistedWaypoints
                } else {
                    buildEditPhotoUploadWaypoints(persistedWaypoints, originalWaypoints)
                }
                when (val result = uploadWaypointPhotos(uploadWaypoints, nodes, token, originalWaypoints)) {
                    is PhotoUploadManager.UploadBatchResult.Completed -> {
                        val failCount = result.failCount
                        mainBlockingUiController.setInspectLoading(false)
                        if (failCount == 0) {
                            activeSheet?.dismissAllowingStateLoss()
                            activeSheet = null
                            pendingDraftId?.let { draftId ->
                                draftCoordinator.deleteDraftAndLocalPhotos(
                                    requireContext(),
                                    draftId,
                                    fallbackWaypoints = persistedWaypoints
                                )
                            }
                            currentSessionDraftId = null
                            currentSessionResumedFromDraft = false
                            if (initialSpiState == "2" && !spiNum.isNullOrBlank()) {
                                initialSpiState = null
                                showRestoreStateDialog(spiNum, persistedWaypoints, token)
                            } else if (!spiNum.isNullOrBlank()) {
                                reopenInspectPreviewAfterUpdate(spiNum, persistedWaypoints, token)
                            } else {
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("上傳成功")
                                    .setMessage(getString(R.string.msg_gutter_uploaded))
                                    .setPositiveButton(getString(R.string.confirm), null)
                                    .show()
                            }
                        } else {
                            val authExpiredFailure = result.failures.firstOrNull { it.code == 401 }
                            if (authExpiredFailure != null) {
                                if (authExpiredHandler.handleIfAuthExpired(
                                        ApiResult.Error(
                                            message = authExpiredFailure.message,
                                            code = authExpiredFailure.code
                                        )
                                    ) {
                                        saveWaypointsAsPendingDraft(persistedWaypoints)
                                    }
                                ) {
                                    return@launch
                                }
                            }
                            val errorUi = UploadFailureClassifier.forPhotoBatchFailures(result.failures)
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle("上傳失敗")
                                .setMessage(errorUi.buildDialogMessage())
                                .setNegativeButton("存入草稿") { _, _ ->
                                    activeSheet?.dismissAllowingStateLoss()
                                    activeSheet = null
                                }
                                .setPositiveButton("重傳") { _, _ ->
                                    finalizePhotoUploadFlow(
                                        spiNum,
                                        persistedWaypoints,
                                        nodes,
                                        pendingDraftId,
                                        token,
                                        originalWaypoints
                                    )
                                }
                                .setCancelable(false)
                                .show()
                        }
                    }
                    is PhotoUploadManager.UploadBatchResult.TimedOut -> {
                        mainBlockingUiController.setInspectLoading(false)
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(R.string.msg_photo_upload_timeout_title))
                            .setMessage(
                                UploadFailureClassifier.forPhotoTimeout(
                                    result.failCount,
                                    result.completedCount
                                ).buildDialogMessage()
                            )
                            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                                activeSheet?.dismissAllowingStateLoss()
                                activeSheet = null
                                restoreMainUiAfterSheetClosed()
                            }
                            .setCancelable(false)
                            .show()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mainBlockingUiController.setInspectLoading(false)
                val errorUi = UploadFailureClassifier.forPhotoException(e.localizedMessage)
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("上傳失敗")
                    .setMessage(errorUi.buildDialogMessage())
                    .setNegativeButton("存入草稿") { _, _ ->
                        activeSheet?.dismissAllowingStateLoss()
                        activeSheet = null
                    }
                    .setPositiveButton("重傳") { _, _ ->
                        finalizePhotoUploadFlow(
                            spiNum,
                            persistedWaypoints,
                            nodes,
                            pendingDraftId,
                            token,
                            originalWaypoints
                        )
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    private suspend fun uploadWaypointPhotos(
        waypoints: List<Waypoint>,
        nodes: List<DitchNode>,
        token: String,
        originalWaypoints: List<WaypointSnapshot>? = null
    ): PhotoUploadManager.UploadBatchResult {
        val pendingCount = photoUploadManager.countPendingPhotos(
            waypoints = waypoints,
            nodes = nodes,
            originalWaypoints = originalWaypoints
        )
        if (pendingCount == 0) return PhotoUploadManager.UploadBatchResult.Completed(0)
        mainBlockingUiController.beginPhotoUpload(pendingCount)
        try {
            return photoUploadManager.uploadWaypointPhotos(
                waypoints = waypoints,
                nodes = nodes,
                token = token,
                originalWaypoints = originalWaypoints,
                listener = object : PhotoUploadManager.UploadListener {
                    override fun onProgressUpdate(completedCount: Int, totalCount: Int) {}
                    override fun onPhotoUploadResult(success: Boolean) {
                        mainBlockingUiController.recordPhotoUploadResult(success)
                    }
                }
            )
        } finally {
            mainBlockingUiController.endPhotoUpload()
        }
    }

    private fun buildEditPhotoUploadWaypoints(
        waypoints: List<Waypoint>,
        originalWaypoints: List<WaypointSnapshot>?
    ): List<Waypoint> {
        if (originalWaypoints.isNullOrEmpty()) return waypoints
        val originalByUid = originalWaypoints.mapNotNull { snapshot ->
            val uid = snapshot.uid.takeIf { it.isNotBlank() }
            if (uid.isNullOrBlank()) null else uid to snapshot
        }.toMap()
        return waypoints.map { waypoint ->
            val currentNodeId = waypoint.basicData["_nodeId"]?.takeIf { it.isNotBlank() }
            val original = waypoint.uid.takeIf { it.isNotBlank() }?.let { originalByUid[it] }
                ?: originalWaypoints.firstOrNull {
                    it.basicData["_nodeId"]?.takeIf { id -> id.isNotBlank() } == currentNodeId
                }
            if (original == null) return@map waypoint
            val merged = HashMap(waypoint.basicData)
            var changed = false
            for (slot in 1..3) {
                val photoKey = "photo$slot"
                val capturedAtKey = "photo${slot}CapturedAt"
                val currentPhoto = merged[photoKey]?.trim().orEmpty()
                val originalPhoto = original.basicData[photoKey]?.trim().orEmpty()
                val currentCapturedAt = merged[capturedAtKey]?.trim().orEmpty()
                val originalCapturedAt = original.basicData[capturedAtKey]?.trim().orEmpty()
                val sameByCapturedAt = currentCapturedAt.isNotEmpty() &&
                    currentCapturedAt == originalCapturedAt &&
                    originalCapturedAt.isNotEmpty()
                val sameByPhotoPath = currentPhoto.isNotEmpty() && currentPhoto == originalPhoto
                if (sameByCapturedAt || sameByPhotoPath) {
                    merged[photoKey] = ""
                    merged[capturedAtKey] = ""
                    changed = true
                }
            }
            if (!changed) waypoint else waypoint.copy(basicData = merged)
        }
    }

    private fun showRestoreStateDialog(spiNum: String, persistedWaypoints: List<Waypoint>, token: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("確認是否變更狀態")
            .setMessage("目前側溝為「待修正」狀態，確認後將變更至「待修正」之前的狀態。")
            .setPositiveButton("是") { _, _ -> performRestoreState(spiNum, persistedWaypoints, token) }
            .setNegativeButton("否") { _, _ -> reopenInspectPreviewAfterUpdate(spiNum, persistedWaypoints, token) }
            .setCancelable(false)
            .show()
    }

    private fun performRestoreState(spiNum: String, persistedWaypoints: List<Waypoint>, token: String) {
        lifecycleScope.launch {
            mainBlockingUiController.setInspectLoading(true, "正在變更側溝狀態…")
            val result = gutterRepository.updateDitchState(spiNum = spiNum, token = token)
            mainBlockingUiController.setInspectLoading(false)
            when (result) {
                is ApiResult.Success -> reopenInspectPreviewAfterUpdate(spiNum, persistedWaypoints, token)
                is ApiResult.Error -> {
                    if (authExpiredHandler.handleIfAuthExpired(result) { saveWaypointsAsPendingDraft(persistedWaypoints) }) {
                        return@launch
                    }
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("狀態變更失敗")
                        .setMessage(result.message)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            reopenInspectPreviewAfterUpdate(spiNum, persistedWaypoints, token)
                        }
                        .show()
                }
            }
        }
    }

    private fun reopenInspectPreviewAfterUpdate(
        spiNum: String,
        persistedWaypoints: List<Waypoint>,
        token: String
    ) {
        val latitudes = persistedWaypoints.mapNotNull { it.latLng?.latitude }.toDoubleArray()
        val longitudes = persistedWaypoints.mapNotNull { it.latLng?.longitude }.toDoubleArray()
        val routeWaypoints = persistedWaypoints.filter { it.latLng != null }
        val start = InspectFlowCoordinator.InspectStart(
            spiNum = spiNum,
            canEdit = true,
            latitudes = latitudes,
            longitudes = longitudes,
            routeWaypoints = routeWaypoints
        )
        lifecycleScope.launch {
            mainBlockingUiController.setInspectLoading(true, "載入更新後側溝資料中…")
            try {
                when (val result = inspectFlowCoordinator.load(start, token)) {
                    is ApiResult.Success -> {
                        scopeGutterPolylineController.clear()
                        submittedPolylines.forEach { it.remove() }
                        submittedPolylines.clear()
                        inspectPreviewIntent = Intent(result.data.intent)
                        shouldReturnToInspectPreview = false
                        inspectWaypoints = buildInspectWaypoints(result.data.nodeDetailsList)
                        currentWaypoints = inspectWaypoints
                        val isCurve = result.data.ditch.isCurve?.trim() == "1" ||
                            result.data.ditch.isCurve?.trim()?.equals("true", true) == true
                        referenceRoutePoints = start.routeWaypoints.mapNotNull { it.latLng }
                        isReferenceRouteActive = false
                        refreshWorkingLayer(inspectWaypoints, isCurve)
                        lockInspectUi()
                        val launched = launchInspectSafely(result.data.intent)
                        if (!launched) {
                            isInEditingMode = false
                            clearReferenceRoute()
                            gutterMapController.clearPreviewLayer()
                            clearWorkingMarkers()
                            unlockInspectUiIfIdle()
                            loadGuttersByViewport(showFeedback = true)
                        }
                    }
                    is ApiResult.Error -> {
                        isInEditingMode = false
                        inspectPreviewIntent = null
                        shouldReturnToInspectPreview = false
                        gutterMapController.clearPreviewLayer()
                        clearWorkingMarkers()
                        unlockInspectUiIfIdle()
                        loadGuttersByViewport(showFeedback = true)
                        if (!authExpiredHandler.handleIfAuthExpired(result) { saveWaypointsAsPendingDraft(persistedWaypoints) }) {
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle("更新成功")
                                .setMessage("側溝已更新，但重新載入檢視資料失敗：${result.message}")
                                .setPositiveButton(getString(R.string.confirm), null)
                                .show()
                        }
                    }
                }
            } finally {
                mainBlockingUiController.setInspectLoading(false)
            }
        }
    }

    companion object {
        private const val KEY_PENDING_WP_INDEX = "pending_wp_index"
        private const val GUTTER_LOAD_DEBOUNCE_MS = 500L
        fun newInstance(): MapWorkspaceFragment = MapWorkspaceFragment()
    }
}
