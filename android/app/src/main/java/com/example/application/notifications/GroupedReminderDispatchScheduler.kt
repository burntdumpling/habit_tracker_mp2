package com.example.application.notifications

import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class GroupedReminderDispatchScheduler(
    private val workManager: WorkManager,
    private val dispatchDelayMillis: Long = HabitNotificationConstants.GROUPED_REMINDER_DISPATCH_DELAY_MS
) {

    fun enqueue(bucketKey: Long) {
        val inputData = Data.Builder()
            .putLong(HabitNotificationConstants.WORK_INPUT_BUCKET_KEY, bucketKey)
            .build()

        val request = OneTimeWorkRequestBuilder<GroupedReminderDispatchWorker>()
            .setInputData(inputData)
            .setInitialDelay(dispatchDelayMillis, TimeUnit.MILLISECONDS)
            .addTag(HabitNotificationConstants.WORK_TAG_GROUPED_REMINDER_DISPATCH)
            .build()

        workManager.enqueueUniqueWork(
            uniqueWorkName(bucketKey),
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    private fun uniqueWorkName(bucketKey: Long): String {
        return HabitNotificationConstants.WORK_NAME_GROUPED_REMINDER_DISPATCH_PREFIX + bucketKey
    }
}
