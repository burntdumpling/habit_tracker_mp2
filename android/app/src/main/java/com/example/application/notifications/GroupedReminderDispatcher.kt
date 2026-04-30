package com.example.application.notifications

import com.example.application.data.notifications.PendingReminderRepository

data class GroupedReminderDispatchResult(
    val dispatched: Boolean,
    val hasRemainingPendingReminders: Boolean
)

class GroupedReminderDispatcher(
    private val pendingReminderRepository: PendingReminderRepository,
    private val reminderDispatchDecider: ReminderDispatchDecider,
    private val reminderNotifier: ReminderNotifier
) {

    suspend fun dispatch(bucketKey: Long): GroupedReminderDispatchResult {
        val reminders = pendingReminderRepository.getPendingReminders(bucketKey)
        return when (val decision = reminderDispatchDecider.decide(reminders)) {
            ReminderDispatchDecision.NothingToDispatch -> GroupedReminderDispatchResult(
                dispatched = false,
                hasRemainingPendingReminders = false
            )

            is ReminderDispatchDecision.Single -> {
                reminderNotifier.showReminder(decision.reminder.toReminderPayload())
                finishDispatch(bucketKey, setOf(decision.reminder.habitId))
            }

            is ReminderDispatchDecision.Grouped -> {
                reminderNotifier.showGroupedReminder(decision.reminders.toGroupedReminderPayload())
                finishDispatch(bucketKey, decision.reminders.map { it.habitId }.toSet())
            }
        }
    }

    private suspend fun finishDispatch(
        bucketKey: Long,
        processedHabitIds: Set<Long>
    ): GroupedReminderDispatchResult {
        pendingReminderRepository.removePendingReminders(bucketKey, processedHabitIds)
        val hasRemaining = pendingReminderRepository.getPendingReminders(bucketKey).isNotEmpty()
        if (!hasRemaining) {
            pendingReminderRepository.clearBucket(bucketKey)
        }
        return GroupedReminderDispatchResult(
            dispatched = true,
            hasRemainingPendingReminders = hasRemaining
        )
    }
}

private fun PendingReminder.toReminderPayload(): ReminderNotificationPayload {
    return ReminderNotificationPayload(
        habitId = habitId,
        habitName = habitName,
        bodyText = bodyText,
        date = date,
        targetText = null
    )
}

private fun List<PendingReminder>.toGroupedReminderPayload(): GroupedReminderPayload {
    val sortedReminders = sortedBy { it.scheduledAtMillis }
    return GroupedReminderPayload(
        habitIds = sortedReminders.map { it.habitId },
        habitNames = sortedReminders.map { it.habitName },
        count = sortedReminders.size,
        date = sortedReminders.firstOrNull()?.date
    )
}
