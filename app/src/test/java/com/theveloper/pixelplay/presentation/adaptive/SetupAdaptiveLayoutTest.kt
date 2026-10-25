package com.theveloper.pixelplay.presentation.adaptive

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SetupAdaptiveLayoutTest {
    @Test
    fun `phone and portrait tablet pages keep a bounded column`() {
        for (width in listOf(320f, 412f, 600f, 800f)) {
            val layout = calculateSetupPageLayout(width, 700f)
            assertFalse(layout.twoPane)
            assertTrue(layout.contentWidth <= width)
            assertTrue(layout.contentWidth <= 600f)
        }
    }

    @Test
    fun `wide onboarding has room for two readable panes`() {
        for (width in listOf(840f, 1024f, 1280f, 1800f)) {
            val layout = calculateSetupPageLayout(width, 700f)
            assertTrue(layout.twoPane)
            val pane = (layout.contentWidth - layout.horizontalPadding * 2 - layout.paneGap) / 2
            assertTrue(pane >= 380f)
            assertTrue(layout.contentWidth <= 1120f)
        }
    }

    @Test
    fun `large fonts and short split-screen windows fall back to scrolling single column`() {
        assertFalse(calculateSetupPageLayout(840f, 700f, 1.5f).twoPane)
        assertFalse(calculateSetupPageLayout(1120f, 700f, 2f).twoPane)
        assertFalse(calculateSetupPageLayout(1000f, 240f).twoPane)
        assertTrue(calculateSetupPageLayout(1024f, 700f, 1.3f).twoPane)
    }

    @Test
    fun `artwork stays bounded and invalid constraints remain safe`() {
        for (height in listOf(200f, 400f, 700f, 1400f)) {
            assertTrue(calculateSetupPageLayout(412f, height).visualHeight in 200f..260f)
        }
        val invalid = calculateSetupPageLayout(Float.NaN, Float.POSITIVE_INFINITY, 0f)
        assertEquals(0f, invalid.contentWidth)
        assertFalse(invalid.twoPane)
    }
}
