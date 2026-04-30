package com.example.application.notifications

import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.application.data.habits.HabitsRepository
import com.example.application.data.notifications.NotificationSettingsRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class WorkManagerNotificationScheduler(
    private val workManager: WorkManager,
    private val habitsRepository: HabitsRepository,
    private val notificationSettingsRepository: NotificationSettingsRepository,
    private val habitCompletionChecker: HabitCompletionChecker,
    private val zoneId: ZoneId = ZoneId.of("Europe/Moscow"),
    private val nextReminderCalculator: NextReminderCalculator = NextReminderCalculator()
) : NotificationScheduler {

    override suspend fun scheduleHabitReminder(habitId: Long) {
        habitsRepository.fetchHabits()
        val habit = habitsRepository.getHabit(habitId)
        val appSettings = notificationSettingsRepository.appSettingsFlow.first()
        val habitSettings = notificationSettingsRepository.getHabitSettingsOnce(habitId)

        if (habit == null) {
            cancelHabitReminder(habitId)
            return
        }

        val nowMillis = System.currentTimeMillis()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val completedToday = habitCompletionChecker.isHabitCompletedOnDate(habitId, today)

        val nextAtMillis = nextReminderCalculator.calculateNextReminderAtMillis(
            habit = habit,
            appNotificationsEnabled = appSettings.notificationsEnabled,
            habitSettings = habitSettings,
            completedToday = completedToday,
            nowMillis = nowMillis
        )

        if (nextAtMillis == null) {
            cancelHabitReminder(habitId)
            return
        }

        val initialDelay = (nextAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val inputData = Data.Builder()
            .putLong(HabitNotificationConstants.WORK_INPUT_HABIT_ID, habitId)
            .putLong(HabitNotificationConstants.WORK_INPUT_SCHEDULED_AT, nextAtMillis)
            .build()

        val request = OneTimeWorkRequestBuilder<HabitReminderWorker>()
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(HabitNotificationConstants.WORK_TAG_HABIT_REMINDER)
            .build()

        workManager.enqueueUniqueWork(
            uniqueWorkName(habitId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    override suspend fun cancelHabitReminder(habitId: Long) {
        workManager.cancelUniqueWork(uniqueWorkName(habitId))
    }

    override suspend fun rescheduleHabitReminder(habitId: Long) {
        scheduleHabitReminder(habitId)
    }

    override suspend fun scheduleAllHabits() {
        habitsRepository.fetchHabits()
        val habits = habitsRepository.habits.value
        habits.forEach { habit ->
            runCatching { scheduleHabitReminder(habit.habitId) }
        }
    }

    override suspend fun cancelAllHabits() {
        workManager.cancelAllWorkByTag(HabitNotificationConstants.WORK_TAG_HABIT_REMINDER)
    }

    private fun uniqueWorkName(habitId: Long): String {
        return HabitNotificationConstants.WORK_NAME_HABIT_REMINDER_PREFIX + habitId
    }
}
