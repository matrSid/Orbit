package com.optimusprime.orbit.ui.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.optimusprime.orbit.ui.components.WeekBars
import com.optimusprime.orbit.ui.components.OrbitCard
import com.optimusprime.orbit.ui.theme.OrbitColors
import com.optimusprime.orbit.ui.theme.OrbitType
import com.optimusprime.orbit.util.formatDurationWords

@Composable
fun ProgressScreen(viewModel: ProgressViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OrbitColors.ink)
            // Same fix as GoalsScreen — this screen had no status-bar inset
            // at all, so "Progress" rendered right at (or under) the top edge
            // instead of clearing the status bar like Home/Study do.
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text(text = "Progress", style = OrbitType.headline, color = OrbitColors.bone)
        }

        item {
            OrbitCard {
                Text(text = "This week", style = OrbitType.title, color = OrbitColors.bone)
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
            Text(text = "Achievements", style = OrbitType.title, color = OrbitColors.bone)
        }

        items(state.achievements.size) { index ->
            val achievement = state.achievements[index]
            AchievementRow(achievement.title, achievement.description, achievement.unlocked)
        }
    }
}

@Composable
private fun SummaryTile(label: String, value: String, modifier: Modifier = Modifier) {
    OrbitCard(modifier = modifier, padding = 14.dp) {
        Column {
            Text(text = value, style = OrbitType.statNumber, color = OrbitColors.brass)
            Text(text = label, style = OrbitType.caption, color = OrbitColors.ash)
        }
    }
}

@Composable
private fun AchievementRow(title: String, description: String, unlocked: Boolean) {
    OrbitCard(modifier = Modifier.fillMaxWidth(), padding = 14.dp) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(if (unlocked) OrbitColors.brass else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                    .border(1.5.dp, if (unlocked) OrbitColors.brass else OrbitColors.hairline, CircleShape)
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = OrbitType.bodyStrong,
                    color = if (unlocked) OrbitColors.bone else OrbitColors.ashFaint
                )
                Text(
                    text = description,
                    style = OrbitType.caption,
                    color = OrbitColors.ashFaint
                )
            }
        }
    }
}