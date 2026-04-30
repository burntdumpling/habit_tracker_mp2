package com.example.application.notifications

import com.example.application.data.habits.HabitsRepository
import com.example.application.habit.Frequency
import com.example.application.habit.Habit
import java.time.Duration
import java.time.LocalDate

interface HabitStreakCalculator {
    suspend fun getCurrentStreak(habitId: Long, date: LocalDate): Int
}

class DefaultHabitStreakCalculator(
    private val habitsRepository: HabitsRepository
) : HabitStreakCalculator {
    companion object {
        private const val MAX_STREAK_THRESHOLD = 30
        private const val CALENDAR_CHUNK_DAYS = 31L
    }

    override suspend fun getCurrentStreak(habitId: Long, date: LocalDate): Int {
        habitsRepository.fetchHabits()
        val habit = habitsRepository.getHabit(habitId) ?: return 0
        if (date.isBefore(habit.startDate)) return 0
        if (habit.endDate != null && date.isAfter(habit.endDate)) return 0

        val completionByDate = mutableMapOf<LocalDate, Boolean>()
        var streak = 0
        var cursor = date
        val minDate = habit.startDate

        while (!cursor.isBefore(minDate) && streak < MAX_STREAK_THRESHOLD) {
            if (!isScheduledOnDate(habit, cursor)) {
                cursor = cursor.minusDays(1)
                continue
            }

            if (!completionByDate.containsKey(cursor)) {
                val chunkEnd = cursor
                val chunkStart = maxOf(minDate, chunkEnd.minusDays(CALENDAR_CHUNK_DAYS - 1))
                val range = habitsRepository.getCalendarRange(chunkStart, chunkEnd, status = null).getOrNull()
                    ?: return streak
                range.days.forEach { day ->
                    val localDate = runCatching { LocalDate.parse(day.date) }.getOrNull() ?: return@forEach
                    val completed = day.habits
                        .firstOrNull { it.habitId == habitId }
                        ?.summary
                        ?.isCompleted == true
                    completionByDate[localDate] = completed
                }
            }

            val completed = completionByDate[cursor] == true
            if (!completed) break

            streak += 1
            cursor = cursor.minusDays(1)
        }

        return streak
    }

    private fun isScheduledOnDate(habit: Habit, date: LocalDate): Boolean {
        if (date.isBefore(habit.startDate)) return false
        if (habit.endDate != null && date.isAfter(habit.endDate)) return false

        return when (val frequency = habit.frequency) {
            Frequency.Daily -> true
            is Frequency.Weekly -> date.dayOfWeek in frequency.weekDays
            is Frequency.EveryNDays -> {
                val daysFromStart = Duration.between(
                    habit.startDate.atStartOfDay(),
                    date.atStartOfDay()
                ).toDays()
                daysFromStart >= 0 && daysFromStart % frequency.n == 0L
            }
        }
    }
}
