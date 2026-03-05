package com.app.movieit.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val MovieItDarkColorScheme = darkColorScheme(
    primary = AccentPurple,
    onPrimary = TextPrimary,

    secondary = GlowPurple,
    onSecondary = DeepBlack,

    tertiary = GoldAccent,
    onTertiary = DeepBlack,

    background = DeepBlack,
    onBackground = TextPrimary,

    surface = Color(0xFF0B0B18),
    onSurface = TextPrimary,

    surfaceVariant = Color(0xFF14142A),
    onSurfaceVariant = TextSecondary,

    outline = BorderColor,

    error = ErrorRed,
    onError = TextPrimary
)

private val MovieItLightColorScheme = lightColorScheme(
    primary = AccentPurple,
    onPrimary = TextPrimary,

    secondary = GlowPurple,
    onSecondary = DeepBlack,

    tertiary = GoldAccent,
    onTertiary = DeepBlack,

    background = DeepBlack,
    onBackground = TextPrimary,

    surface = Color(0xFF0B0B18),
    onSurface = TextPrimary,

    surfaceVariant = Color(0xFF14142A),
    onSurfaceVariant = TextSecondary,

    outline = BorderColor,

    error = ErrorRed,
    onError = TextPrimary
)

@Composable
fun MovieITTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // keep false for consistent palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> MovieItDarkColorScheme
        else -> MovieItLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}