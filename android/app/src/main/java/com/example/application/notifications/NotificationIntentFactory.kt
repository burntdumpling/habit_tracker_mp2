package com.example.application.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.application.MainActivity

class NotificationIntentFactory(
    private val context: Context
) {

    fun reminderContentIntent(payload: ReminderNotificationPayload): PendingIntent {
        return buildContentIntent(
            type = HabitNotificationType.REMINDER,
            habitId = payload.habitId,
            date = payload.date,
            habitIds = null
        )
    }

    fun achievementContentIntent(
        type: HabitNotificationType,
        payload: AchievementNotificationPayload
    ): PendingIntent {
        return buildContentIntent(
            type = type,
            habitId = payload.habitId,
            date = payload.date,
            habitIds = null
        )
    }

    fun groupedReminderContentIntent(payload: GroupedReminderPayload): PendingIntent {
        return buildContentIntent(
            type = HabitNotificationType.GROUPED_REMINDER,
            habitId = null,
            date = payload.date,
            habitIds = payload.habitIds
        )
    }

    private fun buildContentIntent(
        type: HabitNotificationType,
        habitId: Long?,
        date: String?,
        habitIds: List<Long>?
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(HabitNotificationConstants.EXTRA_FROM_NOTIFICATION, true)
            putExtra(HabitNotificationConstants.EXTRA_NOTIFICATION_TYPE, type.name)
            if (habitId != null) {
                putExtra(HabitNotificationConstants.EXTRA_HABIT_ID, habitId)
            }
            if (date != null) {
                putExtra(HabitNotificationConstants.EXTRA_DATE, date)
            }
            if (!habitIds.isNullOrEmpty()) {
                putExtra(HabitNotificationConstants.EXTRA_HABIT_IDS, habitIds.toLongArray())
            }
        }

        val requestCode = buildRequestCode(type, habitId, date)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }

    private fun buildRequestCode(
        type: HabitNotificationType,
        habitId: Long?,
        date: String?
    ): Int {
        var result = type.ordinal + 31
        result = 31 * result + (habitId?.hashCode() ?: 0)
        result = 31 * result + (date?.hashCode() ?: 0)
        return result and Int.MAX_VALUE
    }
}
