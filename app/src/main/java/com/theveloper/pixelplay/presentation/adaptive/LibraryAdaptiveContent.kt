package com.theveloper.pixelplay.presentation.adaptive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import com.theveloper.pixelplay.presentation.navigation.LocalTabletTabDirection
import com.theveloper.pixelplay.presentation.navigation.tabletTabTravelPx
import com.theveloper.pixelplay.presentation.navigation.tabletTabEnterTransition
import com.theveloper.pixelplay.presentation.navigation.tabletTabExitTransition
import androidx.annotation.OptIn
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.theveloper.pixelplay.presentation.navigation.Screen
import com.theveloper.pixelplay.presentation.screens.HomeScreen
import com.theveloper.pixelplay.presentation.screens.LibraryScreen
import com.theveloper.pixelplay.presentation.viewmodel.PlayerViewModel

internal fun isLibraryDetailRoute(route: String?): Boolean = route != null && listOf(
    Screen.AlbumDetail.route, Screen.ArtistDetail.route,
    Screen.PlaylistDetail.route, Screen.GenreDetail.route,
).any { route.startsWith(it.substringBefore('/') + "/") }

internal const val LibraryPaneAnimationDuration = 180

internal val LocalLibraryBrowserPane = staticCompositionLocalOf { false }
internal val LocalLibraryBrowserTargetWidth = staticCompositionLocalOf { 0f }

internal fun albumCassetteCellWidth(widthPx: Int, paddingPx: Int, columns: Int, spacingPx: Int): Int =
    ((widthPx - paddingPx - spacingPx * (columns - 1)) / columns).coerceAtLeast(1)

internal fun albumCassetteColumnCount(sideNavigation: Boolean, detailOpen: Boolean, width: Float): Int =
    if (sideNavigation && !detailOpen && width.isFinite() && width >= 600f) 2 else 1

internal val LocalLibraryDetailPane = staticCompositionLocalOf { false }
internal val LibraryPaneShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

@Composable
internal fun libraryPaneTopPadding() =
    WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() + 8.dp

internal data class LibraryPaneLayout(
    val listWidth: Float,
    val detailLeft: Float,
    val detailWidth: Float,
    val listLeft: Float = 0f,
)

internal fun calculateLibraryPaneLayout(
    width: Float, hingeStart: Float? = null, hingeEnd: Float? = null,
): LibraryPaneLayout? {
    if (!width.isFinite() || width < 720f) return null
    if (hingeStart != null && hingeEnd != null && hingeEnd > hingeStart) {
        // Keep both panes equal while leaving an occluding hinge uncovered.
        val paneWidth = minOf(hingeStart, width - hingeEnd)
        return if (paneWidth >= 320f) {
            LibraryPaneLayout(paneWidth, hingeEnd, paneWidth, hingeStart - paneWidth)
        } else null
    }
    val paneWidth = width / 2f
    return LibraryPaneLayout(paneWidth, paneWidth, paneWidth)
}

/** One persistent library browser; detail destinations retain their own NavHost owners. */
@OptIn(UnstableApi::class)
@Composable
internal fun LibraryAdaptiveContent(
    navController: NavHostController,
    playerViewModel: PlayerViewModel,
    homePadding: PaddingValues,
    onOpenSidebar: () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    val entry by navController.currentBackStackEntryAsState()
    val visibleEntries by navController.visibleEntries.collectAsState()
    val route = entry?.destination?.route
    val tabDirection = LocalTabletTabDirection.current
    val tabTravelPx = tabletTabTravelPx()
    val layout = LocalAdaptiveLayout.current
    var libraryBranchActive by rememberSaveable { mutableStateOf(false) }
    // Root + detail can be navigated to before Compose observes the intermediate root.
    // Read the actual parent immediately so that the first detail frame already uses two panes.
    val hasLibraryParent = isLibraryDetailRoute(route) &&
        navController.previousBackStackEntry?.destination?.route == Screen.Library.route
    LaunchedEffect(route) {
        if (hasLibraryParent) {
            libraryBranchActive = true
        } else if (route != null && !isLibraryDetailRoute(route)) {
            libraryBranchActive = route == Screen.Library.route
        }
    }
    val libraryEntry = remember(entry, visibleEntries) {
        runCatching { navController.getBackStackEntry(Screen.Library.route) }.getOrNull()
            ?: visibleEntries.firstOrNull { it.destination.route == Screen.Library.route }
    }
    // A popped detail remains visible until NavHost finishes its exit transition.
    // Keep its pane bounds and insets until then, rather than expanding the outgoing screen.
    val detailVisible = isLibraryDetailRoute(route) ||
        (route == Screen.Library.route && visibleEntries.any {
            isLibraryDetailRoute(it.destination.route)
        })
    val homeEntry = remember(entry, visibleEntries) {
        runCatching { navController.getBackStackEntry(Screen.Home.route) }.getOrNull()
            ?: visibleEntries.firstOrNull { it.destination.route == Screen.Home.route }
    }
    val homeDetailVisible = route == Screen.DailyMixScreen.route ||
        (route == Screen.Home.route && visibleEntries.any { it.destination.route == Screen.DailyMixScreen.route })
    val homeParent = route == Screen.DailyMixScreen.route &&
        navController.previousBackStackEntry?.destination?.route == Screen.Home.route
    // The expanded rail can leave less than the library's 720 dp breakpoint.
    // Daily Mix still fits beside its browser down to two 260 dp panes.
    val homePaneGeometry = calculateLibraryPaneLayout(layout.contentWidth, layout.hingeStart, layout.hingeEnd)
        ?: if (layout.contentWidth >= 520f && layout.hingeStart == null && layout.hingeEnd == null) {
            val width = layout.contentWidth / 2f
            LibraryPaneLayout(width, width, width)
        } else null
    val homePanes = homePaneGeometry
        ?.takeIf { layout.usesSideNavigation && homeDetailVisible && homeEntry != null &&
            (homeParent || route == Screen.Home.route) }
    val showHome = layout.usesSideNavigation && homeEntry != null &&
        (route == Screen.Home.route || homePanes != null)
    val panes = calculateLibraryPaneLayout(layout.contentWidth, layout.hingeStart, layout.hingeEnd)
        ?.takeIf { (libraryBranchActive || hasLibraryParent) && detailVisible && libraryEntry != null }
    // The persistent browser is only needed for the tablet list/detail arrangement.
    // On phones the NavHost owns it so it animates together with the selected tab.
    val showBrowser = layout.usesSideNavigation && libraryEntry != null &&
        (route == Screen.Library.route || panes != null)
    val stateHolder = rememberSaveableStateHolder()
    // Each pane contributes half the gutter, keeping their visible widths equal.
    val paneInnerInset = if (panes != null) 6.dp else 0.dp
    val activePanes = panes ?: homePanes
    val detailTopPadding = if (activePanes != null) libraryPaneTopPadding() else 0.dp
    val detailWidth = (panes?.detailWidth ?: layout.contentWidth) - paneInnerInset.value
    val hingeStart = layout.hingeStart
    val hingeEnd = layout.hingeEnd
    val fullBrowserWidth = if (hingeStart != null && hingeEnd != null && hingeEnd > hingeStart) {
        maxOf(hingeStart, layout.contentWidth - hingeEnd)
    } else layout.contentWidth
    val browserTargetWidth = ((panes?.listWidth ?: fullBrowserWidth) - paneInnerInset.value).coerceAtMost(1200f)

    Box(Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .clipToBounds()) {
        AnimatedVisibility(
            visible = showHome,
            enter = tabletTabEnterTransition(tabDirection, tabTravelPx),
            exit = if (!homeDetailVisible) tabletTabExitTransition(tabDirection, tabTravelPx) else ExitTransition.None,
        ) {
            if (homeEntry != null) {
                val animatedWidth by animateDpAsState(
                    targetValue = ((homePanes?.listWidth ?: layout.contentWidth) - if (homePanes != null) 6f else 0f).dp,
                    animationSpec = tween(durationMillis = LibraryPaneAnimationDuration, easing = FastOutSlowInEasing),
                    label = "homeBrowserWidth",
                )
                Box(Modifier.absoluteOffset(x = (homePanes?.listLeft ?: 0f).dp)
                    .width(animatedWidth).fillMaxHeight()) {
                    stateHolder.SaveableStateProvider("home-browser") {
                        CompositionLocalProvider(
                            LocalViewModelStoreOwner provides homeEntry,
                            LocalAdaptiveLayout provides layout.copy(
                                contentWidth = animatedWidth.value,
                                minimumContentWidth = if (homePanes != null) 0f else layout.minimumContentWidth,
                                hingeStart = if (homePanes == null) layout.hingeStart else null,
                                hingeEnd = if (homePanes == null) layout.hingeEnd else null,
                            ),
                        ) {
                            HomeScreen(navController = navController, paddingValuesParent = homePadding, playerViewModel = playerViewModel, onOpenSidebar = onOpenSidebar)
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = showBrowser,
            enter = if (layout.usesSideNavigation) tabletTabEnterTransition(tabDirection, tabTravelPx) else EnterTransition.None,
            exit = if (layout.usesSideNavigation && !detailVisible) tabletTabExitTransition(tabDirection, tabTravelPx) else ExitTransition.None,
        ) {
            if (libraryEntry != null) {
                val animatedBrowserWidth by animateDpAsState(
                    targetValue = ((panes?.listWidth ?: layout.contentWidth) - paneInnerInset.value).dp,
                    animationSpec = tween(durationMillis = LibraryPaneAnimationDuration, easing = FastOutSlowInEasing),
                    label = "libraryBrowserWidth",
                )
                val browserWidth = animatedBrowserWidth.value
                Box(Modifier
                    .absoluteOffset(x = (panes?.listLeft ?: 0f).dp)
                    .width(browserWidth.dp)
                    .fillMaxHeight()) {
                    stateHolder.SaveableStateProvider("library-browser") {
                        CompositionLocalProvider(
                            LocalViewModelStoreOwner provides libraryEntry,
                            LocalLibraryBrowserPane provides (panes != null),
                            LocalLibraryBrowserTargetWidth provides browserTargetWidth,
                            LocalAdaptiveLayout provides layout.copy(
                                contentWidth = browserWidth,
                                hingeStart = if (panes == null) layout.hingeStart else null,
                                hingeEnd = if (panes == null) layout.hingeEnd else null,
                            ),
                        ) {
                            AdaptiveScreenContent(1200f) {
                                LibraryScreen(
                                    navController = navController,
                                    playerViewModel = playerViewModel
                                )
                            }
                        }
                    }
                }
            }
        }
        val navigationModifier = if (activePanes == null) Modifier.fillMaxSize() else {
            Modifier
                .absoluteOffset(x = activePanes.detailLeft.dp + 6.dp)
                .width((activePanes.detailWidth - 6f).dp)
                .fillMaxHeight()
                .padding(top = detailTopPadding)
                .consumeWindowInsets(PaddingValues(top = detailTopPadding))
                .clip(LibraryPaneShape)
        }
        CompositionLocalProvider(
            LocalLibraryDetailPane provides (activePanes != null),
            LocalAdaptiveLayout provides if (activePanes == null) layout else layout.copy(
                contentWidth = activePanes.detailWidth - 6f,
                contentHeight = (layout.contentHeight - detailTopPadding.value).coerceAtLeast(0f),
                hingeStart = null,
                hingeEnd = null,
            )
        ) {
            content(navigationModifier)
        }
    }
}
