package com.theveloper.pixelplay.presentation.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.theveloper.pixelplay.R

/** A primary play/pause action joined to a compact, independently activated shuffle action. */
@Composable
fun AlbumPlaybackActions(
    isPlaying: Boolean,
    enabled: Boolean,
    onPlayPause: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playInteraction = remember { MutableInteractionSource() }
    val shuffleInteraction = remember { MutableInteractionSource() }
    val playPressed by playInteraction.collectIsPressedAsState()
    val shufflePressed by shuffleInteraction.collectIsPressedAsState()
    val playOuterCorner by animateDpAsState(
        if (playPressed) 18.dp else 28.dp,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 600f),
        label = "albumPlayCorner",
    )
    val shuffleOuterCorner by animateDpAsState(
        if (shufflePressed) 18.dp else 28.dp,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 600f),
        label = "albumShuffleCorner",
    )

    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Button(
            onClick = onPlayPause,
            enabled = enabled,
            interactionSource = playInteraction,
            shape = RoundedCornerShape(
                topStart = playOuterCorner, bottomStart = playOuterCorner,
                topEnd = 8.dp, bottomEnd = 8.dp,
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 56.dp),
        ) {
            Icon(
                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(if (isPlaying) R.string.common_pause else R.string.album_action_play),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
        FilledTonalButton(
            onClick = onShuffle,
            enabled = enabled,
            interactionSource = shuffleInteraction,
            shape = RoundedCornerShape(
                topStart = 8.dp, bottomStart = 8.dp,
                topEnd = shuffleOuterCorner, bottomEnd = shuffleOuterCorner,
            ),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.width(60.dp).fillMaxHeight().heightIn(min = 56.dp),
        ) {
            Icon(
                Icons.Rounded.Shuffle,
                contentDescription = stringResource(R.string.common_shuffle_play_album),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
