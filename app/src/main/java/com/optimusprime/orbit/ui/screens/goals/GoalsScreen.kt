package com.optimusprime.orbit.ui.screens.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.optimusprime.orbit.data.db.GoalEntity
import com.optimusprime.orbit.data.db.GoalType
import com.optimusprime.orbit.ui.components.GoalRow
import com.optimusprime.orbit.ui.components.OrbitButton
import com.optimusprime.orbit.ui.theme.OrbitColors
import com.optimusprime.orbit.ui.theme.OrbitShapes
import com.optimusprime.orbit.ui.theme.OrbitType

private enum class GoalFilter { ALL, GOALS, HOMEWORK }

@Composable
fun GoalsScreen(viewModel: GoalsViewModel = viewModel()) {
    val goals by viewModel.goals.collectAsState()
    var filter by remember { mutableStateOf(GoalFilter.ALL) }
    var newTitle by remember { mutableStateOf("") }
    var newType by remember { mutableStateOf(GoalType.GOAL) }

    val visible = when (filter) {
        GoalFilter.ALL -> goals
        GoalFilter.GOALS -> goals.filter { it.type == GoalType.GOAL }
        GoalFilter.HOMEWORK -> goals.filter { it.type == GoalType.HOMEWORK }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OrbitColors.ink)
            // This was missing — Home and Study both clear the status bar via
            // windowInsetsPadding/contentPadding, but Goals and Progress had
            // no inset at all, so their heading sat right up against (or
            // under) the status bar. That's the "heading is too high" bug.
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Goals & homework", style = OrbitType.headline, color = OrbitColors.bone)
            Spacer(Modifier.height(14.dp))

            // Add new item
            OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Finish chapter 4 exercises…", style = OrbitType.body) },
                singleLine = true
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeToggle("Goal", newType == GoalType.GOAL) { newType = GoalType.GOAL }
                    TypeToggle("Homework", newType == GoalType.HOMEWORK) { newType = GoalType.HOMEWORK }
                }
                OrbitButton(
                    text = "Add",
                    onClick = {
                        viewModel.addGoal(newTitle, newType)
                        newTitle = ""
                    },
                    enabled = newTitle.isNotBlank()
                )
            }

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterTab("All", filter == GoalFilter.ALL) { filter = GoalFilter.ALL }
                FilterTab("Goals", filter == GoalFilter.GOALS) { filter = GoalFilter.GOALS }
                FilterTab("Homework", filter == GoalFilter.HOMEWORK) { filter = GoalFilter.HOMEWORK }
            }
        }

        if (visible.isEmpty()) {
            Text(
                text = "Nothing here yet.",
                style = OrbitType.body,
                color = OrbitColors.ashFaint,
                modifier = Modifier.padding(20.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)
            ) {
                items(visible, key = { it.id }) { goal: GoalEntity ->
                    GoalRow(
                        title = goal.title,
                        typeLabel = if (goal.type == GoalType.HOMEWORK) "Homework" else "Goal",
                        isDone = goal.isDone,
                        onToggle = { viewModel.toggle(goal) },
                        onDelete = { viewModel.delete(goal) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(OrbitShapes.chip)
            .background(if (selected) OrbitColors.brassSoft else OrbitColors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = OrbitType.label,
            color = if (selected) OrbitColors.brass else OrbitColors.ash
        )
    }
}

@Composable
private fun FilterTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) OrbitColors.brass else OrbitColors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = OrbitType.label,
            color = if (selected) OrbitColors.ink else OrbitColors.ash
        )
    }
}