package com.theveloper.pixelplay.data.artwork

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheKeyFactory
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.hls.offline.HlsDownloader
import androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsPlaylistParser
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.data.preferences.MotionArtworkPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@UnstableApi
@Singleton
class MotionArtworkRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val provider: AppleMotionArtworkProvider
) {
    // noBackupFilesDir is persistent across launches but excluded from Android cloud backups.
    private val root by lazy { File(context.noBackupFilesDir, "motion_artwork").apply { mkdirs() } }
    private val cache by lazy { SimpleCache(File(root, "media"), NoOpCacheEvictor(), StandaloneDatabaseProvider(context)) }
    private val mutex = Mutex()
    private val playbackFailures = mutableMapOf<String, Long>()
    private val mutableRevision = MutableStateFlow(0)
    val revision = mutableRevision.asStateFlow()
    private val mutableBytes = MutableStateFlow(0L)
    val storedBytes = mutableBytes.asStateFlow()

    data class Cover(val key: String, val uri: Uri)

    fun albumKey(song: Song): String = artworkIdentityKey(song.albumArtist?.takeIf { it.isNotBlank() } ?: song.artist, song.album, song.year)
    private fun keyFactory(key: String) = CacheKeyFactory { spec -> "$key|${spec.key ?: spec.uri}" }
    private fun record(key: String) = File(root, "$key.json")

    /** A single serialized request also coalesces consecutive songs from the same album. */
    suspend fun resolve(song: Song, settings: MotionArtworkPreferences): Cover? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val key = albumKey(song)
            if ((playbackFailures[key] ?: 0L) > System.currentTimeMillis()) return@withLock null
            playbackFailures.remove(key)
            val file = record(key)
            val entry = runCatching { JSONObject(file.readText()) }.getOrNull()
            if (entry?.optString("uri").orEmpty().isNotEmpty()) {
                val resources = entry!!.optJSONObject("resources")
                if (resources != null && resources.length() > 0 && resources.keys().asSequence().all { resource ->
                    val length = resources.optLong(resource, -1L)
                    length > 0 && cache.isCached(resource, 0, length)
                }) {
                    file.setLastModified(System.currentTimeMillis())
                    mutableBytes.value = cache.cacheSpace
                    return@withLock Cover(key, Uri.parse(entry.getString("uri")))
                }
                remove(key)
            }
            if (!settings.enabled || !networkAllowed(settings.wifiOnly)) return@withLock null
            if (entry?.optLong("missingUntil", 0L)?.let { it > System.currentTimeMillis() } == true) return@withLock null
            try {
                val remote = provider.find(song)
                if (remote == null) {
                    writeRecord(key, JSONObject().put("missingUntil", System.currentTimeMillis() + 3 * 24 * 60 * 60 * 1000L))
                    trim(settings.cacheLimitMb)
                    return@withLock null
                }
                coroutineContext.ensureActive()
                if (!networkAllowed(settings.wifiOnly)) return@withLock null
                // Partial files from an interrupted process are not considered ready artwork.
                remove(key)
                trim(settings.cacheLimitMb)
                download(key, remote, settings.wifiOnly)
                val resources = JSONObject()
                cache.keys.filter { it.startsWith("$key|") }.forEach { resource ->
                    val end = cache.getCachedSpans(resource).maxOfOrNull { it.position + it.length } ?: 0
                    if (end <= 0 || !cache.isCached(resource, 0, end)) throw IOException("Incomplete artwork download")
                    resources.put(resource, end)
                }
                if (resources.length() == 0) throw IOException("Empty artwork download")
                val manifest = cachedVariantUri ?: throw IOException("Missing artwork manifest")
                writeRecord(key, JSONObject().put("uri", manifest).put("resources", resources))
                trim(settings.cacheLimitMb, keep = key)
                mutableBytes.value = cache.cacheSpace
                Cover(key, Uri.parse(manifest))
            } catch (e: CancellationException) {
                remove(key)
                throw e
            } catch (error: Exception) {
                timber.log.Timber.d("Motion artwork unavailable: %s", error.javaClass.simpleName)
                // Authentication, transport and decoder failures are not "cover does not exist".
                remove(key)
                null
            }
        }
    }

    // Accessed only under mutex; do not keep a downloader or player alive in the background.
    private var cachedVariantUri: String? = null

    private suspend fun download(key: String, remote: String, wifiOnly: Boolean) {
        cachedVariantUri = null
        runInterruptible(Dispatchers.IO) {
            val received = AtomicLong()
            val upstream = DefaultHttpDataSource.Factory().setConnectTimeoutMs(10_000).setReadTimeoutMs(15_000)
                .setTransferListener(object : TransferListener {
                    override fun onTransferInitializing(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) = Unit
                    override fun onTransferStart(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {
                        if (!networkAllowed(wifiOnly)) throw IOException("Artwork network policy changed")
                    }
                    override fun onBytesTransferred(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean, bytesTransferred: Int) {
                        if (received.addAndGet(bytesTransferred.toLong()) > MAX_VIDEO_BYTES) throw IOException("Artwork exceeds size limit")
                    }
                    override fun onTransferEnd(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) = Unit
                })
            val master = loadPlaylist(upstream, Uri.parse(remote))
            val variant = when (master) {
                is HlsMultivariantPlaylist -> master.variants.filter { variant ->
                    val format = variant.format
                    format.roleFlags and C.ROLE_FLAG_TRICK_PLAY == 0 &&
                        format.codecs?.contains("avc1") == true && format.width in 1..720 && format.height in 1..720 &&
                        format.colorInfo?.colorTransfer != C.COLOR_TRANSFER_ST2084 &&
                        format.colorInfo?.colorTransfer != C.COLOR_TRANSFER_HLG
                }.maxByOrNull { it.format.width * it.format.height }?.url
                    ?: throw IOException("No supported SDR artwork variant")
                is HlsMediaPlaylist -> Uri.parse(remote)
                else -> throw IOException("Unsupported artwork playlist")
            }
            val media = loadPlaylist(upstream, variant) as? HlsMediaPlaylist ?: throw IOException("Missing artwork media")
            if (!media.hasEndTag || media.durationUs > 40_000_000 || media.protectionSchemes != null) {
                throw IOException("Artwork must be a short unprotected loop")
            }
            val factory = CacheDataSource.Factory().setCache(cache).setCacheKeyFactory(keyFactory(key))
                .setUpstreamDataSourceFactory(upstream)
            val downloader = HlsDownloader(MediaItem.Builder().setUri(variant).setMimeType(MimeTypes.APPLICATION_M3U8).build(), factory)
            downloader.download { length, bytes, _ ->
                if (Thread.currentThread().isInterrupted || bytes > MAX_VIDEO_BYTES || length > MAX_VIDEO_BYTES) {
                    downloader.cancel()
                    throw IOException("Artwork download cancelled or oversized")
                }
            }
            cachedVariantUri = variant.toString()
        }
    }

    private fun loadPlaylist(factory: DataSource.Factory, uri: Uri) =
        androidx.media3.datasource.DataSourceInputStream(factory.createDataSource(), DataSpec(uri)).use {
            HlsPlaylistParser().parse(uri, it)
        }

    /** No upstream: a video player can never consume mobile data or refetch an expired signed URL. */
    fun offlineDataSource(cover: Cover): DataSource.Factory = CacheDataSource.Factory()
        .setCache(cache).setCacheKeyFactory(keyFactory(cover.key)).setUpstreamDataSourceFactory(null)
        .setCacheWriteDataSinkFactory(null)

    suspend fun reject(cover: Cover) = withContext(Dispatchers.IO) {
        mutex.withLock {
            remove(cover.key)
            // Avoid downloading the same unsupported/corrupted video on every swipe.
            playbackFailures[cover.key] = System.currentTimeMillis() + 60 * 60 * 1000L
        }
    }

    suspend fun refreshSize() = withContext(Dispatchers.IO) { mutableBytes.value = cache.cacheSpace }

    suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock {
            cache.keys.toList().forEach(cache::removeResource)
            root.listFiles()?.filter { it.extension == "json" }?.forEach { it.delete() }
            playbackFailures.clear()
            mutableBytes.value = cache.cacheSpace
            mutableRevision.value += 1
        }
    }

    suspend fun enforceLimit(limitMb: Int) = withContext(Dispatchers.IO) {
        mutex.withLock { trim(limitMb); mutableBytes.value = cache.cacheSpace }
    }

    private fun trim(limitMb: Int, keep: String? = null) {
        val records = root.listFiles()?.filter { it.extension == "json" }?.sortedBy { it.lastModified() }.orEmpty()
        for (file in records) {
            if (cache.cacheSpace <= limitMb * 1024L * 1024) break
            if (file.nameWithoutExtension != keep) remove(file.nameWithoutExtension)
        }
        // Remove incomplete resources left by a killed process.
        cache.keys.map { it.substringBefore('|') }.distinct().filter { !record(it).exists() && it != keep }.forEach(::remove)
        // Bound the small negative-result index as well.
        records.filter { it.exists() }.dropLast(500).forEach { if (it.nameWithoutExtension != keep) remove(it.nameWithoutExtension) }
    }

    private fun remove(key: String) {
        cache.keys.filter { it.startsWith("$key|") }.forEach(cache::removeResource)
        record(key).delete()
        mutableBytes.value = cache.cacheSpace
    }

    private fun writeRecord(key: String, data: JSONObject) {
        val temporary = File(root, "$key.tmp")
        temporary.writeText(data.toString())
        if (!temporary.renameTo(record(key))) throw IOException("Cannot save artwork index")
    }

    fun networkAllowed(wifiOnly: Boolean): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val caps = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
            (!wifiOnly || caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
    }

    companion object { private const val MAX_VIDEO_BYTES = 48L * 1024 * 1024 }
}
