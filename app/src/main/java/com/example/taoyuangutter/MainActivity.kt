package com.example.taoyuangutter

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import android.net.Uri
import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.api.DitchDetails
import com.example.taoyuangutter.api.DitchNode
import com.example.taoyuangutter.api.GutterRepository
import com.example.taoyuangutter.api.StoreDitchNodeRequest
import com.example.taoyuangutter.api.StoreDitchRequest
import com.google.gson.Gson
import com.example.taoyuangutter.databinding.ActivityMainBinding
import com.example.taoyuangutter.gutter.AddGutterBottomSheet
import com.example.taoyuangutter.gutter.GutterFormActivity
import com.example.taoyuangutter.gutter.GutterInspectActivity
import com.example.taoyuangutter.gutter.Waypoint
import com.example.taoyuangutter.gutter.WaypointType
import com.example.taoyuangutter.login.LoginActivity
import com.example.taoyuangutter.offline.OfflineDraftsActivity
import com.example.taoyuangutter.pending.GutterSessionDraft
import com.example.taoyuangutter.pending.GutterSessionRepository
import com.example.taoyuangutter.pending.PendingDraftsBottomSheet
import com.example.taoyuangutter.pending.WaypointSnapshot
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.UrlTileProvider
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import java.net.MalformedURLException
import java.net.URL

class MainActivity : AppCompatActivity(),
    OnMapReadyCallback,
    AddGutterBottomSheet.LocationPickerHost {

    private enum class MapMode { EMAP, PHOTO2 }

    companion object {
        private const val KEY_PENDING_WP_INDEX = "pending_wp_index"
    }

    private lateinit var binding: ActivityMainBinding
    private var googleMap: GoogleMap? = null

    // ── NLSC WMTS 底圖 ────────────────────────────────────────────────────
    private var currentTileOverlay: TileOverlay? = null
    private var currentMapMode = MapMode.EMAP

    // ── 定位 ─────────────────────────────────────────────────────────────
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>

    // ── Repository ───────────────────────────────────────────────────────
    private val gutterRepository = GutterRepository()
    private val sessionDraftRepository by lazy { GutterSessionRepository(this) }

    // ── 目前存活的 BottomSheet 與正在選點的索引 ───────────────────────────
    private var activeSheet: AddGutterBottomSheet? = null
    private var pickingIndex: Int = -1

    /** 目前正在開啟 GutterFormActivity 的點位索引（新增模式）。 */
    private var pendingWaypointFormIndex: Int = -1

    // ── 檢視線段模式 ──────────────────────────────────────────────────────
    private var inspectSheet: AddGutterBottomSheet? = null
    private var inspectWaypoints: List<Waypoint> = emptyList()

    // ── 地圖疊加層 ────────────────────────────────────────────────────────
    private val workingMarkers = mutableListOf<Marker>()
    private var highlightedMarkerIndex: Int = -1
    private var workingPolyline: Polyline? = null
    private val submittedPolylines = mutableListOf<Polyline>()
    /** scopeSearch 從後端載入的 Polyline（依 SPI_NUM 索引，方便重繪時移除舊線段） */
    private val scopePolylines = mutableMapOf<String, Polyline>()
    private var currentWaypoints: List<Waypoint> = emptyList()

    private lateinit var gutterFormLauncher: ActivityResultLauncher<Intent>
    private lateinit var inspectLauncher: ActivityResultLauncher<Intent>

    // ── Lifecycle ─────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                          permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) enableMyLocationAndMove()
        }

        gutterFormLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            binding.btnAddGutter.visibility = View.VISIBLE

            when {
                activeSheet != null -> {
                    when (result.resultCode) {
                        Activity.RESULT_OK -> if (pendingWaypointFormIndex >= 0) {
                            val data = result.data

                            // ── 更新地圖定位座標 ────────────────────────────────
                            // 優先使用 GutterFormActivity 回傳的 lat/lng（已保證永遠有值）；
                            // 若萬一仍為 NaN（例如舊版快取），退回使用 currentWaypoints 中
                            // 地圖選點時已儲存的座標，確保大頭針不會消失。
                            val resultLat = data?.getDoubleExtra(GutterFormActivity.RESULT_LATITUDE,  Double.NaN) ?: Double.NaN
                            val resultLng = data?.getDoubleExtra(GutterFormActivity.RESULT_LONGITUDE, Double.NaN) ?: Double.NaN
                            val fallbackLatLng = currentWaypoints.getOrNull(pendingWaypointFormIndex)?.latLng
                            val effectiveLat = if (!resultLat.isNaN()) resultLat else fallbackLatLng?.latitude  ?: Double.NaN
                            val effectiveLng = if (!resultLng.isNaN()) resultLng else fallbackLatLng?.longitude ?: Double.NaN
                            if (!effectiveLat.isNaN() && !effectiveLng.isNaN()) {
                                activeSheet?.updateWaypointLocation(pendingWaypointFormIndex, LatLng(effectiveLat, effectiveLng))
                            }

                            // ── 更新表單填寫的基本資料 ──────────────────────────
                            val newData = extractBasicData(result.data)
                            activeSheet?.updateWaypointBasicData(pendingWaypointFormIndex, newData)
                        }
                        GutterFormActivity.RESULT_DELETE -> if (pendingWaypointFormIndex >= 0) {
                            // 使用者放棄填寫 → 清除該點位的座標與資料（同時更新地圖大頭針）
                            activeSheet?.clearWaypointLocation(pendingWaypointFormIndex)
                        }
                    }
                    resetHighlightedMarker()
                    pendingWaypointFormIndex = -1
                    activeSheet?.showSelf()
                    if (currentWaypoints.isNotEmpty()) fitCameraToWaypoints(currentWaypoints)
                }
                inspectSheet != null -> {
                    if (result.resultCode == Activity.RESULT_OK) {
                        val data = result.data
                        val idx  = data?.getIntExtra(GutterFormActivity.RESULT_WAYPOINT_INDEX, -1) ?: -1
                        if (idx >= 0) {
                            val newData = extractBasicData(data)
                            inspectWaypoints.getOrNull(idx)?.basicData = newData
                        }
                    }
                    inspectSheet?.showSelf()
                    fitCameraToWaypoints(inspectWaypoints)
                }
                else -> clearWorkingMarkers()
            }
        }

    // ── GutterInspectActivity 的 launcher ────────────────────────────
        inspectLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == GutterInspectActivity.RESULT_EDIT_DITCH) {
                val json   = result.data?.getStringExtra(GutterInspectActivity.EXTRA_RESULT_WAYPOINTS_JSON) ?: return@registerForActivityResult
                val spiNum = result.data?.getStringExtra(GutterInspectActivity.EXTRA_RESULT_SPI_NUM) ?: ""

                // ── 若 FM 內仍有同 TAG 的舊 sheet（例如先前新增流程的 activeSheet）
                //    必須先 dismiss 並等待事務完成，否則 show() 會因 TAG 衝突而失敗 ──
                (supportFragmentManager.findFragmentByTag(AddGutterBottomSheet.TAG)
                        as? AddGutterBottomSheet)
                    ?.dismissAllowingStateLoss()
                supportFragmentManager.executePendingTransactions()
                activeSheet = null

                // 直接以 WaypointSnapshot JSON 建立編輯模式的 BottomSheet
                val wpsType = object : com.google.gson.reflect.TypeToken<List<WaypointSnapshot>>() {}.type
                val snapshots: List<WaypointSnapshot> = try {
                    Gson().fromJson(json, wpsType)
                } catch (e: Exception) {
                    emptyList()
                }

                val wps = snapshots.map { snap ->
                    val wpType = WaypointType.entries.firstOrNull { it.name == snap.type } ?: WaypointType.NODE
                    val latLng = if (snap.latitude != null && snap.longitude != null)
                        LatLng(snap.latitude, snap.longitude) else null
                    Waypoint(wpType, snap.label, latLng, snap.basicData)
                }

                val sheet = AddGutterBottomSheet.newInstanceForEdit(wps, spiNum)
                sheet.onWaypointsChanged = { updated ->
                    if (updated == null) {
                        clearWorkingMarkers()
                        activeSheet = null
                    } else {
                        currentWaypoints = updated.toMutableList()
                        refreshWorkingMarkers(updated)
                    }
                }
                activeSheet = sheet
                sheet.show(supportFragmentManager, AddGutterBottomSheet.TAG)
            }
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupButtons()
        setupLocationPickerOverlay()

        // ── 系統重建後恢復狀態（相機 Activity 期間 MainActivity 被殺掉時觸發）──
        if (savedInstanceState != null) {
            restoreStateAfterRecreation(savedInstanceState)
        }
    }

    /**
     * 在系統重建（拍照流程期間記憶體不足）後恢復關鍵狀態：
     * - pendingWaypointFormIndex：讓 gutterFormLauncher 知道要更新哪個點位
     * - activeSheet / inspectSheet：重新綁定已被 FragmentManager 恢復的 BottomSheet
     */
    private fun restoreStateAfterRecreation(savedState: Bundle) {
        pendingWaypointFormIndex = savedState.getInt(KEY_PENDING_WP_INDEX, -1)

        val restoredSheet = supportFragmentManager
            .findFragmentByTag(AddGutterBottomSheet.TAG) as? AddGutterBottomSheet
            ?: return

        if (restoredSheet.isAddMode()) {
            // 新增模式：重新綁定 activeSheet 與 onWaypointsChanged
            activeSheet = restoredSheet
            restoredSheet.onWaypointsChanged = { wps ->
                currentWaypoints = wps ?: emptyList()
                refreshWorkingLayer(wps ?: emptyList())
                if (wps == null) activeSheet = null
            }
        } else {
            // 檢視模式：重新綁定 inspectSheet
            inspectSheet = restoredSheet
            restoredSheet.onWaypointsChanged = { if (it == null) { clearWorkingMarkers(); inspectSheet = null } }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // 儲存正在開啟表單的點位索引，確保 MainActivity 重建後仍能正確回寫
        outState.putInt(KEY_PENDING_WP_INDEX, pendingWaypointFormIndex)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        // 關閉 Google 預設底圖，改用 NLSC WMTS 圖層
        googleMap?.mapType = GoogleMap.MAP_TYPE_NONE
        setMapTiles(MapMode.EMAP)

        val taoyuan = LatLng(24.9936, 121.3010)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(taoyuan, 20f))
        googleMap?.uiSettings?.apply {
            isZoomControlsEnabled = true
            isCompassEnabled      = true
            isMapToolbarEnabled   = false
        }

        map.setOnMarkerClickListener { marker ->
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

        map.setOnPolylineClickListener { polyline -> openInspectBottomSheet(polyline) }

        // 地圖停止移動後，依目前可視範圍向後端查詢側溝線段
        map.setOnCameraIdleListener { loadGuttersByViewport() }
    }

    // ── LocationPickerHost 實作 ───────────────────────────────────────────
    override fun startLocationPick(sheet: AddGutterBottomSheet, waypointIndex: Int) {
        activeSheet  = sheet
        pickingIndex = waypointIndex
        highlightMarker(waypointIndex)
        sheet.hideSelf()
        binding.btnAddGutter.visibility = View.GONE
        binding.locationPickerOverlay.root.visibility = View.VISIBLE
        googleMap?.setPadding(0, 0, 0, 0)
    }

    override fun onGutterSubmitted(waypoints: List<Waypoint>) {
        activeSheet?.onWaypointsChanged = null
        workingPolyline?.remove()
        workingPolyline = null
        activeSheet = null
        clearWorkingMarkers()
        binding.btnAddGutter.visibility = View.VISIBLE
        drawSubmittedGutter(waypoints)
        googleMap?.setPadding(0, 0, 0, 0)

        val token = LoginActivity.getSavedToken(this) ?: run {
            Toast.makeText(this, "請先登入", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val request = buildStoreDitchRequest(waypoints)
            when (val result = gutterRepository.storeDitch(request, token)) {
                is ApiResult.Success -> {
                    Toast.makeText(this@MainActivity, "側溝上傳成功", Toast.LENGTH_SHORT).show()
                    // storeDitch 回傳的 nodes 列表含各點位的 node_id，依序上傳本機照片
                    val nodes = result.data.data?.nodes ?: emptyList()
                    uploadWaypointPhotos(waypoints, nodes, token)
                }
                is ApiResult.Error -> {
                    Toast.makeText(this@MainActivity, "上傳失敗，已儲存為待上傳草稿", Toast.LENGTH_LONG).show()
                    saveWaypointsAsPendingDraft(waypoints)
                }
            }
        }
    }

    /**
     * 將 waypoints 轉換為 [StoreDitchRequest]（新增模式，spiNum = null）。
     * basicData 中的字串欄位依 storeDitch API 型別規格轉換。
     */
    private fun buildStoreDitchRequest(
        waypoints: List<Waypoint>,
        spiNum: String? = null
    ): StoreDitchRequest {
        return StoreDitchRequest(
            spiNum = spiNum,
            nodes  = waypoints.mapIndexed { idx, wp ->
                StoreDitchNodeRequest(
                    nodeId    = wp.basicData["_nodeId"]?.toIntOrNull(),
                    nodeAtt   = when (wp.type) {
                        WaypointType.START -> 1
                        WaypointType.NODE  -> 2
                        WaypointType.END   -> 3
                    },
                    nodeNum   = idx.toString(),
                    nodeTyp   = wp.basicData["gutterType"]?.toIntOrNull() ?: 1,
                    matTyp    = wp.basicData["matTyp"]?.toIntOrNull() ?: 1,
                    latitude  = wp.latLng?.latitude  ?: 0.0,
                    longitude = wp.latLng?.longitude ?: 0.0,
                    nodeLe    = wp.basicData["coordZ"]?.toDoubleOrNull() ?: 0.0,
                    xyNum     = wp.basicData["measureId"] ?: "",
                    nodeDep   = wp.basicData["depth"]?.toIntOrNull() ?: 0,
                    nodeWid   = wp.basicData["topWidth"]?.toIntOrNull() ?: 0,
                    isBroken  = wp.basicData["isBroken"]?.let { it == "true" || it == "1" } ?: false,
                    isHanging = wp.basicData["isHanging"]?.let { it == "true" || it == "1" } ?: false,
                    isSilt    = wp.basicData["isSilt"]?.toIntOrNull() ?: 0,
                    nodeNote  = wp.basicData["remarks"]?.takeIf { it.isNotEmpty() }
                )
            }
        )
    }

    /**
     * storeDitch 成功後，依照 API 回傳的 [nodes] 順序，
     * 找出對應 waypoint 的本機照片（content:// / file:// scheme）並上傳。
     * https:// 照片代表已在伺服器，略過。
     */
    private suspend fun uploadWaypointPhotos(
        waypoints: List<Waypoint>,
        nodes: List<DitchNode>,
        token: String
    ) {
        nodes.forEachIndexed { i, node ->
            val wp = waypoints.getOrNull(i) ?: return@forEachIndexed
            listOf(
                wp.basicData["photo1"] to 1,
                wp.basicData["photo2"] to 2,
                wp.basicData["photo3"] to 3
            ).forEach { (path, category) ->
                if (path.isNullOrEmpty()) return@forEach
                val scheme = Uri.parse(path).scheme?.lowercase() ?: return@forEach
                if (scheme == "http" || scheme == "https") return@forEach
                when (val r = gutterRepository.uploadNodeImage(
                    context      = this,
                    nodeId       = node.nodeId,
                    fileCategory = category,
                    imageUri     = Uri.parse(path),
                    token        = token
                )) {
                    is ApiResult.Error ->
                        android.util.Log.w("PhotoUpload", "node${node.nodeId} photo$category 失敗: ${r.message}")
                    is ApiResult.Success -> { /* OK */ }
                }
            }
        }
    }

    override fun onUpdateGutter(waypoints: List<Waypoint>, spiNum: String) {
        // TODO: 呼叫更新側溝 API
        Toast.makeText(this, "更新側溝（$spiNum）— 待實作", Toast.LENGTH_SHORT).show()
        activeSheet = null
        clearWorkingMarkers()
        binding.btnAddGutter.visibility = View.VISIBLE
    }

    override fun onDeleteGutter(spiNum: String) {
        AlertDialog.Builder(this)
            .setTitle("刪除側溝")
            .setMessage("確定要刪除側溝「$spiNum」及其所有點位嗎？此操作無法復原。")
            .setPositiveButton("確定刪除") { _, _ ->
                val token = LoginActivity.getSavedToken(this)
                if (token.isNullOrEmpty()) {
                    Toast.makeText(this, "請先登入", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    when (val result = gutterRepository.deleteDitch(spiNum, token)) {
                        is ApiResult.Success -> {
                            // 從地圖移除該側溝的線段
                            scopePolylines.remove(spiNum)?.remove()
                            // 關閉 BottomSheet 並清除暫存標記
                            activeSheet?.onWaypointsChanged = null
                            workingPolyline?.remove()
                            workingPolyline = null
                            activeSheet?.dismiss()
                            activeSheet = null
                            clearWorkingMarkers()
                            binding.btnAddGutter.visibility = View.VISIBLE
                            googleMap?.setPadding(0, 0, 0, 0)
                            Toast.makeText(this@MainActivity, "側溝「$spiNum」已成功刪除", Toast.LENGTH_SHORT).show()
                        }
                        is ApiResult.Error -> {
                            Toast.makeText(this@MainActivity, "刪除失敗：${result.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /** 將 waypoints 序列化並存入待上傳草稿 Repository。 */
    private fun saveWaypointsAsPendingDraft(waypoints: List<Waypoint>) {
        val snapshots = waypoints.map { wp ->
            WaypointSnapshot(
                type      = wp.type.name,
                label     = wp.label,
                latitude  = wp.latLng?.latitude,
                longitude = wp.latLng?.longitude,
                basicData = wp.basicData
            )
        }
        val draft = GutterSessionDraft(waypoints = snapshots)
        sessionDraftRepository.save(draft)
    }

    override fun getInspectWaypoints(): List<Waypoint> = inspectWaypoints

    override fun openWaypointForEdit(sheet: AddGutterBottomSheet, waypointIndex: Int) {
        currentWaypoints = sheet.getWaypoints()
        val wp = currentWaypoints.getOrNull(waypointIndex) ?: return

        // ── API 編輯流程：若有 _nodeId，優先呼叫 nodeDetails 取得完整資料 ──────
        val nodeId = wp.basicData["_nodeId"]?.toIntOrNull()
        val token  = LoginActivity.getSavedToken(this)

        if (nodeId != null && token != null) {
            sheet.hideSelf()
            binding.btnAddGutter.visibility = View.GONE

            lifecycleScope.launch {
                when (val result = gutterRepository.getNodeDetails(nodeId, token)) {
                    is ApiResult.Success -> {
                        val nd = result.data.data ?: run {
                            Toast.makeText(this@MainActivity, "查無點位資料", Toast.LENGTH_SHORT).show()
                            sheet.showSelf()
                            binding.btnAddGutter.visibility = View.VISIBLE
                            return@launch
                        }
                        val lat = nd.latitude?.toDoubleOrNull()
                        val lng = nd.longitude?.toDoubleOrNull()
                        val latLng = if (lat != null && lng != null) LatLng(lat, lng) else wp.latLng

                        // 擷取照片：依 fileCategory (1,2,3) 填入 photo1,2,3
                        val p1 = nd.nodeImg.firstOrNull { it.fileCategory == "1" }?.url ?: ""
                        val p2 = nd.nodeImg.firstOrNull { it.fileCategory == "2" }?.url ?: ""
                        val p3 = nd.nodeImg.firstOrNull { it.fileCategory == "3" }?.url ?: ""

                        val updatedData = hashMapOf(
                            "_nodeId"    to nodeId.toString(),
                            "gutterId"   to (wp.basicData["gutterId"] ?: ""),
                            "gutterType" to (nd.nodeTyP?.takeIf { it.isNotEmpty() }?.let { spiTypToText(it) } ?: wp.basicData["gutterType"] ?: ""),
                            "matTyp"     to (nd.matTyp?.takeIf { it.isNotEmpty() }?.let { matTypToText(it) } ?: wp.basicData["matTyp"] ?: ""),
                            "coordX"     to (nd.nodeX   ?: wp.basicData["coordX"] ?: ""),
                            "coordY"     to (nd.nodeY   ?: wp.basicData["coordY"] ?: ""),
                            "coordZ"     to (nd.nodeLe  ?: wp.basicData["coordZ"] ?: ""),
                            // nodeDepAsString / nodeWidAsString：API 回傳整數（Double?）→ 轉純數字字串
                            "depth"      to nd.nodeDepAsString.ifEmpty { wp.basicData["depth"] ?: "" },
                            "topWidth"   to nd.nodeWidAsString.ifEmpty { wp.basicData["topWidth"] ?: "" },
                            "isBroken"   to (nd.isBroken?.takeIf { it.isNotEmpty() }?.let { isBrokenToText(it) } ?: wp.basicData["isBroken"] ?: ""),
                            "isHanging"  to (nd.isHanging?.takeIf { it.isNotEmpty() }?.let { isHangingToText(it) } ?: wp.basicData["isHanging"] ?: ""),
                            "isSilt"     to (nd.isSilt?.takeIf { it.isNotEmpty() }?.let { isSiltToText(it) } ?: wp.basicData["isSilt"] ?: ""),
                            "remarks"    to (nd.note    ?: wp.basicData["remarks"] ?: ""),
                            // XY_NUM → measureId（測量座標編號）
                            "xyNum"      to (nd.xyNum   ?: wp.basicData["xyNum"]   ?: ""),
                            "photo1"     to (p1.takeIf { it.isNotEmpty() } ?: wp.basicData["photo1"] ?: ""),
                            "photo2"     to (p2.takeIf { it.isNotEmpty() } ?: wp.basicData["photo2"] ?: ""),
                            "photo3"     to (p3.takeIf { it.isNotEmpty() } ?: wp.basicData["photo3"] ?: "")
                        )

                        if (latLng != null) {
                            activeSheet?.updateWaypointLocation(waypointIndex, latLng)
                        }
                        activeSheet?.updateWaypointBasicData(waypointIndex, updatedData)

                        currentWaypoints = activeSheet?.getWaypoints() ?: currentWaypoints
                        val updatedWp = currentWaypoints.getOrNull(waypointIndex) ?: return@launch

                        pendingWaypointFormIndex = waypointIndex
                        if (latLng != null) highlightMarker(waypointIndex)
                        openAddForm(waypointIndex, updatedWp, latLng ?: LatLng(0.0, 0.0), isEditMode = true)
                    }
                    is ApiResult.Error -> {
                        Toast.makeText(this@MainActivity, "取得點位詳情失敗：${result.message}", Toast.LENGTH_SHORT).show()
                        sheet.showSelf()
                        binding.btnAddGutter.visibility = View.VISIBLE
                    }
                }
            }
            return
        }

        // ── 新增流程：已有 WGS84 座標，直接開表單 ──────────────────────────
        val existingLatLng = wp.latLng
        if (existingLatLng != null) {
            pendingWaypointFormIndex = waypointIndex
            highlightMarker(waypointIndex)
            sheet.hideSelf()
            binding.btnAddGutter.visibility = View.GONE
            openAddForm(waypointIndex, wp, existingLatLng)
            return
        }

        // 編輯模式中新增的節點：無 _nodeId 且無座標，進入地圖選點流程（與新增模式相同）
        startLocationPick(sheet, waypointIndex)
    }

    override fun openWaypointForInspect(sheet: AddGutterBottomSheet, waypointIndex: Int) {
        val wp     = inspectWaypoints.getOrNull(waypointIndex) ?: return
        val latLng = wp.latLng ?: return
        highlightMarker(waypointIndex)
        sheet.hideSelf()
        binding.btnAddGutter.visibility = View.GONE
        openInspectForm(waypointIndex, wp, latLng)
    }

    private fun openInspectForm(waypointIndex: Int, wp: Waypoint, latLng: LatLng) {
        moveCameraToLatLngOffset(latLng, 0.75) 
        val intent = GutterFormActivity.newViewIntent(this, wp.label, latLng.latitude, latLng.longitude, waypointIndex, wp.basicData)
        gutterFormLauncher.launch(intent)
    }

    private fun openAddForm(
        currentIndex: Int,
        wp: Waypoint,
        latLng: LatLng,
        isEditMode: Boolean = false
    ) {
        moveCameraToLatLngOffset(latLng, 0.75)
        val labels = ArrayList(currentWaypoints.map { it.label })
        val lats   = currentWaypoints.map { it.latLng?.latitude ?: 0.0 }.toDoubleArray()
        val lngs   = currentWaypoints.map { it.latLng?.longitude ?: 0.0 }.toDoubleArray()
        val intent = GutterFormActivity.newIntent(this, labels, lats, lngs, currentIndex, wp.basicData, isEditMode)
        gutterFormLauncher.launch(intent)
    }

    private fun moveCameraToLatLngOffset(latLng: LatLng, offsetRatio: Double) {
        val map = googleMap ?: return
        val screenH = resources.displayMetrics.heightPixels
        // 暫時設定底部 padding，讓地圖移動時把點位顯示在視窗上半段；
        // 動畫結束（或被取消）後立即歸零，確保地圖始終佔滿全螢幕。
        map.setPadding(0, 0, 0, (screenH * offsetRatio).toInt())
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(latLng, 20f),
            object : GoogleMap.CancelableCallback {
                override fun onFinish() { map.setPadding(0, 0, 0, 0) }
                override fun onCancel() { map.setPadding(0, 0, 0, 0) }
            }
        )
    }

    private fun fitCameraToWaypoints(waypoints: List<Waypoint>, bottomOffsetRatio: Double = 0.52) {
        val map = googleMap ?: return
        val points = waypoints.mapNotNull { it.latLng }
        if (points.isEmpty()) return
        val dm      = resources.displayMetrics
        val screenH = dm.heightPixels
        val padding = (64 * dm.density).toInt()
        // 暫時設定 padding，讓相機計算範圍時排除底部 BottomSheet 區域；
        // 動畫結束（或被取消）後歸零，避免 Google Maps 縮放鈕 / 羅盤位置偏移。
        map.setPadding(padding, padding, padding, (screenH * bottomOffsetRatio).toInt())
        val boundsBuilder = LatLngBounds.Builder()
        points.forEach { boundsBuilder.include(it) }
        try {
            map.animateCamera(
                CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), padding),
                object : GoogleMap.CancelableCallback {
                    override fun onFinish() { map.setPadding(0, 0, 0, 0) }
                    override fun onCancel() { map.setPadding(0, 0, 0, 0) }
                }
            )
        } catch (e: Exception) {
            map.setPadding(0, 0, 0, 0)   // 例外時也立即歸零
            map.setOnMapLoadedCallback { fitCameraToWaypoints(waypoints, bottomOffsetRatio) }
        }
    }

    private fun fitCameraToAllGutters() {
        val allWaypoints = mutableListOf<Waypoint>()
        submittedPolylines.forEach { poly -> (poly.tag as? ArrayList<Waypoint>)?.let { allWaypoints.addAll(it) } }
        if (allWaypoints.isNotEmpty()) fitCameraToWaypoints(allWaypoints)
    }

    private fun requestLocationAndMove() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED) enableMyLocationAndMove()
        else locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocationAndMove() {
        googleMap?.isMyLocationEnabled = true
        googleMap?.uiSettings?.isMyLocationButtonEnabled = false
        val cts = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { loc -> if (loc != null) googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 18f)) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun openInspectBottomSheet(polyline: Polyline) {
        val tag      = polyline.tag as? Pair<*, *> ?: return
        val spiNum   = tag.first  as? String ?: return
        val groupId  = tag.second as? String ?: ""
        val token    = LoginActivity.getSavedToken(this)  ?: return

        // 登入的 group_id 與線段的 group_id 一致時才允許編輯
        val savedGroupId = LoginActivity.getSavedGroupId(this)
        val canEdit = savedGroupId != -1 &&
                groupId.isNotEmpty() &&
                groupId.toIntOrNull() == savedGroupId

        // 擷取線段點位的 WGS84 座標，供編輯模式預填大頭針位置
        val lats = polyline.points.map { it.latitude }.toDoubleArray()
        val lngs = polyline.points.map { it.longitude }.toDoubleArray()

        lifecycleScope.launch {
            when (val result = gutterRepository.getDitchDetails(spiNum, token)) {
                is ApiResult.Success -> {
                    val ditch = result.data.data
                    if (ditch != null) {
                        val intent = GutterInspectActivity.newIntent(
                            context    = this@MainActivity,
                            ditch      = ditch,
                            canEdit    = canEdit,
                            latitudes  = lats,
                            longitudes = lngs
                        )
                        // 若有進行中的新增流程 sheet，先隱藏它（不 dismiss，
                        // 讓使用者按返回時仍可繼續；若最終進入編輯模式則由 inspectLauncher 清除）
                        activeSheet?.hideSelf()
                        inspectLauncher.launch(intent)
                    } else {
                        android.widget.Toast.makeText(
                            this@MainActivity, "查無線段資料", android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                is ApiResult.Error -> {
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "查詢失敗(${result.code}): ${result.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // ── API code→顯示文字轉換（對應 GutterBasicInfoFragment 下拉選單選項）────
    private fun spiTypToText(code: String?): String = when (code) {
        "1" -> "U形溝（明溝）"; "2" -> "U形溝（加蓋）"
        "3" -> "L形溝與暗溝渠併用"; "4" -> "其他"
        else -> code ?: ""
    }
    private fun matTypToText(code: String?): String = when (code) {
        "1" -> "混凝土"; "2" -> "卵礫石"; "3" -> "紅磚"
        else -> code ?: ""
    }
    private fun isBrokenToText(code: String?): String = when (code) {
        "0" -> "否"; "1" -> "是"; else -> code ?: ""
    }
    private fun isHangingToText(code: String?): String = when (code) {
        "0" -> "無"; "1" -> "有"; else -> code ?: ""
    }
    private fun isSiltToText(code: String?): String = when (code) {
        "0" -> "無"; "1" -> "輕度"; "2" -> "中度"; "3" -> "嚴重"
        else -> code ?: ""
    }

    private fun setupButtons() {
        binding.btnLogout.setOnClickListener {
            val token = LoginActivity.getSavedToken(this)
            if (token.isNullOrEmpty()) {
                // 本機無 token，直接跳登入頁
                goToLogin()
                return@setOnClickListener
            }
            binding.btnLogout.isEnabled = false
            lifecycleScope.launch {
                when (val result = gutterRepository.logout(token)) {
                    is ApiResult.Success -> {
                        clearAuthAndGoLogin()
                    }
                    is ApiResult.Error -> {
                        binding.btnLogout.isEnabled = true
                        Toast.makeText(
                            this@MainActivity,
                            result.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
        binding.btnViewDrafts.setOnClickListener { showPendingDraftsSheet() }
        binding.btnLegend.setOnClickListener { showLegendDialog() }
        binding.btnLayers.setOnClickListener { showMapTypeDialog() }
        binding.btnMyLocation.setOnClickListener { requestLocationAndMove() }
        binding.btnAddGutter.setOnClickListener {
            workingPolyline?.remove()
            clearWorkingMarkers()
            fitCameraToAllGutters()
            val sheet = AddGutterBottomSheet.newInstance()
            activeSheet = sheet
            sheet.onWaypointsChanged = { wps ->
                currentWaypoints = wps ?: emptyList()
                refreshWorkingLayer(wps ?: emptyList())
                if (wps == null) activeSheet = null
            }
            sheet.show(supportFragmentManager, AddGutterBottomSheet.TAG)
        }
    }

    /** 顯示「待上傳草稿」BottomSheet，並處理「繼續編輯」回呼。 */
    private fun showPendingDraftsSheet() {
        val sheet = PendingDraftsBottomSheet.newInstance()
        sheet.onResumeDraft = { draft -> resumePendingDraft(draft) }
        sheet.show(supportFragmentManager, PendingDraftsBottomSheet.TAG)
    }

    /**
     * 從待上傳草稿恢復 AddGutterBottomSheet。
     *
     * 先 dismiss PendingDraftsBottomSheet（與任何現有 activeSheet），
     * 再以 [View.post] 延到下一幀才 show AddGutterBottomSheet，
     * 確保兩個 FragmentTransaction 不會同時競爭同一個 FragmentManager。
     */
    private fun resumePendingDraft(draft: GutterSessionDraft) {
        // ① 關閉 PendingDraftsBottomSheet
        (supportFragmentManager.findFragmentByTag(PendingDraftsBottomSheet.TAG)
                as? PendingDraftsBottomSheet)
            ?.dismissAllowingStateLoss()

        // ② 清除現有進行中的 sheet 與地圖疊加層
        activeSheet?.dismissAllowingStateLoss()
        activeSheet = null
        workingPolyline?.remove()
        workingPolyline = null
        clearWorkingMarkers()

        // ③ 草稿已取出，立即從 repository 刪除（提交成功才算完成）
        sessionDraftRepository.delete(draft.id)

        // ④ 等待 dismiss 動畫（約 250ms）完全結束後再 show AddGutterBottomSheet，
        //    避免兩個 FragmentTransaction 競爭同一個 FragmentManager 而靜默失敗。
        Handler(Looper.getMainLooper()).postDelayed({
            if (isFinishing || isDestroyed) return@postDelayed
            val sheet = AddGutterBottomSheet.newInstanceFromDraft(draft)
            activeSheet = sheet
            sheet.onWaypointsChanged = { wps ->
                currentWaypoints = wps ?: emptyList()
                refreshWorkingLayer(wps ?: emptyList())
                if (wps == null) activeSheet = null
            }
            sheet.show(supportFragmentManager, AddGutterBottomSheet.TAG)
        }, 300L)
    }

    private fun refreshWorkingLayer(waypoints: List<Waypoint>) {
        val map = googleMap ?: return
        clearWorkingMarkers()
        workingPolyline?.remove()
        if (waypoints.isEmpty()) return
        val routePoints = mutableListOf<LatLng>()
        for ((idx, wp) in waypoints.withIndex()) {
            val latLng = wp.latLng ?: continue
            routePoints.add(latLng)
            val marker = map.addMarker(MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.defaultMarker(markerHue(wp.type))))
            marker?.tag = idx
            marker?.let { workingMarkers.add(it) }
        }
        if (routePoints.size >= 2) {
            workingPolyline = map.addPolyline(PolylineOptions().addAll(routePoints).color(Color.parseColor("#5C35CC")).width(10f).geodesic(true).clickable(false))
        }
    }

    private fun refreshWorkingMarkers(waypoints: List<Waypoint>) {
        val map = googleMap ?: return
        clearWorkingMarkers()
        for ((idx, wp) in waypoints.withIndex()) {
            val latLng = wp.latLng ?: continue
            val marker = map.addMarker(MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.defaultMarker(markerHue(wp.type))))
            marker?.tag = idx
            marker?.let { workingMarkers.add(it) }
        }
    }

    // ── 登出輔助 ──────────────────────────────────────────────────────────

    /** 清除本機 token 並跳回登入頁（完成登出後呼叫） */
    private fun clearAuthAndGoLogin() {
        getSharedPreferences("taoyuan_prefs", MODE_PRIVATE).edit()
            .remove("auth_token")
            .remove("user_name")
            .remove("user_company")
            .apply()
        goToLogin()
    }

    /** 跳回登入頁並清除 Activity 堆疊 */
    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    private fun clearWorkingMarkers() {
        workingMarkers.forEach { it.remove() }
        workingMarkers.clear()
        highlightedMarkerIndex = -1
    }

    private fun drawSubmittedGutter(waypoints: List<Waypoint>) {
        val map = googleMap ?: return
        val routePoints = waypoints.mapNotNull { it.latLng }
        if (routePoints.size < 2) return
        val polyline = map.addPolyline(PolylineOptions().addAll(routePoints).color(Color.parseColor("#5C35CC")).width(10f).geodesic(true).clickable(true))
        polyline.tag = ArrayList(waypoints)
        submittedPolylines.add(polyline)
    }

    private fun highlightMarker(waypointIndex: Int) {
        resetHighlightedMarker()
        val waypoints = if (inspectSheet != null) inspectWaypoints else currentWaypoints
        val wp = waypoints.getOrNull(waypointIndex) ?: return
        if (wp.latLng == null) return
        highlightedMarkerIndex = waypointIndex
        workingMarkers.firstOrNull { it.tag == waypointIndex }?.let { marker ->
            marker.setIcon(createEnlargedMarkerIcon(wp.type))
            marker.zIndex = 1f
        }
    }

    private fun resetHighlightedMarker() {
        if (highlightedMarkerIndex < 0) return
        val waypoints = if (inspectSheet != null) inspectWaypoints else currentWaypoints
        val wp = waypoints.getOrNull(highlightedMarkerIndex)
        workingMarkers.firstOrNull { it.tag == highlightedMarkerIndex }?.let { marker ->
            marker.setIcon(BitmapDescriptorFactory.defaultMarker(markerHue(wp?.type ?: WaypointType.NODE)))
            marker.zIndex = 0f
        }
        highlightedMarkerIndex = -1
    }

    // ── 側溝座標 API（scopeSearch）────────────────────────────────────────

    /**
     * 取得目前地圖可視範圍（LatLngBounds）並呼叫 scopeSearch API，
     * 成功後呼叫 [drawScopePolylines] 更新地圖上的線段。
     */
    private fun loadGuttersByViewport() {
        val map = googleMap ?: return
        val token = LoginActivity.getSavedToken(this) ?: return
        val bounds = map.projection.visibleRegion.latLngBounds
        lifecycleScope.launch {
            when (val result = gutterRepository.getGuttersByScope(
                minLat = bounds.southwest.latitude,
                maxLat = bounds.northeast.latitude,
                minLng = bounds.southwest.longitude,
                maxLng = bounds.northeast.longitude,
                token  = token
            )) {
                is ApiResult.Success -> {
                    drawScopePolylines(result.data.data?.features ?: emptyList())
                }
                is ApiResult.Error -> {
                    // 靜默失敗：不打擾使用者，僅在 logcat 留紀錄
                    android.util.Log.w("ScopeSearch", "查詢失敗: ${result.message}")
                }
            }
        }
    }

    /**
     * 將 scopeSearch 回傳的 GeoJSON features 畫成地圖 Polyline。
     * 已存在的同 SPI_NUM 線段會先移除再重繪，避免重複疊加。
     */
    private fun drawScopePolylines(features: List<com.example.taoyuangutter.api.GeoFeature>) {
        val map = googleMap ?: return
        features.forEach { feature ->
            val spiNum  = feature.properties?.spiNum  ?: return@forEach
            val groupId = feature.properties?.groupId ?: ""
            val coords  = feature.geometry?.coordinates ?: return@forEach
            if (coords.size < 2) return@forEach

            // 移除舊線段
            scopePolylines.remove(spiNum)?.remove()

            val points = coords.map { LatLng(it[1], it[0]) }   // GeoJSON: [lng, lat]
            val polyline = map.addPolyline(
                com.google.android.gms.maps.model.PolylineOptions()
                    .addAll(points)
                    .color(android.graphics.Color.parseColor("#6236FF"))
                    .width(6f)
                    .clickable(true)
            )
            // tag 同時儲存 spiNum 與 groupId，供點擊時比對編輯權限
            polyline.tag = Pair(spiNum, groupId)
            scopePolylines[spiNum] = polyline
        }
    }

    private fun createEnlargedMarkerIcon(type: WaypointType): com.google.android.gms.maps.model.BitmapDescriptor {
        val dp = resources.displayMetrics.density
        val w  = (48 * dp).toInt()
        val h  = (76 * dp).toInt()
        val r  = w / 2f
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val fillColor = when (type) {
            WaypointType.START -> Color.rgb(0x34, 0xA8, 0x53)
            WaypointType.NODE  -> Color.rgb(0x42, 0x85, 0xF4)
            WaypointType.END   -> Color.rgb(0xEA, 0x43, 0x35)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillColor }
        canvas.drawCircle(r, r, r, paint)
        val tail = Path().apply { moveTo(r * 0.35f, r * 1.55f); lineTo(r, h.toFloat()); lineTo(r * 1.65f, r * 1.55f); close() }
        canvas.drawPath(tail, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(r, r, r * 0.38f, paint)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun markerHue(type: WaypointType): Float = when (type) {
        WaypointType.START -> BitmapDescriptorFactory.HUE_GREEN
        WaypointType.NODE  -> BitmapDescriptorFactory.HUE_AZURE
        WaypointType.END   -> BitmapDescriptorFactory.HUE_RED
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

    /** 切換 NLSC WMTS 底圖圖層（電子地圖 / 正射影像）。 */
    private fun setMapTiles(mode: MapMode) {
        currentTileOverlay?.remove()
        currentMapMode = mode

        val layer = when (mode) {
            MapMode.EMAP   -> "EMAP"
            MapMode.PHOTO2 -> "PHOTO2"
        }
        val urlTemplate = "https://wmts.nlsc.gov.tw/wmts/$layer/default/GoogleMapsCompatible/%d/%d/%d"

        val tileProvider = object : UrlTileProvider(256, 256) {
            override fun getTileUrl(x: Int, y: Int, zoom: Int): URL? = try {
                URL(String.format(urlTemplate, zoom, y, x))
            } catch (e: MalformedURLException) { null }
        }
        currentTileOverlay = googleMap?.addTileOverlay(
            TileOverlayOptions().tileProvider(tileProvider).zIndex(-1f)
        )
    }

    private fun showMapTypeDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_layers, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val ivRadioNormal    = dialogView.findViewById<android.widget.ImageView>(R.id.icRadioNormal)
        val ivRadioSatellite = dialogView.findViewById<android.widget.ImageView>(R.id.icRadioSatellite)

        fun updateRadio(mode: MapMode) {
            ivRadioNormal.setImageResource(
                if (mode == MapMode.EMAP) R.drawable.ic_radio_checked else R.drawable.ic_radio_unchecked
            )
            ivRadioSatellite.setImageResource(
                if (mode == MapMode.PHOTO2) R.drawable.ic_radio_checked else R.drawable.ic_radio_unchecked
            )
        }
        updateRadio(currentMapMode)

        dialogView.findViewById<android.view.View>(R.id.rowNormalMap).setOnClickListener {
            setMapTiles(MapMode.EMAP)
            updateRadio(MapMode.EMAP)
            dialog.dismiss()
        }
        dialogView.findViewById<android.view.View>(R.id.rowSatelliteMap).setOnClickListener {
            setMapTiles(MapMode.PHOTO2)
            updateRadio(MapMode.PHOTO2)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showLegendDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_legend, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun extractBasicData(data: Intent?): HashMap<String, String> =
        hashMapOf(
            "gutterId"   to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_GUTTER_ID)   ?: ""),
            "gutterType" to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_GUTTER_TYPE) ?: ""),
            "matTyp"     to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_MAT_TYP)     ?: ""),
            "coordX"     to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_COORD_X)     ?: ""),
            "coordY"     to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_COORD_Y)     ?: ""),
            "coordZ"     to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_COORD_Z)     ?: ""),
            "measureId"  to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_MEASURE_ID)  ?: ""),
            "depth"      to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_DEPTH)       ?: ""),
            "topWidth"   to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_TOP_WIDTH)   ?: ""),
            "isBroken"   to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_IS_BROKEN)   ?: ""),
            "isHanging"  to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_IS_HANGING)  ?: ""),
            "isSilt"     to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_IS_SILT)     ?: ""),
            "remarks"    to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_REMARKS)     ?: ""),
            "photo1"     to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_PHOTO_1)     ?: ""),
            "photo2"     to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_PHOTO_2)     ?: ""),
            "photo3"     to (data?.getStringExtra(GutterFormActivity.RESULT_DATA_PHOTO_3)     ?: "")
        )
}
