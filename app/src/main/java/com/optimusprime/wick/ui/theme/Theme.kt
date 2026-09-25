package com.optimusprime.wick.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// A handful of native Material3 components (checkboxes, text fields, the
// odd ripple) still read colors from a ColorScheme. This maps that scheme
// onto Wick's own palette so those defaults never look out of place next
// to the custom components — but everything custom-drawn reads directly
// from WickColors, not from this.
private val wickDarkScheme = darkColorScheme(
    primary = WickColors.brass,
    onPrimary = WickColors.ink,
    secondary = WickColors.moss,
    onSecondary = WickColors.ink,
    background = WickColors.ink,
    onBackground = WickColors.bone,
    surface = WickColors.surface,
    onSurface = WickColors.bone,
    surfaceVariant = WickColors.surfaceRaised,
    onSurfaceVariant = WickColors.ash,
    outline = WickColors.hairline,
    error = WickColors.oxide,
    onError = WickColors.ink
)

@Composable
fun WickTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = wickDarkScheme,
        content = content
    )
}
