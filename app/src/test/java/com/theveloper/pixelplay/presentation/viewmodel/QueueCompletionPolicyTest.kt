package com.theveloper.pixelplay.presentation.viewmodel

import androidx.media3.common.Player
import org.junit.Assert.*
import org.junit.Test

class QueueCompletionPolicyTest {
    private fun completed(
        state: Int = Player.STATE_ENDED,
        repeat: Int = Player.REPEAT_MODE_OFF,
        error: Boolean = false,
        hasItems: Boolean = true,
        hasNext: Boolean = false,
        transitioning: Boolean = false,
    ) = hasCompletedPlaybackQueue(state, repeat, error, hasItems, hasNext, transitioning)

    @Test fun `natural end of final track shows continuation actions`() {
        assertTrue(completed())
    }
    @Test fun `pausing final track does not mean queue is complete`() {
        assertFalse(completed(state = Player.STATE_READY))
    }
    @Test fun `buffering or stopping never presents queue completion`() {
        assertFalse(completed(state = Player.STATE_BUFFERING))
        assertFalse(completed(state = Player.STATE_IDLE))
    }
    @Test fun `playback failure is not a completed queue`() {
        assertFalse(completed(error = true))
    }
    @Test fun `repeat one and repeat all keep completion hidden`() {
        assertFalse(completed(repeat = Player.REPEAT_MODE_ONE))
        assertFalse(completed(repeat = Player.REPEAT_MODE_ALL))
    }
    @Test fun `another shuffled item or another queue window keeps completion hidden`() {
        assertFalse(completed(hasNext = true))
    }
    @Test fun `crossfade handoff is not queue completion`() {
        assertFalse(completed(transitioning = true))
    }
    @Test fun `cleared queue does not offer replay`() {
        assertFalse(completed(hasItems = false))
    }
}
