package com.example.application.data.notifications

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.notificationSettingsDataStore by preferencesDataStore(name = "notification_settings")

class DataStoreNotificationSettingsRepository(
    private val dataStore: DataStore<Preferences>
) : NotificationSettingsRepository {

    constructor(context: Context) : this(context.notificationSettingsDataStore)

    override val appSettingsFlow: Flow<AppNotificationSettings> = dataStore.data.map { preferences ->
        AppNotificationSettings(
            notificationsEnabled = preferences[APP_NOTIFICATIONS_ENABLED_KEY] ?: false
        )
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[APP_NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }

    override fun habitSettingsFlow(habitId: Long): Flow<HabitNotificationSettings> {
        return dataStore.data.map { preferences ->
            preferences.toHabitSettings(habitId)
        }
    }

    override suspend fun updateHabitReminderEnabled(habitId: Long, enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[habitReminderEnabledKey(habitId)] = enabled
        }
    }

    override suspend fun updateHabitReminderTime(habitId: Long, reminderTime: String?) {
        dataStore.edit { preferences ->
            val key = habitReminderTimeKey(habitId)
            if (reminderTime == null) {
                preferences.remove(key)
            } else {
                preferences[key] = reminderTime
            }
        }
    }

    override suspend fun updateLastReminderScheduledAt(habitId: Long, value: String?) {
        dataStore.edit { preferences ->
            val key = habitLastReminderScheduledAtKey(habitId)
            if (value == null) {
                preferences.remove(key)
            } else {
                preferences[key] = value
            }
        }
    }

    override suspend fun updateLastCompletionNotifiedDate(habitId: Long, value: String?) {
        dataStore.edit { preferences ->
            val key = habitLastCompletionNotifiedDateKey(habitId)
            if (value == null) {
                preferences.remove(key)
            } else {
                preferences[key] = value
            }
        }
    }

    override suspend fun updateLastStreakNotified(habitId: Long, value: Int?) {
        dataStore.edit { preferences ->
            val key = habitLastStreakNotifiedKey(habitId)
            if (value == null) {
                preferences.remove(key)
            } else {
                preferences[key] = value
            }
        }
    }

    override suspend fun clearHabitSettings(habitId: Long) {
        dataStore.edit { preferences ->
            preferences.remove(habitReminderEnabledKey(habitId))
            preferences.remove(habitReminderTimeKey(habitId))
            preferences.remove(habitLastReminderScheduledAtKey(habitId))
            preferences.remove(habitLastCompletionNotifiedDateKey(habitId))
            preferences.remove(habitLastStreakNotifiedKey(habitId))
        }
    }

    override suspend fun getHabitSettingsOnce(habitId: Long): HabitNotificationSettings {
        return habitSettingsFlow(habitId).first()
    }

    private fun Preferences.toHabitSettings(habitId: Long): HabitNotificationSettings {
        return HabitNotificationSettings(
            habitId = habitId,
            reminderEnabled = this[habitReminderEnabledKey(habitId)] ?: false,
            reminderTime = this[habitReminderTimeKey(habitId)],
            lastReminderScheduledAt = this[habitLastReminderScheduledAtKey(habitId)],
            lastCompletionNotifiedDate = this[habitLastCompletionNotifiedDateKey(habitId)],
            lastStreakNotified = this[habitLastStreakNotifiedKey(habitId)]
        )
    }

    private companion object {
        val APP_NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("app_notifications_enabled")

        fun habitReminderEnabledKey(habitId: Long): Preferences.Key<Boolean> =
            booleanPreferencesKey("habit_${habitId}_reminder_enabled")

        fun habitReminderTimeKey(habitId: Long): Preferences.Key<String> =
            stringPreferencesKey("habit_${habitId}_reminder_time")

        fun habitLastReminderScheduledAtKey(habitId: Long): Preferences.Key<String> =
            stringPreferencesKey("habit_${habitId}_last_reminder_scheduled_at")

        fun habitLastCompletionNotifiedDateKey(habitId: Long): Preferences.Key<String> =
            stringPreferencesKey("habit_${habitId}_last_completion_notified_date")

        fun habitLastStreakNotifiedKey(habitId: Long): Preferences.Key<Int> =
            intPreferencesKey("habit_${habitId}_last_streak_notified")
    }
}
