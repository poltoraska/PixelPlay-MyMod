package com.theveloper.pixelplay.presentation.components.scoped

import org.junit.Assert.*
import org.junit.Test

class PhoneChromeScrollStateTest {
    private fun state() = PhoneChromeScrollState(36f, 24f)
    @Test fun `small movements and direction jitter do not collapse controls`() {
        val state = state()
        repeat(20) { assertFalse(state.onScroll(-8f)); assertFalse(state.onScroll(8f)) }
    }
    @Test fun `accumulated downward drag collapses and upward drag restores`() {
        val state = state()
        assertFalse(state.onScroll(-20f))
        assertTrue(state.onScroll(-16f))
        assertTrue(state.onScroll(12f))
        assertFalse(state.onScroll(12f))
    }
    @Test fun `long continued downward scrolling does not delay upward response`() {
        val state = state()
        state.onScroll(-1000f)
        state.onScroll(-1000f)
        assertFalse(state.onScroll(24f))
    }
    @Test fun `route change or reaching top resets state and pending distance`() {
        val state = state()
        state.onScroll(-50f)
        state.reset()
        assertFalse(state.collapsed)
        assertFalse(state.onScroll(-35f))
    }
    @Test fun `invalid and horizontal only scroll values are ignored`() {
        val state = state()
        assertFalse(state.onScroll(Float.NaN))
        assertFalse(state.onScroll(Float.NEGATIVE_INFINITY))
        assertFalse(state.onScroll(0f))
        assertFalse(state.onScroll(-35f))
    }
}
