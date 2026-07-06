package com.doge.simulator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SpaceColorScheme = darkColorScheme(
    primary = SpaceAccent,
    onPrimary = TextPrimary,
    primaryContainer = SpaceMid,
    onPrimaryContainer = TextPrimary,
    secondary = GoldAccent,
    onSecondary = SpaceDark,
    secondaryContainer = SpaceMid,
    onSecondaryContainer = GoldAccent,
    background = SpaceDark,
    onBackground = TextPrimary,
    surface = SpaceNavy,
    onSurface = TextPrimary,
    surfaceVariant = SpaceMid,
    onSurfaceVariant = TextSecondary,
    error = StatusRed,
    onError = TextPrimary,
    outline = SpaceBlue
)

@Composable
fun DogeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SpaceColorScheme,
        typography = PixelTypography,
        content = content
    )
}
