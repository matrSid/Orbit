package com.optimusprime.orbit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// A handful of native Material3 components (checkboxes, text fields, the
// odd ripple) still read colors from a ColorScheme. This maps that scheme
// onto Orbit's own palette so those defaults never look out of place next
// to the custom components — but everything custom-drawn reads directly
// from OrbitColors, not from this.
private val orbitDarkScheme = darkColorScheme(
    primary = OrbitColors.brass,
    onPrimary = OrbitColors.ink,
    secondary = OrbitColors.moss,
    onSecondary = OrbitColors.ink,
    background = OrbitColors.ink,
    onBackground = OrbitColors.bone,
    surface = OrbitColors.surface,
    onSurface = OrbitColors.bone,
    surfaceVariant = OrbitColors.surfaceRaised,
    onSurfaceVariant = OrbitColors.ash,
    outline = OrbitColors.hairline,
    error = OrbitColors.oxide,
    onError = OrbitColors.ink
)

@Composable
fun OrbitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = orbitDarkScheme,
        content = content
    )
}
