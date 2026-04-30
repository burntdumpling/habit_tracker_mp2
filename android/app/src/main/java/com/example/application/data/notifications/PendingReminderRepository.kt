package com.example.application.data.notifications

import com.example.application.notifications.PendingReminder

interface PendingReminderRepository {
    suspend fun addPendingReminder(bucketKey: Long, reminder: PendingReminder)
    suspend fun getPendingReminders(bucketKey: Long): List<PendingReminder>
    suspend fun removePendingReminders(bucketKey: Long, habitIds: Set<Long>)
    suspend fun clearBucket(bucketKey: Long)
}
