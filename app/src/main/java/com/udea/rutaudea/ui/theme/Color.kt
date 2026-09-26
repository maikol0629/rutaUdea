package com.udea.rutaudea.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Forest Green Palette
val ForestGreen = Color(0xFF1B4D3E)
val ForestGreenLight = Color(0xFF2D6A4F)
val ForestGreenContainer = Color(0xFFD8F3DC)
val OnForestGreen = Color.White
val OnForestGreenContainer = Color(0xFF1B4D3E)

// Blue Accent Palette
val AccentBlue = Color(0xFF2196F3)
val AccentBlueContainer = Color(0xFFBBDEFB)
val OnAccentBlue = Color.White
val OnAccentBlueContainer = Color(0xFF0D47A1)

// Surface
val SurfaceWhite = Color.White
val OnSurface = Color(0xFF1C1B1F)
val SurfaceVariant = Color(0xFFF5F5F5)
val OnSurfaceVariant = Color(0xFF49454F)

// Outline
val Outline = Color(0xFF79747E)
val OutlineVariant = Color(0xFFCAC4D0)

// Status
val Success = Color(0xFF2E7D32)
val SuccessContainer = Color(0xFFC8E6C9)
val Error = Color(0xFFC62828)
val ErrorContainer = Color(0xFFFFCDD2)
val Warning = Color(0xFFF57F17)
val WarningContainer = Color(0xFFFFF8E1)

// Background
val BackgroundColor = Color(0xFFFDFDFD)
val OnBackground = Color(0xFF1C1B1F)

private val DarkColorPalette = darkColorScheme(
    primary = ForestGreenLight,
    onPrimary = OnForestGreen,
    primaryContainer = ForestGreen,
    onPrimaryContainer = ForestGreenContainer,
    secondary = AccentBlue,
    onSecondary = OnAccentBlue,
    secondaryContainer = AccentBlue,
    onSecondaryContainer = AccentBlueContainer,
    surface = Color(0xFF1C1B1F),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    background = Color(0xFF1C1B1F),
    onBackground = Color.White,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF938F99),
)

private val LightColorPalette = lightColorScheme(
    primary = ForestGreen,
    onPrimary = OnForestGreen,
    primaryContainer = ForestGreenContainer,
    onPrimaryContainer = OnForestGreenContainer,
    secondary = AccentBlue,
    onSecondary = OnAccentBlue,
    secondaryContainer = AccentBlueContainer,
    onSecondaryContainer = OnAccentBlueContainer,
    surface = SurfaceWhite,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    background = BackgroundColor,
    onBackground = OnBackground,
    error = Error,
    onError = OnForestGreen,
    errorContainer = ErrorContainer,
    onErrorContainer = Error,
    outline = Outline,
)

val RutaUdeaLightColorScheme = LightColorPalette
val RutaUdeaDarkColorScheme = DarkColorPalette