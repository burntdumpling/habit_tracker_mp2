package com.example.application.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class HabitNotificationChannels(
    private val context: Context
) {
    fun ensureCreated() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val reminderChannel = NotificationChannel(
            HabitNotificationConstants.CHANNEL_HABIT_REMINDERS,
            "Напоминания",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Напоминания о привычках"
        }

        val achievementsChannel = NotificationChannel(
            HabitNotificationConstants.CHANNEL_HABIT_ACHIEVEMENTS,
            "Достижения",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Уведомления о сериях и выполнении целей"
        }

        manager.createNotificationChannels(listOf(reminderChannel, achievementsChannel))
    }
}
