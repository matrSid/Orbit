package com.optimusprime.wick.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class GoalType { GOAL, HOMEWORK }

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: GoalType,
    val isDone: Boolean = false,
    val createdAtEpochMillis: Long,
    val completedAtEpochMillis: Long? = null
)
