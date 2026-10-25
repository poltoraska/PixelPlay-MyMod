package com.theveloper.pixelplay.data.backup.model

import com.theveloper.pixelplay.R

enum class BackupSection(
    val key: String,
    val label: String,
    val description: String,
    val iconRes: Int,
    val sinceVersion: Int = 1
) {
    PLAYLISTS(
        key = "playlists",
        label = "Playlists",
        description = "Your custom playlists and ordering preferences.",
        iconRes = R.drawable.rounded_playlist_play_24
    ),
    GLOBAL_SETTINGS(
        key = "global_settings",
        label = "Global Settings",
        description = "Themes, behavior, playback, and app preferences.",
        iconRes = R.drawable.rounded_settings_24
    ),
    FAVORITES(
        key = "favorites",
        label = "Favorites",
        description = "Songs marked as favorite.",
        iconRes = R.drawable.rounded_favorite_24
    ),
    LYRICS(
        key = "lyrics",
        label = "Saved Lyrics",
        description = "Lyrics you've saved or imported.",
        iconRes = R.drawable.rounded_lyrics_24
    ),
    SEARCH_HISTORY(
        key = "search_history",
        label = "Search History",
        description = "Recent search terms in the app.",
        iconRes = R.drawable.rounded_search_24
    ),
    TRANSITIONS(
        key = "transitions",
        label = "Transition Rules",
        description = "Custom transition settings between songs.",
        iconRes = R.drawable.rounded_align_justify_space_even_24
    ),
    ENGAGEMENT_STATS(
        key = "engagement_stats",
        label = "Engagement Stats",
        description = "Play count and listening duration per song.",
        iconRes = R.drawable.rounded_monitoring_24
    ),
    PLAYBACK_HISTORY(
        key = "playback_history",
        label = "Playback History",
        description = "Timeline-based listening history for stats.",
        iconRes = R.drawable.rounded_schedule_24
    ),
    QUICK_FILL(
        key = "quick_fill",
        label = "QuickFill Genres",
        description = "Custom genres and their icons.",
        iconRes = R.drawable.rounded_instant_mix_24,
        sinceVersion = 3
    ),
    ARTIST_IMAGES(
        key = "artist_images",
        label = "Artist Images",
        description = "Cached artist image URLs from Deezer.",
        iconRes = R.drawable.rounded_person_24,
        sinceVersion = 3
    ),
    EQUALIZER(
        key = "equalizer",
        label = "Equalizer",
        description = "Your custom equalizer presets and audio profiles.",
        iconRes = R.drawable.rounded_surround_sound_24,
        sinceVersion = 3
    ),
    AI_USAGE_LOGS(
        key = "ai_usage_logs",
        label = "AI Activity Logs",
        description = "History of AI requests and token consumption.",
        iconRes = R.drawable.rounded_monitoring_24,
        sinceVersion = 4
    );

    @get:androidx.annotation.StringRes
    val labelRes: Int get() = when (this) {
        PLAYLISTS -> R.string.settings_backup_module_playlists_title
        GLOBAL_SETTINGS -> R.string.settings_backup_module_global_settings_title
        FAVORITES -> R.string.settings_backup_module_favorites_title
        LYRICS -> R.string.settings_backup_module_lyrics_title
        SEARCH_HISTORY -> R.string.settings_backup_module_search_history_title
        TRANSITIONS -> R.string.settings_backup_module_transitions_title
        ENGAGEMENT_STATS -> R.string.settings_backup_module_engagement_stats_title
        PLAYBACK_HISTORY -> R.string.settings_backup_module_playback_history_title
        QUICK_FILL -> R.string.settings_backup_module_quick_fill_title
        ARTIST_IMAGES -> R.string.settings_backup_module_artist_images_title
        EQUALIZER -> R.string.settings_backup_module_equalizer_title
        AI_USAGE_LOGS -> R.string.settings_backup_module_ai_usage_logs_title
    }

    @get:androidx.annotation.StringRes
    val descriptionRes: Int get() = when (this) {
        PLAYLISTS -> R.string.settings_backup_module_playlists_description
        GLOBAL_SETTINGS -> R.string.settings_backup_module_global_settings_description
        FAVORITES -> R.string.settings_backup_module_favorites_description
        LYRICS -> R.string.settings_backup_module_lyrics_description
        SEARCH_HISTORY -> R.string.settings_backup_module_search_history_description
        TRANSITIONS -> R.string.settings_backup_module_transitions_description
        ENGAGEMENT_STATS -> R.string.settings_backup_module_engagement_stats_description
        PLAYBACK_HISTORY -> R.string.settings_backup_module_playback_history_description
        QUICK_FILL -> R.string.settings_backup_module_quick_fill_description
        ARTIST_IMAGES -> R.string.settings_backup_module_artist_images_description
        EQUALIZER -> R.string.settings_backup_module_equalizer_description
        AI_USAGE_LOGS -> R.string.settings_backup_module_ai_usage_logs_description
    }

    companion object {
        val defaultSelection: Set<BackupSection> = entries.toSet()

        fun fromKey(key: String): BackupSection? = entries.find { it.key == key }
    }
}
