package com.optimusprime.orbit.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.optimusprime.orbit.data.db.GoalType
import com.optimusprime.orbit.ui.components.GoalRow
import com.optimusprime.orbit.ui.components.OrbitButtonFullWidth
import com.optimusprime.orbit.ui.components.OrbitCard
import com.optimusprime.orbit.ui.theme.OrbitColors
import com.optimusprime.orbit.ui.theme.OrbitType
import com.optimusprime.orbit.util.formatDurationWords
import java.time.LocalTime

private fun greeting(): String {
    val hour = LocalTime.now().hour
    return when {
        hour < 5 -> "Still up?"
        hour < 12 -> "Good morning."
        hour < 17 -> "Good afternoon."
        hour < 21 -> "Good evening."
        else -> "Late one tonight."
    }
}

@Composable
fun HomeScreen(
    onStartStudying: () -> Unit,
    onSeeAllGoals: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(OrbitColors.ink),
        contentPadding = WindowInsets.statusBars.asPaddingValues().let { insets ->
            androidx.compose.foundation.layout.PaddingValues(
                top    = insets.calculateTopPadding() + 20.dp,
                start  = 20.dp,
                end    = 20.dp,
                bottom = 20.dp
            )
        },
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column {
                Text(text = greeting(), style = OrbitType.headline, color = OrbitColors.bone)
                Text(
                    text = "Let's get some focused time in.",
                    style = OrbitType.body,
                    color = OrbitColors.ash
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatChip(
                    value = formatDurationWords(state.todayActiveMillis),
                    label = "today",
                    modifier = Modifier.weight(1f)
                )
                StatChip(
                    value = state.todayQuestions.toString(),
                    label = "questions",
                    modifier = Modifier.weight(1f)
                )
                StatChip(
                    value = state.streak.toString(),
                    label = if (state.streak == 1) "day streak" else "day streak",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            OrbitButtonFullWidth(text = "Start studying", onClick = onStartStudying)
        }

        item {
            OrbitCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Today's goals", style = OrbitType.title, color = OrbitColors.bone)
                    Text(
                        text = "View all",
                        style = OrbitType.label,
                        color = OrbitColors.brass,
                        modifier = Modifier
                            .clickable(onClick = onSeeAllGoals)
                            .padding(4.dp)
                    )
                }
                if (state.topGoals.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Nothing pending — add a goal or some homework from the Goals tab.",
                        style = OrbitType.body,
                        color = OrbitColors.ashFaint
                    )
                } else {
                    state.topGoals.forEach { goal ->
                        GoalRow(
                            title = goal.title,
                            typeLabel = if (goal.type == GoalType.HOMEWORK) "Homework" else "Goal",
                            isDone = goal.isDone,
                            onToggle = { viewModel.toggleGoal(goal) },
                            onDelete = {}
                        )
                    }
                }
            }
        }

        item {
            OrbitCard {
                Text(text = "Worth trying today", style = OrbitType.label, color = OrbitColors.brassDim)
                Spacer(Modifier.height(6.dp))
                Text(text = state.tip, style = OrbitType.italicNote, color = OrbitColors.bone)
            }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String, modifier: Modifier = Modifier) {
    OrbitCard(modifier = modifier, padding = 14.dp) {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(text = value, style = OrbitType.statNumber, color = OrbitColors.brass, textAlign = TextAlign.Center)
            Text(text = label, style = OrbitType.caption, color = OrbitColors.ash, textAlign = TextAlign.Center)
        }
    }
}

