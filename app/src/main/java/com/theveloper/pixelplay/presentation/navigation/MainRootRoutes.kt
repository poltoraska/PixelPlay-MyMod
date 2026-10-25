package com.theveloper.pixelplay.presentation.navigation

internal fun isMainRootRoute(route: String?): Boolean = when (route) {
    Screen.Home.route,
    Screen.Search.route,
    Screen.Library.route -> true
    else -> false
}

internal fun mainRootRouteIndex(route: String?): Int? = when (route) {
    Screen.Home.route -> 0
    Screen.Search.route -> 1
    Screen.Library.route -> 2
    else -> null
}



/** Settings descendants still belong to the same top-level tablet destination. */
internal fun tabletTabRouteIndex(route: String?): Int? = mainRootRouteIndex(route)
    ?: if (com.theveloper.pixelplay.presentation.adaptive.isSettingsDestination(route)) 3 else null

internal fun isTabletTabSwitch(fromRoute: String?, toRoute: String?): Boolean {
    val from = tabletTabRouteIndex(fromRoute) ?: return false
    val to = tabletTabRouteIndex(toRoute) ?: return false
    return from != to
}


internal fun tabletTabDirection(fromRoute: String?, toRoute: String?): Int {
    val from = tabletTabRouteIndex(fromRoute) ?: return 0
    val to = tabletTabRouteIndex(toRoute) ?: return 0
    return to.compareTo(from)
}

internal fun usesTabletTabSpring(fromRoute: String?, toRoute: String?, direction: Int): Boolean =
    isTabletTabSwitch(fromRoute, toRoute) ||
        // A quick return can target the root that AnimatedContent still considers its initial state.
        (direction != 0 && fromRoute == toRoute &&
            (isMainRootRoute(toRoute) || toRoute == Screen.Settings.route))

/** Keep the last switch direction while a tab's own pages are opening or closing. */
internal class TabletTabMotionState {
    private var previousTabRoute: String? = null
    private var direction = 0

    fun onDestinationChanged(route: String?): Int {
        if (tabletTabRouteIndex(route) == null) return direction
        val nextDirection = tabletTabDirection(previousTabRoute, route)
        if (nextDirection != 0) direction = nextDirection
        previousTabRoute = route
        return direction
    }
}
