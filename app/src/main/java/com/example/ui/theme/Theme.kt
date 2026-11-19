package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val PulseDarkColorScheme = darkColorScheme(
    primary = PulsePrimary,
    onPrimary = PulseBackground,
    primaryContainer = PulsePrimaryContainer,
    onPrimaryContainer = PulseTextPrimary,
    secondary = PulseSecondary,
    onSecondary = PulseBackground,
    secondaryContainer = PulseSecondaryContainer,
    onSecondaryContainer = PulseTextPrimary,
    tertiary = PulseEmerald,
    onTertiary = PulseBackground,
    background = PulseBackground,
    onBackground = PulseTextPrimary,
    surface = PulseSurface1,
    onSurface = PulseTextPrimary,
    surfaceVariant = PulseSurface2,
    onSurfaceVariant = PulseTextMuted,
    surfaceContainerLowest = PulseBackground,
    surfaceContainerLow = PulseSurface1,
    surfaceContainer = PulseSurface2,
    surfaceContainerHigh = PulseSurface3,
    surfaceContainerHighest = PulseSurface3,
    outline = PulseGlassBorder,
    outlineVariant = PulseGlassHighlight,
    error = PulseDanger,
    onError = PulseBackground
)

val PulseShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun PulseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PulseDarkColorScheme,
        typography = PulseTypography,
        shapes = PulseShapes,
        content = content
    )
}
