package com.theveloper.pixelplay.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.theveloper.pixelplay.R
import kotlinx.coroutines.currentCoroutineContext
import kotlin.math.PI
import kotlin.math.sin

@Composable
internal fun HomeMusicMascot(isPlaying: Boolean) {
    val context = LocalContext.current
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    var elapsed by remember { mutableLongStateOf(0L) }
    var animationsEnabled by remember { mutableStateOf(true) }
    LaunchedEffect(isPlaying, lifecycle) {
        if (!isPlaying || !lifecycle.isAtLeast(Lifecycle.State.STARTED)) return@LaunchedEffect
        animationsEnabled = currentCoroutineContext()[MotionDurationScale]?.scaleFactor != 0f
        if (!animationsEnabled) return@LaunchedEffect
        var lastFrame = 0L
        while (true) {
            withFrameNanos { frame ->
                if (lastFrame != 0L) elapsed += (frame - lastFrame).coerceAtMost(100_000_000L)
                lastFrame = frame
            }
        }
    }
    val resources = remember {
        listOf(R.raw.mascot_note_one, R.raw.mascot_note_pair, R.raw.mascot_note_two, R.raw.mascot_note_three)
    }
    val requests = remember(context) {
        (resources + R.raw.mascot_home).associateWith { resource ->
            ImageRequest.Builder(context)
                .data(resource)
                .decoderFactory(SvgDecoder.Factory())
                // Keep the decoded image stable while the Fold window changes size.
                .size(if (resource == R.raw.mascot_home) 960 else 96, if (resource == R.raw.mascot_home) 522 else 96)
                .build()
        }
    }
    BoxWithConstraints(
        Modifier.padding(horizontal = 16.dp).widthIn(max = 480.dp).fillMaxWidth(),
    ) {
        val availableWidth = maxWidth
        val artHeight = availableWidth * (340.55801f / 627.43272f)
        Box(Modifier.fillMaxWidth().height(artHeight + 48.dp)) {
            AnimatedVisibility(
                visible = isPlaying && animationsEnabled && lifecycle.isAtLeast(Lifecycle.State.STARTED),
                enter = fadeIn(tween(350)),
                exit = fadeOut(tween(600)),
            ) {
              Box(Modifier.fillMaxSize()) {
                val positions = listOf(0.14f, 0.36f, 0.61f, 0.83f)
                resources.forEachIndexed { index, resource ->
                    AsyncImage(
                        model = requests.getValue(resource), contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.align(Alignment.TopStart).size(if (index == 1) 29.dp else 24.dp)
                            .graphicsLayer {
                                val phase = ((elapsed / 1_000_000f / 7000f + index * 0.25f) % 1f)
                                val fade = minOf(phase / 0.18f, (1f - phase) / 0.22f, 1f).coerceIn(0f, 1f)
                                alpha = fade * 0.9f
                                translationX = (positions[index] * (availableWidth.toPx() - size.width)) + sin((phase * 2f * PI).toFloat()) * 5.dp.toPx()
                                translationY = (artHeight.toPx() * 0.46f + 48.dp.toPx()) * (1f - phase)
                                rotationZ = sin((phase * 2f * PI).toFloat()) * 8f
                            },
                    )
                }
              }
            }
            AsyncImage(
                model = requests.getValue(R.raw.mascot_home), contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(artHeight),
            )
        }
    }
}
