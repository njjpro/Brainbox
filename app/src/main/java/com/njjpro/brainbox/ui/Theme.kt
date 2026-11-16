package com.njjpro.brainbox.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F6F5E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB4EBD9),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF7A5C2E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF2DFC0),
    onSecondaryContainer = Color(0xFF2A1D02),
    tertiary = Color(0xFF3D6373),
    onTertiary = Color.White,
    background = Color(0xFFFBF8F6),
    onBackground = Color(0xFF1B1C1B),
    surface = Color(0xFFFBF8F6),
    onSurface = Color(0xFF1B1C1B),
    surfaceVariant = Color(0xFFEDE7E3),
    onSurfaceVariant = Color(0xFF4A4644),
    outline = Color(0xFF7E7A78),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9BD1C0),
    onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF1B5445),
    onPrimaryContainer = Color(0xFFB4EBD9),
    secondary = Color(0xFFE0C69A),
    onSecondary = Color(0xFF402D06),
    secondaryContainer = Color(0xFF5A4419),
    onSecondaryContainer = Color(0xFFF2DFC0),
    background = Color(0xFF12100F),
    onBackground = Color(0xFFE6E2DF),
    surface = Color(0xFF12100F),
    onSurface = Color(0xFFE6E2DF),
    surfaceVariant = Color(0xFF32302E),
    onSurfaceVariant = Color(0xFFCFC7C2),
)

@Composable
fun BrainboxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
