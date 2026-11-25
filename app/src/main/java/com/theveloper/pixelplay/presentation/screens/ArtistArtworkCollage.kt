package com.theveloper.pixelplay.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.size.Size
import com.theveloper.pixelplay.presentation.components.SmartImage

/** A stable 2×2 selection, shared by the blurred background and the missing-photo fallback. */
@Composable
internal fun ArtistArtworkCollage(artwork: List<String>, modifier: Modifier = Modifier) {
    if (artwork.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Person, null, Modifier.fillMaxSize(0.5f), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else if (artwork.size == 1) {
        SmartImage(artwork.first(), null, modifier = modifier, targetSize = Size(384, 384), contentScale = ContentScale.Crop)
    } else {
        Column(modifier) {
            repeat(2) { row ->
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    repeat(2) { column ->
                        SmartImage(artwork[(row * 2 + column) % artwork.size], null,
                            modifier = Modifier.weight(1f).fillMaxHeight(), targetSize = Size(256, 256), contentScale = ContentScale.Crop)
                    }
                }
            }
        }
    }
}
