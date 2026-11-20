package com.example.taoyuangutter.main

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import com.example.taoyuangutter.map.MapOverlayController
import org.junit.Assert.assertEquals
import org.junit.Test

class MainViewModelTest {
    @Test
    fun saveAndLoadOverlayStatePreservesDistrictsAndGeneration() {
        val context = testContext()
        val viewModel = MainViewModel()
        val state = MapOverlayController.OverlayState(
            selectedLayer = "PHOTO2",
            showPlan = false,
            showWaterOld = true,
            showPossible = false,
            showRegion = true,
            showNoDitchPoints = true,
            showDeletedArea = false,
            selectedDistricts = setOf("桃園區", "中壢區"),
            generation = 7L,
        )

        viewModel.saveOverlayState(context, state)

        val restored = viewModel.loadOverlayState(context)
        assertEquals(state.selectedLayer, restored?.selectedLayer)
        assertEquals(state.showPlan, restored?.showPlan)
        assertEquals(state.showWaterOld, restored?.showWaterOld)
        assertEquals(state.showPossible, restored?.showPossible)
        assertEquals(state.showRegion, restored?.showRegion)
        assertEquals(state.showNoDitchPoints, restored?.showNoDitchPoints)
        assertEquals(state.showDeletedArea, restored?.showDeletedArea)
        assertEquals(state.selectedDistricts, restored?.selectedDistricts)
        assertEquals(state.generation, restored?.generation)
    }

    @Test
    fun loadOverlayStateReturnsNullWhenNothingIsStored() {
        val context = testContext()
        val restored = MainViewModel().loadOverlayState(context)

        assertEquals(null, restored)
    }

    @Test
    fun saveAndLoadOverlayStatePreservesExplicitlyEmptyDistrictSelection() {
        val context = testContext()
        val viewModel = MainViewModel()
        val state = MapOverlayController.OverlayState(
            selectedLayer = "EMAP",
            showPlan = true,
            showWaterOld = true,
            showPossible = true,
            showRegion = true,
            showNoDitchPoints = false,
            showDeletedArea = false,
            selectedDistricts = emptySet(),
        )

        viewModel.saveOverlayState(context, state)

        assertEquals(emptySet<String>(), viewModel.loadOverlayState(context)?.selectedDistricts)
    }

    private fun testContext(): Context = object : ContextWrapper(null) {
        private val preferences = InMemorySharedPreferences()

        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = preferences
    }

    private class InMemorySharedPreferences : SharedPreferences {
        private val values = mutableMapOf<String, Any?>()

        override fun getAll(): Map<String, *> = values.toMap()
        override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue
        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? =
            (values[key] as? Set<String>)?.toSet() ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = values.containsKey(key)

        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            private val updates = mutableMapOf<String, Any?>()
            private var clearRequested = false

            override fun putString(key: String?, value: String?) = apply { updates[key.orEmpty()] = value }
            override fun putStringSet(key: String?, values: Set<String>?) = apply { updates[key.orEmpty()] = values?.toSet() }
            override fun putInt(key: String?, value: Int) = apply { updates[key.orEmpty()] = value }
            override fun putLong(key: String?, value: Long) = apply { updates[key.orEmpty()] = value }
            override fun putFloat(key: String?, value: Float) = apply { updates[key.orEmpty()] = value }
            override fun putBoolean(key: String?, value: Boolean) = apply { updates[key.orEmpty()] = value }
            override fun remove(key: String?) = apply { updates[key.orEmpty()] = REMOVED }
            override fun clear() = apply { clearRequested = true }
            override fun commit(): Boolean { applyChanges(); return true }
            override fun apply() { applyChanges() }

            private fun applyChanges() {
                if (clearRequested) values.clear()
                updates.forEach { (key, value) -> if (value === REMOVED) values.remove(key) else values[key] = value }
            }
        }

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

        private companion object {
            val REMOVED = Any()
        }
    }
}
