package com.example.taoyuangutter.map

internal enum class DistrictSelectionState {
    NONE,
    PARTIAL,
    ALL
}

internal fun districtSelectionState(selectedCount: Int, totalCount: Int): DistrictSelectionState = when {
    selectedCount <= 0 -> DistrictSelectionState.NONE
    selectedCount >= totalCount -> DistrictSelectionState.ALL
    else -> DistrictSelectionState.PARTIAL
}
