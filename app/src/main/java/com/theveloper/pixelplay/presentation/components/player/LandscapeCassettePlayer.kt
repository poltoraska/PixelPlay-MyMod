package com.theveloper.pixelplay.presentation.components.player

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.size.Size
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.presentation.components.CassetteWindow
import com.theveloper.pixelplay.presentation.components.SmartImage

/** A phone-only landscape transport: the reel hubs double as previous/next buttons. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun LandscapeCassettePlayer(
    song: Song,
    paddingValues: PaddingValues,
    colors: ColorScheme,
    isPlayingProvider: () -> Boolean,
    isFavoriteProvider: () -> Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: Int,
    shuffleEnabled: Boolean,
    isCastConnecting: Boolean,
    animateReels: Boolean,
    seekState: CassetteSeekState,
    progressSection: @Composable () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onFavorite: () -> Unit,
    onAlbum: () -> Unit,
    onArtist: () -> Unit,
    onLyrics: () -> Unit,
    onQueue: () -> Unit,
    onCast: () -> Unit,
    onCollapse: () -> Unit,
) {
    val isPlaying = isPlayingProvider()
    val favorite = isFavoriteProvider()
    val haptics = LocalHapticFeedback.current
    val seekGeneration = seekState.generation
    LaunchedEffect(seekState, seekGeneration) {
        if (seekState.isSeeking && seekState.direction != 0) {
            delay(180)
            seekState.stopIfUnchanged(seekGeneration)
        }
    }
    if (animateReels) CassetteSeekSound(direction = seekState.direction, enabled = seekState.isSeeking)
    MaterialTheme(colorScheme = colors) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 8.dp, vertical = 4.dp),
            shape = RoundedCornerShape(28.dp),
            color = colors.primaryContainer,
            contentColor = colors.onPrimaryContainer,
        ) {
            Box {
                // Subtle screw heads suggest the shell without adding realistic textures or shadows.
                Canvas(Modifier.fillMaxSize()) {
                    val inset = 10.dp.toPx()
                    for (x in listOf(inset, size.width - inset)) {
                        for (y in listOf(inset, size.height - inset)) {
                            drawCircle(colors.onPrimaryContainer.copy(alpha = 0.16f), 2.dp.toPx(), Offset(x, y))
                        }
                    }
                }
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // The album cover is the tape label; its tap still opens the album.
                    Surface(shape = RoundedCornerShape(18.dp), color = colors.surfaceContainerLowest) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 8.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SmartImage(
                                model = song.albumArtUriString,
                                contentDescription = stringResource(R.string.common_album_art_for_title, song.album),
                                contentScale = ContentScale.Crop,
                                targetSize = Size(128, 128),
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                                    .clickable(role = Role.Button, onClick = onAlbum),
                            )
                            Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
                                Text(
                                    song.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee(),
                                )
                                Text(
                                    song.displayArtist,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable(role = Role.Button, onClick = onArtist),
                                )
                            }
                            CassetteAction(R.drawable.rounded_lyrics_24, R.string.common_lyrics, onLyrics)
                            CassetteAction(R.drawable.rounded_queue_music_24, R.string.player_cd_open_queue, onQueue)
                            CassetteAction(R.drawable.rounded_keyboard_arrow_down_24, R.string.player_cd_collapse, onCollapse)
                        }
                    }
                    BoxWithConstraints(
                        Modifier.weight(1f).widthIn(max = 840.dp).fillMaxWidth().align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center,
                    ) {
                        CassetteWindow(
                            modifier = Modifier.fillMaxSize(),
                            container = colors.surfaceContainerHighest,
                            reel = colors.onSurfaceVariant,
                            hub = colors.surfaceContainerLow,
                            isPlaying = isPlaying && animateReels,
                            isSeeking = seekState.isSeeking && animateReels,
                            seekDirection = seekState.direction,
                        )
                        // Match the shared renderer's physical reel centers exactly.
                        val hubSize = minOf(48.dp, maxHeight)
                        val playSize = minOf(72.dp, maxHeight)
                        CassetteTransportButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onPrevious()
                            },
                            label = stringResource(R.string.common_previous_track),
                            size = hubSize,
                            restingCorner = hubSize / 2,
                            modifier = Modifier.align(Alignment.CenterStart)
                                .absoluteOffset(x = maxWidth * 0.26f - hubSize / 2),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary,
                            ),
                        ) { iconScale ->
                            Icon(
                                painterResource(R.drawable.rounded_skip_previous_24),
                                contentDescription = null,
                                modifier = Modifier.graphicsLayer { scaleX = iconScale; scaleY = iconScale },
                            )
                        }
                        CassetteTransportButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onPlayPause()
                            },
                            label = stringResource(if (isPlaying) R.string.common_pause else R.string.common_play),
                            size = playSize,
                            restingCorner = if (isPlaying) playSize / 3 else playSize / 2,
                            enabled = !isCastConnecting,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = colors.tertiaryContainer,
                                contentColor = colors.onTertiaryContainer,
                            ),
                        ) { iconScale ->
                            if (isCastConnecting) {
                                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                            } else {
                                val motionScheme = remember { MotionScheme.expressive() }
                                Crossfade(
                                    targetState = isPlaying,
                                    animationSpec = motionScheme.fastEffectsSpec(),
                                    label = "cassettePlayPauseIcon",
                                ) { playing ->
                                    Icon(
                                        painterResource(if (playing) R.drawable.rounded_pause_24 else R.drawable.rounded_play_arrow_24),
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp).graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        },
                                    )
                                }
                            }
                        }
                        CassetteTransportButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNext()
                            },
                            label = stringResource(R.string.common_next_track),
                            size = hubSize,
                            restingCorner = hubSize / 2,
                            modifier = Modifier.align(Alignment.CenterStart)
                                .absoluteOffset(x = maxWidth * 0.74f - hubSize / 2),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary,
                            ),
                        ) { iconScale ->
                            Icon(
                                painterResource(R.drawable.rounded_skip_next_24),
                                contentDescription = null,
                                modifier = Modifier.graphicsLayer { scaleX = iconScale; scaleY = iconScale },
                            )
                        }
                    }
                    // The lower tape-head panel holds the timeline and secondary controls in one row.
                    Surface(shape = RoundedCornerShape(20.dp), color = colors.surfaceContainerLowest) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CassetteAction(R.drawable.rounded_shuffle_24, R.string.common_shuffle, onShuffle,
                                active = isShuffleEnabled, enabled = shuffleEnabled)
                            CassetteAction(
                                if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.rounded_repeat_one_24 else R.drawable.rounded_repeat_24,
                                R.string.common_repeat, onRepeat, active = repeatMode != Player.REPEAT_MODE_OFF,
                            )
                            Box(Modifier.weight(1f).padding(horizontal = 8.dp)) { progressSection() }
                            CassetteAction(
                                if (favorite) R.drawable.round_favorite_24 else R.drawable.rounded_favorite_24,
                                R.string.common_favorite, onFavorite, active = favorite,
                            )
                            CassetteAction(R.drawable.rounded_cast_24, R.string.player_cd_cast, onCast)
                        }
                    }
                }
            }
        }
    }
}

/** Animate only the visible shape and icon, keeping the touch target and reel centers stable. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CassetteTransportButton(
    onClick: () -> Unit,
    label: String,
    size: Dp,
    restingCorner: Dp,
    colors: IconButtonColors,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable (Float) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    var visualPressed by remember { mutableStateOf(false) }
    LaunchedEffect(pressed, enabled) {
        if (pressed && enabled) {
            visualPressed = true
        } else {
            // Briefly retain the visual state for quick taps; the action itself remains immediate.
            delay(90)
            visualPressed = false
        }
    }
    val motionScheme = remember { MotionScheme.expressive() }
    val corner by animateDpAsState(
        targetValue = if (visualPressed) size * 0.24f else restingCorner,
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "cassetteButtonCorner",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (visualPressed) 0.84f else 1f,
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "cassetteButtonIconScale",
    )
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(size).semantics { contentDescription = label },
        enabled = enabled,
        shape = RoundedCornerShape(corner.coerceAtLeast(0.dp)),
        colors = colors,
        interactionSource = interactionSource,
    ) {
        content(iconScale)
    }
}

@Composable
private fun CassetteAction(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    onClick: () -> Unit,
    active: Boolean? = null,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(48.dp).semantics { if (active != null) selected = active },
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (active == true) colors.secondaryContainer else Color.Transparent,
            contentColor = if (active == true) colors.onSecondaryContainer else colors.onSurfaceVariant,
        ),
    ) {
        Icon(painterResource(icon), stringResource(label))
    }
}
