package com.optimusprime.orbit.ui.components

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
import com.optimusprime.orbit.ui.theme.OrbitColors
import com.optimusprime.orbit.ui.theme.OrbitShapes
import com.optimusprime.orbit.ui.theme.OrbitType

enum class OrbitButtonStyle { PRIMARY, SECONDARY, GHOST }

/**
 * One button component, three looks — filled brass for the one action per
 * screen that matters, an outlined version for the alternative, and a
 * borderless ghost for anything low-stakes. No default Material ripple
 * shape/color fighting the rest of the palette.
 */
@Composable
fun OrbitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: OrbitButtonStyle = OrbitButtonStyle.PRIMARY,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val background = when {
        !enabled -> OrbitColors.surfaceRaised
        style == OrbitButtonStyle.PRIMARY -> OrbitColors.brass
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    val textColor = when {
        !enabled -> OrbitColors.ashFaint
        style == OrbitButtonStyle.PRIMARY -> OrbitColors.ink
        style == OrbitButtonStyle.SECONDARY -> OrbitColors.bone
        else -> OrbitColors.brass
    }

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(OrbitShapes.button)
            .background(background)
            .then(
                if (style == OrbitButtonStyle.SECONDARY) {
                    Modifier.border(1.dp, OrbitColors.hairline, OrbitShapes.button)
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
        Text(text = text, style = OrbitType.button, color = textColor)
    }
}

@Composable
fun OrbitButtonFullWidth(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: OrbitButtonStyle = OrbitButtonStyle.PRIMARY,
    enabled: Boolean = true
) {
    OrbitButton(text, onClick, modifier.fillMaxWidth(), style, enabled)
}
