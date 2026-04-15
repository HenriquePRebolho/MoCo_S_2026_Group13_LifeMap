package com.example.livemap.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light color scheme using LifeMap's Figma palette.
private val LifeMapLightScheme = lightColorScheme(
    primary = GreenDark,                // Main action color (darkgreen)
    onPrimary = Color.White,            // Text on top of primary
    primaryContainer = GreenLight,      // Lighter variant (lightgreen)
    onPrimaryContainer = TextPrimary,   // Text on primaryContainer

    secondary = CategoryArt,            // Secondary accent (pink)
    onSecondary = TextPrimary,

    background = BackgroundWarm,        // App background (#F5EEE6)
    onBackground = TextPrimary,         // Text on background

    surface = SurfaceWhite,             // Card/sheet background
    onSurface = TextPrimary,            // Text on surfaces
    surfaceVariant = BackgroundGold,    // Alternate surface (#FFF4D6)
    onSurfaceVariant = TextSecondary,   // Secondary text on surfaces

    outline = BorderLight,              // Borders and dividers
)

// reuse the light scheme for dark mode.
// TODO: create dark theme
private val LifeMapDarkScheme = darkColorScheme(
    primary = GreenLight,
    onPrimary = TextPrimary,
    primaryContainer = GreenDark,
    onPrimaryContainer = Color.White,

    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),

    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
)

@Composable
fun LiveMapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // skip dynamic color (for now)
    // TODO: implement dynamic color
    val colorScheme = if (darkTheme) LifeMapDarkScheme else LifeMapLightScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}