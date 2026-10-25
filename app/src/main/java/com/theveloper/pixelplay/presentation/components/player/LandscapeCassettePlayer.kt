package com.theveloper.pixelplay.presentation.components.player

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
                        )
                        // Match the shared renderer's physical reel centers exactly.
                        val hubSize = minOf(48.dp, maxHeight)
                        val playSize = minOf(72.dp, maxHeight)
                        FilledIconButton(
                            onClick = onPrevious,
                            modifier = Modifier.align(Alignment.CenterStart)
                                .absoluteOffset(x = maxWidth * 0.26f - hubSize / 2).size(hubSize),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary,
                            ),
                        ) {
                            Icon(painterResource(R.drawable.rounded_skip_previous_24), stringResource(R.string.common_previous_track))
                        }
                        FilledIconButton(
                            onClick = onPlayPause,
                            enabled = !isCastConnecting,
                            modifier = Modifier.size(playSize),
                            shape = RoundedCornerShape(if (isPlaying) 24.dp else 36.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = colors.tertiaryContainer,
                                contentColor = colors.onTertiaryContainer,
                            ),
                        ) {
                            if (isCastConnecting) {
                                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    painterResource(if (isPlaying) R.drawable.rounded_pause_24 else R.drawable.rounded_play_arrow_24),
                                    stringResource(if (isPlaying) R.string.common_pause else R.string.common_play),
                                    Modifier.size(32.dp),
                                )
                            }
                        }
                        FilledIconButton(
                            onClick = onNext,
                            modifier = Modifier.align(Alignment.CenterStart)
                                .absoluteOffset(x = maxWidth * 0.74f - hubSize / 2).size(hubSize),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary,
                            ),
                        ) {
                            Icon(painterResource(R.drawable.rounded_skip_next_24), stringResource(R.string.common_next_track))
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
