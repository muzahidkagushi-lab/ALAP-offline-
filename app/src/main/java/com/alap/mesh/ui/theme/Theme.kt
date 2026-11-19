package com.alap.mesh.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class AlapColorPalette(
    val isDark: Boolean,
    val background: Color,
    val cardSurface: Color,
    val cardBorder: Color,
    val subtleSurface: Color,
    val subtleBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textCaption: Color,
    val textPlaceholder: Color,
    val navBarSurface: Color,
    val navBarBorder: Color,
    val divider: Color
)

private val LightAlapPalette = AlapColorPalette(
    isDark = false,
    background = AlapBgLight,
    cardSurface = Color.White,
    cardBorder = Slate200.copy(alpha = 0.8f),
    subtleSurface = Slate50,
    subtleBorder = Slate200,
    textPrimary = Slate900,
    textSecondary = Slate600,
    textCaption = Slate500,
    textPlaceholder = Slate400,
    navBarSurface = Color.White.copy(alpha = 0.95f),
    navBarBorder = Slate200.copy(alpha = 0.9f),
    divider = Slate100
)

private val DarkAlapPalette = AlapColorPalette(
    isDark = true,
    background = AlapDark,
    cardSurface = AlapDarkCard,
    cardBorder = Slate800,
    subtleSurface = AlapDarkSubtle,
    subtleBorder = Slate700,
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Slate300,
    textCaption = Slate400,
    textPlaceholder = Slate500,
    navBarSurface = Color(0xFF0F172A).copy(alpha = 0.94f),
    navBarBorder = Slate800,
    divider = Slate800
)

val LocalAlapColors = staticCompositionLocalOf { LightAlapPalette }

private val AlapLightColorScheme = lightColorScheme(
    primary = AlapBlue,
    onPrimary = Color.White,
    secondary = AlapMint,
    onSecondary = Color.White,
    tertiary = Sky600,
    background = AlapBgLight,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    outline = Slate200,
    error = Rose600,
    onError = Color.White
)

private val AlapDarkColorScheme = darkColorScheme(
    primary = AlapBlue,
    onPrimary = Color.White,
    secondary = AlapMint,
    onSecondary = Color.White,
    tertiary = Sky400,
    background = AlapDark,
    onBackground = Color(0xFFF8FAFC),
    surface = AlapDarkCard,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = AlapDarkSubtle,
    onSurfaceVariant = Slate300,
    outline = Slate700,
    error = Rose600,
    onError = Color.White
)

private val AlapShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AlapTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkAlapPalette else LightAlapPalette
    val colorScheme = if (darkTheme) AlapDarkColorScheme else AlapLightColorScheme

    CompositionLocalProvider(LocalAlapColors provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AlapTypography,
            shapes = AlapShapes,
            content = content
        )
    }
}
