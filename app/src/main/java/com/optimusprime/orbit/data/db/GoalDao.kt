package com.optimusprime.orbit.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Insert
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Delete
    suspend fun delete(goal: GoalEntity)

    @Query("SELECT * FROM goals ORDER BY isDone ASC, createdAtEpochMillis DESC")
    fun allGoals(): Flow<List<GoalEntity>>

    @Query("SELECT COUNT(*) FROM goals WHERE isDone = 1")
    fun completedCount(): Flow<Int>
}
