package com.theveloper.pixelplay.data.artwork

import java.util.Base64
import com.theveloper.pixelplay.data.model.Song
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Experimental web provider. Failure always leaves the embedded/static artwork intact. */
@Singleton
class AppleMotionArtworkProvider @Inject constructor() {
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS).callTimeout(25, TimeUnit.SECONDS).build()
    private var webToken: String? = null
    private var tokenExpiry = 0L

    suspend fun find(song: Song): String? {
        val artist = song.albumArtist?.takeIf { it.isNotBlank() } ?: song.artist
        if (artist.isBlank() || song.album.isBlank()) return null
        val country = Locale.getDefault().country.lowercase(Locale.ROOT)
            .takeIf { it.length == 2 } ?: "us"
        // Storefront availability differs. Use US only when the local storefront is unavailable.
        return try { findInStorefront(song, artist, country) } catch (e: HttpFailure) {
            if (country != "us" && e.status in listOf(400, 404)) findInStorefront(song, artist, "us") else throw e
        }
    }

    private suspend fun findInStorefront(song: Song, artist: String, country: String): String? {
        val albums = catalog(country, "search", mapOf("types" to "albums", "limit" to "10", "term" to "$artist ${song.album}"))
            .optJSONObject("results")?.optJSONObject("albums")?.optJSONArray("data") ?: JSONArray()
        val candidates = (0 until albums.length()).map { albums.getJSONObject(it) }
            .filter { matchesRelease(it.optJSONObject("attributes"), artist, song.album) }
            .sortedByDescending { it.optJSONObject("attributes")?.optString("releaseDate")?.startsWith(song.year.toString()) == true }
        for (album in candidates.take(3)) {
            val result = catalog(country, "albums/${album.getString("id")}", mapOf("extend" to "editorialVideo"))
            result.optJSONArray("data")?.optJSONObject(0)?.let { video(it)?.let { url -> return url } }
        }
        // Track + ISRC fallback, still requiring the exact album/edition and artist.
        val songs = catalog(country, "search", mapOf("types" to "songs", "limit" to "10", "term" to "${song.artist} ${song.title}"))
            .optJSONObject("results")?.optJSONObject("songs")?.optJSONArray("data") ?: return null
        for (i in 0 until songs.length()) {
            val attrs = songs.getJSONObject(i).optJSONObject("attributes") ?: continue
            if (normalizeArtworkIdentity(attrs.optString("name")) != normalizeArtworkIdentity(song.title) ||
                normalizeArtworkIdentity(attrs.optString("artistName")) != normalizeArtworkIdentity(song.artist) ||
                kotlin.math.abs(attrs.optLong("durationInMillis") - song.duration) > 3000) continue
            val isrc = attrs.optString("isrc").takeIf { it.isNotBlank() } ?: continue
            val related = catalog(country, "songs", mapOf("filter[isrc]" to isrc, "include" to "albums", "extend[albums]" to "editorialVideo"))
                .optJSONArray("data") ?: continue
            for (j in 0 until related.length()) {
                val releases = related.getJSONObject(j).optJSONObject("relationships")?.optJSONObject("albums")?.optJSONArray("data") ?: continue
                for (k in 0 until releases.length()) {
                    val release = releases.getJSONObject(k)
                    if (matchesRelease(release.optJSONObject("attributes"), artist, song.album)) {
                        video(release)?.let { return it }
                    }
                }
            }
        }
        return null
    }

    private fun matchesRelease(attrs: JSONObject?, artist: String, album: String) = attrs != null &&
        sameArtworkRelease(artist, album, attrs.optString("artistName"), attrs.optString("name"))

    private fun video(album: JSONObject): String? {
        val editorial = album.optJSONObject("attributes")?.optJSONObject("editorialVideo") ?: return null
        return listOf("motionDetailSquare", "motionSquareVideo1x1").firstNotNullOfOrNull {
            editorial.optJSONObject(it)?.optString("video")?.takeIf { url -> url.startsWith("https://") }
        }
    }

    private suspend fun catalog(country: String, path: String, params: Map<String, String>): JSONObject {
        val url = "https://amp-api.music.apple.com/v1/catalog/$country/$path".toHttpUrl().newBuilder()
            .apply { params.forEach { (key, value) -> addQueryParameter(key, value) } }.build()
        repeat(2) { attempt ->
            val token = token()
            try {
                return JSONObject(fetch(Request.Builder().url(url).header("Authorization", "Bearer $token")
                    .header("Origin", "https://music.apple.com")
                    .header("Referer", "https://music.apple.com/")
                    .header("Accept", "application/json").build()))
            } catch (e: HttpFailure) {
                if (e.status != 401 || attempt != 0) throw e
                webToken = null
            }
        }
        throw IOException("Artwork authorization unavailable")
    }

    private suspend fun token(): String {
        webToken?.takeIf { System.currentTimeMillis() < tokenExpiry }?.let { return it }
        val html = fetch(Request.Builder().url("https://music.apple.com/us/browse").build())
        val scripts = Regex("(?:https://music\\.apple\\.com)?(/assets/index[^\"'<>\\s]+\\.js)")
            .findAll(html).map { it.groupValues[1] }.distinct().take(5).toList()
        for (script in scripts) {
            val js = fetch(Request.Builder().url("https://music.apple.com$script").build())
            for (match in Regex("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+").findAll(js)) {
                val candidate = match.value
                val payload = runCatching {
                    JSONObject(String(Base64.getUrlDecoder().decode(candidate.split('.')[1]), Charsets.UTF_8))
                }.getOrNull() ?: continue
                val header = runCatching {
                    JSONObject(String(Base64.getUrlDecoder().decode(candidate.split('.')[0]), Charsets.UTF_8))
                }.getOrNull() ?: continue
                if (payload.optString("iss") != "AMPWebPlay" && header.optString("kid") != "WebPlayKid") continue
                val expires = payload.optLong("exp") * 1000 - 60_000
                if (expires <= System.currentTimeMillis()) continue
                webToken = candidate
                tokenExpiry = expires
                return candidate
            }
        }
        throw IOException("Apple web artwork token unavailable")
    }

    private suspend fun fetch(request: Request): String = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }
            override fun onResponse(call: Call, response: Response) {
                val result = runCatching {
                    response.use {
                        if (!it.isSuccessful) throw HttpFailure(it.code)
                        it.body?.byteStream()?.use { input ->
                            val output = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                if (output.size() + count > 8 * 1024 * 1024) throw IOException("Artwork response too large")
                                output.write(buffer, 0, count)
                            }
                            output.toString("UTF-8")
                        } ?: throw IOException("Empty artwork response")
                    }
                }
                if (continuation.isActive) result.fold(continuation::resume, continuation::resumeWithException)
            }
        })
    }

    private class HttpFailure(val status: Int) : IOException("Artwork HTTP $status")
}
