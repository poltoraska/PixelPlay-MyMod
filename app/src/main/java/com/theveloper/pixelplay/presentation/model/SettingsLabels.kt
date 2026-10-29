package com.theveloper.pixelplay.presentation.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.equalizer.EqualizerPreset
import com.theveloper.pixelplay.data.preferences.AlbumArtPaletteStyle
import com.theveloper.pixelplay.data.preferences.CollagePattern

internal val AlbumArtPaletteStyle.labelRes: Int get() = when (this) {
    AlbumArtPaletteStyle.TONAL_SPOT -> R.string.settings_palette_style_tonal_spot
    AlbumArtPaletteStyle.VIBRANT -> R.string.settings_palette_style_vibrant
    AlbumArtPaletteStyle.EXPRESSIVE -> R.string.settings_palette_style_expressive
    AlbumArtPaletteStyle.FRUIT_SALAD -> R.string.settings_palette_style_fruit_salad
}

internal val CollagePattern.labelRes: Int get() = when (this) {
    CollagePattern.COSMIC_SWIRL -> R.string.settings_collage_cosmic_swirl
    CollagePattern.HONEYCOMB_GROOVE -> R.string.settings_collage_honeycomb_groove
    CollagePattern.VINYL_STACK -> R.string.settings_collage_vinyl_stack
    CollagePattern.PIXEL_MOSAIC -> R.string.settings_collage_pixel_mosaic
    CollagePattern.STARDUST_SCATTER -> R.string.settings_collage_stardust_scatter
}

@Composable
internal fun EqualizerPreset.localizedDisplayName(): String {
    if (isCustom) return displayName
    val nameRes = when (name) {
        "flat" -> R.string.settings_eq_preset_flat
        "rock" -> R.string.settings_eq_preset_rock
        "pop" -> R.string.settings_eq_preset_pop
        "hip_hop" -> R.string.settings_eq_preset_hip_hop
        "jazz" -> R.string.settings_eq_preset_jazz
        "classical" -> R.string.settings_eq_preset_classical
        "electronic" -> R.string.settings_eq_preset_electronic
        "bass_boost" -> R.string.settings_eq_preset_bass_boost
        "treble_boost" -> R.string.settings_eq_preset_treble_boost
        "vocal" -> R.string.settings_eq_preset_vocal
        "custom" -> R.string.settings_eq_preset_custom
        else -> return displayName
    }
    return stringResource(nameRes)
}
