package com.example.taoyuangutter

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.taoyuangutter.databinding.ActivityMainBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupButtons()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // 預設移到桃園市中心 (避免看到空白地圖)
        val taoyuan = LatLng(24.9936, 121.3010)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(taoyuan, 13f))
        
        // 設定地圖類型
        googleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
        
        // 啟用 UI 控制項
        googleMap?.uiSettings?.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMapToolbarEnabled = true
        }
    }

    private fun setupButtons() {
        binding.btnLegend.setOnClickListener {
            showLegendDialog()
        }
        
        binding.btnLayers.setOnClickListener {
            showMapTypeDialog()
        }
        
        binding.btnMyLocation.setOnClickListener {
            // TODO: 請求權限並移至當前位置
        }
    }

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
