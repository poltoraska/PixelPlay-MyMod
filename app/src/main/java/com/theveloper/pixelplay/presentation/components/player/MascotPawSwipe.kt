package com.theveloper.pixelplay.presentation.components.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.theveloper.pixelplay.R
import kotlinx.coroutines.currentCoroutineContext

/** Decorative, non-interactive gesture; actual playback is never delayed by it. */
@Composable
internal fun MascotPawSwipe(
    requestId: Long,
    direction: Int,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    var visible by remember { mutableStateOf(false) }
    // A newly created cover (for example after rotation) must not replay an old request.
    var handledId by remember { mutableStateOf(requestId) }
    LaunchedEffect(requestId, active) {
        if (!active || requestId == 0L || requestId == handledId) {
            visible = false
            return@LaunchedEffect
        }
        handledId = requestId
        if (currentCoroutineContext()[MotionDurationScale]?.scaleFactor == 0f) {
            visible = false
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        visible = true
        try {
            progress.animateTo(1f, tween(durationMillis = 650, easing = LinearEasing))
        } finally {
            visible = false
        }
    }
    if (!visible || !active) return
    BoxWithConstraints(modifier.clipToBounds(), contentAlignment = Alignment.TopCenter) {
        val areaWidth = maxWidth
        val pawWidth = minOf(areaWidth * 0.23f, 100.dp)
        Image(
            painter = painterResource(R.drawable.mascot_paw),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.width(pawWidth).aspectRatio(361f / 723f).graphicsLayer {
                val phase = progress.value
                val enter = (phase / 0.25f).coerceIn(0f, 1f)
                val exit = ((phase - 0.72f) / 0.28f).coerceIn(0f, 1f)
                val contact = enter * (1f - exit)
                val sweep = ((phase - 0.20f) / 0.52f).coerceIn(0f, 1f)
                translationY = -size.height * (1f - contact)
                translationX = direction * areaWidth.toPx() * (-0.12f + 0.28f * sweep)
                rotationZ = direction * (-7f + 12f * sweep)
            },
        )
    }
}
