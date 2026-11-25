package com.theveloper.pixelplay.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import coil.size.Size
import com.theveloper.pixelplay.data.model.Song
import kotlin.math.roundToInt

@Stable
internal class PlayerArtworkTransition(
    val expansion: () -> Float,
    val sheetY: () -> Float,
    val canCaptureSource: () -> Boolean,
) {
    var source by mutableStateOf<Rect?>(null)
    var target by mutableStateOf<Rect?>(null)
    val active: Boolean get() = source != null && target != null && expansion() > 0.001f && expansion() < 0.999f
}
internal val LocalPlayerArtworkTransition = staticCompositionLocalOf<PlayerArtworkTransition?> { null }

@Composable
internal fun Modifier.playerArtworkSource(): Modifier {
    val state = LocalPlayerArtworkTransition.current ?: return this
    return this.onGloballyPositioned { coordinates ->
        if (state.canCaptureSource()) {
            state.source = Rect(coordinates.localToRoot(Offset.Zero),
                coordinates.localToRoot(Offset(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())))
        }
    }.graphicsLayer { if (state.active) alpha = 0f }
}

@Composable
internal fun Modifier.playerArtworkTarget(): Modifier {
    val state = LocalPlayerArtworkTransition.current ?: return this
    return this.onGloballyPositioned { coordinates ->
        // The full player is kept offscreen while collapsed. Measure only once it is laid out
        // at its real position, removing the sheet translation to obtain the expanded endpoint.
        if (state.expansion() > 0.01f) {
            val position = coordinates.localToRoot(Offset.Zero) - Offset(0f, state.sheetY())
            val bottomRight = coordinates.localToRoot(Offset(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())) - Offset(0f, state.sheetY())
            state.target = Rect(position, bottomRight)
        }
    }.graphicsLayer { if (state.active) alpha = 0f }
}

/** A static cover travels between measured endpoints; video remains owned by the full player. */
@Composable
internal fun PlayerArtworkTransitionOverlay(state: PlayerArtworkTransition, song: Song) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    val active by remember(state) { derivedStateOf { state.active } }
    Box(Modifier.fillMaxSize().onGloballyPositioned { origin = it.localToRoot(Offset.Zero) }) {
        if (active) Box(Modifier.fillMaxSize()
            .layout { measurable, constraints ->
                val bounds = interpolatePlayerArtworkBounds(state.source!!, state.target!!, state.expansion())
                val width = bounds.width.roundToInt().coerceAtLeast(1)
                val height = bounds.height.roundToInt().coerceAtLeast(1)
                val placeable = measurable.measure(Constraints.fixed(width, height))
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.place((bounds.left - origin.x).roundToInt(), (bounds.top - origin.y).roundToInt())
                }
            }) {
            SmartImage(model = song.albumArtUriString, contentDescription = null, targetSize = Size(800, 800),
                crossfadeDurationMillis = 0, modifier = Modifier.fillMaxSize().graphicsLayer {
                    val fraction = state.expansion().coerceIn(0f, 1f)
                    shape = RoundedCornerShape((22f + (18f - 22f) * fraction).dp)
                    clip = true
                })
        }
    }
}

internal fun interpolatePlayerArtworkBounds(from: Rect, to: Rect, fraction: Float): Rect =
    androidx.compose.ui.geometry.lerp(from, to, if (fraction.isFinite()) fraction.coerceIn(0f, 1f) else 0f)
