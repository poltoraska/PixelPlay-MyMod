package com.theveloper.pixelplay.data.artwork

import org.junit.Assert.*
import org.junit.Test

class MotionArtworkIdentityTest {
    @Test fun `equivalent punctuation and case match`() {
        assertTrue(sameArtworkRelease("AC/DC", "Back In Black", "ac dc", "BACK IN BLACK"))
        assertTrue(sameArtworkRelease("Artist & Guest", "Album", "Artist and Guest", "Album"))
    }
    @Test fun `different artists with identical album names do not match`() {
        assertFalse(sameArtworkRelease("Artist A", "Greatest Hits", "Artist B", "Greatest Hits"))
    }
    @Test fun `deluxe live remix and remastered editions remain distinct`() {
        for (edition in listOf("Deluxe", "Live", "Remixes", "Remastered")) {
            assertFalse(sameArtworkRelease("Artist", "Album", "Artist", "Album ($edition)"))
        }
    }
    @Test fun `missing album or artist is never an exact release`() {
        assertFalse(sameArtworkRelease("", "Album", "", "Album"))
        assertFalse(sameArtworkRelease("Artist", "", "Artist", ""))
    }
    @Test fun `cyrillic and unicode characters are retained`() {
        assertTrue(sameArtworkRelease("Кино", "Группа крови", "КИНО", "Группа крови"))
        assertNotEquals(artworkIdentityKey("Кино", "Группа крови", 1988), artworkIdentityKey("Кино", "Звезда по имени Солнце", 1989))
    }
    @Test fun `cache identity is stable but separates artists editions and years`() {
        val base = artworkIdentityKey("Artist", "Album", 2000)
        assertEquals(base, artworkIdentityKey("ARTIST", "album", 2000))
        assertNotEquals(base, artworkIdentityKey("Other", "Album", 2000))
        assertNotEquals(base, artworkIdentityKey("Artist", "Album (Deluxe)", 2000))
        assertNotEquals(base, artworkIdentityKey("Artist", "Album", 2020))
        assertTrue(base.matches(Regex("[a-f0-9]{64}")))
    }
}
