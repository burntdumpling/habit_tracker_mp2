package com.example.application.notifications

import com.example.application.data.habits.HabitsRepository
import com.example.application.data.notifications.NotificationSettingsRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

interface HabitAchievementEvaluator {
    suspend fun evaluateForHabitDate(habitId: Long, date: LocalDate)
}

class DefaultHabitAchievementEvaluator(
    private val habitsRepository: HabitsRepository,
    private val notificationSettingsRepository: NotificationSettingsRepository,
    private val habitCompletionChecker: HabitCompletionChecker,
    private val habitStreakCalculator: HabitStreakCalculator,
    private val achievementNotifier: HabitAchievementNotifier,
    private val notificationAvailabilityChecker: SystemNotificationAvailabilityChecker,
    private val zoneId: ZoneId = ZoneId.of("Europe/Moscow")
) : HabitAchievementEvaluator {
    private val streakThresholds = listOf(3, 7, 14, 30)

    override suspend fun evaluateForHabitDate(habitId: Long, date: LocalDate) {
        val today = LocalDate.now(zoneId)
        if (date != today) return

        val appNotificationsEnabled = notificationSettingsRepository.appSettingsFlow.first().notificationsEnabled
        if (!appNotificationsEnabled) return
        if (!notificationAvailabilityChecker.areNotificationsAvailable()) return

        val isCompleted = habitCompletionChecker.isHabitCompletedOnDate(habitId, date)
        if (!isCompleted) return

        habitsRepository.fetchHabits()
        val habit = habitsRepository.getHabit(habitId) ?: return
        val settings = notificationSettingsRepository.getHabitSettingsOnce(habitId)
        val dateText = date.toString()

        if (settings.lastCompletionNotifiedDate != dateText) {
            val completionText = buildCompletionText(habitId = habitId, habitName = habit.name, date = date)
            achievementNotifier.showDailyCompletion(
                AchievementNotificationPayload(
                    habitId = habitId,
                    habitName = habit.name,
                    title = "Цель дня выполнена",
                    bodyText = completionText,
                    date = dateText
                )
            )
            notificationSettingsRepository.updateLastCompletionNotifiedDate(habitId, dateText)
        }

        val currentStreak = habitStreakCalculator.getCurrentStreak(habitId, date)
        val maxReachedThreshold = streakThresholds.lastOrNull { it <= currentStreak } ?: return
        val lastNotified = settings.lastStreakNotified ?: 0
        if (maxReachedThreshold <= lastNotified) return

        achievementNotifier.showStreakAchievement(
            AchievementNotificationPayload(
                habitId = habitId,
                habitName = habit.name,
                title = "Серия: $maxReachedThreshold дней",
                bodyText = "Ты стабильно выполняешь привычку «${habit.name}»",
                streakDays = maxReachedThreshold,
                date = dateText
            )
        )
        notificationSettingsRepository.updateLastStreakNotified(habitId, maxReachedThreshold)
    }

    private suspend fun buildCompletionText(habitId: Long, habitName: String, date: LocalDate): String {
        val day = habitsRepository.getCalendarDay(date = date).getOrNull()
        val summary = day?.habits?.firstOrNull { it.habitId == habitId }?.summary
        if (summary == null) return "$habitName — цель закрыта"
        return "$habitName — ${summary.actualTotal}/${summary.targetTotal}"
    }
}
