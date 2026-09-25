package com.optimusprime.wick.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.optimusprime.wick.ui.theme.WickColors
import com.optimusprime.wick.ui.theme.WickShapes
import com.optimusprime.wick.ui.theme.WickType

enum class WickButtonStyle { PRIMARY, SECONDARY, GHOST }

/**
 * One button component, three looks — filled brass for the one action per
 * screen that matters, an outlined version for the alternative, and a
 * borderless ghost for anything low-stakes. No default Material ripple
 * shape/color fighting the rest of the palette.
 */
@Composable
fun WickButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: WickButtonStyle = WickButtonStyle.PRIMARY,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val background = when {
        !enabled -> WickColors.surfaceRaised
        style == WickButtonStyle.PRIMARY -> WickColors.brass
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    val textColor = when {
        !enabled -> WickColors.ashFaint
        style == WickButtonStyle.PRIMARY -> WickColors.ink
        style == WickButtonStyle.SECONDARY -> WickColors.bone
        else -> WickColors.brass
    }

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(WickShapes.button)
            .background(background)
            .then(
                if (style == WickButtonStyle.SECONDARY) {
                    Modifier.border(1.dp, WickColors.hairline, WickShapes.button)
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = WickType.button, color = textColor)
    }
}

@Composable
fun WickButtonFullWidth(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: WickButtonStyle = WickButtonStyle.PRIMARY,
    enabled: Boolean = true
) {
    WickButton(text, onClick, modifier.fillMaxWidth(), style, enabled)
}
