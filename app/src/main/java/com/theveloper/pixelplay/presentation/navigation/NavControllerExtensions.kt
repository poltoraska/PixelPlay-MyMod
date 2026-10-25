package com.theveloper.pixelplay.presentation.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

private fun NavController.isReadyForNavigation(): Boolean {
    return runCatching {
        // We allow navigation if the current entry is at least STARTED.
        // This is safer than strictly RESUMED as transitions can sometimes delay RESUMED state.
        currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) == true
    }.getOrDefault(false)
}

fun NavController.navigateSafely(route: String): Boolean {
    if (!isReadyForNavigation()) return false
    navigate(route) {
        launchSingleTop = true
    }
    return true
}

fun NavController.navigateSafely(
    route: String,
    builder: NavOptionsBuilder.() -> Unit
): Boolean {
    if (!isReadyForNavigation()) return false
    navigate(route) {
        launchSingleTop = true
        builder()
    }
    return true
}

fun NavController.navigateSafelyReplacing(
    route: String,
    patternToPop: String,
    builder: NavOptionsBuilder.() -> Unit = {}
): Boolean {
    if (!isReadyForNavigation()) return false
    navigate(route) {
        launchSingleTop = false
        popUpTo(patternToPop) {
            inclusive = true
        }
        builder()
    }
    return true
}

fun NavController.navigateToTopLevelSafely(route: String): Boolean {
    if ((!isMainRootRoute(route) && route != Screen.Settings.route) || !isReadyForNavigation()) return false
    if (currentDestination?.route == route) return true
    val startDestinationId = runCatching { graph.startDestinationId }.getOrNull() ?: return false
    navigate(route) {
        popUpTo(startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
    // Restoring a tab can restore its child screen (e.g. Home -> Settings) as well.
    // A top-level button always shows the requested root while retaining its saved state.
    if (currentDestination?.route != route) {
        popBackStack(route, inclusive = false)
    }
    return currentDestination?.route == route
}


// Selecting from the persistent library list replaces the right pane, including across categories.
fun NavController.navigateLibraryDetailSafely(route: String, patternToPop: String): Boolean {
    if (!isReadyForNavigation()) return false
    val hasLibrary = runCatching { getBackStackEntry(Screen.Library.route) }.isSuccess
    if (hasLibrary && com.theveloper.pixelplay.presentation.adaptive.isLibraryDetailRoute(
            currentDestination?.route
        )
    ) {
        navigate(route) {
            popUpTo(Screen.Library.route) { inclusive = false }
            launchSingleTop = true
        }
        return true
    }
    return navigateSafelyReplacing(route, patternToPop)
}
