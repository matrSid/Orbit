package com.optimusprime.orbit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.optimusprime.orbit.ui.theme.OrbitColors
import com.optimusprime.orbit.ui.theme.OrbitShapes

/**
 * A flat surface with a single hairline border — no elevation, no drop
 * shadow. Every card in Orbit looks like this; nothing looks like it's
 * floating above anything else.
 */
@Composable
fun OrbitCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = OrbitShapes.card,
    padding: Dp = 18.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .clip(shape)
            .background(OrbitColors.surface)
            .border(1.dp, OrbitColors.hairline, shape)
            .padding(padding)
    ) {
        content()
    }
}
