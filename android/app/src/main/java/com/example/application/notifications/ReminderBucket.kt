package com.example.application.notifications

object ReminderBucket {
    private const val MILLIS_PER_MINUTE = 60_000L

    fun fromScheduledAtMillis(scheduledAtMillis: Long): Long {
        return (scheduledAtMillis / MILLIS_PER_MINUTE) * MILLIS_PER_MINUTE
    }
}
