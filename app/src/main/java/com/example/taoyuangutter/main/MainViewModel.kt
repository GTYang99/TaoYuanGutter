package com.example.taoyuangutter.main

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import com.example.taoyuangutter.map.MapOverlayController

class MainViewModel : ViewModel() {
    var overlayState: MapOverlayController.OverlayState? = null

    fun loadOverlayState(context: Context): MapOverlayController.OverlayState? {
        val prefs = prefs(context)
        val selectedLayer = prefs.getString(KEY_SELECTED_LAYER, null) ?: return null
        val showPlan = prefs.getBoolean(KEY_SHOW_PLAN, true)
        val showWaterOld = prefs.getBoolean(KEY_SHOW_WATER_OLD, true)
        val showPossible = prefs.getBoolean(KEY_SHOW_POSSIBLE, true)
        val showRegion = prefs.getBoolean(KEY_SHOW_REGION, true)
        val showNoDitchPoints = prefs.getBoolean(KEY_SHOW_NO_DITCH_POINTS, false)
        val showDeletedArea = prefs.getBoolean(KEY_SHOW_DELETED_AREA, false)
        val selectedDistricts = if (prefs.contains(KEY_SELECTED_DISTRICTS)) {
            prefs.getStringSet(KEY_SELECTED_DISTRICTS, emptySet()).orEmpty().toSet()
        } else {
            DEFAULT_DISTRICTS
        }
        val generation = prefs.getLong(KEY_GENERATION, 0L)
        return MapOverlayController.OverlayState(
            selectedLayer = selectedLayer,
            showPlan = showPlan,
            showWaterOld = showWaterOld,
            showPossible = showPossible,
            showRegion = showRegion,
            showNoDitchPoints = showNoDitchPoints,
            showDeletedArea = showDeletedArea,
            selectedDistricts = selectedDistricts,
            generation = generation,
        )
    }

    fun saveOverlayState(context: Context, state: MapOverlayController.OverlayState) {
        val editor = prefs(context).edit()
        editor.putString(KEY_SELECTED_LAYER, state.selectedLayer)
        editor.putBoolean(KEY_SHOW_PLAN, state.showPlan)
        editor.putBoolean(KEY_SHOW_WATER_OLD, state.showWaterOld)
        editor.putBoolean(KEY_SHOW_POSSIBLE, state.showPossible)
        editor.putBoolean(KEY_SHOW_REGION, state.showRegion)
        editor.putBoolean(KEY_SHOW_NO_DITCH_POINTS, state.showNoDitchPoints)
        editor.putBoolean(KEY_SHOW_DELETED_AREA, state.showDeletedArea)
        editor.putStringSet(KEY_SELECTED_DISTRICTS, state.selectedDistricts)
        editor.putLong(KEY_GENERATION, state.generation)
        editor.apply()
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "overlay_state_prefs"
        private const val KEY_SELECTED_LAYER = "selected_layer"
        private const val KEY_SHOW_PLAN = "show_plan"
        private const val KEY_SHOW_WATER_OLD = "show_water_old"
        private const val KEY_SHOW_POSSIBLE = "show_possible"
        private const val KEY_SHOW_REGION = "show_region"
        private const val KEY_SHOW_NO_DITCH_POINTS = "show_no_ditch_points"
        private const val KEY_SHOW_DELETED_AREA = "show_deleted_area"
        private const val KEY_SELECTED_DISTRICTS = "selected_districts"
        private const val KEY_GENERATION = "generation"
        private val DEFAULT_DISTRICTS = MapOverlayController.DEFAULT_DISTRICTS
    }
}
