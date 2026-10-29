package com.theveloper.pixelplay.presentation.components.player

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CassetteSeekStateTest {
    @Test
    fun `only pointer movement spins in its direction and release stops it`() {
        val state = CassetteSeekState()
        state.move(0.8f)
        assertEquals(0, state.direction)
        state.begin()
        state.move(0.4f)
        assertEquals(0, state.direction)
        state.move(0.6f)
        assertEquals(1, state.direction)
        state.move(0.2f)
        assertEquals(-1, state.direction)
        state.end()
        assertFalse(state.isSeeking)
        assertEquals(0, state.direction)
    }

    @Test
    fun `idle timeout cannot stop newer motion and next gesture starts fresh`() {
        val state = CassetteSeekState()
        state.begin()
        state.move(0.3f)
        state.move(0.4f)
        val oldGeneration = state.generation
        state.move(0.1f)
        state.stopIfUnchanged(oldGeneration)
        assertEquals(-1, state.direction)
        state.stopIfUnchanged(state.generation)
        assertEquals(0, state.direction)
        state.end()
        state.begin()
        state.move(0.9f)
        assertEquals(0, state.direction)
    }
}
