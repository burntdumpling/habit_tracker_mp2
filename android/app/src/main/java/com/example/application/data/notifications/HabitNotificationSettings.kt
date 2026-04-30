package com.example.application.data.notifications

/**
 * Настройки уведомлений и служебные метаданные для конкретной привычки.
 *
 * Сервисные поля используются для дедупликации уведомлений и последующего перепланирования.
 */
data class HabitNotificationSettings(
    val habitId: Long,
    val reminderEnabled: Boolean = false,
    val reminderTime: String? = null,
    val lastReminderScheduledAt: String? = null,
    val lastCompletionNotifiedDate: String? = null,
    val lastStreakNotified: Int? = null
)
