package com.example.application.notifications

import com.example.application.data.habits.HabitsRepository
import java.time.LocalDate

interface HabitCompletionChecker {
    suspend fun isHabitCompletedOnDate(habitId: Long, date: LocalDate): Boolean
}

class BackendHabitCompletionChecker(
    private val habitsRepository: HabitsRepository
) : HabitCompletionChecker {
    override suspend fun isHabitCompletedOnDate(habitId: Long, date: LocalDate): Boolean {
        val day = habitsRepository.getCalendarDay(date = date).getOrNull() ?: return false
        return day.habits.firstOrNull { it.habitId == habitId }?.summary?.isCompleted == true
    }
}

object NoOpHabitCompletionChecker : HabitCompletionChecker {
    override suspend fun isHabitCompletedOnDate(habitId: Long, date: LocalDate): Boolean = false
}
