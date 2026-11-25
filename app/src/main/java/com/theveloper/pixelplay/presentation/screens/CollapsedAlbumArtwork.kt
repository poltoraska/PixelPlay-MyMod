package com.theveloper.pixelplay.presentation.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import coil.size.Size
import com.theveloper.pixelplay.data.model.Album
import com.theveloper.pixelplay.presentation.components.SmartImage

/** One feedback event per settled endpoint, never on every scroll update or cancelled spring. */
@Composable
internal fun rememberCollapsedAlbumArtworkProgress(collapseFraction: Float): Float {
    val haptic = LocalHapticFeedback.current
    val latestCollapse by rememberUpdatedState(collapseFraction)
    var settledCompact by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = ((collapseFraction - 0.65f) / 0.35f).coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 180f, visibilityThreshold = 0.001f),
        label = "collapsedAlbumArtwork",
        finishedListener = { value ->
            if (value >= 0.999f && latestCollapse >= 0.995f && !settledCompact) {
                settledCompact = true
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } else if (value <= 0.001f && latestCollapse <= 0.01f && settledCompact) {
                settledCompact = false
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    )
    // The spring may settle at zero before the scroll reaches the fully expanded header.
    // Reset at that scroll endpoint too, so the next collapse can produce feedback again.
    LaunchedEffect(collapseFraction <= 0.01f) {
        if (collapseFraction <= 0.01f && progress <= 0.001f && settledCompact) {
            settledCompact = false
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    return progress
}

@Composable
internal fun CollapsedAlbumArtwork(album: Album, progress: Float) {
    Box(Modifier.padding(end = 16.dp).size(44.dp).graphicsLayer {
        alpha = progress.coerceIn(0f, 1f)
        scaleX = 0.78f + 0.22f * progress
        scaleY = scaleX
        translationX = 12.dp.toPx() * (1f - progress)
        translationY = 8.dp.toPx() * (1f - progress)
    }.clip(RoundedCornerShape(14.dp))) {
        // Deliberately static: no cassette layer, video player or motion artwork lookup.
        SmartImage(model = album.albumArtUriString, contentDescription = null,
            targetSize = Size(128, 128), modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(14.dp))
    }
}
