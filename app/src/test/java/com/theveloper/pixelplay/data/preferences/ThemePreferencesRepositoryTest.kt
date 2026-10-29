package com.theveloper.pixelplay.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.theveloper.pixelplay.presentation.viewmodel.SetupUiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Files

class ThemePreferencesRepositoryTest {
    @Test
    fun `fresh setup follows the system and explicit theme choices are retained`() = runTest {
        val tempDir = Files.createTempDirectory("pixelplay-theme-preferences")
        try {
            val repository = ThemePreferencesRepository(
                PreferenceDataStoreFactory.create(
                    scope = backgroundScope,
                    produceFile = { tempDir.resolve("settings.preferences_pb").toFile() },
                )
            )
            assertEquals(AppThemeMode.FOLLOW_SYSTEM, SetupUiState().appThemeMode)
            assertEquals(AppThemeMode.FOLLOW_SYSTEM, repository.appThemeModeFlow.first())

            repository.setAppThemeMode(AppThemeMode.DARK)
            assertEquals(AppThemeMode.DARK, repository.appThemeModeFlow.first())
            repository.setAppThemeMode(AppThemeMode.LIGHT)
            assertEquals(AppThemeMode.LIGHT, repository.appThemeModeFlow.first())
            repository.setAppThemeMode(AppThemeMode.FOLLOW_SYSTEM)
            assertEquals(AppThemeMode.FOLLOW_SYSTEM, repository.appThemeModeFlow.first())
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }
}
