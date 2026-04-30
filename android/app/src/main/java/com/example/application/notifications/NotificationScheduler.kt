package com.example.application.notifications

interface NotificationScheduler {
    suspend fun scheduleHabitReminder(habitId: Long)
    suspend fun cancelHabitReminder(habitId: Long)
    suspend fun rescheduleHabitReminder(habitId: Long)
    suspend fun scheduleAllHabits()
    suspend fun cancelAllHabits()
}
