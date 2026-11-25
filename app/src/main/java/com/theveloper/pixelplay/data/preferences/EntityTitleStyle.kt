package com.theveloper.pixelplay.data.preferences

import com.theveloper.pixelplay.R

enum class EntityTitleStyle(val labelRes: Int) {
    AUTO(R.string.title_style_auto),
    WIDE(R.string.title_style_wide),
    COMPACT(R.string.title_style_compact),
    SOFT(R.string.title_style_soft),
    ACCENT(R.string.title_style_accent);

    companion object {
        fun fromStorage(value: String?) = entries.firstOrNull { it.name == value } ?: AUTO
    }
}

internal fun titleUsesCyrillic(text: String): Boolean =
    text.codePoints().anyMatch { Character.UnicodeScript.of(it) == Character.UnicodeScript.CYRILLIC }

/** Width is measured using the actual font, device density and accessibility font scale. */
internal fun automaticEntityTitleStyle(measuredWidth: Float, availableWidth: Float, identity: String): EntityTitleStyle {
    if (!measuredWidth.isFinite() || !availableWidth.isFinite() || availableWidth <= 0f) return EntityTitleStyle.COMPACT
    return when {
        measuredWidth <= availableWidth * 0.75f -> EntityTitleStyle.WIDE
        measuredWidth > availableWidth * 1.9f -> EntityTitleStyle.COMPACT
        identity.hashCode() and 1 == 0 -> EntityTitleStyle.SOFT
        else -> EntityTitleStyle.ACCENT
    }
}
