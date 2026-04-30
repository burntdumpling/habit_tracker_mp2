package com.example.application.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderDispatchDeciderTest {

    private val decider = ReminderDispatchDecider()

    @Test
    fun oneReminderInBucket_selectsSingle() {
        val decision = decider.decide(listOf(pendingReminder(habitId = 1L)))

        assertTrue(decision is ReminderDispatchDecision.Single)
        assertEquals(1L, (decision as ReminderDispatchDecision.Single).reminder.habitId)
    }

    @Test
    fun twoRemindersInBucket_selectsGrouped() {
        val decision = decider.decide(
            listOf(
                pendingReminder(habitId = 1L),
                pendingReminder(habitId = 2L)
            )
        )

        assertTrue(decision is ReminderDispatchDecision.Grouped)
        assertEquals(2, (decision as ReminderDispatchDecision.Grouped).reminders.size)
    }

    private fun pendingReminder(habitId: Long): PendingReminder {
        return PendingReminder(
            habitId = habitId,
            habitName = "Habit $habitId",
            bodyText = "Body $habitId",
            date = "2026-04-30",
            scheduledAtMillis = 1_000L + habitId
        )
    }
}
