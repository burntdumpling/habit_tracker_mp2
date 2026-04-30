package com.example.application.data.notifications

import kotlinx.coroutines.flow.Flow

interface NotificationSettingsRepository {
    val appSettingsFlow: Flow<AppNotificationSettings>

    suspend fun setNotificationsEnabled(enabled: Boolean)

    fun habitSettingsFlow(habitId: Long): Flow<HabitNotificationSettings>

    suspend fun updateHabitReminderEnabled(habitId: Long, enabled: Boolean)

    suspend fun updateHabitReminderTime(habitId: Long, reminderTime: String?)

    suspend fun updateLastReminderScheduledAt(habitId: Long, value: String?)

    suspend fun updateLastCompletionNotifiedDate(habitId: Long, value: String?)

    suspend fun updateLastStreakNotified(habitId: Long, value: Int?)

    suspend fun clearHabitSettings(habitId: Long)

    suspend fun getHabitSettingsOnce(habitId: Long): HabitNotificationSettings
}
