package com.theveloper.pixelplay.presentation.components

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.*
import org.junit.Test

class PlayerArtworkTransitionTest {
    private val mini = Rect(280f, 700f, 320f, 740f)
    private val full = Rect(20f, 100f, 340f, 420f)
    @Test fun `cover meets the measured endpoints exactly`() {
        assertEquals(mini, interpolatePlayerArtworkBounds(mini, full, 0f))
        assertEquals(full, interpolatePlayerArtworkBounds(mini, full, 1f))
    }
    @Test fun `interactive gesture reverses on the same path`() {
        val midpoint = Rect(150f, 400f, 330f, 580f)
        assertEquals(midpoint, interpolatePlayerArtworkBounds(mini, full, 0.5f))
        assertEquals(midpoint, interpolatePlayerArtworkBounds(full, mini, 0.5f))
    }
    @Test fun `spring overshoot and invalid geometry progress stay bounded`() {
        assertEquals(mini, interpolatePlayerArtworkBounds(mini, full, -0.2f))
        assertEquals(full, interpolatePlayerArtworkBounds(mini, full, 1.2f))
        assertEquals(mini, interpolatePlayerArtworkBounds(mini, full, Float.NaN))
    }
    @Test fun `unknown track duration has no invented progress`() {
        assertEquals(0f, playbackRingProgress(3000, 0), 0f)
        assertEquals(0f, playbackRingProgress(3000, -1), 0f)
    }
    @Test fun `progress follows seeks and is clamped at track bounds`() {
        assertEquals(0.5f, playbackRingProgress(5000, 10000), 0f)
        assertEquals(0f, playbackRingProgress(0, 10000), 0f)
        assertEquals(0f, playbackRingProgress(-1, 10000), 0f)
        assertEquals(1f, playbackRingProgress(12000, 10000), 0f)
    }
}
