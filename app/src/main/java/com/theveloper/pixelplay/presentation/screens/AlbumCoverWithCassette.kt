package com.theveloper.pixelplay.presentation.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImagePainter
import coil.size.Size
import com.theveloper.pixelplay.data.model.Album
import com.theveloper.pixelplay.presentation.components.CassetteWindow
import com.theveloper.pixelplay.presentation.components.SmartImage

/** A small part of the cassette peeks out without obscuring the album artwork. */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun AlbumCoverWithCassette(
    album: Album,
    imageRequestSize: Size,
    contentDescription: String,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    onArtworkState: ((AsyncImagePainter.State) -> Unit)? = null,
) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(album.id) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(durationMillis = 480, easing = FastOutSlowInEasing))
    }
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(modifier.aspectRatio(1f)) {
        val inset = minOf(18.dp, maxWidth * 0.09f)
        Box(
            modifier = Modifier.fillMaxSize(0.88f).align(Alignment.TopEnd)
                .graphicsLayer {
                    translationX = inset.toPx() * 0.35f * reveal.value
                    translationY = inset.toPx() * (1f - reveal.value)
                    this.alpha = alpha
                }
                .clip(RoundedCornerShape(18.dp))
                .background(colors.primaryContainer),
        ) {
            CassetteWindow(
                modifier = Modifier.fillMaxWidth(0.55f).height(inset * 1.5f)
                    .align(Alignment.TopCenter).padding(top = 2.dp),
                container = colors.surfaceContainerHighest,
                reel = colors.onPrimaryContainer,
                hub = colors.primaryContainer,
                isPlaying = false,
            )
        }
        SmartImage(
            model = album.albumArtUriString,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            targetSize = imageRequestSize,
            allowHardware = true,
            crossfadeDurationMillis = 0,
            alpha = alpha,
            onState = onArtworkState,
            modifier = Modifier.fillMaxSize().padding(top = inset, end = inset)
                .clip(RoundedCornerShape(24.dp)),
        )
    }
}
