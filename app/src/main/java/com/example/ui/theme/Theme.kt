package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val StudioDarkColorScheme = darkColorScheme(
    primary = StudioBluePrimary,
    onPrimary = StudioBgObsidian,
    primaryContainer = StudioBlueDeep,
    onPrimaryContainer = StudioCyanAccent,
    secondary = StudioCobaltSecondary,
    onSecondary = StudioTextPrimary,
    secondaryContainer = StudioSurfaceCard,
    onSecondaryContainer = StudioTextPrimary,
    tertiary = StudioCyanAccent,
    onTertiary = StudioBgObsidian,
    background = StudioBgObsidian,
    onBackground = StudioTextPrimary,
    surface = StudioSurfaceNavy,
    onSurface = StudioTextPrimary,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = StudioTextSecondary,
    surfaceContainerLowest = StudioBgObsidian,
    surfaceContainerLow = StudioSurfaceNavy,
    surfaceContainer = StudioSurfaceElevated,
    surfaceContainerHigh = StudioSurfaceCard,
    outline = StudioBorderSubtle,
    outlineVariant = StudioBorderSubtle,
    error = StudioError,
    onError = StudioBgObsidian
)

val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

@Composable
fun CSPluginStudioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StudioDarkColorScheme,
        typography = StudioTypography,
        shapes = StudioShapes,
        content = content
    )
}
