package com.example.application.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.application.MyApp

class GroupedReminderDispatchWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val bucketKey = inputData.getLong(HabitNotificationConstants.WORK_INPUT_BUCKET_KEY, -1L)
        if (bucketKey <= 0L) return Result.success()

        val app = applicationContext as? MyApp ?: return Result.success()
        val container = app.appContainer

        return runCatching {
            val dispatchResult = container.groupedReminderDispatcher.dispatch(bucketKey)
            if (dispatchResult.hasRemainingPendingReminders) {
                container.groupedReminderDispatchScheduler.enqueue(bucketKey)
            }
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }
}
