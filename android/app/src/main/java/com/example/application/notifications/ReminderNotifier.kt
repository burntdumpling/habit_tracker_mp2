package com.example.application.notifications

interface ReminderNotifier {
    fun showReminder(payload: ReminderNotificationPayload)
    fun showGroupedReminder(payload: GroupedReminderPayload)
}
