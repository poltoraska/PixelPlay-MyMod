package com.theveloper.pixelplay.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.theveloper.pixelplay.presentation.components.rememberExpressivePressScale
import com.theveloper.pixelplay.data.model.LibraryTabId
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.database.MusicDao
import com.theveloper.pixelplay.data.database.LocalPlaylistDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay

@HiltViewModel
class HomeLibrarySummaryViewModel @Inject constructor(music: MusicDao, playlists: LocalPlaylistDao) : ViewModel() {
    val albums = music.getAlbumCount()
    val artists = music.getArtistCount()
    val songs = music.getSongCount()
    val favorites = music.getFavoriteSongCount(emptyList(), false, 0)
    val playlists = playlists.observePlaylistCount()
}

@Composable
fun HomeLibrarySummaryCard(onDetails: () -> Unit, onOpenTab: (LibraryTabId) -> Unit, viewModel: HomeLibrarySummaryViewModel = hiltViewModel()) {
    val albums by viewModel.albums.collectAsStateWithLifecycle(initialValue = 0)
    val artists by viewModel.artists.collectAsStateWithLifecycle(initialValue = 0)
    val songs by viewModel.songs.collectAsStateWithLifecycle(initialValue = 0)
    val playlists by viewModel.playlists.collectAsStateWithLifecycle(initialValue = 0)
    val favorites by viewModel.favorites.collectAsStateWithLifecycle(initialValue = 0)
    val values = listOf(R.string.home_library_albums to albums, R.string.home_library_artists to artists,
        R.string.home_library_songs to songs, R.string.home_library_playlists to playlists,
        R.string.home_library_favorites to favorites)
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(16.dp)) {
            val wide = maxWidth >= 600.dp
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    LibraryMascot(Modifier.width(180.dp).height(200.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LibrarySummaryHeader(onDetails)
                        LibrarySummaryTiles(values, wide = true, onOpenTab = onOpenTab)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LibraryMascot(Modifier.width(108.dp).height(108.dp))
                        Box(Modifier.weight(1f)) { LibrarySummaryHeader(onDetails) }
                    }
                    LibrarySummaryTiles(values, wide = false, onOpenTab = onOpenTab)
                }
            }
        }
    }
}

@Composable
private fun LibraryMascot(modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)) {
        Image(painterResource(R.drawable.mascot_library_summary), contentDescription = null,
            contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(8.dp))
    }
}

@Composable
private fun LibrarySummaryHeader(onDetails: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.home_library_summary), modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        val haptics = LocalHapticFeedback.current
        val interaction = remember { MutableInteractionSource() }
        val scale by rememberExpressivePressScale(interaction)
        FilledIconButton(onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onDetails() },
            interactionSource = interaction, modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = stringResource(R.string.home_library_details))
        }
    }
}

@Composable
private fun LibrarySummaryTiles(values: List<Pair<Int, Int>>, wide: Boolean, onOpenTab: (LibraryTabId) -> Unit) {
    val tabs = listOf(LibraryTabId.ALBUMS, LibraryTabId.ARTISTS, LibraryTabId.SONGS, LibraryTabId.PLAYLISTS, LibraryTabId.LIKED)
    if (wide) {
        Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (0..1).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (0..1).forEach { col ->
                            val i = row * 2 + col
                            LibrarySummaryTile(values[i], tabs[i], i, Modifier.weight(1f), onOpenTab)
                        }
                    }
                }
            }
            LibrarySummaryTile(values[4], tabs[4], 4, Modifier.weight(1f).fillMaxHeight(), onOpenTab, verticalFavorite = true)
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            (0..1).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (0..1).forEach { col ->
                        val i = row * 2 + col
                        LibrarySummaryTile(values[i], tabs[i], i, Modifier.weight(1f), onOpenTab)
                    }
                }
            }
            LibrarySummaryTile(values[4], tabs[4], 4, Modifier.fillMaxWidth(), onOpenTab, horizontalFavorite = true)
        }
    }
}

@Composable
private fun LibrarySummaryTile(value: Pair<Int, Int>, tab: LibraryTabId, index: Int, modifier: Modifier,
    onOpenTab: (LibraryTabId) -> Unit, verticalFavorite: Boolean = false, horizontalFavorite: Boolean = false) {
    val (label, count) = value
    val favorite = tab == LibraryTabId.LIKED
    val labelText = stringResource(label)
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val scale by rememberExpressivePressScale(interaction)
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(index * 55L); visible = true }
    val alpha by androidx.compose.animation.core.animateFloatAsState(if (visible) 1f else 0f, tween(220), label = "summaryTileAlpha")
    Surface(modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
        .semantics(mergeDescendants = true) { contentDescription = "$labelText: $count" }
        .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onOpenTab(tab)
        },
        shape = RoundedCornerShape(18.dp),
        color = if (favorite) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = if (favorite) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface) {
        if (verticalFavorite || horizontalFavorite) {
            if (verticalFavorite) {
                Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(24.dp))
                    Text("%d".format(count), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(labelText, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                    Text("%d".format(count), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                }
            }
        } else {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text("%d".format(count), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(labelText, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
