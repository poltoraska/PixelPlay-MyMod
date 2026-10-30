@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.theveloper.pixelplay.ui.theme

import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.graphics.fonts.Font as NativeFont
import android.graphics.fonts.FontFamily as NativeFontFamily
import android.graphics.fonts.FontStyle as NativeFontStyle
import android.util.LruCache
import androidx.annotation.FontRes
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import com.theveloper.pixelplay.R
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/** Keep Flex's Latin variations, using Nunito for glyphs absent from Flex (including Cyrillic). */
internal fun GoogleSansNunitoFont(
    @FontRes resId: Int,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal,
    variationSettings: FontVariation.Settings = FontVariation.Settings(weight, style),
): Font = RoundedResourceFont(resId, weight, style, variationSettings)

private data class RoundedResourceFont(
    @FontRes val resId: Int,
    override val weight: FontWeight,
    override val style: FontStyle,
    val settings: FontVariation.Settings,
) : AndroidFont(FontLoadingStrategy.Blocking, RoundedTypefaceLoader, settings)

private object RoundedTypefaceLoader : AndroidFont.TypefaceLoader {
    private val nunitoFamilies = LruCache<Int, NativeFontFamily>(32)
    // Different variable-font axes share the same bytes. Memory-map uncompressed
    // resources where possible; use one direct buffer per resource as a fallback.
    private val fontData = LruCache<Int, ByteBuffer>(2)

    private fun fontBuffer(context: Context, @FontRes resId: Int): ByteBuffer = synchronized(fontData) {
        fontData[resId] ?: run {
            val buffer = try {
                context.resources.openRawResourceFd(resId)?.use { descriptor ->
                    FileInputStream(descriptor.fileDescriptor).channel.use { channel ->
                        channel.map(FileChannel.MapMode.READ_ONLY, descriptor.startOffset, descriptor.length)
                    }
                }
            } catch (_: Resources.NotFoundException) {
                null
            } catch (_: IOException) {
                null
            } ?: context.resources.openRawResource(resId).use { stream ->
                val bytes = stream.readBytes()
                ByteBuffer.allocateDirect(bytes.size).apply { put(bytes); flip() }.asReadOnlyBuffer()
            }
            buffer.asReadOnlyBuffer().also { fontData.put(resId, it) }
        }
    }.asReadOnlyBuffer()

    override fun loadBlocking(context: Context, font: AndroidFont): Typeface {
        val resourceFont = font as RoundedResourceFont
        val density = Density(
            context.resources.displayMetrics.density,
            context.resources.configuration.fontScale,
        )
        val axes = resourceFont.settings.settings
        val variationString = axes.joinToString(",") {
            "'${it.axisName}' ${it.toVariationValue(density)}"
        }
        val visualWeight = axes.firstOrNull { it.axisName == "wght" }
            ?.toVariationValue(density)?.toInt() ?: font.weight.weight
        // Nunito needs a slightly higher weight to match Flex's visual density.
        // Keep the requested 850 display weight exact.
        val fallbackWeight = when {
            visualWeight <= 400 -> visualWeight + 50
            visualWeight <= 700 -> visualWeight + 100
            visualWeight < 850 -> (visualWeight + 50).coerceAtMost(850)
            else -> visualWeight
        }.coerceIn(200, 1000)
        val fallbackFamily = synchronized(nunitoFamilies) {
            nunitoFamilies[fallbackWeight] ?: NativeFontFamily.Builder(
                NativeFont.Builder(context.resources, R.font.nunito_variable)
                    .setWeight(fallbackWeight)
                    .setFontVariationSettings("'wght' $fallbackWeight")
                    .build()
            ).build().also { nunitoFamilies.put(fallbackWeight, it) }
        }
        val slant = if (font.style == FontStyle.Italic) NativeFontStyle.FONT_SLANT_ITALIC
            else NativeFontStyle.FONT_SLANT_UPRIGHT
        val primaryFont = NativeFont.Builder(fontBuffer(context, resourceFont.resId))
            .setWeight(font.weight.weight)
            .setSlant(slant)
            .setFontVariationSettings(variationString)
            .build()
        return Typeface.CustomFallbackBuilder(NativeFontFamily.Builder(primaryFont).build())
            .addCustomFallback(fallbackFamily)
            .setSystemFallback("sans-serif")
            .setStyle(NativeFontStyle(font.weight.weight, slant))
            .build()
    }

    override suspend fun awaitLoad(context: Context, font: AndroidFont): Typeface =
        loadBlocking(context, font)
}
