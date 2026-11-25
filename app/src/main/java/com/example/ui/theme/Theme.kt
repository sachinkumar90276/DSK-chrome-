package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.ThemeMode

private val DskDarkColorScheme = darkColorScheme(
    primary = DskCyan,
    onPrimary = Color(0xFF001F2A),
    primaryContainer = Color(0xFF0A324D),
    onPrimaryContainer = Color(0xFFB8EAFF),
    secondary = DskCobalt,
    onSecondary = Color.White,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkOnSurface,
    tertiary = Color(0xFF64FFDA),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerHigh = DarkSurfaceHigh,
    outline = DarkOutline,
    outlineVariant = Color(0xFF1C2740),
    error = DangerRed,
    onError = Color.White
)

private val DskLightColorScheme = lightColorScheme(
    primary = DskCobaltDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8FF),
    onPrimaryContainer = Color(0xFF001B4D),
    secondary = DskTeal,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = LightOnSurface,
    tertiary = Color(0xFF006D77),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerHigh = LightSurfaceHigh,
    outline = LightOutline,
    outlineVariant = Color(0xFFE0E7F3),
    error = DangerRed,
    onError = Color.White
)

private val DskIncognitoColorScheme = darkColorScheme(
    primary = IncognitoPrimary,
    onPrimary = Color(0xFF210042),
    primaryContainer = Color(0xFF382454),
    onPrimaryContainer = Color(0xFFF0DFFF),
    secondary = IncognitoSecondary,
    onSecondary = Color(0xFF002028),
    secondaryContainer = IncognitoSurfaceVariant,
    onSecondaryContainer = IncognitoOnSurface,
    background = IncognitoBackground,
    onBackground = IncognitoOnSurface,
    surface = IncognitoSurface,
    onSurface = IncognitoOnSurface,
    surfaceVariant = IncognitoSurfaceVariant,
    onSurfaceVariant = IncognitoOnSurfaceVariant,
    surfaceContainerHigh = IncognitoSurfaceHigh,
    outline = IncognitoOutline,
    outlineVariant = Color(0xFF29213E),
    error = DangerRed,
    onError = Color.White
)

val DskShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DskChromeTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    isIncognito: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when {
        isIncognito -> true
        themeMode == ThemeMode.DARK -> true
        themeMode == ThemeMode.LIGHT -> false
        else -> systemDark
    }

    val colorScheme = when {
        isIncognito -> DskIncognitoColorScheme
        useDark -> DskDarkColorScheme
        else -> DskLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DskTypography,
        shapes = DskShapes,
        content = content
    )
}
