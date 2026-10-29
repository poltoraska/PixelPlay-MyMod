package com.theveloper.pixelplay.presentation.components.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.theveloper.pixelplay.R

/** A quiet UI sound: no audio-focus request, and no playback after the player leaves the screen. */
@Composable
internal fun CassetteSeekSound(direction: Int, enabled: Boolean) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateAsState()
    var pool by remember { mutableStateOf<SoundPool?>(null) }
    var sampleId by remember { mutableIntStateOf(0) }
    var loaded by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        var disposed = false
        val handler = Handler(Looper.getMainLooper())
        val soundPool = SoundPool.Builder().setMaxStreams(1).setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()
        soundPool.setOnLoadCompleteListener { _, _, status ->
            handler.post { if (!disposed) loaded = status == 0 }
        }
        pool = soundPool
        sampleId = soundPool.load(context, R.raw.cassette_rewind, 1)
        onDispose {
            disposed = true
            loaded = false
            pool = null
            soundPool.release()
        }
    }

    DisposableEffect(pool, sampleId, loaded, direction, enabled, lifecycleState) {
        val soundPool = pool
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val soundEffectsEnabled = runCatching {
            Settings.System.getInt(context.contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, 1) != 0
        }.getOrDefault(true)
        val canPlay = enabled && direction != 0 && loaded &&
            lifecycleState.isAtLeast(Lifecycle.State.STARTED) &&
            audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL && soundEffectsEnabled
        val stream = if (canPlay) soundPool?.play(sampleId, 0.18f, 0.18f, 1, -1, 1f) ?: 0 else 0
        onDispose { if (stream != 0) soundPool?.stop(stream) }
    }
}
