package com.theveloper.pixelplay.presentation.viewmodel

import androidx.media3.common.Player

/** A paused, buffering, failed or still-transitioning track is not a completed queue. */
internal fun hasCompletedPlaybackQueue(
    playbackState: Int,
    repeatMode: Int,
    hasError: Boolean,
    hasItems: Boolean,
    hasNextItem: Boolean,
    transitionRunning: Boolean,
): Boolean = playbackState == Player.STATE_ENDED && repeatMode == Player.REPEAT_MODE_OFF &&
    !hasError && hasItems && !hasNextItem && !transitionRunning
