package com.theveloper.pixelplay.presentation.components.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.view.TextureView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.theveloper.pixelplay.data.artwork.MotionArtworkRepository
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.presentation.viewmodel.MotionArtworkViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Overlays the existing image, never replacing its placeholder or its swipe/click handling. */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun MotionArtworkOverlay(song: Song, active: Boolean, playing: Boolean) {
    val vm: MotionArtworkViewModel = hiltViewModel()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val revision by vm.revision.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val power = remember(context) { context.getSystemService(PowerManager::class.java) }
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var screenOn by remember { mutableStateOf(power.isInteractive) }
    var powerSave by remember { mutableStateOf(power.isPowerSaveMode) }
    DisposableEffect(lifecycle, context) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            screenOn = power.isInteractive
            powerSave = power.isPowerSaveMode
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                screenOn = power.isInteractive
                powerSave = power.isPowerSaveMode
            }
        }
        lifecycle.addObserver(observer)
        context.registerReceiver(receiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        })
        onDispose { lifecycle.removeObserver(observer); context.unregisterReceiver(receiver) }
    }
    var networkPermitted by remember(settings.wifiOnly) { mutableStateOf(vm.artwork.networkAllowed(settings.wifiOnly)) }
    DisposableEffect(context, settings.wifiOnly) {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                networkPermitted = vm.artwork.networkAllowed(settings.wifiOnly)
            }
            override fun onLost(network: Network) { networkPermitted = vm.artwork.networkAllowed(settings.wifiOnly) }
        }
        manager.registerDefaultNetworkCallback(callback)
        onDispose { manager.unregisterNetworkCallback(callback) }
    }
    val visible = active && settings.enabled && resumed && screenOn && !powerSave
    val albumKey = remember(song.albumArtist, song.artist, song.album, song.year) { vm.artwork.albumKey(song) }
    val cover by produceState<MotionArtworkRepository.Cover?>(null, albumKey, visible, settings, revision, networkPermitted) {
        value = null
        if (visible) {
            // Rapid skips should not issue one catalog request per transient song.
            delay(350)
            value = vm.artwork.resolve(song, settings)
        }
    }
    if (visible) cover?.takeIf { it.key == albumKey }?.let { cached ->
        key(cached.key, revision) {
            CachedMotionVideo(cached, playing, vm.artwork)
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun CachedMotionVideo(cover: MotionArtworkRepository.Cover, playing: Boolean, repository: MotionArtworkRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var firstFrame by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (firstFrame && !failed) 1f else 0f, tween(350), label = "motionCoverFade")
    // This player has no MediaSession, no audio focus and no audio renderer selected.
    val player = remember(cover) {
        ExoPlayer.Builder(context.applicationContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(repository.offlineDataSource(cover)))
            .build().apply {
                volume = 0f
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true).build()
                repeatMode = Player.REPEAT_MODE_ONE
                setMediaItem(MediaItem.Builder().setUri(cover.uri).setMimeType(MimeTypes.APPLICATION_M3U8).build())
            }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() { firstFrame = true }
            override fun onPlayerError(error: PlaybackException) {
                failed = true
                player.stop()
                scope.launch { repository.reject(cover) }
            }
        }
        player.addListener(listener)
        player.prepare()
        onDispose { player.removeListener(listener); player.release() }
    }
    LaunchedEffect(player, playing, failed) { player.playWhenReady = playing && !failed }
    AndroidView(
        factory = { TextureView(it).apply { isOpaque = false; player.setVideoTextureView(this) } },
        modifier = Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha },
        onRelease = { player.clearVideoTextureView(it) }
    )
}
