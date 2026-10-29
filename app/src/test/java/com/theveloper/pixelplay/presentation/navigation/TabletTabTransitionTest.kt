package com.theveloper.pixelplay.presentation.navigation

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TabletTabTransitionTest {
    @Test
    fun `all tablet root tabs use the same switch policy in both directions`() {
        val tabs = listOf(Screen.Home.route, Screen.Search.route, Screen.Library.route, Screen.Settings.route)
        for (from in tabs) for (to in tabs) assertEquals(from != to, isTabletTabSwitch(from, to))
    }

    @Test
    fun `settings children remain in the settings tab`() {
        assertFalse(isTabletTabSwitch(Screen.Settings.route, Screen.SettingsCategory.route))
        assertFalse(isTabletTabSwitch(Screen.SettingsCategory.route, Screen.PaletteStyle.route))
        assertTrue(isTabletTabSwitch(Screen.SettingsCategory.route, Screen.Library.route))
        assertTrue(isTabletTabSwitch(Screen.Home.route, Screen.About.route))
    }

    @Test
    fun `library detail transitions and phone root ordering retain their own policy`() {
        assertFalse(isTabletTabSwitch(Screen.Library.route, Screen.AlbumDetail.route))
        assertFalse(isTabletTabSwitch(Screen.AlbumDetail.route, Screen.Library.route))
        assertFalse(isTabletTabSwitch(null, Screen.Library.route))
        assertFalse(isTabletTabSwitch(Screen.Stats.route, Screen.Library.route))
        assertNull(mainRootRouteIndex(Screen.Settings.route))
        assertEquals(2, mainRootRouteIndex(Screen.Library.route))
    }

    @Test
    fun `spring direction follows tab order and reverses when returning`() {
        assertEquals(1, tabletTabDirection(Screen.Home.route, Screen.Library.route))
        assertEquals(-1, tabletTabDirection(Screen.Library.route, Screen.Home.route))
        assertEquals(-1, tabletTabDirection(Screen.SettingsCategory.route, Screen.Library.route))
        assertEquals(0, tabletTabDirection(Screen.Settings.route, Screen.PaletteStyle.route))
        assertEquals(0, tabletTabDirection(null, Screen.Library.route))
    }

    @Test
    fun `interrupted return keeps the spring without moving settings subpages`() {
        assertTrue(usesTabletTabSpring(Screen.Home.route, Screen.Home.route, -1))
        assertTrue(usesTabletTabSpring(Screen.Settings.route, Screen.Settings.route, 1))
        assertFalse(usesTabletTabSpring(Screen.Home.route, Screen.Home.route, 0))
        assertFalse(usesTabletTabSpring(Screen.SettingsCategory.route, Screen.SettingsCategory.route, 1))
        assertFalse(usesTabletTabSpring(Screen.Settings.route, Screen.PaletteStyle.route, 1))
    }

    @Test
    fun `direction persists through nested pages and follows rapid tab reversals`() {
        val motion = TabletTabMotionState()
        val routes = listOf(
            Screen.Home.route, Screen.Library.route, Screen.AlbumDetail.route,
            Screen.Library.route, Screen.Settings.route, Screen.SettingsCategory.route,
            Screen.PaletteStyle.route, Screen.Library.route, Screen.Search.route,
            Screen.Settings.route, Screen.Home.route,
        )
        val expected = listOf(0, 1, 1, 1, 1, 1, 1, -1, -1, 1, -1)
        routes.forEachIndexed { index, route ->
            assertEquals(expected[index], motion.onDestinationChanged(route))
        }
    }

}
