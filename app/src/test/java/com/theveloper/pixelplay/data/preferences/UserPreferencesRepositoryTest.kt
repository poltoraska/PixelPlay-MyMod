package com.theveloper.pixelplay.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

class UserPreferencesRepositoryTest {

    @Test
    fun `entity title styles persist independently and auto removes the override`() = runTest {
        val dir = Files.createTempDirectory("entity-title-styles")
        try {
            val store = PreferenceDataStoreFactory.create(scope = backgroundScope,
                produceFile = { dir.resolve("settings.preferences_pb").toFile() })
            val repository = UserPreferencesRepository(store, Json)
            assertEquals(emptyMap<String, String>(), repository.entityTitleStylesFlow.first())
            repository.setEntityTitleStyle("album:1", EntityTitleStyle.WIDE)
            repository.setEntityTitleStyle("artist:1", EntityTitleStyle.SOFT)
            repository.setEntityTitleStyle("album:2", EntityTitleStyle.ACCENT)
            val restored = UserPreferencesRepository(store, Json)
            assertEquals(mapOf("album:1" to "WIDE", "artist:1" to "SOFT", "album:2" to "ACCENT"), restored.entityTitleStylesFlow.first())
            restored.setEntityTitleStyle("album:1", EntityTitleStyle.AUTO)
            assertEquals(mapOf("artist:1" to "SOFT", "album:2" to "ACCENT"), repository.entityTitleStylesFlow.first())
        } finally { dir.toFile().deleteRecursively() }
    }

    @Test
    fun `title style hint is consumed only once across view models`() = runTest {
        val dir = Files.createTempDirectory("entity-title-hint")
        try {
            val store = PreferenceDataStoreFactory.create(scope = backgroundScope,
                produceFile = { dir.resolve("settings.preferences_pb").toFile() })
            assertEquals(true, UserPreferencesRepository(store, Json).consumeEntityTitleHint())
            assertEquals(false, UserPreferencesRepository(store, Json).consumeEntityTitleHint())
        } finally { dir.toFile().deleteRecursively() }
    }


    @Test
    fun `scroll collapse preference persists without changing existing compact style`() = runTest {
        val dir = Files.createTempDirectory("phone-chrome-preferences")
        try {
            val store = PreferenceDataStoreFactory.create(scope = backgroundScope,
                produceFile = { dir.resolve("settings.preferences_pb").toFile() })
            val repository = UserPreferencesRepository(store, Json)
            assertEquals(true, repository.collapsePhoneChromeFlow.first())
            repository.setNavBarCompactMode(true)
            repository.setCollapsePhoneChrome(false)
            val reloaded = UserPreferencesRepository(store, Json)
            assertEquals(false, reloaded.collapsePhoneChromeFlow.first())
            assertEquals(true, reloaded.navBarCompactModeFlow.first())
        } finally { dir.toFile().deleteRecursively() }
    }


    @Test
    fun `motion artwork is opt in with wifi caching and bounded saved settings`() = runTest {
        val tempDir = Files.createTempDirectory("motion-artwork-settings-test")
        try {
            val store = PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { tempDir.resolve("settings.preferences_pb").toFile() }
            )
            val repository = UserPreferencesRepository(store, Json)
            assertEquals(MotionArtworkPreferences(enabled = false, wifiOnly = true, cacheLimitMb = 200), repository.motionArtworkPreferences.first())
            repository.setMotionArtworkEnabled(true)
            repository.setMotionArtworkWifiOnly(false)
            repository.setMotionArtworkCacheLimit(500)
            assertEquals(MotionArtworkPreferences(true, false, 500), UserPreferencesRepository(store, Json).motionArtworkPreferences.first())
            repository.setMotionArtworkCacheLimit(Int.MAX_VALUE)
            assertEquals(500, repository.motionArtworkPreferences.first().cacheLimitMb)
            repository.setMotionArtworkCacheLimit(-1)
            assertEquals(100, repository.motionArtworkPreferences.first().cacheLimitMb)
        } finally { tempDir.toFile().deleteRecursively() }
    }


    @Test
    fun `fresh view defaults use lists and explicit saved choices remain authoritative`() = runTest {
        val tempDir = Files.createTempDirectory("user-preferences-view-defaults-test")
        try {
            val repository = UserPreferencesRepository(
                dataStore = PreferenceDataStoreFactory.create(
                    scope = backgroundScope,
                    produceFile = { tempDir.resolve("settings.preferences_pb").toFile() }
                ),
                json = Json
            )

            assertEquals(false, repository.isGenreGridViewFlow.first())
            assertEquals(true, repository.isAlbumsListViewFlow.first())

            repository.setGenreGridView(true)
            repository.setAlbumsListView(false)
            repository.saveLibraryTabsOrder("[\"SONGS\",\"ALBUMS\",\"ARTIST\",\"PLAYLISTS\",\"FOLDERS\",\"LIKED\"]")

            assertEquals(true, repository.isGenreGridViewFlow.first())
            assertEquals(false, repository.isAlbumsListViewFlow.first())
            assertEquals(
                "[\"SONGS\",\"ALBUMS\",\"ARTIST\",\"PLAYLISTS\",\"FOLDERS\",\"LIKED\"]",
                repository.libraryTabsOrderFlow.first()
            )
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `default artist delimiters avoid common characters inside artist names`() {
        assertEquals(listOf(";"), UserPreferencesRepository.DEFAULT_ARTIST_DELIMITERS)
    }

    @Test
    fun `artistDelimitersFlow normalizes stored legacy defaults`() = runTest {
        val tempDir = Files.createTempDirectory("user-preferences-repository-test")
        try {
            val repository = UserPreferencesRepository(
                dataStore = PreferenceDataStoreFactory.create(
                    scope = backgroundScope,
                    produceFile = { tempDir.resolve("settings.preferences_pb").toFile() }
                ),
                json = Json
            )

            repository.setArtistDelimiters(listOf("/", ";", ",", "+", "&"))

            assertEquals(
                UserPreferencesRepository.DEFAULT_ARTIST_DELIMITERS,
                repository.artistDelimitersFlow.first()
            )
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `clearPreferencesExceptKeys preserves initial setup completion`() = runTest {
        val tempDir = Files.createTempDirectory("user-preferences-repository-test")
        try {
            val repository = UserPreferencesRepository(
                dataStore = PreferenceDataStoreFactory.create(
                    scope = backgroundScope,
                    produceFile = { tempDir.resolve("settings.preferences_pb").toFile() }
                ),
                json = Json
            )

            repository.setInitialSetupDone(true)
            repository.setNavBarStyle("compact")

            repository.clearPreferencesExceptKeys(emptySet())

            assertTrue(repository.initialSetupDoneFlow.first())
            assertEquals(NavBarStyle.DEFAULT, repository.navBarStyleFlow.first())
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `importPreferencesFromBackup clearExisting preserves initial setup completion`() = runTest {
        val tempDir = Files.createTempDirectory("user-preferences-repository-test")
        try {
            val repository = UserPreferencesRepository(
                dataStore = PreferenceDataStoreFactory.create(
                    scope = backgroundScope,
                    produceFile = { tempDir.resolve("settings.preferences_pb").toFile() }
                ),
                json = Json
            )

            repository.setInitialSetupDone(true)
            repository.setNavBarStyle("compact")

            repository.importPreferencesFromBackup(
                entries = listOf(
                    PreferenceBackupEntry(
                        key = "nav_bar_style",
                        type = "string",
                        stringValue = "restored"
                    )
                ),
                clearExisting = true
            )

            assertTrue(repository.initialSetupDoneFlow.first())
            assertEquals("restored", repository.navBarStyleFlow.first())
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `navBarCornerRadiusFlow clamps values outside the supported UI range`() = runTest {
        val tempDir = Files.createTempDirectory("user-preferences-repository-test")
        try {
            val repository = UserPreferencesRepository(
                dataStore = PreferenceDataStoreFactory.create(
                    scope = backgroundScope,
                    produceFile = { tempDir.resolve("settings.preferences_pb").toFile() }
                ),
                json = Json
            )

            repository.setNavBarCornerRadius(-1)
            assertEquals(MIN_NAV_BAR_CORNER_RADIUS, repository.navBarCornerRadiusFlow.first())

            repository.setNavBarCornerRadius(999)
            assertEquals(MAX_NAV_BAR_CORNER_RADIUS, repository.navBarCornerRadiusFlow.first())

            repository.importPreferencesFromBackup(
                entries = listOf(
                    PreferenceBackupEntry(
                        key = "nav_bar_corner_radius",
                        type = "int",
                        intValue = -1
                    )
                ),
                clearExisting = false
            )
            assertEquals(MIN_NAV_BAR_CORNER_RADIUS, repository.navBarCornerRadiusFlow.first())
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }
}
