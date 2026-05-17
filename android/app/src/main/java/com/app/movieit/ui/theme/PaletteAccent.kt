package com.app.movieit.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AccentPalette(
    val primary: Color,
    val onPrimary: Color,
    val container: Color,
    val glow: Color
) {
    companion object {
        val Default = AccentPalette(
            primary = AccentPurple,
            onPrimary = Color.White,
            container = AccentPurple.copy(alpha = 0.15f),
            glow = GlowPurple
        )
    }
}

val LocalAccentPalette = compositionLocalOf { AccentPalette.Default }

/**
 * Returns the extracted [AccentPalette] and a Boolean that is true once extraction
 * has finished (or was skipped because imageUrl is null/blank). Gate any UI that
 * should never render in the default purple on the ready flag.
 */
@Composable
fun rememberAccentPaletteFor(imageUrl: String?): Pair<AccentPalette, Boolean> {
    val context = LocalContext.current
    // isReady starts true when there is no URL to extract from
    var palette by remember(imageUrl) { mutableStateOf(AccentPalette.Default) }
    var isReady by remember(imageUrl) { mutableStateOf(imageUrl.isNullOrBlank()) }

    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrBlank()) return@LaunchedEffect
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .allowHardware(false)
            .build()
        val result = context.imageLoader.execute(request)
        val bitmap = (result as? SuccessResult)?.drawable?.toBitmap()
        if (bitmap != null) {
            val extracted = withContext(Dispatchers.IO) {
                Palette.from(bitmap).maximumColorCount(16).generate()
            }
            val swatch = extracted.vibrantSwatch
                ?: extracted.mutedSwatch
                ?: extracted.darkVibrantSwatch
                ?: extracted.darkMutedSwatch
            if (swatch != null) {
                val primary = Color(swatch.rgb)
                palette = AccentPalette(
                    primary = primary,
                    onPrimary = Color(swatch.titleTextColor),
                    container = primary.copy(alpha = 0.15f),
                    glow = primary.copy(alpha = 0.85f)
                )
            }
        }
        isReady = true
    }

    return palette to isReady
}
