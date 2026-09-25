package com.optimusprime.wick.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class DayTotal(
    val dateEpochDay: Long,
    val totalMillis: Long,
    val questionsSolved: Int
)

@Dao
interface SessionDao {

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query("SELECT * FROM sessions ORDER BY startEpochMillis DESC LIMIT :limit")
    fun recentSessions(limit: Int = 20): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE dateEpochDay = :epochDay")
    fun sessionsForDay(epochDay: Long): Flow<List<SessionEntity>>

    @Query(
        "SELECT dateEpochDay, SUM(activeDurationMillis) AS totalMillis, SUM(questionsSolved) AS questionsSolved " +
            "FROM sessions WHERE dateEpochDay BETWEEN :fromEpochDay AND :toEpochDay GROUP BY dateEpochDay"
    )
    fun dayTotals(fromEpochDay: Long, toEpochDay: Long): Flow<List<DayTotal>>

    @Query("SELECT COALESCE(SUM(questionsSolved), 0) FROM sessions")
    fun totalQuestionsSolved(): Flow<Int>

    @Query("SELECT COALESCE(SUM(activeDurationMillis), 0) FROM sessions")
    fun totalActiveMillis(): Flow<Long>

    @Query("SELECT COUNT(*) FROM sessions")
    fun totalSessionCount(): Flow<Int>

    @Query("SELECT DISTINCT dateEpochDay FROM sessions ORDER BY dateEpochDay DESC")
    fun distinctStudyDays(): Flow<List<Long>>
}
