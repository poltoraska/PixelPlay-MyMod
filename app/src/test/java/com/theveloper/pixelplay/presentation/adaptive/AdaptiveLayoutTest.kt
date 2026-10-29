package com.theveloper.pixelplay.presentation.adaptive

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AdaptiveLayoutTest {
    @Test
    fun `cassette player is limited to landscape phones even when their rotated width exceeds tablet breakpoint`() {
        assertTrue(usesLandscapeCassettePlayer(412, landscape = true, tabletop = false))
        assertTrue(usesLandscapeCassettePlayer(599, landscape = true, tabletop = false))
        assertFalse(usesLandscapeCassettePlayer(600, landscape = true, tabletop = false))
        assertFalse(usesLandscapeCassettePlayer(851, landscape = true, tabletop = false))
        assertFalse(usesLandscapeCassettePlayer(412, landscape = false, tabletop = false))
        assertFalse(usesLandscapeCassettePlayer(412, landscape = true, tabletop = true))
        assertFalse(usesLandscapeCassettePlayer(0, landscape = true, tabletop = false))
    }

    @Test
    fun `home columns and collage layout stay stable throughout rail expansion and collapse`() {
        for ((width, expectedColumns) in listOf(600f to 1, 800f to 1, 851f to 2, 1024f to 2, 1280f to 2)) {
            val widths = (80..256 step 4).map(Int::toFloat) + listOf(256f, 80f)
            val columns = (widths + widths.reversed()).map { railWidth ->
                val layout = calculateAdaptiveLayout(width, 900f, railExpanded = true, railWidth = railWidth)
                assertEquals(width - 256f, layout.minimumContentWidth)
                homeGridColumnCount(layout.usesSideNavigation, layout.minimumContentWidth)
            }
            assertTrue(columns.all { it == expectedColumns }, "window width=$width")
        }
    }

    @Test
    fun `home breakpoint respects the usable fold pane rather than counting the physical gap`() {
        val hinge = SeparatingHinge(660f, 680f, vertical = true)
        for (railWidth in listOf(80f, 160f, 256f)) {
            val layout = calculateAdaptiveLayout(1280f, 900f, hinge, railExpanded = true, railWidth = railWidth)
            assertEquals(600f, layout.minimumContentWidth)
            assertEquals(2, homeGridColumnCount(true, layout.minimumContentWidth))
        }
        assertEquals(1, homeGridColumnCount(false, 1000f))
        assertEquals(1, homeGridColumnCount(true, Float.NaN))
    }

    @Test
    fun `cassette cells fit pixel bounds and stay similar across the tablet pane transition`() {
        for (width in listOf(600, 771, 772, 1041, 1200)) {
            val cell = albumCassetteCellWidth(width, 28, 2, 12)
            assertTrue(cell * 2 + 12 + 28 <= width)
            val splitCell = albumCassetteCellWidth(width / 2 - 6, 28, 1, 12)
            assertTrue(kotlin.math.abs(cell - splitCell) <= 15)
        }
        assertEquals(1, albumCassetteCellWidth(20, 28, 1, 12))
    }

    @Test
    fun `album cassette lists use two columns only in the full tablet browser`() {
        assertEquals(1, albumCassetteColumnCount(false, false, 412f))
        assertEquals(1, albumCassetteColumnCount(true, false, 520f))
        assertEquals(2, albumCassetteColumnCount(true, false, 600f))
        assertEquals(2, albumCassetteColumnCount(true, false, 1200f))
        assertEquals(1, albumCassetteColumnCount(true, true, 1200f))
        assertEquals(1, albumCassetteColumnCount(true, true, 380f))
        assertEquals(1, albumCassetteColumnCount(true, false, 480f))
        assertEquals(1, albumCassetteColumnCount(true, false, Float.NaN))
    }

    @Test
    fun `phone and folded cover display retain bottom navigation`() {
        val layout = calculateAdaptiveLayout(412f, 915f)
        assertEquals(NavigationLayout.BottomBar, layout.navigation)
        assertEquals(412f, layout.contentWidth)
        assertEquals(0f, layout.contentLeft)
        assertFalse(layout.usesHorizontalPlayer)
    }

    @Test
    fun `medium window switches at 600 dp without covering content`() {
        assertEquals(NavigationLayout.BottomBar, calculateAdaptiveLayout(599f, 800f).navigation)
        val layout = calculateAdaptiveLayout(600f, 800f)
        assertEquals(NavigationLayout.Rail, layout.navigation)
        assertEquals(80f, layout.contentLeft)
        assertEquals(520f, layout.contentWidth)
    }

    @Test
    fun `expanded rail resizes content alongside it on fold and tablet`() {
        for (width in listOf(600f, 851f, 1024f, 1280f)) {
            val collapsed = calculateAdaptiveLayout(width, 800f)
            val expanded = calculateAdaptiveLayout(width, 800f, railExpanded = true)
            assertEquals(NavigationLayout.Rail, collapsed.navigation)
            assertEquals(NavigationLayout.Sidebar, expanded.navigation)
            assertEquals(80f, collapsed.navigationWidth)
            assertEquals(256f, expanded.navigationWidth)
            assertEquals(176f, collapsed.contentWidth - expanded.contentWidth)
            assertEquals(width, expanded.contentLeft + expanded.contentWidth)
        }
    }

    @Test
    fun `content stays attached to the rail throughout its width animation`() {
        for (railWidth in listOf(80f, 104f, 160f, 224f, 256f)) {
            val layout = calculateAdaptiveLayout(851f, 883f, railExpanded = true, railWidth = railWidth)
            assertEquals(railWidth, layout.navigationWidth)
            assertEquals(railWidth, layout.contentLeft)
            assertEquals(851f, layout.contentLeft + layout.contentWidth)
        }
    }

    @Test
    fun `unfolded portrait can use horizontal player without relying on orientation`() {
        assertTrue(calculateAdaptiveLayout(900f, 1200f).usesHorizontalPlayer)
    }

    @Test
    fun `short landscape phone has horizontal player`() {
        val layout = calculateAdaptiveLayout(550f, 360f)
        assertEquals(NavigationLayout.BottomBar, layout.navigation)
        assertTrue(layout.usesHorizontalPlayer)
    }

    @Test
    fun `book hinge never inflates the rail to half the screen`() {
        for (expanded in listOf(false, true)) {
            val layout =
                calculateAdaptiveLayout(1100f, 800f, SeparatingHinge(540f, 560f, true), expanded)
            assertTrue(layout.navigationWidth <= 256f)
            assertEquals(1100f, layout.contentLeft + layout.contentWidth)
            assertEquals(540f, layout.contentLeft + layout.hingeStart!!)
            assertEquals(560f, layout.contentLeft + layout.hingeEnd!!)
        }
    }

    @Test
    fun `library panes share the available width equally without an artificial gap`() {
        assertNull(calculateLibraryPaneLayout(719f))
        for (width in listOf(720f, 755f, 1040f, 1600f)) {
            val panes = calculateLibraryPaneLayout(width)!!
            assertEquals(width / 2f, panes.listWidth)
            assertEquals(panes.listWidth, panes.detailWidth)
            assertEquals(panes.listWidth, panes.detailLeft)
            assertEquals(0f, panes.listLeft)
            assertTrue(panes.detailWidth >= 360f)
            assertEquals(width, panes.detailLeft + panes.detailWidth, 0.001f)
        }
    }

    @Test
    fun `library panes avoid physical hinge and zero width fold`() {
        val crease = calculateLibraryPaneLayout(1000f, 430f, 430f)!!
        assertEquals(500f, crease.listWidth)
        assertEquals(500f, crease.detailLeft)
        for ((start, end) in listOf(430f to 450f, 570f to 590f)) {
            val panes = calculateLibraryPaneLayout(1000f, start, end)!!
            assertEquals(panes.listWidth, panes.detailWidth)
            assertEquals(start, panes.listLeft + panes.listWidth)
            assertEquals(end, panes.detailLeft)
            assertTrue(panes.detailLeft + panes.detailWidth <= 1000f)
        }
        assertNull(calculateLibraryPaneLayout(1000f, 200f, 220f))
        assertNull(calculateLibraryPaneLayout(1000f, 800f, 820f))
    }

    @Test
    fun `only album artist playlist and genre details qualify for library panes`() {
        for (route in listOf(
            "album_detail/{albumId}",
            "artist_detail/{artistId}",
            "playlist_detail/{playlistId}",
            "genre_detail/{genreId}",
            "album_detail/42"
        )) {
            assertTrue(isLibraryDetailRoute(route))
        }
        for (route in listOf(null, "home", "library", "settings", "album_details", "daily_mix")) {
            assertFalse(isLibraryDetailRoute(route))
        }
    }

    @Test
    fun `tabletop preserves the full app viewport when upper half is larger`() {
        val layout = calculateAdaptiveLayout(900f, 900f, SeparatingHinge(500f, 520f, false, isHalfOpened = true))
        assertEquals(0f, layout.contentTop)
        assertEquals(900f, layout.contentHeight)
        assertEquals(820f, layout.contentWidth)
        assertTrue(layout.isTabletop)
        assertEquals(500f, layout.horizontalFoldStart)
        assertEquals(520f, layout.horizontalFoldEnd)
    }

    @Test
    fun `tabletop preserves the full app viewport when lower half is larger`() {
        val layout = calculateAdaptiveLayout(900f, 900f, SeparatingHinge(300f, 320f, false, isHalfOpened = true))
        assertEquals(0f, layout.contentTop)
        assertEquals(900f, layout.contentHeight)
        assertEquals(80f, layout.navigationWidth)
    }

    @Test
    fun `flat and book postures do not activate the top bottom player`() {
        assertFalse(calculateAdaptiveLayout(900f, 900f).isTabletop)
        assertFalse(calculateAdaptiveLayout(900f, 900f, SeparatingHinge(440f, 460f, false)).isTabletop)
        assertFalse(calculateAdaptiveLayout(900f, 900f, SeparatingHinge(440f, 460f, true, isHalfOpened = true)).isTabletop)
    }

    @Test
    fun `a non occluding book fold leaves the app layout unchanged`() {
        val folded = calculateAdaptiveLayout(
            900f, 900f,
            SeparatingHinge(440f, 460f, true, isHalfOpened = true, occludes = false),
        )
        assertEquals(calculateAdaptiveLayout(900f, 900f), folded)
    }

    @Test
    fun `player panes account for status bar and sheet inset without crossing the hinge`() {
        val panes = calculateTabletopPlayerLayout(860f, 40f, 440f, 460f)!!
        assertEquals(400f, panes.artworkHeight)
        assertEquals(420f, panes.controlsTop)
        assertEquals(440f, panes.controlsHeight)
        assertEquals(440f, 40f + panes.artworkHeight)
        assertEquals(460f, 40f + panes.controlsTop)
        assertEquals(900f, 40f + panes.controlsTop + panes.controlsHeight)
    }

    @Test
    fun `zero width fold keeps both player halves and all available height`() {
        val panes = calculateTabletopPlayerLayout(864f, 36f, 450f, 450f)!!
        assertEquals(panes.artworkHeight, panes.controlsTop)
        assertEquals(864f, panes.artworkHeight + panes.controlsHeight)
    }

    @Test
    fun `player ignores folds outside its viewport and invalid bounds`() {
        assertNull(calculateTabletopPlayerLayout(860f, 40f, null, null))
        assertNull(calculateTabletopPlayerLayout(860f, 40f, 20f, 20f))
        assertNull(calculateTabletopPlayerLayout(860f, 40f, 900f, 920f))
        assertNull(calculateTabletopPlayerLayout(860f, 40f, 500f, 490f))
        assertNull(calculateTabletopPlayerLayout(Float.NaN, 40f, 440f, 460f))
    }

    @Test
    fun `resizing between cover screen and tablet always fits viewport`() {
        for (width in listOf(360f, 599f, 600f, 839f, 840f, 1024f, 1600f)) {
            val layout = calculateAdaptiveLayout(width, 900f)
            assertEquals(width, layout.contentLeft + layout.contentWidth)
            assertTrue(layout.contentWidth > 0f)
        }
    }
}
