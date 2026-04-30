package com.example.application.notifications

sealed interface ReminderDispatchDecision {
    object NothingToDispatch : ReminderDispatchDecision
    data class Single(val reminder: PendingReminder) : ReminderDispatchDecision
    data class Grouped(val reminders: List<PendingReminder>) : ReminderDispatchDecision
}

class ReminderDispatchDecider {
    fun decide(reminders: List<PendingReminder>): ReminderDispatchDecision {
        return when (reminders.size) {
            0 -> ReminderDispatchDecision.NothingToDispatch
            1 -> ReminderDispatchDecision.Single(reminders.first())
            else -> ReminderDispatchDecision.Grouped(reminders.sortedBy { it.scheduledAtMillis })
        }
    }
}
