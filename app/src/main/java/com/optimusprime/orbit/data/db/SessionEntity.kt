package com.optimusprime.orbit.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One completed study session. Durations are stored in milliseconds and
 * already exclude any paused time — [activeDurationMillis] is the number
 * that should show up in a "you studied for X" sentence.
 *
 * [dateEpochDay] is the session's start date as [java.time.LocalDate.toEpochDay],
 * stored plainly so daily/weekly totals are a simple GROUP BY with no
 * TypeConverter involved.
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val activeDurationMillis: Long,
    val dateEpochDay: Long,
    val questionsSolved: Int,
    val distractionCount: Int
)
