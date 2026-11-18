package com.theveloper.pixelplay.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.model.Album
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.presentation.viewmodel.PlayerViewModel

@Composable
internal fun AlbumMetadataDialog(albums: List<Album>, playerViewModel: PlayerViewModel, onDismiss: () -> Unit) {
    val album = albums.first()
    var songs by remember(album.id) { mutableStateOf<List<Song>?>(null) }
    var title by remember(album.id) { mutableStateOf(album.title) }
    var artist by remember(album.id) { mutableStateOf(album.albumArtist ?: album.artist) }
    var year by remember(album.id) { mutableStateOf(album.year.takeIf { it > 0 }?.toString().orEmpty()) }
    LaunchedEffect(album.id) {
        songs = try { playerViewModel.getSongsForAlbums(albums) }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Exception) { emptyList() }
    }
    val yearValue = year.toIntOrNull()
    val validYear = year.isBlank() || (yearValue != null && yearValue in 1..9999)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.album_edit_metadata)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.album_edit_hint), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(R.string.album_edit_title)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text(stringResource(R.string.album_edit_artist)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text(stringResource(R.string.album_edit_year)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, isError = !validYear, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                if (songs == null) Text(stringResource(R.string.album_edit_loading))
                else if (songs.isNullOrEmpty()) Text(stringResource(R.string.album_edit_no_songs))
            }
        },
        confirmButton = {
            TextButton(enabled = !songs.isNullOrEmpty() && title.isNotBlank() && validYear, onClick = {
                playerViewModel.saveBatchMetadata(
                    songs = songs.orEmpty(), title = null, artist = null,
                    album = title.trim().takeIf { it != album.title },
                    albumArtist = artist.trim().takeIf { it != (album.albumArtist ?: album.artist) },
                    composer = null, genre = null, lyrics = null, trackNumber = null, discNumber = null,
                    replayGainTrackGainDb = null, replayGainAlbumGainDb = null, coverArtUpdate = null,
                    newYear = yearValue,
                )
                onDismiss()
            }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
