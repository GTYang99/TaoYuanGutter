package com.example.taoyuangutter.main

const val MAIN_MAP_SCOPE_SEARCH_MIN_ZOOM = 16f

enum class MainMapLoadIndicatorMode {
    HIDDEN,
    LOW_ZOOM,
    LOADING,
    ERROR
}

data class MainMapLoadIndicatorState(
    val mode: MainMapLoadIndicatorMode,
    val zoom: Float
)

class MainMapLoadIndicatorStateMachine {
    private var state = MainMapLoadIndicatorState(
        mode = MainMapLoadIndicatorMode.HIDDEN,
        zoom = 0f
    )

    fun syncZoom(zoom: Float): MainMapLoadIndicatorState {
        state = when (state.mode) {
            MainMapLoadIndicatorMode.LOADING,
            MainMapLoadIndicatorMode.ERROR -> state.copy(zoom = zoom)
            MainMapLoadIndicatorMode.LOW_ZOOM,
            MainMapLoadIndicatorMode.HIDDEN ->
                if (zoom < MAIN_MAP_SCOPE_SEARCH_MIN_ZOOM) {
                    state.copy(mode = MainMapLoadIndicatorMode.LOW_ZOOM, zoom = zoom)
                } else {
                    state.copy(mode = MainMapLoadIndicatorMode.HIDDEN, zoom = zoom)
                }
        }
        return state
    }

    fun prepareForNewOperation(zoom: Float): MainMapLoadIndicatorState {
        state = if (zoom < MAIN_MAP_SCOPE_SEARCH_MIN_ZOOM) {
            MainMapLoadIndicatorState(MainMapLoadIndicatorMode.LOW_ZOOM, zoom)
        } else {
            MainMapLoadIndicatorState(MainMapLoadIndicatorMode.HIDDEN, zoom)
        }
        return state
    }

    fun beginLoading(zoom: Float): MainMapLoadIndicatorState {
        state = MainMapLoadIndicatorState(MainMapLoadIndicatorMode.LOADING, zoom)
        return state
    }

    fun finishLoading(zoom: Float): MainMapLoadIndicatorState {
        state = if (zoom < MAIN_MAP_SCOPE_SEARCH_MIN_ZOOM) {
            MainMapLoadIndicatorState(MainMapLoadIndicatorMode.LOW_ZOOM, zoom)
        } else {
            MainMapLoadIndicatorState(MainMapLoadIndicatorMode.HIDDEN, zoom)
        }
        return state
    }

    fun failLoading(zoom: Float): MainMapLoadIndicatorState {
        state = MainMapLoadIndicatorState(MainMapLoadIndicatorMode.ERROR, zoom)
        return state
    }

    fun currentState(): MainMapLoadIndicatorState = state
}
