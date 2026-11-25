package com.theveloper.pixelplay.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.theveloper.pixelplay.MainCoroutineExtension
import com.theveloper.pixelplay.data.model.Album
import com.theveloper.pixelplay.data.model.Artist
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.data.repository.MusicRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainCoroutineExtension::class)
class AlbumArtistNavigationTest {
    private val repository = mockk<MusicRepository>()
    private val album = Album.empty().copy(id = 7, title = "Album", artist = "Album Artist", albumArtist = "Album Artist")
    private val song = Song.emptySong().copy(id = "8", artist = "Guest", artistId = 12, albumId = 7)
    private fun viewModel(songs: List<Song> = listOf(song)): AlbumDetailViewModel {
        every { repository.getAlbumById(7) } returns flowOf(album)
        every { repository.getSongsForAlbum(7) } returns flowOf(songs)
        return AlbumDetailViewModel(mockk<Context>(relaxed = true), repository, SavedStateHandle(mapOf("albumId" to "7")))
    }
    @Test fun `album artist wins over first track guest`() = runTest {
        val artist = Artist(22, "Album Artist", 5)
        coEvery { repository.getArtistIdByName("Album Artist") } returns 22
        every { repository.getArtistById(22) } returns flowOf(artist)
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(listOf(artist), vm.resolveAlbumArtists())
        verify(exactly = 0) { repository.getArtistsForSong(any()) }
    }
    @Test fun `collaborators offer distinct valid artist destinations`() = runTest {
        val first = Artist(22, "First", 5)
        val second = Artist(23, "Second", 3)
        coEvery { repository.getArtistIdByName(any()) } returns null
        every { repository.getArtistsForSong(8) } returns flowOf(listOf(first, second, first, Artist(-1, "Unknown", 0)))
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(listOf(first, second), vm.resolveAlbumArtists())
    }
    @Test fun `track artist id is a fallback when song links are absent`() = runTest {
        val artist = Artist(12, "Guest", 1)
        coEvery { repository.getArtistIdByName(any()) } returns null
        every { repository.getArtistsForSong(8) } returns flowOf(emptyList())
        every { repository.getArtistById(12) } returns flowOf(artist)
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(listOf(artist), vm.resolveAlbumArtists())
    }
    @Test fun `missing artist data never navigates to a fabricated id`() = runTest {
        coEvery { repository.getArtistIdByName(any()) } returns null
        val vm = viewModel(emptyList())
        advanceUntilIdle()
        assertEquals(emptyList<Artist>(), vm.resolveAlbumArtists())
    }
}
