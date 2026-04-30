package com.example.application.notifications

enum class HabitNotificationType {
    REMINDER,
    DAILY_COMPLETION,
    STREAK_ACHIEVEMENT,
    GROUPED_REMINDER
}

data class ReminderNotificationPayload(
    val habitId: Long,
    val habitName: String,
    val bodyText: String,
    val date: String? = null,
    val targetText: String? = null
)

data class AchievementNotificationPayload(
    val habitId: Long,
    val habitName: String,
    val title: String,
    val bodyText: String,
    val streakDays: Int? = null,
    val date: String? = null
)

data class GroupedReminderPayload(
    val habitIds: List<Long>,
    val habitNames: List<String>,
    val count: Int,
    val date: String? = null
)

data class PendingReminder(
    val habitId: Long,
    val habitName: String,
    val bodyText: String,
    val date: String? = null,
    val scheduledAtMillis: Long
)
