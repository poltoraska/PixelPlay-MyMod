package com.theveloper.pixelplay.presentation.components.scoped

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.NavHostController
import com.theveloper.pixelplay.presentation.adaptive.LocalAdaptiveLayout
import com.theveloper.pixelplay.presentation.navigation.Screen
import com.theveloper.pixelplay.presentation.navigation.navigateLibraryDetailSafely
import com.theveloper.pixelplay.presentation.navigation.navigateSafelyReplacing
import com.theveloper.pixelplay.presentation.navigation.navigateToTopLevelSafely
import com.theveloper.pixelplay.presentation.viewmodel.PlayerViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
internal fun PlayerAlbumNavigationEffect(
    navController: NavHostController,
    sheetCollapsedTargetY: Float,
    sheetMotionController: SheetMotionController,
    playerViewModel: PlayerViewModel
) {
    val latestSheetCollapsedTargetY by rememberUpdatedState(sheetCollapsedTargetY)
    val usesSideNavigation by rememberUpdatedState(LocalAdaptiveLayout.current.usesSideNavigation)
    LaunchedEffect(navController) {
        playerViewModel.albumNavigationRequests.collectLatest { albumId ->
            sheetMotionController.snapCollapsed(latestSheetCollapsedTargetY)
            playerViewModel.collapsePlayerSheet()

            if (usesSideNavigation) {
                // Restore the library root first, including when its saved tab contains a detail.
                if (navController.navigateToTopLevelSafely(Screen.Library.route)) {
                    navController.navigateLibraryDetailSafely(
                        route = Screen.AlbumDetail.createRoute(albumId),
                        patternToPop = Screen.AlbumDetail.route
                    )
                }
            } else {
                navController.navigateSafelyReplacing(
                    route = Screen.AlbumDetail.createRoute(albumId),
                    patternToPop = Screen.AlbumDetail.route
                )
            }
        }
    }
}
