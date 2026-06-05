package com.example.taoyuangutter.main

import androidx.lifecycle.ViewModel
import com.example.taoyuangutter.map.MapOverlayController

class MainViewModel : ViewModel() {
    var overlayState: MapOverlayController.OverlayState? = null
}
