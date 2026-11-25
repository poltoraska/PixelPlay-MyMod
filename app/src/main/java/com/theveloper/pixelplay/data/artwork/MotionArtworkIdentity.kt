package com.theveloper.pixelplay.data.artwork

import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

/** Keep edition words: a deluxe, live or remixed release must not borrow another release's video. */
internal fun normalizeArtworkIdentity(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT).replace("&", " and ")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim().replace(Regex("\\s+"), " ")

internal fun sameArtworkRelease(artist: String, album: String, otherArtist: String, otherAlbum: String): Boolean =
    normalizeArtworkIdentity(artist).let { it.isNotEmpty() && it == normalizeArtworkIdentity(otherArtist) } &&
        normalizeArtworkIdentity(album).let { it.isNotEmpty() && it == normalizeArtworkIdentity(otherAlbum) }

internal fun artworkIdentityKey(artist: String, album: String, year: Int): String =
    MessageDigest.getInstance("SHA-256").digest(
        "${normalizeArtworkIdentity(artist)}\n${normalizeArtworkIdentity(album)}\n$year".toByteArray(Charsets.UTF_8)
    ).joinToString("") { "%02x".format(it) }
