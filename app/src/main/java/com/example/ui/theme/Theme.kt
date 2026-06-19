package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = GeminiDarkPrimary,
    onPrimary = GeminiDarkOnPrimary,
    primaryContainer = GeminiDarkPrimaryContainer,
    onPrimaryContainer = GeminiDarkOnPrimaryContainer,
    background = GeminiDarkBackground,
    onBackground = GeminiDarkOnBackground,
    surface = GeminiDarkSurface,
    onSurface = GeminiDarkOnBackground,
    surfaceVariant = GeminiDarkSurfaceVariant,
    onSurfaceVariant = GeminiDarkTextSubtle,
    outline = GeminiDarkTextSubtle.copy(alpha = 0.5f)
)

private val LightColorScheme = lightColorScheme(
    primary = GeminiLightPrimary,
    onPrimary = GeminiLightOnPrimary,
    primaryContainer = GeminiLightPrimaryContainer,
    onPrimaryContainer = GeminiLightOnPrimaryContainer,
    background = GeminiLightBackground,
    onBackground = GeminiLightOnBackground,
    surface = GeminiLightSurface,
    onSurface = GeminiLightOnBackground,
    surfaceVariant = GeminiLightSurfaceVariant,
    onSurfaceVariant = GeminiLightTextSubtle,
    outline = GeminiLightTextSubtle.copy(alpha = 0.5f)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
