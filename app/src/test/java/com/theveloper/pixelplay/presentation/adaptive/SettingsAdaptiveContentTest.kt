package com.theveloper.pixelplay.presentation.adaptive

import com.theveloper.pixelplay.presentation.model.SettingsCategory
import com.theveloper.pixelplay.presentation.navigation.Screen
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SettingsAdaptiveContentTest {
    @Test
    fun `tablet menu stays narrower than detail and fits the viewport`() {
        for (width in listOf(520f, 600f, 740f, 1024f, 1600f)) {
            val panes = requireNotNull(calculateSettingsPaneLayout(width))
            assertTrue(panes.listWidth in 200f..320f)
            assertTrue(panes.listWidth < panes.detailWidth)
            assertTrue(panes.detailWidth >= 300f)
            assertEquals(width, panes.detailLeft + panes.detailWidth)
            assertEquals(12f, panes.detailLeft - panes.listWidth)
        }
    }

    @Test
    fun `insufficient and invalid window sizes retain single screen navigation`() {
        for (width in listOf(412f, 519f, 0f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertNull(calculateSettingsPaneLayout(width))
        }
    }

    @Test
    fun `physical hinge remains uncovered by either settings pane`() {
        val panes = requireNotNull(calculateSettingsPaneLayout(1000f, 480f, 500f))
        assertTrue(panes.listWidth < 480f)
        assertTrue(panes.detailLeft > 500f)
        assertEquals(1000f, panes.detailLeft + panes.detailWidth)
        assertNull(calculateSettingsPaneLayout(1000f, 800f, 820f))
    }

    @Test
    fun `nested destinations keep their parent category selected`() {
        assertEquals(SettingsCategory.APPEARANCE.id, settingsSectionForRoute(Screen.PaletteStyle.route, null))
        assertEquals(SettingsCategory.LIBRARY.id, settingsSectionForRoute(Screen.WordDelimiterConfig.route, null))
        assertEquals(SettingsCategory.DEVELOPER.id, settingsSectionForRoute(Screen.Experimental.route, null))
        assertEquals(SettingsCategory.ABOUT.id, settingsSectionForRoute(Screen.OpenSourceLicenses.route, null))
        assertEquals(Screen.Accounts.route, settingsSectionForRoute(Screen.JellyfinDashboard.route, null))
        assertEquals(SettingsCategory.PLAYBACK.id, settingsSectionForRoute(Screen.SettingsCategory.route, "playback"))
    }

    @Test
    fun `all menu sections open a recognized destination and hide only the root back button`() {
        for (category in SettingsCategory.entries) {
            val target = settingsSectionRoute(category.id)
            val route = if (target.startsWith("settings_category/")) Screen.SettingsCategory.route else target
            assertTrue(isSettingsDestination(route), target)
            assertTrue(isSettingsRootSection(route), target)
        }
        assertFalse(isSettingsRootSection(Screen.PaletteStyle.route))
        assertFalse(isSettingsRootSection(Screen.OpenSourceLicenses.route))
        assertFalse(isSettingsDestination(Screen.AlbumDetail.route))
        assertFalse(isSettingsDestination(Screen.Home.route))
    }
}
