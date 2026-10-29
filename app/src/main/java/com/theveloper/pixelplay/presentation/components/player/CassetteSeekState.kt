package com.theveloper.pixelplay.presentation.components.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs

/** Tracks pointer movement only; accessibility seeks and single taps do not spin the reels. */
internal class CassetteSeekState {
    var isSeeking by mutableStateOf(false)
        private set
    var direction by mutableIntStateOf(0)
        private set
    var generation by mutableIntStateOf(0)
        private set
    private var previous: Float? = null

    fun begin() {
        isSeeking = true
        previous = null
        direction = 0
        generation++
    }

    fun move(value: Float) {
        if (!isSeeking || !value.isFinite()) return
        val old = previous
        previous = value
        if (old == null || abs(value - old) < 0.00001f) return
        direction = if (value > old) 1 else -1
        generation++
    }

    fun stopIfUnchanged(expectedGeneration: Int) {
        if (generation == expectedGeneration) direction = 0
    }

    fun end() {
        isSeeking = false
        previous = null
        direction = 0
        generation++
    }
}
