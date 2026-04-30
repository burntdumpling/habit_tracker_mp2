package com.example.application.notifications

import com.example.application.data.notifications.NotificationSettingsRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

/**
 * Coordinates notification scheduling based on high-level app events.
 */
class NotificationSchedulingCoordinator(
    private val notificationSettingsRepository: NotificationSettingsRepository,
    private val notificationScheduler: NotificationScheduler,
    private val habitCompletionChecker: HabitCompletionChecker = NoOpHabitCompletionChecker,
    private val zoneId: ZoneId = ZoneId.of("Europe/Moscow")
) {

    suspend fun syncAllOnAppEntry() {
        val enabled = notificationSettingsRepository.appSettingsFlow.first().notificationsEnabled
        if (enabled) {
            notificationScheduler.scheduleAllHabits()
        } else {
            notificationScheduler.cancelAllHabits()
        }
    }

    suspend fun onGlobalNotificationsChanged(enabled: Boolean) {
        if (enabled) {
            notificationScheduler.scheduleAllHabits()
        } else {
            notificationScheduler.cancelAllHabits()
        }
    }

    suspend fun onHabitCreatedOrUpdated(habitId: Long) {
        notificationScheduler.rescheduleHabitReminder(habitId)
    }

    suspend fun onHabitDeleted(habitId: Long) {
        notificationScheduler.cancelHabitReminder(habitId)
    }

    suspend fun onHabitCompletedForDate(habitId: Long, date: LocalDate) {
        if (date != LocalDate.now(zoneId)) return
        val completed = habitCompletionChecker.isHabitCompletedOnDate(habitId, date)
        if (completed) {
            notificationScheduler.rescheduleHabitReminder(habitId)
        }
    }
}
