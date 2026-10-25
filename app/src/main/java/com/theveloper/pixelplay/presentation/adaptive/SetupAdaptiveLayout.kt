package com.theveloper.pixelplay.presentation.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class SetupPageLayoutInfo(
    val twoPane: Boolean,
    val contentWidth: Float,
    val horizontalPadding: Float,
    val paneGap: Float,
    val visualHeight: Float,
)

/** Use the actual pager viewport, including split-screen and accessibility font scaling. */
internal fun calculateSetupPageLayout(width: Float, height: Float, fontScale: Float = 1f): SetupPageLayoutInfo {
    val safeWidth = width.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    val safeHeight = height.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    val scale = fontScale.takeIf { it.isFinite() && it > 0f } ?: 1f
    val twoPane = safeWidth >= 840f && safeWidth / scale >= 720f && safeHeight >= 280f
    return SetupPageLayoutInfo(
        twoPane = twoPane,
        contentWidth = safeWidth.coerceAtMost(if (twoPane) 1120f else 600f),
        horizontalPadding = if (twoPane) 24f else 20f,
        paneGap = if (twoPane) 32f else 24f,
        visualHeight = if (twoPane) 280f else (safeHeight * 0.36f).coerceIn(200f, 260f),
    )
}

private val LocalSetupWindowWidth = staticCompositionLocalOf { 0f }
internal val LocalSetupTwoPane = staticCompositionLocalOf { false }
internal val LocalSetupTablet = staticCompositionLocalOf { false }

@Composable
internal fun SetupAdaptiveHost(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        CompositionLocalProvider(
            LocalSetupWindowWidth provides maxWidth.value,
            LocalSetupTablet provides (maxWidth >= 600.dp),
        ) { content() }
    }
}

@Composable
internal fun SetupBottomBarFrame(content: @Composable () -> Unit) {
    val width = LocalSetupWindowWidth.current
    val density = LocalDensity.current
    val maxWidth = if (width >= 840f && width / density.fontScale >= 720f) 1120.dp else 600.dp
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.widthIn(max = maxWidth).fillMaxWidth()) { content() }
    }
}

/** All setup steps share one bounded, scrollable layout instead of stretching separate columns. */
@Composable
internal fun SetupPageFrame(
    header: @Composable () -> Unit,
    visual: @Composable () -> Unit,
    actions: @Composable () -> Unit = {},
) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val info = calculateSetupPageLayout(maxWidth.value, maxHeight.value, LocalDensity.current.fontScale)
        CompositionLocalProvider(LocalSetupTwoPane provides info.twoPane) {
            BoxWithConstraints(
                Modifier.width(info.contentWidth.dp).fillMaxHeight()
                    .padding(horizontal = info.horizontalPadding.dp, vertical = 16.dp)
            ) {
                val viewportHeight = maxHeight
                if (info.twoPane) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(info.paneGap.dp)) {
                        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().height(viewportHeight.coerceAtMost(420.dp)),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(36.dp),
                            ) {
                                Box(Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                                    Box(Modifier.widthIn(max = 420.dp).fillMaxWidth().height(info.visualHeight.dp),
                                        contentAlignment = Alignment.Center) { visual() }
                                }
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f).fillMaxHeight()
                                .verticalScroll(rememberScrollState()).heightIn(min = viewportHeight)
                                .padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
                        ) {
                            header()
                            actions()
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                            .heightIn(min = viewportHeight).padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
                    ) {
                        header()
                        Box(Modifier.widthIn(max = 400.dp).fillMaxWidth().height(info.visualHeight.dp),
                            contentAlignment = Alignment.Center) { visual() }
                        actions()
                    }
                }
            }
        }
    }
}

@Composable
internal fun SetupPageHeading(title: String, description: String) {
    val alignment = if (LocalSetupTwoPane.current) TextAlign.Start else TextAlign.Center
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineLarge.copy(
            fontSize = 32.sp, fontWeight = FontWeight.ExtraBold), textAlign = alignment)
        Text(description, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = alignment)
    }
}

/** Dialog content stays full-screen on phones and becomes a bounded modal during tablet setup. */
@Composable
internal fun SetupModalFrame(content: @Composable () -> Unit) {
    if (!LocalSetupTablet.current) {
        content()
        return
    }
    Box(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.union(WindowInsets.ime))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.widthIn(max = 840.dp).fillMaxWidth().fillMaxHeight(0.94f)
            .clip(RoundedCornerShape(32.dp))) { content() }
    }
}
