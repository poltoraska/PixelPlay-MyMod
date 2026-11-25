@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package com.theveloper.pixelplay.presentation.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImagePainter
import coil.size.Size
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.presentation.components.player.MotionArtworkOverlay
import com.theveloper.pixelplay.data.model.Album
import com.theveloper.pixelplay.presentation.components.CassetteWindow
import com.theveloper.pixelplay.presentation.components.SmartImage

/** The sleeve stays still while the cassette slides out on playback and settles back on pause. */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun AlbumCoverWithCassette(
    album: Album,
    imageRequestSize: Size,
    contentDescription: String,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    isPlaying: Boolean = false,
    artworkSong: Song? = null,
    allowMotionArtwork: Boolean = true,
    onArtworkState: ((AsyncImagePainter.State) -> Unit)? = null,
) {
    val lift by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 200f),
        label = "albumCassetteLift"
    )
    var visible by remember(album.id) { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(modifier.aspectRatio(0.90f).onGloballyPositioned {
        visible = !it.boundsInWindow().isEmpty
    }) {
        // Reserve the reveal space permanently so the sleeve never changes size on play/pause.
        val sleeveSize = maxWidth * 0.96f
        val inset = (maxHeight - sleeveSize).coerceAtLeast(0.dp)
        val sleeveShape = RoundedCornerShape(22.dp)
        Box(
            modifier = Modifier.width(sleeveSize * 0.96f).height(sleeveSize * 0.64f).align(Alignment.TopCenter)
                .graphicsLayer {
                    translationY = 3.dp.toPx() + inset.toPx() * 0.70f * (1f - lift)
                    rotationZ = -0.6f * lift
                    this.alpha = alpha
                }
                .clip(RoundedCornerShape(18.dp))
                .background(colors.primaryContainer),
        ) {
            CassetteWindow(
                modifier = Modifier.fillMaxWidth(0.82f).height(inset * 0.80f)
                    .align(Alignment.TopCenter).padding(top = 2.dp),
                container = colors.surfaceContainerHighest,
                reel = colors.onPrimaryContainer,
                hub = colors.primaryContainer,
                isPlaying = isPlaying && visible && allowMotionArtwork && alpha > 0.01f,
            )
        }
        Box(
            Modifier.size(sleeveSize).align(Alignment.BottomCenter)
                .shadow(4.dp, sleeveShape).clip(sleeveShape)
                .graphicsLayer { this.alpha = alpha }
        ) {
            SmartImage(
                model = album.albumArtUriString,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                targetSize = imageRequestSize,
                allowHardware = true,
                crossfadeDurationMillis = 0,
                onState = onArtworkState,
                modifier = Modifier.fillMaxSize(),
            )
            artworkSong?.let { song ->
                MotionArtworkOverlay(
                    song = song,
                    active = allowMotionArtwork && visible && alpha > 0.05f,
                    // Album artwork is ambient while the page is visible, independent of audio.
                    playing = true,
                )
            }
        }
    }
}
