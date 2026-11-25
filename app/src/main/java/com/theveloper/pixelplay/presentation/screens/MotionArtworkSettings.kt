package com.theveloper.pixelplay.presentation.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.presentation.viewmodel.MotionArtworkViewModel

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun MotionArtworkSettings() {
    val vm: MotionArtworkViewModel = hiltViewModel()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val bytes by vm.storedBytes.collectAsStateWithLifecycle()
    LaunchedEffect(vm) { vm.refreshSize() }
    SwitchSettingItem(
        title = stringResource(R.string.motion_artwork_title),
        subtitle = stringResource(R.string.motion_artwork_description),
        checked = settings.enabled,
        onCheckedChange = vm::setEnabled,
        leadingIcon = { Icon(Icons.Outlined.Movie, null, tint = MaterialTheme.colorScheme.secondary) }
    )
    SwitchSettingItem(
        title = stringResource(R.string.motion_artwork_wifi),
        subtitle = stringResource(R.string.motion_artwork_wifi_description),
        checked = settings.wifiOnly,
        onCheckedChange = vm::setWifiOnly,
        leadingIcon = { Icon(Icons.Outlined.Wifi, null, tint = MaterialTheme.colorScheme.secondary) }
    )
    ThemeSelectorItem(
        label = stringResource(R.string.motion_artwork_cache),
        description = stringResource(R.string.motion_artwork_cache_description),
        options = listOf(100, 200, 500).associate { it.toString() to stringResource(R.string.motion_artwork_mb, it) },
        selectedKey = settings.cacheLimitMb.toString(),
        onSelectionChanged = { it.toIntOrNull()?.let(vm::setLimit) },
        leadingIcon = { Icon(Icons.Outlined.Storage, null, tint = MaterialTheme.colorScheme.secondary) }
    )
    SettingsItem(
        title = stringResource(R.string.motion_artwork_clear),
        subtitle = stringResource(R.string.motion_artwork_used, bytes / (1024f * 1024f)),
        leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, tint = MaterialTheme.colorScheme.secondary) },
        onClick = vm::clear
    )
}
