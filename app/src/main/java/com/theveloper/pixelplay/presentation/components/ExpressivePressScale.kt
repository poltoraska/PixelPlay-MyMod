package com.theveloper.pixelplay.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun rememberExpressivePressScale(source: MutableInteractionSource, pressedScale: Float = 0.92f): State<Float> {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(source) {
        var pressedAt = 0L
        var releaseJob: kotlinx.coroutines.Job? = null
        source.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    releaseJob?.cancel()
                    pressedAt = android.os.SystemClock.uptimeMillis()
                    scale.snapTo(pressedScale)
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    val remaining = (70L - (android.os.SystemClock.uptimeMillis() - pressedAt)).coerceAtLeast(0L)
                    releaseJob = launch {
                        delay(remaining)
                        scale.animateTo(1f, spring(dampingRatio = 0.48f, stiffness = 450f))
                    }
                }
            }
        }
    }
    return scale.asState()
}
