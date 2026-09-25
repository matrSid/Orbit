package com.optimusprime.wick.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.optimusprime.wick.WickApplication
import com.optimusprime.wick.data.db.GoalEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val todayActiveMillis: Long = 0L,
    val todayQuestions: Int = 0,
    val streak: Int = 0,
    val topGoals: List<GoalEntity> = emptyList(),
    val tip: String = ""
)

private val dailyTips = listOf(
    "Short breaks between subjects help more than one long slog.",
    "Say a question out loud before you check the answer — it sticks better.",
    "Study the hardest subject first, while you still have patience for it.",
    "A five-minute recap of yesterday beats five extra minutes today.",
    "Put your phone somewhere else, not just on silent.",
    "Write down what you got stuck on — it's the fastest way to find a pattern.",
    "Explaining an answer to someone else is the real test of knowing it."
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as WickApplication).container.repository

    val uiState: StateFlow<HomeUiState> = combine(
        repository.sessionsForToday(),
        repository.allGoals(),
        repository.currentStreak()
    ) { sessions, goals, streak ->
        HomeUiState(
            todayActiveMillis = sessions.sumOf { it.activeDurationMillis },
            todayQuestions = sessions.sumOf { it.questionsSolved },
            streak = streak,
            topGoals = goals.filter { !it.isDone }.take(3),
            tip = dailyTips[LocalDate.now().dayOfYear % dailyTips.size]
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun toggleGoal(goal: GoalEntity) {
        viewModelScope.launch { repository.toggleGoal(goal) }
    }
}
