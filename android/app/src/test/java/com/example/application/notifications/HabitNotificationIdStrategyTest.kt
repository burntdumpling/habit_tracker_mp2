package com.example.application.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class HabitNotificationIdStrategyTest {

    @Test
    fun ids_areDeterministicForSameHabit() {
        val reminder1 = HabitNotificationIdStrategy.reminderId(42L)
        val reminder2 = HabitNotificationIdStrategy.reminderId(42L)
        assertEquals(reminder1, reminder2)
    }

    @Test
    fun ids_doNotConflictAcrossTypes() {
        val habitId = 42L
        val reminder = HabitNotificationIdStrategy.reminderId(habitId)
        val daily = HabitNotificationIdStrategy.dailyCompletionId(habitId)
        val streak = HabitNotificationIdStrategy.streakAchievementId(habitId, 7)
        val grouped = HabitNotificationIdStrategy.groupedReminderId()

        assertNotEquals(reminder, daily)
        assertNotEquals(reminder, streak)
        assertNotEquals(daily, streak)
        assertNotEquals(grouped, reminder)
        assertNotEquals(grouped, daily)
        assertNotEquals(grouped, streak)
    }
}
