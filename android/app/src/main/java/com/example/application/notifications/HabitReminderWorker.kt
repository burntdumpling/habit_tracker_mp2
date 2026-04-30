package com.example.application.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.application.MyApp
import com.example.application.habit.HabitStatus
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import kotlin.math.abs

class HabitReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val habitId = inputData.getLong(HabitNotificationConstants.WORK_INPUT_HABIT_ID, -1L)
        if (habitId <= 0L) return Result.success()
        val scheduledAt = inputData.getLong(HabitNotificationConstants.WORK_INPUT_SCHEDULED_AT, -1L)

        val app = applicationContext as? MyApp ?: return Result.success()
        val container = app.appContainer
        val calculator = NextReminderCalculator()
        val zoneId = ZoneId.of("Europe/Moscow")

        return runCatching {
            container.habitsRepository.fetchHabits()

            val habit = container.habitsRepository.getHabit(habitId) ?: return@runCatching
            val appSettings = container.notificationSettingsRepository.appSettingsFlow.first()
            val habitSettings = container.notificationSettingsRepository.getHabitSettingsOnce(habitId)
            val today = LocalDate.now(zoneId)
            val completedToday = container.habitCompletionChecker.isHabitCompletedOnDate(habitId, today)

            val baselineNow = if (scheduledAt > 0L) {
                (scheduledAt - 1_000L).coerceAtLeast(0L)
            } else {
                System.currentTimeMillis()
            }
            val expectedReminderAt = calculator.calculateNextReminderAtMillis(
                habit = habit,
                appNotificationsEnabled = appSettings.notificationsEnabled,
                habitSettings = habitSettings,
                completedToday = completedToday,
                nowMillis = baselineNow
            )

            val shouldShow = appSettings.notificationsEnabled &&
                NotificationPermissionHelper.areNotificationsAvailable(applicationContext) &&
                habitSettings.reminderEnabled &&
                !habitSettings.reminderTime.isNullOrBlank() &&
                !completedToday &&
                habit.status == HabitStatus.ACTIVE &&
                expectedReminderAt != null &&
                (scheduledAt <= 0L || abs(expectedReminderAt - scheduledAt) <= 60_000L)

            if (shouldShow) {
                val reminderScheduledAt = if (scheduledAt > 0L) scheduledAt else expectedReminderAt
                val payload = PendingReminder(
                    habitId = habit.habitId,
                    habitName = habit.name,
                    bodyText = habit.name,
                    date = Instant.ofEpochMilli(reminderScheduledAt).atZone(zoneId).toLocalDate().toString(),
                    scheduledAtMillis = reminderScheduledAt
                )
                val bucketKey = ReminderBucket.fromScheduledAtMillis(reminderScheduledAt)
                container.pendingReminderRepository.addPendingReminder(bucketKey, payload)
                container.groupedReminderDispatchScheduler.enqueue(bucketKey)
            }

            container.notificationScheduler.rescheduleHabitReminder(habitId)
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }
}
