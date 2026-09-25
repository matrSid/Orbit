package com.optimusprime.wick.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun goalTypeToString(type: GoalType): String = type.name

    @TypeConverter
    fun stringToGoalType(value: String): GoalType = GoalType.valueOf(value)
}
