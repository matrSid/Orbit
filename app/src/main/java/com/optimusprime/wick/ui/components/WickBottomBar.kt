package com.optimusprime.wick.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.optimusprime.wick.ui.theme.WickColors
import com.optimusprime.wick.ui.theme.WickType
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Text tabs with a sliding underline instead of an icon rail — closer to
 * the dividers on a planner's tab index than to a stock Material nav bar.
 */
@Composable
fun WickBottomBar(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(WickColors.surface)
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(WickColors.hairline))
        BoxWithConstraints(Modifier.fillMaxWidth().navigationBarsPadding()) {
            val tabWidth = maxWidth / items.size
            val indicatorOffset by animateDpAsState(
                targetValue = tabWidth * selectedIndex,
                label = "navIndicator"
            )
            Column {
                Row(Modifier.fillMaxWidth().height(60.dp)) {
                    val interactionSource = remember { MutableInteractionSource() }
                    items.forEachIndexed { index, label ->
                        val selected = index == selectedIndex
                        Box(
                            modifier = Modifier
                                .width(tabWidth)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) { onSelect(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = if (selected) WickType.bodyStrong else WickType.body,
                                color = if (selected) WickColors.brass else WickColors.ash
                            )
                        }
                    }
                }
                Box(
                    Modifier
                        .offset(x = indicatorOffset)
                        .width(tabWidth)
                        .height(2.dp)
                        .background(WickColors.brass)
                )
            }
        }
    }
}
