@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.theveloper.pixelplay.presentation.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import coil.size.Size
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.model.Album
import com.theveloper.pixelplay.presentation.components.CassetteWindow
import com.theveloper.pixelplay.presentation.components.ShimmerBox
import com.theveloper.pixelplay.presentation.components.SmartImage
import com.theveloper.pixelplay.presentation.viewmodel.ColorSchemePair
import com.theveloper.pixelplay.ui.theme.GoogleSansRounded
import com.theveloper.pixelplay.ui.theme.LocalPixelPlayDarkTheme
import com.theveloper.pixelplay.utils.formatSongCount
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt
import kotlin.math.cos
import kotlin.math.sin

/** A cassette-inspired list tile, with artwork and decorative reels at a stable size. */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun AlbumListItem(
    album: Album,
    albumColorSchemePairFlow: StateFlow<ColorSchemePair?>,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    selectionIndex: Int? = null,
    onLongPress: () -> Unit = {},
    onSelectionToggle: () -> Unit = {},
    onPlayClick: () -> Unit = {},
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val pair by albumColorSchemePairFlow.collectAsStateWithLifecycle()
    val dark = LocalPixelPlayDarkTheme.current
    val colors = pair?.let { if (dark) it.dark else it.light } ?: MaterialTheme.colorScheme
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 700f),
        label = "cassettePress",
    )
    val corner by animateDpAsState(
        targetValue = if (isSelected) 18.dp else 24.dp,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 600f),
        label = "cassetteCorner",
    )
    val shape = RoundedCornerShape(corner)
    Box(modifier.fillMaxWidth()) {
        // Keep the cassette internals stable as the browser and column count change.
        val artworkSize = 72.dp
        val controlsWidth = 96.dp
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
                .clip(shape)
                .semantics { selected = isSelected }
                .combinedClickable(
                    enabled = !isLoading,
                    interactionSource = interactions,
                    indication = LocalIndication.current,
                    onClick = { if (isSelectionMode) onSelectionToggle() else onClick() },
                    onLongClick = onLongPress,
                ),
            shape = shape,
            color = if (isSelected) colors.secondaryContainer else colors.primaryContainer,
            contentColor = if (isSelected) colors.onSecondaryContainer else colors.onPrimaryContainer,
        ) {
            if (isLoading) {
                Row(
                    Modifier.fillMaxWidth().height(132.dp).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ShimmerBox(Modifier.size(artworkSize).clip(RoundedCornerShape(16.dp)))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShimmerBox(Modifier.fillMaxWidth(0.8f).height(20.dp).clip(RoundedCornerShape(6.dp)))
                        ShimmerBox(Modifier.fillMaxWidth(0.6f).height(14.dp).clip(RoundedCornerShape(6.dp)))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 132.dp).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SmartImage(
                        model = album.albumArtUriString,
                        contentDescription = stringResource(R.string.common_album_art_for_title, album.title),
                        contentScale = ContentScale.Crop,
                        targetSize = Size(256, 256),
                        modifier = Modifier.size(artworkSize).clip(RoundedCornerShape(16.dp)),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = album.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = GoogleSansRounded,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                lineHeight = 26.sp,
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = album.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalContentColor.current.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = listOfNotNull(album.year.takeIf { it > 0 }?.toString(), formatSongCount(album.songCount)).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalContentColor.current.copy(alpha = 0.75f),
                            minLines = 2,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Column(
                        modifier = Modifier.width(controlsWidth),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CassetteWindow(
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            container = colors.surfaceContainerHighest,
                            reel = colors.onSurfaceVariant,
                            hub = colors.surfaceContainerLow,
                            isPlaying = isPlaying,
                        )
                        if (isSelectionMode) {
                            Box(
                                modifier = Modifier.size(48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier.size(28.dp)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (isSelected) {
                                        if (selectionIndex != null) {
                                            Text(selectionIndex.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                                        } else {
                                            Icon(Icons.Default.Check, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                        }
                                    }
                                }
                            }
                        } else {
                            FilledIconButton(
                                onClick = onPlayClick,
                                modifier = Modifier.size(48.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = if (isPlaying) colors.primary else colors.surfaceContainerLow,
                                    contentColor = if (isPlaying) colors.onPrimary else colors.primary,
                                ),
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = stringResource(if (isPlaying) R.string.common_pause else R.string.common_play_album),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
