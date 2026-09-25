package com.optimusprime.wick.ui.screens.goals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.optimusprime.wick.WickApplication
import com.optimusprime.wick.data.db.GoalEntity
import com.optimusprime.wick.data.db.GoalType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GoalsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as WickApplication).container.repository

    val goals: StateFlow<List<GoalEntity>> = repository.allGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addGoal(title: String, type: GoalType) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.addGoal(trimmed, type) }
    }

    fun toggle(goal: GoalEntity) {
        viewModelScope.launch { repository.toggleGoal(goal) }
    }

    fun delete(goal: GoalEntity) {
        viewModelScope.launch { repository.deleteGoal(goal) }
    }
}
