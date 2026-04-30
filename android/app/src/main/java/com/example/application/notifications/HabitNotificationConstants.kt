package com.example.application.notifications

object HabitNotificationConstants {
    const val CHANNEL_HABIT_REMINDERS = "habit_reminders"
    const val CHANNEL_HABIT_ACHIEVEMENTS = "habit_achievements"

    const val GROUP_KEY_REMINDERS = "habit_group_reminders"
    const val GROUP_KEY_ACHIEVEMENTS = "habit_group_achievements"

    const val EXTRA_FROM_NOTIFICATION = "extra_from_notification"
    const val EXTRA_NOTIFICATION_TYPE = "extra_notification_type"
    const val EXTRA_HABIT_ID = "extra_habit_id"
    const val EXTRA_HABIT_IDS = "extra_habit_ids"
    const val EXTRA_DATE = "extra_date"

    const val WORK_TAG_HABIT_REMINDER = "habit_reminder_work"
    const val WORK_NAME_HABIT_REMINDER_PREFIX = "habit_reminder_"
    const val WORK_TAG_GROUPED_REMINDER_DISPATCH = "grouped_reminder_dispatch_work"
    const val WORK_NAME_GROUPED_REMINDER_DISPATCH_PREFIX = "grouped_reminder_dispatch_"
    const val WORK_INPUT_HABIT_ID = "work_input_habit_id"
    const val WORK_INPUT_SCHEDULED_AT = "work_input_scheduled_at"
    const val WORK_INPUT_BUCKET_KEY = "work_input_bucket_key"
    const val GROUPED_REMINDER_DISPATCH_DELAY_MS = 7_000L
}
