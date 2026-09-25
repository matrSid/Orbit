package com.optimusprime.orbit.ui.screens.progress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.optimusprime.orbit.OrbitApplication
import com.optimusprime.orbit.data.db.DayTotal
import com.optimusprime.orbit.ui.components.DayBar
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class Achievement(val title: String, val description: String, val unlocked: Boolean)

data class ProgressUiState(
    val weekBars: List<DayBar> = emptyList(),
    val totalQuestions: Int = 0,
    val totalActiveMillis: Long = 0L,
    val totalSessions: Int = 0,
    val streak: Int = 0,
    val completedGoals: Int = 0,
    val achievements: List<Achievement> = emptyList()
)

private data class Interim(
    val weekTotals: List<DayTotal>,
    val totalQuestions: Int,
    val totalActiveMillis: Long,
    val totalSessions: Int,
    val streak: Int
)

class ProgressViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as OrbitApplication).container.repository

    val uiState: StateFlow<ProgressUiState> = combine(
        repository.weekTotals(),
        repository.totalQuestionsSolved(),
        repository.totalActiveMillis(),
        repository.totalSessionCount(),
        repository.currentStreak()
    ) { weekTotals, totalQuestions, totalActiveMillis, totalSessions, streak ->
        Interim(weekTotals, totalQuestions, totalActiveMillis, totalSessions, streak)
    }.combine(repository.completedGoalsCount()) { interim, completedGoals ->
        buildState(interim, completedGoals)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProgressUiState())

    private fun buildState(interim: Interim, completedGoals: Int): ProgressUiState {
        val totalsByDay = interim.weekTotals.associateBy { it.dateEpochDay }
        val today = LocalDate.now()
        val bars = (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val total = totalsByDay[date.toEpochDay()]
            DayBar(
                label = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                minutes = (total?.totalMillis ?: 0L) / 60000f,
                isToday = offset == 0
            )
        }

        val hours = interim.totalActiveMillis / 3_600_000f
        val achievements = listOf(
            Achievement("First light", "Finish your first session.", interim.totalSessions >= 1),
            Achievement("Three in a row", "Study on three days in a row.", interim.streak >= 3),
            Achievement("Half a hundred", "Solve 50 questions in total.", interim.totalQuestions >= 50),
            Achievement("Five hours in", "Log 5 hours of active study time.", hours >= 5f),
            Achievement("First tick", "Complete your first goal or homework item.", completedGoals >= 1),
            Achievement("Regular", "Complete 10 study sessions.", interim.totalSessions >= 10)
        )

        return ProgressUiState(
            weekBars = bars,
            totalQuestions = interim.totalQuestions,
            totalActiveMillis = interim.totalActiveMillis,
            totalSessions = interim.totalSessions,
            streak = interim.streak,
            completedGoals = completedGoals,
            achievements = achievements
        )
    }
}
