package com.theveloper.pixelplay.presentation.adaptive

import android.os.SystemClock
import android.view.SoundEffectConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuOpen
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.theveloper.pixelplay.BottomNavItem
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.presentation.navigation.Screen
import com.theveloper.pixelplay.presentation.navigation.navigateToTopLevelSafely
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun AdaptiveNavigationPanel(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    navItems: ImmutableList<BottomNavItem>,
    currentRoute: String?,
    navController: NavHostController,
    onBeforeNavigate: () -> Unit,
    onSearchDoubleTap: () -> Unit,
) {
    var lastSearchTap by remember { mutableLongStateOf(0L) }
    val destinations = remember(navItems) {
        navItems + BottomNavItem("Settings", R.string.common_settings,
            R.drawable.rounded_settings_24, R.drawable.rounded_settings_24, Screen.Settings)
    }
    Surface(Modifier.fillMaxSize().clipToBounds(), color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.wrapContentWidth(Alignment.Start, unbounded = true)
                .requiredWidth(if (expanded) 256.dp else 80.dp)
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (expanded) 20.dp else 8.dp),
            horizontalAlignment = if (expanded) Alignment.Start else Alignment.CenterHorizontally,
        ) {
            IconButton(
                onClick = onToggleExpanded,
                modifier = Modifier.padding(top = 8.dp).size(48.dp),
            ) {
                Icon(
                    if (expanded) Icons.AutoMirrored.Rounded.MenuOpen else Icons.Rounded.Menu,
                    stringResource(if (expanded) R.string.common_collapse_menu else R.string.common_expand_menu),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(32.dp))
            Column(
                Modifier.fillMaxWidth().selectableGroup().padding(bottom = 16.dp),
                horizontalAlignment = if (expanded) Alignment.Start else Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                destinations.forEach { item ->
                    val selected = currentRoute == item.screen.route ||
                        (item.screen == Screen.Library && isLibraryDetailRoute(currentRoute)) ||
                        (item.screen == Screen.Settings && isSettingsDestination(currentRoute))
                    RailDestination(item, selected, expanded) {
                        onBeforeNavigate()
                        if (item.screen == Screen.Search) {
                            val now = SystemClock.elapsedRealtime()
                            if (selected && now - lastSearchTap <= 350L) onSearchDoubleTap()
                            lastSearchTap = now
                        } else lastSearchTap = 0L
                        navController.navigateToTopLevelSafely(item.screen.route)
                    }
                }
            }
        }
    }
}

@Composable
private fun RailDestination(item: BottomNavItem, selected: Boolean, expanded: Boolean, onClick: () -> Unit) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, spring(), label = "railPress")
    val background by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, label = "railBackground",
    )
    val tint by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "railTint",
    )
    val label = stringResource(item.labelResId)
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val clickModifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }.selectable(
        selected = selected,
        interactionSource = interactions,
        indication = null,
        role = Role.Tab,
        onClick = {
            if (!selected) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            view.playSoundEffect(SoundEffectConstants.CLICK)
            onClick()
        },
    )
    val icon: @Composable () -> Unit = {
        Icon(painterResource(if (selected) item.selectedIconResId ?: item.iconResId else item.iconResId),
            contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
    }
    if (expanded) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).then(clickModifier)
                .clip(RoundedCornerShape(28.dp)).background(background)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            icon()
            Text(label, color = tint, style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold)
        }
    } else {
        Column(Modifier.width(64.dp).then(clickModifier), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(56.dp, 40.dp).clip(RoundedCornerShape(20.dp)).background(background),
                contentAlignment = Alignment.Center) { icon() }
            Spacer(Modifier.height(4.dp))
            Text(label, modifier = Modifier.requiredWidth(80.dp),
                color = if (selected) MaterialTheme.colorScheme.onSurface else tint,
                style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
                maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold)
        }
    }
}
