package com.optimusprime.wick.data.repository

import com.optimusprime.wick.data.db.AppDatabase
import com.optimusprime.wick.data.db.DayTotal
import com.optimusprime.wick.data.db.GoalEntity
import com.optimusprime.wick.data.db.GoalType
import com.optimusprime.wick.data.db.SessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * The one place that knows both DAOs exist. Screens talk to this, never to
 * Room directly — makes it a single seam to swap in a cloud sync layer
 * later without touching any ViewModel.
 */
class StudyRepository(private val db: AppDatabase) {

    // --- Sessions -----------------------------------------------------

    fun recentSessions(limit: Int = 20): Flow<List<SessionEntity>> =
        db.sessionDao().recentSessions(limit)

    fun sessionsForToday(): Flow<List<SessionEntity>> =
        db.sessionDao().sessionsForDay(LocalDate.now().toEpochDay())

    suspend fun saveSession(session: SessionEntity): Long =
        db.sessionDao().insert(session)

    /** Last 7 days including today, oldest first is left to the caller to sort. */
    fun weekTotals(): Flow<List<DayTotal>> {
        val today = LocalDate.now().toEpochDay()
        return db.sessionDao().dayTotals(today - 6, today)
    }

    fun totalQuestionsSolved(): Flow<Int> = db.sessionDao().totalQuestionsSolved()

    fun totalActiveMillis(): Flow<Long> = db.sessionDao().totalActiveMillis()

    fun totalSessionCount(): Flow<Int> = db.sessionDao().totalSessionCount()

    /** Consecutive days (ending today or yesterday) with at least one session. */
    fun currentStreak(): Flow<Int> =
        db.sessionDao().distinctStudyDays().map { days -> computeStreak(days) }

    private fun computeStreak(studyDays: List<Long>): Int {
        if (studyDays.isEmpty()) return 0
        val daySet = studyDays.toHashSet()
        val today = LocalDate.now().toEpochDay()
        var cursor = if (daySet.contains(today)) today else today - 1
        var streak = 0
        while (daySet.contains(cursor)) {
            streak++
            cursor--
        }
        return streak
    }

    // --- Goals & homework ----------------------------------------------

    fun allGoals(): Flow<List<GoalEntity>> = db.goalDao().allGoals()

    fun completedGoalsCount(): Flow<Int> = db.goalDao().completedCount()

    suspend fun addGoal(title: String, type: GoalType) {
        db.goalDao().insert(
            GoalEntity(
                title = title,
                type = type,
                createdAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun toggleGoal(goal: GoalEntity) {
        val nowDone = !goal.isDone
        db.goalDao().update(
            goal.copy(
                isDone = nowDone,
                completedAtEpochMillis = if (nowDone) System.currentTimeMillis() else null
            )
        )
    }

    suspend fun deleteGoal(goal: GoalEntity) {
        db.goalDao().delete(goal)
    }
}
