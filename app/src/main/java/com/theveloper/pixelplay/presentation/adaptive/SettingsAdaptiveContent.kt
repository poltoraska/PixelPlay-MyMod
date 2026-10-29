package com.theveloper.pixelplay.presentation.adaptive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.saveable.rememberSaveable
import com.theveloper.pixelplay.presentation.navigation.LocalTabletTabDirection
import com.theveloper.pixelplay.presentation.navigation.rememberTabletTabDirection
import com.theveloper.pixelplay.presentation.navigation.tabletTabTravelPx
import com.theveloper.pixelplay.presentation.navigation.tabletTabEnterTransition
import com.theveloper.pixelplay.presentation.navigation.tabletTabExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.theveloper.pixelplay.presentation.model.SettingsCategory
import com.theveloper.pixelplay.presentation.navigation.Screen
import com.theveloper.pixelplay.presentation.navigation.navigateSafely
import com.theveloper.pixelplay.presentation.screens.SettingsPaneMenu

internal val LocalSettingsDetailPane = staticCompositionLocalOf { false }
internal val LocalSettingsRootSection = staticCompositionLocalOf { false }

internal fun isSettingsDestination(route: String?): Boolean = route in setOf(
    Screen.Settings.route, Screen.SettingsCategory.route, Screen.Accounts.route,
    Screen.PaletteStyle.route, Screen.Experimental.route, Screen.NavBarCrRad.route,
    Screen.About.route, Screen.OpenSourceLicenses.route, Screen.EasterEgg.route,
    Screen.ArtistSettings.route, Screen.DelimiterConfig.route, Screen.WordDelimiterConfig.route,
    Screen.Equalizer.route, Screen.DeviceCapabilities.route, Screen.NeteaseDashboard.route,
    Screen.QqMusicDashboard.route, Screen.NavidromeDashboard.route, Screen.JellyfinDashboard.route,
)

internal fun settingsSectionForRoute(route: String?, categoryId: String?): String = when (route) {
    Screen.SettingsCategory.route -> categoryId ?: SettingsCategory.LIBRARY.id
    Screen.PaletteStyle.route, Screen.NavBarCrRad.route -> SettingsCategory.APPEARANCE.id
    Screen.Experimental.route -> SettingsCategory.DEVELOPER.id
    Screen.ArtistSettings.route, Screen.DelimiterConfig.route, Screen.WordDelimiterConfig.route -> SettingsCategory.LIBRARY.id
    Screen.Equalizer.route -> SettingsCategory.EQUALIZER.id
    Screen.DeviceCapabilities.route -> SettingsCategory.DEVICE_CAPABILITIES.id
    Screen.Accounts.route, Screen.NeteaseDashboard.route, Screen.QqMusicDashboard.route,
    Screen.NavidromeDashboard.route, Screen.JellyfinDashboard.route -> Screen.Accounts.route
    Screen.About.route, Screen.OpenSourceLicenses.route, Screen.EasterEgg.route -> SettingsCategory.ABOUT.id
    else -> SettingsCategory.LIBRARY.id
}

internal fun settingsSectionRoute(section: String): String = when (section) {
    Screen.Accounts.route -> Screen.Accounts.route
    SettingsCategory.EQUALIZER.id -> Screen.Equalizer.route
    SettingsCategory.DEVICE_CAPABILITIES.id -> Screen.DeviceCapabilities.route
    SettingsCategory.ABOUT.id -> Screen.About.route
    else -> Screen.SettingsCategory.createRoute(section)
}

internal fun isSettingsRootSection(route: String?): Boolean = route in setOf(
    Screen.Settings.route, Screen.SettingsCategory.route, Screen.Accounts.route,
    Screen.Equalizer.route, Screen.DeviceCapabilities.route, Screen.About.route,
)

internal data class SettingsPaneLayout(val listWidth: Float, val detailLeft: Float, val detailWidth: Float)

internal fun calculateSettingsPaneLayout(
    width: Float, hingeStart: Float? = null, hingeEnd: Float? = null,
): SettingsPaneLayout? {
    if (!width.isFinite() || width < 520f) return null
    if (hingeStart != null && hingeEnd != null && hingeStart.isFinite() && hingeEnd.isFinite() &&
        hingeEnd > hingeStart && hingeStart >= 200f && width - hingeEnd >= 300f
    ) return SettingsPaneLayout(hingeStart - 6f, hingeEnd + 6f, width - hingeEnd - 6f)
    // Do not place controls across an occluding hinge when either side is too narrow.
    if (hingeStart != null && hingeEnd != null && hingeEnd > hingeStart) return null
    val listWidth = (width * 0.34f).coerceIn(200f, 320f)
    return SettingsPaneLayout(listWidth, listWidth + 12f, width - listWidth - 12f)
}

/** Reuse the existing navigation graph and each destination's own ViewModel/saved state. */
@Composable
internal fun SettingsAdaptiveContent(
    navController: NavHostController,
    content: @Composable (Modifier) -> Unit,
) {
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val tabDirection = rememberTabletTabDirection(route)
    val tabTravelPx = tabletTabTravelPx()
    val layout = LocalAdaptiveLayout.current
    val settingsEntry = remember(entry) {
        runCatching { navController.getBackStackEntry(Screen.Settings.route) }.getOrNull()
    }
    val panes = calculateSettingsPaneLayout(layout.contentWidth, layout.hingeStart, layout.hingeEnd)
        ?.takeIf { layout.usesSideNavigation }
    val showSettings = panes != null && isSettingsDestination(route) && settingsEntry != null
    val topPadding = if (panes != null) libraryPaneTopPadding() else 0.dp
    var lastSection by rememberSaveable { mutableStateOf(SettingsCategory.LIBRARY.id) }
    val selectedSection = if (showSettings) {
        settingsSectionForRoute(route, entry?.arguments?.getString("categoryId"))
    } else lastSection
    SideEffect { if (showSettings) lastSection = selectedSection }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).clipToBounds()) {
        // Each ScreenWrapper applies the frame belonging to its own back-stack entry.
        CompositionLocalProvider(
            LocalSettingsDetailPane provides showSettings,
            LocalTabletTabDirection provides tabDirection,
        ) {
            content(Modifier.fillMaxSize())
        }
        AnimatedVisibility(
            visible = showSettings,
            enter = tabletTabEnterTransition(tabDirection, tabTravelPx),
            exit = tabletTabExitTransition(tabDirection, tabTravelPx),
        ) {
            if (panes != null) {
                Box(Modifier.width(panes.listWidth.dp).fillMaxHeight().padding(top = topPadding)) {
                    SettingsPaneMenu(selectedSection = selectedSection, onSelectSection = { section ->
                        if (showSettings) {
                            val target = settingsSectionRoute(section)
                            val currentTarget = if (route == Screen.SettingsCategory.route) {
                                Screen.SettingsCategory.createRoute(selectedSection)
                            } else route
                            val showingDefault = route == Screen.Settings.route && section == SettingsCategory.LIBRARY.id
                            if (target != currentTarget && !showingDefault) {
                                navController.navigateSafely(target) {
                                    popUpTo(Screen.Settings.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        }
                    })
                }
            }
        }
    }
}


@Composable
internal fun settingsPaneTitleStyle() = MaterialTheme.typography.headlineMedium.let { style ->
    style.copy(fontSize = style.fontSize * 1.2f, lineHeight = style.fontSize * 1.32f)
}

@Composable
internal fun settingsPaneHeaderHeight(title: String, trailingWidth: androidx.compose.ui.unit.Dp = 0.dp): androidx.compose.ui.unit.Dp? {
    if (!LocalSettingsDetailPane.current) return null
    val density = androidx.compose.ui.platform.LocalDensity.current
    val titleWidth = (LocalAdaptiveLayout.current.contentWidth.dp - 40.dp - trailingWidth -
        if (LocalSettingsRootSection.current) 0.dp else 48.dp).coerceAtLeast(1.dp)
    val measured = androidx.compose.ui.text.rememberTextMeasurer().measure(
        text = title,
        style = settingsPaneTitleStyle(),
        constraints = androidx.compose.ui.unit.Constraints(maxWidth = with(density) { titleWidth.roundToPx() }),
    )
    return maxOf(80.dp, with(density) { measured.size.height.toDp() } + 32.dp)
}
