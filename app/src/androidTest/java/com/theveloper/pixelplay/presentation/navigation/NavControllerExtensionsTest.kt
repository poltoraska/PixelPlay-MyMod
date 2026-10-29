package com.theveloper.pixelplay.presentation.navigation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.navigation.*
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavControllerExtensionsTest {
    @Test
    fun homeButtonRestoresHomeInsteadOfItsSavedSettingsChild() = onMain {
        for (start in listOf(Screen.Home.route, Screen.Search.route, Screen.Library.route)) {
            val controller = controller(start)
            assertTrue(controller.navigateToTopLevelSafely(Screen.Home.route))
            assertTrue(controller.navigateSafely(Screen.Settings.route))
            assertTrue(controller.navigateToTopLevelSafely(Screen.Library.route))
            assertTrue(controller.navigateToTopLevelSafely(Screen.Home.route))
            assertEquals(Screen.Home.route, controller.currentDestination?.route)
            assertTrue(controller.navigateToTopLevelSafely(Screen.Home.route))
            assertEquals(Screen.Home.route, controller.currentDestination?.route)
        }
    }

    @Test
    fun selectingOtherEntitiesReplacesDetailAndBackReturnsToLibrary() = onMain {
        val controller = controller(Screen.Library.route)
        val selections = listOf(
            Screen.AlbumDetail.createRoute(1) to Screen.AlbumDetail.route,
            Screen.AlbumDetail.createRoute(2) to Screen.AlbumDetail.route,
            Screen.ArtistDetail.createRoute(7) to Screen.ArtistDetail.route,
            Screen.PlaylistDetail.createRoute("test") to Screen.PlaylistDetail.route,
            Screen.GenreDetail.createRoute("rock") to Screen.GenreDetail.route,
        )
        for ((route, pattern) in selections) {
            assertTrue(controller.navigateLibraryDetailSafely(route, pattern))
            assertEquals(pattern, controller.currentDestination?.route)
            assertEquals(
                Screen.Library.route,
                controller.previousBackStackEntry?.destination?.route
            )
        }
        assertTrue(controller.popBackStack())
        assertEquals(Screen.Library.route, controller.currentDestination?.route)
    }

    @Test
    fun settingsIsAFourthDestinationAndDoesNotRedirectOtherTabs() = onMain {
        val controller = controller(Screen.Home.route)
        repeat(3) {
            for (screen in listOf(Screen.Settings, Screen.Library, Screen.Settings, Screen.Search, Screen.Home)) {
                assertTrue(controller.navigateToTopLevelSafely(screen.route))
                assertEquals(screen.route, controller.currentDestination?.route)
            }
        }
        assertTrue(controller.navigateToTopLevelSafely(Screen.Settings.route))
        assertTrue(controller.navigateToTopLevelSafely(Screen.Settings.route))
        assertEquals(Screen.Home.route, controller.previousBackStackEntry?.destination?.route)
    }

    private fun onMain(block: () -> Unit) =
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)

    private fun controller(start: String): NavHostController {
        val controller = NavHostController(ApplicationProvider.getApplicationContext())
        val owner = object : LifecycleOwner {
            override val lifecycle =
                LifecycleRegistry(this).apply { currentState = Lifecycle.State.RESUMED }
        }
        controller.setLifecycleOwner(owner)
        controller.setViewModelStore(ViewModelStore())
        val navigator = ImmediateNavigator()
        controller.navigatorProvider.addNavigator(navigator)
        controller.graph = controller.createGraph(startDestination = start) {
            for (screen in listOf(
                Screen.Home, Screen.Search, Screen.Library, Screen.Settings,
                Screen.AlbumDetail, Screen.ArtistDetail, Screen.PlaylistDetail, Screen.GenreDetail
            )) {
                addDestination(navigator.createDestination().apply { route = screen.route })
            }
        }
        return controller
    }

    @Navigator.Name("immediate")
    private class ImmediateNavigator : Navigator<NavDestination>() {
        override fun createDestination() = NavDestination(this)
        override fun navigate(
            entries: List<NavBackStackEntry>,
            navOptions: NavOptions?,
            navigatorExtras: Extras?
        ) {
            entries.forEach { state.push(it) }
        }

        override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
            state.pop(popUpTo, savedState)
        }
    }
}
