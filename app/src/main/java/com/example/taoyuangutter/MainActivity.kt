package com.example.taoyuangutter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.taoyuangutter.databinding.ActivityMainBinding
import com.example.taoyuangutter.gutter.AddGutterBottomSheet
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng

class MainActivity : AppCompatActivity(),
    OnMapReadyCallback,
    AddGutterBottomSheet.LocationPickerHost {

    private lateinit var binding: ActivityMainBinding
    private var googleMap: GoogleMap? = null

    // 目前存活的 BottomSheet 與正在選點的索引
    private var activeSheet: AddGutterBottomSheet? = null
    private var pickingIndex: Int = -1

    // ── Lifecycle ────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupButtons()
        setupLocationPickerOverlay()
    }

    // ── Google Map ────────────────────────────────────────────────────────
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val taoyuan = LatLng(24.9936, 121.3010)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(taoyuan, 13f))
        googleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
        googleMap?.uiSettings?.apply {
            isZoomControlsEnabled = true
            isCompassEnabled      = true
            isMapToolbarEnabled   = true
        }
    }

    // ── 地圖按鈕 ──────────────────────────────────────────────────────────
    private fun setupButtons() {
        binding.btnLegend.setOnClickListener { showLegendDialog() }
        binding.btnLayers.setOnClickListener { showMapTypeDialog() }
        binding.btnMyLocation.setOnClickListener {
            // TODO: 請求權限並移至當前位置
        }
        binding.btnAddGutter.setOnClickListener {
            val sheet = AddGutterBottomSheet.newInstance()
            activeSheet = sheet
            sheet.show(supportFragmentManager, AddGutterBottomSheet.TAG)
        }
    }

    // ── LocationPickerHost 實作 ───────────────────────────────────────────
    /**
     * 由 AddGutterBottomSheet cell 點選時呼叫。
     * 隱藏底部面板，顯示地圖選點 overlay。
     */
    override fun startLocationPick(
        sheet: AddGutterBottomSheet,
        waypointIndex: Int
    ) {
        activeSheet  = sheet
        pickingIndex = waypointIndex

        // 隱藏 BottomSheet，以地圖為主
        sheet.hideSelf()

        // 隱藏 + FAB，顯示 overlay
        binding.btnAddGutter.visibility = View.GONE
        binding.locationPickerOverlay.root.visibility = View.VISIBLE
    }

    // ── 選點 Overlay 按鈕邏輯 ────────────────────────────────────────────
    private fun setupLocationPickerOverlay() {
        // 確認：取地圖中心點，回傳給 BottomSheet
        binding.locationPickerOverlay.btnConfirmPick.setOnClickListener {
            val latLng = googleMap?.cameraPosition?.target
            binding.locationPickerOverlay.root.visibility = View.GONE
            binding.btnAddGutter.visibility = View.VISIBLE

            if (latLng != null && pickingIndex >= 0) {
                activeSheet?.updateWaypointLocation(pickingIndex, latLng)
            }

            activeSheet?.showSelf()
            pickingIndex = -1
        }

        // 取消：直接回到 BottomSheet
        binding.locationPickerOverlay.btnCancelPick.setOnClickListener {
            binding.locationPickerOverlay.root.visibility = View.GONE
            binding.btnAddGutter.visibility = View.VISIBLE
            activeSheet?.showSelf()
            pickingIndex = -1
        }
    }

    // ── Dialog ────────────────────────────────────────────────────────────
    private fun showMapTypeDialog() {
        val options = arrayOf(getString(R.string.map_normal), getString(R.string.map_satellite))
        AlertDialog.Builder(this)
            .setTitle(R.string.layers_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> googleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
                    1 -> googleMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
                }
            }
            .show()
    }

    private fun showLegendDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_legend, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
}
