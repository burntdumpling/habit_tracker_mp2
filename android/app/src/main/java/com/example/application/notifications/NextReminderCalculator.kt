package com.example.application.notifications

import com.example.application.data.notifications.HabitNotificationSettings
import com.example.application.habit.Frequency
import com.example.application.habit.Habit
import com.example.application.habit.HabitStatus
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class NextReminderCalculator(
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {

    fun calculateNextReminderAtMillis(
        habit: Habit,
        appNotificationsEnabled: Boolean,
        habitSettings: HabitNotificationSettings,
        completedToday: Boolean = false,
        nowMillis: Long = System.currentTimeMillis()
    ): Long? {
        if (!appNotificationsEnabled) return null
        if (!habitSettings.reminderEnabled) return null
        if (habit.status != HabitStatus.ACTIVE) return null

        val reminderTime = parseReminderTime(habitSettings.reminderTime) ?: return null
        val now = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(nowMillis), zoneId)
        val today = now.toLocalDate()

        if (habit.endDate != null && today.isAfter(habit.endDate)) return null

        val searchStart = maxOf(today, habit.startDate)
        val maxDaysAhead = 730L
        for (offset in 0L..maxDaysAhead) {
            val candidateDate = searchStart.plusDays(offset)
            if (habit.endDate != null && candidateDate.isAfter(habit.endDate)) {
                return null
            }
            if (!isScheduledOnDate(habit, candidateDate)) {
                continue
            }
            if (candidateDate == today && completedToday) {
                continue
            }

            val candidateDateTime = ZonedDateTime.of(candidateDate, reminderTime, zoneId)
            if (!candidateDateTime.isAfter(now)) {
                continue
            }
            return candidateDateTime.toInstant().toEpochMilli()
        }

        return null
    }

    private fun parseReminderTime(value: String?): LocalTime? {
        if (value.isNullOrBlank()) return null
        return runCatching { LocalTime.parse(value) }.getOrNull()
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
