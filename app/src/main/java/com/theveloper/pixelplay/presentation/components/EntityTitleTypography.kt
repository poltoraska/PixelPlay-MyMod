@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
package com.theveloper.pixelplay.presentation.components

import android.util.LruCache
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.util.lerp
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.preferences.*
import com.theveloper.pixelplay.ui.theme.GoogleSansNunitoFont

private data class TitleFontKey(val cyrillic: Boolean, val weight: Int, val width: Int, val roundness: Int, val opticalSize: Int)
private val titleFontCache = LruCache<TitleFontKey, FontFamily>(128)
private fun titleFont(cyrillic: Boolean, weight: Float, width: Float, roundness: Float, opticalSize: Float): FontFamily {
    val key = TitleFontKey(cyrillic, (weight / 50).toInt() * 50, (width / 5).toInt() * 5,
        (roundness / 10).toInt() * 10, (opticalSize / 2).toInt() * 2)
    return synchronized(titleFontCache) {
        titleFontCache[key] ?: FontFamily(
            if (cyrillic) Font(R.font.nunito_variable, FontWeight(key.weight), variationSettings = FontVariation.Settings(FontVariation.weight(key.weight)))
            else GoogleSansNunitoFont(R.font.gflex_variable, FontWeight(key.weight), variationSettings = FontVariation.Settings(
                FontVariation.weight(key.weight), FontVariation.width(key.width.toFloat()),
                FontVariation.Setting("ROND", key.roundness.toFloat()), FontVariation.Setting("opsz", key.opticalSize.toFloat())
            ))
        ).also { titleFontCache.put(key, it) }
    }
}

private data class TitleProfile(val size: Float, val weight: Float, val width: Float, val roundness: Float)
private fun titleProfile(style: EntityTitleStyle) = when (style) {
    EntityTitleStyle.WIDE -> TitleProfile(42f, 800f, 125f, 60f)
    EntityTitleStyle.COMPACT -> TitleProfile(36f, 750f, 78f, 30f)
    EntityTitleStyle.ACCENT -> TitleProfile(40f, 950f, 110f, 10f)
    else -> TitleProfile(38f, 700f, 100f, 100f)
}

/** Both the page and its preview use identical fitting and script fallback rules. */
@Composable
internal fun EntityTitleText(
    text: String,
    identity: String,
    selection: EntityTitleStyle,
    collapseFraction: Float = 0f,
    expandedWidth: Dp? = null,
    modifier: Modifier = Modifier,
    autoReferenceWidth: Dp? = null,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val cyrillic = remember(text) { titleUsesCyrillic(text) }
    val color = MaterialTheme.colorScheme.onSurface
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val referenceWidth = with(density) { (expandedWidth ?: maxWidth).roundToPx().coerceAtLeast(1) }
        val autoWidth = with(density) { (autoReferenceWidth ?: expandedWidth ?: maxWidth).roundToPx().coerceAtLeast(1) }
        val referenceFont = remember(cyrillic) { titleFont(cyrillic, 700f, 100f, 100f, 38f) }
        val measuredWidth = remember(text, referenceFont, density) {
            measurer.measure(AnnotatedString(text), TextStyle(fontFamily = referenceFont, fontSize = 38.sp), softWrap = false, maxLines = 1).size.width
        }
        val resolved = if (selection == EntityTitleStyle.AUTO) automaticEntityTitleStyle(measuredWidth.toFloat(), autoWidth.toFloat(), identity) else selection
        val profile = titleProfile(resolved)
        val expandedFont = remember(cyrillic, resolved) { titleFont(cyrillic, profile.weight, profile.width, profile.roundness, profile.size) }
        val fittedSize = remember(text, resolved, expandedFont, referenceWidth, density) {
            var low = 18f
            var high = profile.size
            repeat(7) {
                val candidate = (low + high) / 2
                val result = measurer.measure(AnnotatedString(text),
                    TextStyle(fontFamily = expandedFont, fontSize = candidate.sp, lineHeight = (candidate * 1.06f).sp),
                    constraints = Constraints(maxWidth = referenceWidth), maxLines = 2, overflow = TextOverflow.Clip)
                if (result.hasVisualOverflow) high = candidate else low = candidate
            }
            low
        }
        val fraction = collapseFraction.coerceIn(0f, 1f)
        val size = lerp(fittedSize, 18f, fraction)
        val font = titleFont(cyrillic, lerp(profile.weight, 850f, fraction),
            lerp(profile.width, 100f, fraction), lerp(profile.roundness, 100f, fraction), size)
        Text(text, fontFamily = font, fontWeight = FontWeight((lerp(profile.weight, 850f, fraction) / 50).toInt() * 50),
            fontSize = size.sp, lineHeight = (size * 1.06f).sp, color = color,
            maxLines = if (fraction < 0.55f) 2 else 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth())
    }
}
