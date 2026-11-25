@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package com.theveloper.pixelplay.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.presentation.viewmodel.PlayerViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

/** Debounce the terminal state so queue/window handoffs never flash the mascot. */
@Composable
internal fun rememberQueueCompletionVisible(viewModel: PlayerViewModel): Boolean {
    val ended by viewModel.queuePlaybackEnded.collectAsStateWithLifecycle()
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(ended) {
        if (ended) delay(250)
        visible = ended
    }
    return ended && visible
}

/** Fits the artwork slot, a narrow tablet queue pane, or a short landscape viewport. */
@Composable
internal fun QueueCompletionCard(
    onMix: () -> Unit,
    onReplay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val scale by animateFloatAsState(if (entered) 1f else 0.92f,
        spring(dampingRatio = 0.68f, stiffness = 220f), label = "queueCompletionEntrance")
    val opacity by animateFloatAsState(if (entered) 1f else 0f, label = "queueCompletionOpacity")
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val short = maxHeight < 420.dp
        val sideBySideActions = maxWidth >= 360.dp && maxHeight < 300.dp
        val illustrationHeight = if (short) 88.dp else 160.dp
        Column(
            Modifier.widthIn(max = 420.dp).fillMaxWidth()
                .graphicsLayer { scaleX = scale; scaleY = scale; alpha = opacity }
                .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val illustration: @Composable () -> Unit = {
                Image(painterResource(R.drawable.mascot_home_empty), contentDescription = null,
                    modifier = Modifier.size(illustrationHeight))
            }
            val text: @Composable () -> Unit = {
                Text(stringResource(R.string.queue_finished_title), style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.queue_finished_message), style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (short) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                illustration()
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { text() }
            } else {
                illustration()
                Column(horizontalAlignment = Alignment.CenterHorizontally) { text() }
            }
            if (sideBySideActions) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { CompletionAction(primary = true, onClick = onMix) }
                Box(Modifier.weight(1f)) { CompletionAction(primary = false, onClick = onReplay) }
            } else {
                CompletionAction(primary = true, onClick = onMix)
                CompletionAction(primary = false, onClick = onReplay)
            }
        }
    }
}

@Composable
private fun CompletionAction(primary: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressScale by rememberExpressivePressScale(source)
    val haptic = LocalHapticFeedback.current
    val action = {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onClick()
    }
    val content: @Composable RowScope.() -> Unit = {
        Icon(if (primary) Icons.Rounded.Shuffle else Icons.Rounded.Replay, null, Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(if (primary) R.string.queue_finished_mix else R.string.queue_finished_replay),
            textAlign = TextAlign.Center, modifier = Modifier.weight(1f, fill = false))
    }
    val modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
        .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
    if (primary) Button(action, modifier, shape = RoundedCornerShape(28.dp), interactionSource = source, content = content)
    else FilledTonalButton(action, modifier, shape = RoundedCornerShape(28.dp), interactionSource = source, content = content)
}
