package com.example.application.notifications

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.application.R

class HabitNotificationHelper(
    private val context: Context,
    private val notificationIntentFactory: NotificationIntentFactory = NotificationIntentFactory(context)
) : ReminderNotifier {

    override
    fun showReminder(payload: ReminderNotificationPayload) {
        if (!NotificationPermissionHelper.areNotificationsAvailable(context)) {
            return
        }

        val text = payload.targetText?.let { "${payload.bodyText} — $it" } ?: payload.bodyText
        val notification = NotificationCompat.Builder(
            context,
            HabitNotificationConstants.CHANNEL_HABIT_REMINDERS
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Пора выполнить привычку")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setGroup(HabitNotificationConstants.GROUP_KEY_REMINDERS)
            .setContentIntent(notificationIntentFactory.reminderContentIntent(payload))
            .build()

        notify(
            id = HabitNotificationIdStrategy.reminderId(payload.habitId),
            notification = notification
        )
    }

    fun showDailyCompletion(payload: AchievementNotificationPayload) {
        if (!NotificationPermissionHelper.areNotificationsAvailable(context)) {
            return
        }

        val title = "Цель дня выполнена"
        val text = payload.bodyText.ifBlank { "${payload.habitName} — цель закрыта" }
        val notification = NotificationCompat.Builder(
            context,
            HabitNotificationConstants.CHANNEL_HABIT_ACHIEVEMENTS
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setGroup(HabitNotificationConstants.GROUP_KEY_ACHIEVEMENTS)
            .setContentIntent(
                notificationIntentFactory.achievementContentIntent(
                    type = HabitNotificationType.DAILY_COMPLETION,
                    payload = payload
                )
            )
            .build()

        notify(
            id = HabitNotificationIdStrategy.dailyCompletionId(payload.habitId),
            notification = notification
        )
    }

    fun showStreakAchievement(payload: AchievementNotificationPayload) {
        if (!NotificationPermissionHelper.areNotificationsAvailable(context)) {
            return
        }

        val streakText = payload.streakDays?.let { "Серия: $it дней" } ?: payload.title
        val body = payload.bodyText.ifBlank {
            "Ты стабильно выполняешь привычку «${payload.habitName}»"
        }
        val notification = NotificationCompat.Builder(
            context,
            HabitNotificationConstants.CHANNEL_HABIT_ACHIEVEMENTS
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(streakText)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setGroup(HabitNotificationConstants.GROUP_KEY_ACHIEVEMENTS)
            .setContentIntent(
                notificationIntentFactory.achievementContentIntent(
                    type = HabitNotificationType.STREAK_ACHIEVEMENT,
                    payload = payload
                )
            )
            .build()

        notify(
            id = HabitNotificationIdStrategy.streakAchievementId(payload.habitId, payload.streakDays),
            notification = notification
        )
    }

    override
    fun showGroupedReminder(payload: GroupedReminderPayload) {
        if (!NotificationPermissionHelper.areNotificationsAvailable(context)) {
            return
        }

        val count = payload.count.coerceAtLeast(payload.habitIds.size)
        val title = "У тебя $count привычки на сейчас"
        val namesPreview = payload.habitNames.take(3).joinToString(", ")
        val text = if (namesPreview.isNotBlank()) namesPreview else "Проверь список привычек"
        val notification = NotificationCompat.Builder(
            context,
            HabitNotificationConstants.CHANNEL_HABIT_REMINDERS
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setGroup(HabitNotificationConstants.GROUP_KEY_REMINDERS)
            .setContentIntent(notificationIntentFactory.groupedReminderContentIntent(payload))
            .build()

        notify(
            id = HabitNotificationIdStrategy.groupedReminderId(),
            notification = notification
        )
    }

    @SuppressLint("MissingPermission")
    private fun notify(id: Int, notification: android.app.Notification) {
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
