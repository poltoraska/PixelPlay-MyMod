package com.theveloper.pixelplay.presentation.adaptive

import android.app.Activity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo

/** A rotated phone can be wider than 600 dp; that alone must not select the tablet player. */
internal fun usesLandscapeCassettePlayer(
    smallestScreenWidthDp: Int,
    landscape: Boolean,
    tabletop: Boolean,
): Boolean = smallestScreenWidthDp in 1 until 600 && landscape && !tabletop

internal enum class NavigationLayout { BottomBar, Rail, Sidebar }

@Immutable
internal data class AdaptiveLayoutInfo(
    val navigation: NavigationLayout = NavigationLayout.BottomBar,
    val navigationWidth: Float = 0f,
    val navigationLeft: Float = 0f,
    val contentLeft: Float = 0f,
    val contentTop: Float = 0f,
    val contentWidth: Float = 0f,
    val contentHeight: Float = 0f,
    // Conservative width for layout breakpoints; it stays fixed while the rail animates.
    val minimumContentWidth: Float = contentWidth,
    val hingeStart: Float? = null,
    val hingeEnd: Float? = null,
    val horizontalFoldStart: Float? = null,
    val horizontalFoldEnd: Float? = null,
) {
    val usesSideNavigation: Boolean get() = navigation != NavigationLayout.BottomBar
    val usesTwoColumns: Boolean get() = contentWidth >= 600f
    val usesHorizontalPlayer: Boolean get() = contentWidth >= 600f || (contentWidth >= 480f && contentHeight < 480f)
    val isTabletop: Boolean get() = horizontalFoldStart != null && horizontalFoldEnd != null
}

// Width changes every animation frame. Track actual readers instead of invalidating the whole app.
internal val LocalAdaptiveLayout = compositionLocalOf { AdaptiveLayoutInfo() }

internal fun homeGridColumnCount(sideNavigation: Boolean, minimumContentWidth: Float): Int =
    if (sideNavigation && minimumContentWidth.isFinite() && minimumContentWidth >= 560f) 2 else 1

@Immutable
internal data class SeparatingHinge(
    val start: Float,
    val end: Float,
    val vertical: Boolean,
    val isHalfOpened: Boolean = false,
    val occludes: Boolean = true,
)

/** The rail belongs to the window edge, never to an entire fold pane. */
internal fun calculateAdaptiveLayout(
    width: Float,
    height: Float,
    hinge: SeparatingHinge? = null,
    railExpanded: Boolean = false,
    railWidth: Float? = null,
): AdaptiveLayoutInfo {
    val safeWidth = width.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    val safeHeight = height.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    // A bent display is still one full window. Only the player opts into tabletop panes.
    val horizontalFold = hinge?.takeIf {
        !it.vertical && it.isHalfOpened && it.start > 0f && it.end >= it.start && it.end < safeHeight
    }
    val hasRail = safeWidth >= 600f
    val expanded = hasRail && railExpanded
    val navigationWidth = if (!hasRail) 0f else {
        railWidth?.takeIf { it.isFinite() }?.coerceIn(80f, 256f) ?: if (expanded) 256f else 80f
    }
    val contentLeft = navigationWidth
    val verticalHinge = hinge?.takeIf {
        it.vertical && it.occludes && it.start >= contentLeft && it.end >= it.start && it.end < safeWidth
    }
    val expandedContentLeft = if (hasRail) 256f else 0f
    val expandedContentWidth = (safeWidth - expandedContentLeft).coerceAtLeast(0f)
    val minimumContentWidth = hinge?.takeIf {
        it.vertical && it.occludes && it.start >= expandedContentLeft &&
            it.end > it.start && it.end < safeWidth
    }?.let {
        maxOf(it.start - expandedContentLeft, safeWidth - it.end)
    } ?: expandedContentWidth
    return AdaptiveLayoutInfo(
        navigation = if (!hasRail) NavigationLayout.BottomBar else if (expanded) NavigationLayout.Sidebar else NavigationLayout.Rail,
        navigationWidth = navigationWidth,
        contentLeft = contentLeft,
        contentTop = 0f,
        contentWidth = (safeWidth - contentLeft).coerceAtLeast(0f),
        contentHeight = safeHeight,
        minimumContentWidth = minimumContentWidth,
        hingeStart = verticalHinge?.start?.minus(contentLeft),
        hingeEnd = verticalHinge?.end?.minus(contentLeft),
        horizontalFoldStart = horizontalFold?.start,
        horizontalFoldEnd = horizontalFold?.end,
    )
}

internal data class TabletopPlayerLayout(
    val artworkHeight: Float,
    val controlsTop: Float,
    val controlsHeight: Float,
)

/** Translate window-coordinate fold bounds into the inset player viewport. */
internal fun calculateTabletopPlayerLayout(
    viewportHeight: Float,
    viewportTop: Float,
    foldStart: Float?,
    foldEnd: Float?,
): TabletopPlayerLayout? {
    if (foldStart == null || foldEnd == null ||
        !listOf(viewportHeight, viewportTop, foldStart, foldEnd).all { it.isFinite() } ||
        viewportHeight <= 0f || foldEnd < foldStart
    ) return null
    val artworkHeight = foldStart - viewportTop
    val controlsTop = foldEnd - viewportTop
    if (artworkHeight <= 0f || controlsTop >= viewportHeight) return null
    return TabletopPlayerLayout(artworkHeight, controlsTop, viewportHeight - controlsTop)
}

@Composable
internal fun rememberAdaptiveLayout(
    activity: Activity, width: Float, height: Float, railExpanded: Boolean,
): AdaptiveLayoutInfo {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val layoutInfo by remember(activity) {
        WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity)
    }.collectAsStateWithLifecycle(initialValue = WindowLayoutInfo(emptyList()))
    val fold = layoutInfo.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull {
        it.isSeparating || it.occlusionType == FoldingFeature.OcclusionType.FULL
    }
    val hinge = fold?.let {
        val vertical = it.orientation == FoldingFeature.Orientation.VERTICAL
        SeparatingHinge(
            (if (vertical) it.bounds.left else it.bounds.top) / density.density,
            (if (vertical) it.bounds.right else it.bounds.bottom) / density.density,
            vertical,
            isHalfOpened = it.state == FoldingFeature.State.HALF_OPENED,
            occludes = it.occlusionType == FoldingFeature.OcclusionType.FULL,
        )
    }
    val railWidth by animateFloatAsState(
        targetValue = if (railExpanded) 256f else 80f,
        // Layout width must settle monotonically; overshoot causes repeated remeasure/reflow.
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "navigationRailWidth",
    )
    return calculateAdaptiveLayout(width, height, hinge, railExpanded, railWidth)
}

@Composable
internal fun AdaptiveAppLayout(
    layout: AdaptiveLayoutInfo,
    navigationVisibility: Float = 1f,
    navigation: @Composable (expanded: Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .absoluteOffset(layout.contentLeft.dp, layout.contentTop.dp)
                .width(layout.contentWidth.dp)
                .height(layout.contentHeight.dp)
        ) { content() }
        if (layout.usesSideNavigation && navigationVisibility > 0f) {
            Box(
                Modifier
                    .absoluteOffset(x = layout.navigationLeft.dp, y = layout.contentTop.dp)
                    .width(layout.navigationWidth.dp)
                    .height(layout.contentHeight.dp)
                    .graphicsLayer {
                        alpha = navigationVisibility
                        translationX = -size.width * (1f - navigationVisibility)
                    }
                    .background(MaterialTheme.colorScheme.surface)
            ) { navigation(layout.navigation == NavigationLayout.Sidebar) }
        }
    }
}

@Composable
internal fun AdaptiveScreenContent(maxWidth: Float, content: @Composable () -> Unit) {
    val layout = LocalAdaptiveLayout.current
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = androidx.compose.ui.Alignment.TopCenter,
    ) {
        // A physical gap may occlude a single-pane destination. Library panes handle it separately.
        val start = layout.hingeStart
        val end = layout.hingeEnd
        val hasGap = start != null && end != null && end > start
        val useRight = hasGap && this.maxWidth.value - end!! > start!!
        val paneLeft = if (useRight) end!! else 0f
        val availableWidth =
            if (!hasGap) this.maxWidth.value else if (useRight) this.maxWidth.value - end!! else start!!
        val width = minOf(availableWidth, maxWidth)
        val availableHeight = maxHeight
        Box(Modifier
            .fillMaxSize()
            .padding(start = paneLeft.dp)) {
            Box(
                Modifier
                    .width(availableWidth.dp)
                    .fillMaxHeight(),
                contentAlignment = androidx.compose.ui.Alignment.TopCenter
            ) {
                CompositionLocalProvider(
                    LocalAdaptiveLayout provides layout.copy(
                        contentWidth = width,
                        minimumContentWidth = minOf(width, layout.minimumContentWidth),
                        contentHeight = availableHeight.value,
                        hingeStart = null,
                        hingeEnd = null,
                    )
                ) {
                    Box(Modifier
                        .width(width.dp)
                        .height(availableHeight)) { content() }
                }
            }
        }
    }
}
