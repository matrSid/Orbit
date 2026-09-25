package com.optimusprime.wick.ui.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.optimusprime.wick.ui.components.WeekBars
import com.optimusprime.wick.ui.components.WickCard
import com.optimusprime.wick.ui.theme.WickColors
import com.optimusprime.wick.ui.theme.WickType
import com.optimusprime.wick.util.formatDurationWords

@Composable
fun ProgressScreen(viewModel: ProgressViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(WickColors.ink),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text(text = "Progress", style = WickType.headline, color = WickColors.bone)
        }

        item {
            WickCard {
                Text(text = "This week", style = WickType.title, color = WickColors.bone)
                Spacer(Modifier.height(12.dp))
                WeekBars(days = state.weekBars, modifier = Modifier.fillMaxWidth())
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryTile("Total time", formatDurationWords(state.totalActiveMillis), Modifier.weight(1f))
                SummaryTile("Sessions", state.totalSessions.toString(), Modifier.weight(1f))
                SummaryTile("Streak", "${state.streak}d", Modifier.weight(1f))
            }
        }

        item {
            Text(text = "Achievements", style = WickType.title, color = WickColors.bone)
        }

        items(state.achievements.size) { index ->
            val achievement = state.achievements[index]
            AchievementRow(achievement.title, achievement.description, achievement.unlocked)
        }
    }
}

@Composable
private fun SummaryTile(label: String, value: String, modifier: Modifier = Modifier) {
    WickCard(modifier = modifier, padding = 14.dp) {
        Column {
            Text(text = value, style = WickType.statNumber, color = WickColors.brass)
            Text(text = label, style = WickType.caption, color = WickColors.ash)
        }
    }
}

@Composable
private fun AchievementRow(title: String, description: String, unlocked: Boolean) {
    WickCard(modifier = Modifier.fillMaxWidth(), padding = 14.dp) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(if (unlocked) WickColors.brass else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                    .border(1.5.dp, if (unlocked) WickColors.brass else WickColors.hairline, CircleShape)
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = WickType.bodyStrong,
                    color = if (unlocked) WickColors.bone else WickColors.ashFaint
                )
                Text(
                    text = description,
                    style = WickType.caption,
                    color = WickColors.ashFaint
                )
            }
        }
    }
}
