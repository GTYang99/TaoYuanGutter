package com.example.taoyuangutter.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng

class NoDitchReportViewModel : ViewModel() {
    private val _selectedLatLng = MutableLiveData<LatLng?>(null)
    val selectedLatLng: LiveData<LatLng?> = _selectedLatLng

    private val _note = MutableLiveData("")
    val note: LiveData<String> = _note

    private val _isSubmitting = MutableLiveData(false)
    val isSubmitting: LiveData<Boolean> = _isSubmitting

    fun setPick(latLng: LatLng) {
        _selectedLatLng.value = latLng
    }

    fun clearPick() {
        _selectedLatLng.value = null
    }

    fun setNote(note: String) {
        _note.value = note
    }

    fun setSubmitting(submitting: Boolean) {
        _isSubmitting.value = submitting
    }

    fun canSubmit(): Boolean {
        val hasPoint = _selectedLatLng.value != null
        val hasNote = _note.value.orEmpty().trim().isNotEmpty()
        return hasPoint && hasNote
    }

    fun clearAll() {
        _selectedLatLng.value = null
        _note.value = ""
        _isSubmitting.value = false
    }
}
