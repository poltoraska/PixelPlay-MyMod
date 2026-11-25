@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.theveloper.pixelplay.presentation.components

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.preferences.EntityTitleStyle
import com.theveloper.pixelplay.presentation.viewmodel.EntityTitleStyleViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun PersonalizedEntityTitle(
    text: String, entityKey: String, collapseFraction: Float = 0f,
    expandedWidth: Dp? = null, modifier: Modifier = Modifier,
) {
    val vm: EntityTitleStyleViewModel = hiltViewModel()
    val overrides by vm.styles.collectAsStateWithLifecycle()
    val saved = EntityTitleStyle.fromStorage(overrides[entityKey])
    var open by rememberSaveable(entityKey) { mutableStateOf(false) }
    val context = LocalContext.current
    val hint = stringResource(R.string.title_style_hint)
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(entityKey) {
        delay(1200)
        if (vm.consumeHint()) Toast.makeText(context, hint, Toast.LENGTH_LONG).show()
    }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val pageWidth = expandedWidth ?: maxWidth
        EntityTitleText(text, entityKey, saved, collapseFraction, pageWidth,
            Modifier.clickable(role = Role.Button, onClickLabel = stringResource(R.string.title_style_picker_title)) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                open = true
            })
        if (open) EntityTitleStyleSheet(text, entityKey, saved, vm, pageWidth, onDismiss = { open = false })
    }
}

@Composable
private fun EntityTitleStyleSheet(
    text: String, entityKey: String, initial: EntityTitleStyle,
    vm: EntityTitleStyleViewModel, pageWidth: Dp, onDismiss: () -> Unit,
) {
    var selectedName by rememberSaveable(entityKey) { mutableStateOf(initial.name) }
    val selected = EntityTitleStyle.fromStorage(selectedName)
    var saving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true,
        confirmValueChange = { !saving || it != SheetValue.Hidden })
    ModalBottomSheet(onDismissRequest = { if (!saving) onDismiss() }, sheetState = sheet,
        sheetMaxWidth = 560.dp, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.title_style_picker_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.title_style_picker_description), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(24.dp)) {
                Box(Modifier.fillMaxWidth().heightIn(min = 140.dp).padding(20.dp), contentAlignment = Alignment.Center) {
                    Crossfade(selected, animationSpec = tween(180), label = "titleStylePreview") { style ->
                        EntityTitleText(text, entityKey, style, autoReferenceWidth = pageWidth)
                    }
                }
            }
            val choose: (EntityTitleStyle) -> Unit = { style ->
                if (!saving && selected != style) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedName = style.name
                }
            }
            StyleChoice(EntityTitleStyle.AUTO, selected, !saving, onClick = choose)
            listOf(EntityTitleStyle.WIDE to EntityTitleStyle.COMPACT, EntityTitleStyle.SOFT to EntityTitleStyle.ACCENT).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { StyleChoice(pair.first, selected, !saving, choose) }
                    Box(Modifier.weight(1f)) { StyleChoice(pair.second, selected, !saving, choose) }
                }
            }
            if (failed) Text(stringResource(R.string.title_style_save_failed), color = MaterialTheme.colorScheme.error)
            val source = remember { MutableInteractionSource() }
            val pressScale by rememberExpressivePressScale(source)
            Button(onClick = {
                saving = true
                failed = false
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                scope.launch {
                    try { vm.save(entityKey, selected); onDismiss() }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { failed = true }
                    finally { saving = false }
                }
            }, enabled = !saving, interactionSource = source,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).graphicsLayer { scaleX = pressScale; scaleY = pressScale }) {
                Text(stringResource(R.string.title_style_done))
            }
        }
    }
}

@Composable
private fun StyleChoice(style: EntityTitleStyle, selected: EntityTitleStyle, enabled: Boolean, onClick: (EntityTitleStyle) -> Unit) {
    val active = style == selected
    val source = remember { MutableInteractionSource() }
    val pressScale by rememberExpressivePressScale(source, 0.96f)
    Surface(shape = RoundedCornerShape(20.dp), color = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .selectable(active, enabled = enabled, role = Role.RadioButton, interactionSource = source,
                indication = ripple(), onClick = { onClick(style) })) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp).heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RadioButton(selected = active, onClick = null, enabled = enabled)
            Text(stringResource(style.labelRes), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        }
    }
}
