@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package com.theveloper.pixelplay.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as DrawSize
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.size.Size
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.model.Song

internal val LocalPhoneChromeCollapse = compositionLocalOf { 0f }
internal val CompactPhonePlayerWidth = 112.dp

@Composable
internal fun CompactPhonePlayer(song: Song, playing: Boolean, preparing: Boolean, onPlayPause: () -> Unit, position: () -> Long, duration: () -> Long) {
    val colors = LocalMaterialTheme.current
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.9f else 1f,
        MotionScheme.expressive().fastSpatialSpec(), label = "compactPlayerPress"
    )
    Row(Modifier.fillMaxWidth().height(MiniPlayerHeight).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            SmartImage(model = song.albumArtUriString, contentDescription = song.title,
                shape = CircleShape, targetSize = Size(150, 150), crossfadeDurationMillis = 0,
                modifier = Modifier.size(40.dp).playerArtworkSource())
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 2.dp.toPx()
                val arcSize = DrawSize(size.width - stroke * 2, size.height - stroke * 2)
                val arcOffset = Offset(stroke, stroke)
                drawArc(colors.onPrimaryContainer.copy(alpha = 0.16f), -90f, 360f, false,
                    topLeft = arcOffset, size = arcSize, style = Stroke(stroke))
                val fraction = playbackRingProgress(position(), duration())
                if (fraction > 0f) drawArc(colors.primary, -90f, fraction * 360f, false,
                    topLeft = arcOffset, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        IconButton(
            onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onPlayPause() },
            enabled = !preparing, interactionSource = interaction,
            modifier = Modifier.size(48.dp).graphicsLayer { scaleX = scale; scaleY = scale },
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
        ) {
            if (preparing) CircularProgressIndicator(Modifier.size(22.dp), color = colors.onPrimary, strokeWidth = 2.dp)
            else Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(if (playing) R.string.common_pause else R.string.common_play))
        }
    }
}

internal fun playbackRingProgress(positionMs: Long, durationMs: Long): Float =
    if (durationMs <= 0L) 0f else (positionMs.toDouble() / durationMs).toFloat().coerceIn(0f, 1f)
