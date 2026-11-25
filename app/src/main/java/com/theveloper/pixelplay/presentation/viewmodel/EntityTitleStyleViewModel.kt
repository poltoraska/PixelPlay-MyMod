package com.theveloper.pixelplay.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theveloper.pixelplay.data.preferences.EntityTitleStyle
import com.theveloper.pixelplay.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class EntityTitleStyleViewModel @Inject constructor(private val preferences: UserPreferencesRepository) : ViewModel() {
    val styles = preferences.entityTitleStylesFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    suspend fun save(key: String, style: EntityTitleStyle) = preferences.setEntityTitleStyle(key, style)
    suspend fun consumeHint() = preferences.consumeEntityTitleHint()
}
