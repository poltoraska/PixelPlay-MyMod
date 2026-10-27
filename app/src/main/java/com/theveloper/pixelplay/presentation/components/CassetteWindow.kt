package com.theveloper.pixelplay.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin

/** Shared reels read their rotation only while drawing, without recomposing their controls. */
@Composable
internal fun CassetteWindow(
    modifier: Modifier,
    container: Color,
    reel: Color,
    hub: Color,
    isPlaying: Boolean,
    isSeeking: Boolean = false,
    seekDirection: Int = 0,
) {
    val rotation = remember { Animatable(0f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val direction = if (isSeeking) seekDirection else if (isPlaying) 1 else 0
    LaunchedEffect(direction, isSeeking, lifecycle) {
        if (direction == 0) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                if (currentCoroutineContext()[MotionDurationScale]?.scaleFactor == 0f) awaitCancellation()
                val duration = if (isSeeking) 1100 else 6000
                rotation.animateTo(rotation.value + direction * 360f, tween(duration, easing = LinearEasing))
                rotation.snapTo(((rotation.value % 360f) + 360f) % 360f)
                // Yield even if system animation scale changes to zero during a turn.
                withFrameNanos { }
            }
        }
    }
    // Read the angle only during drawing: playback does not recompose the card each frame.
    Canvas(modifier) {
        drawRoundRect(container, cornerRadius = CornerRadius(size.height / 2f))
        val radius = minOf(size.height * 0.34f, size.width * 0.18f)
        val centers = listOf(Offset(size.width * 0.26f, size.height / 2f), Offset(size.width * 0.74f, size.height / 2f))
        drawLine(reel.copy(alpha = 0.35f), centers[0], centers[1], strokeWidth = 3.dp.toPx())
        centers.forEach { center ->
            drawCircle(hub, radius, center)
            drawCircle(reel, radius, center, style = Stroke(3.dp.toPx()))
            repeat(6) { tooth ->
                val angle = tooth * Math.PI / 3.0 + Math.toRadians(rotation.value.toDouble())
                val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                drawLine(reel, center + direction * (radius * 0.55f), center + direction * (radius * 0.94f), 3.dp.toPx())
            }
            drawCircle(reel, radius * 0.2f, center)
        }
    }
}
