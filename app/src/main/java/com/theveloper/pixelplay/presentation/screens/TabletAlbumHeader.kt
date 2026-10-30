package com.theveloper.pixelplay.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImagePainter
import coil.size.Size
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.model.Album
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.presentation.components.AlbumPlaybackActions
import com.theveloper.pixelplay.presentation.components.SmartImage
import java.util.concurrent.TimeUnit

/** Measured as a list item: a long title never competes with shuffle or a fixed header height. */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun TabletAlbumHeader(
    album: Album,
    songs: List<Song>,
    imageRequestSize: Size,
    onArtworkState: (AsyncImagePainter.State) -> Unit,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
) {
    val surface = MaterialTheme.colorScheme.surface
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(songs.sumOf { it.duration })
    val duration = if (totalMinutes >= 60) {
        stringResource(R.string.duration_hours_minutes_long, totalMinutes / 60, totalMinutes % 60)
    } else {
        stringResource(R.string.duration_minutes_long, totalMinutes)
    }
    Box(Modifier.fillMaxWidth().clipToBounds()) {
        SmartImage(
            model = album.albumArtUriString,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            targetSize = imageRequestSize,
            modifier = Modifier.matchParentSize().blur(25.dp),
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, surface.copy(alpha = 0.82f), surface))
            )
        )
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AlbumCoverWithCassette(
                album = album,
                contentDescription = stringResource(R.string.album_cover_for, album.title),
                imageRequestSize = imageRequestSize,
                onArtworkState = onArtworkState,
                modifier = Modifier.align(Alignment.CenterHorizontally)
                    .widthIn(max = 250.dp).fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = album.title,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp, lineHeight = 32.sp),
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = album.artist,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = listOfNotNull(
                    pluralStringResource(R.plurals.album_detail_song_count, songs.size, songs.size),
                    duration,
                    album.year.takeIf { it > 0 }?.toString(),
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AlbumPlaybackActions(
                isPlaying = isPlaying,
                enabled = songs.isNotEmpty(),
                onPlayPause = onPlay,
                onShuffle = onShuffle,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
