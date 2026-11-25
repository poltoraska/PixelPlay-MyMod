package com.theveloper.pixelplay.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.theveloper.pixelplay.data.artwork.MotionArtworkRepository
import com.theveloper.pixelplay.data.preferences.MotionArtworkPreferences
import com.theveloper.pixelplay.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class MotionArtworkViewModel @Inject constructor(
    val artwork: MotionArtworkRepository,
    private val preferences: UserPreferencesRepository
) : ViewModel() {
    val settings = preferences.motionArtworkPreferences.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), MotionArtworkPreferences()
    )
    val storedBytes = artwork.storedBytes
    val revision = artwork.revision
    fun setEnabled(value: Boolean) { viewModelScope.launch { preferences.setMotionArtworkEnabled(value) } }
    fun setWifiOnly(value: Boolean) { viewModelScope.launch { preferences.setMotionArtworkWifiOnly(value) } }
    fun setLimit(value: Int) { viewModelScope.launch {
        preferences.setMotionArtworkCacheLimit(value)
        artwork.enforceLimit(value)
    } }
    fun refreshSize() { viewModelScope.launch { artwork.refreshSize() } }
    fun clear() { viewModelScope.launch { artwork.clear() } }
}
