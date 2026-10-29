package com.theveloper.pixelplay.presentation.components.player

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/** Shared geometry keeps the player and its queue on the same animation frame. */
@Immutable
internal data class TabletPlayerQueuePane(
    val width: Float = 0f,
    val openFraction: Float = 0f,
    val viewportTop: Float = 0f,
) {
    val enabled: Boolean get() = width > 0f
    val visibleWidth: Float get() = width * openFraction.coerceIn(0f, 1f)
}

internal val LocalTabletPlayerQueuePane = staticCompositionLocalOf { TabletPlayerQueuePane() }

internal fun tabletQueuePaneWidth(contentWidth: Float): Float =
    if (!contentWidth.isFinite() || contentWidth <= 0f) 0f
    else (contentWidth * 0.4f).coerceIn(240f, 400f).coerceAtMost(contentWidth * 0.48f)
