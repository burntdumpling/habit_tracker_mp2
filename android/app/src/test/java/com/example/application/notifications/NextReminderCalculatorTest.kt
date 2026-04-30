package com.example.application.notifications

import com.example.application.data.notifications.HabitNotificationSettings
import com.example.application.habit.Frequency
import com.example.application.habit.Habit
import com.example.application.habit.HabitStatus
import com.example.application.habit.HabitTarget
import com.example.application.habit.DayPart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class NextReminderCalculatorTest {
    private val zone = ZoneId.of("Europe/Moscow")
    private val calculator = NextReminderCalculator(zone)

    @Test
    fun daily_timeNotPassed_returnsToday() {
        val now = zdt(2026, 4, 23, 9, 0)
        val habit = habit(
            startDate = LocalDate.of(2026, 4, 20),
            endDate = null,
            frequency = Frequency.Daily
        )
        val settings = settings(reminderEnabled = true, reminderTime = "10:30")

        val result = calculator.calculateNextReminderAtMillis(
            habit = habit,
            appNotificationsEnabled = true,
            habitSettings = settings,
            nowMillis = now.toInstant().toEpochMilli()
        )

        val resultTime = result?.let { ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), zone) }
        assertNotNull(resultTime)
        assertEquals(LocalDate.of(2026, 4, 23), resultTime?.toLocalDate())
        assertEquals(10, resultTime?.hour)
        assertEquals(30, resultTime?.minute)
    }

    @Test
    fun daily_timeNotPassed_completedToday_returnsTomorrow() {
        val now = zdt(2026, 4, 23, 9, 0)
        val habit = habit(
            startDate = LocalDate.of(2026, 4, 20),
            endDate = null,
            frequency = Frequency.Daily
        )
        val settings = settings(reminderEnabled = true, reminderTime = "10:30")

        val result = calculator.calculateNextReminderAtMillis(
            habit = habit,
            appNotificationsEnabled = true,
            habitSettings = settings,
            completedToday = true,
            nowMillis = now.toInstant().toEpochMilli()
        )

        val resultTime = result?.let { ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), zone) }
        assertNotNull(resultTime)
        assertEquals(LocalDate.of(2026, 4, 24), resultTime?.toLocalDate())
        assertEquals(10, resultTime?.hour)
        assertEquals(30, resultTime?.minute)
    }

    @Test
    fun daily_timePassed_returnsTomorrow() {
        val now = zdt(2026, 4, 23, 21, 0)
        val habit = habit(
            startDate = LocalDate.of(2026, 4, 20),
            endDate = null,
            frequency = Frequency.Daily
        )
        val settings = settings(reminderEnabled = true, reminderTime = "10:30")

        val result = calculator.calculateNextReminderAtMillis(
            habit = habit,
            appNotificationsEnabled = true,
            habitSettings = settings,
            nowMillis = now.toInstant().toEpochMilli()
        )

        val resultTime = result?.let { ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), zone) }
        assertNotNull(resultTime)
        assertEquals(LocalDate.of(2026, 4, 24), resultTime?.toLocalDate())
    }

    @Test
    fun weekly_returnsNextMatchingWeekday() {
        val now = zdt(2026, 4, 23, 10, 0) // Thursday
        val habit = habit(
            startDate = LocalDate.of(2026, 4, 1),
            endDate = null,
            frequency = Frequency.Weekly(setOf(DayOfWeek.FRIDAY))
        )
        val settings = settings(reminderEnabled = true, reminderTime = "08:00")

        val result = calculator.calculateNextReminderAtMillis(
            habit = habit,
            appNotificationsEnabled = true,
            habitSettings = settings,
            nowMillis = now.toInstant().toEpochMilli()
        )

        val resultDate = result?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        assertEquals(LocalDate.of(2026, 4, 24), resultDate)
    }

    @Test
    fun everyNDays_returnsNextValidDay() {
        val now = zdt(2026, 4, 23, 11, 0)
        val habit = habit(
            startDate = LocalDate.of(2026, 4, 20),
            endDate = null,
            frequency = Frequency.EveryNDays(3)
        )
        val settings = settings(reminderEnabled = true, reminderTime = "10:00")

        val result = calculator.calculateNextReminderAtMillis(
            habit = habit,
            appNotificationsEnabled = true,
            habitSettings = settings,
            nowMillis = now.toInstant().toEpochMilli()
        )

        val resultDate = result?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        assertEquals(LocalDate.of(2026, 4, 26), resultDate)
    }

    @Test
    fun weekly_futureDay_notAffectedByCompletedTodayFlag() {
        val now = zdt(2026, 4, 23, 10, 0) // Thursday
        val habit = habit(
            startDate = LocalDate.of(2026, 4, 1),
            endDate = null,
            frequency = Frequency.Weekly(setOf(DayOfWeek.FRIDAY))
        )
        val settings = settings(reminderEnabled = true, reminderTime = "08:00")

        val result = calculator.calculateNextReminderAtMillis(
            habit = habit,
            appNotificationsEnabled = true,
            habitSettings = settings,
            completedToday = true,
            nowMillis = now.toInstant().toEpochMilli()
        )

        val resultDate = result?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        assertEquals(LocalDate.of(2026, 4, 24), resultDate)
    }

    @Test
    fun reminderDisabled_returnsNull() {
        val result = calculator.calculateNextReminderAtMillis(
            habit = habit(
                startDate = LocalDate.of(2026, 4, 20),
                endDate = null,
                frequency = Frequency.Daily
            ),
            appNotificationsEnabled = true,
            habitSettings = settings(reminderEnabled = false, reminderTime = "10:00"),
            nowMillis = zdt(2026, 4, 23, 9, 0).toInstant().toEpochMilli()
        )

        assertNull(result)
    }

    @Test
    fun reminderTimeNull_returnsNull() {
        val result = calculator.calculateNextReminderAtMillis(
            habit = habit(
                startDate = LocalDate.of(2026, 4, 20),
                endDate = null,
                frequency = Frequency.Daily
            ),
            appNotificationsEnabled = true,
            habitSettings = settings(reminderEnabled = true, reminderTime = null),
            nowMillis = zdt(2026, 4, 23, 9, 0).toInstant().toEpochMilli()
        )

        assertNull(result)
    }

    @Test
    fun inactiveOrOutOfRange_returnsNull() {
        val inactive = calculator.calculateNextReminderAtMillis(
            habit = habit(
                startDate = LocalDate.of(2026, 4, 20),
                endDate = null,
                frequency = Frequency.Daily,
                status = HabitStatus.PAUSED
            ),
            appNotificationsEnabled = true,
            habitSettings = settings(reminderEnabled = true, reminderTime = "10:00"),
            nowMillis = zdt(2026, 4, 23, 9, 0).toInstant().toEpochMilli()
        )
        val outOfRange = calculator.calculateNextReminderAtMillis(
            habit = habit(
                startDate = LocalDate.of(2026, 4, 1),
                endDate = LocalDate.of(2026, 4, 10),
                frequency = Frequency.Daily
            ),
            appNotificationsEnabled = true,
            habitSettings = settings(reminderEnabled = true, reminderTime = "10:00"),
            nowMillis = zdt(2026, 4, 23, 9, 0).toInstant().toEpochMilli()
        )

        assertNull(inactive)
        assertNull(outOfRange)
    }

    private fun zdt(year: Int, month: Int, day: Int, hour: Int, minute: Int): ZonedDateTime {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone)
    }

    private fun habit(
        startDate: LocalDate,
        endDate: LocalDate?,
        frequency: Frequency,
        status: HabitStatus = HabitStatus.ACTIVE
    ): Habit {
        return Habit(
            habitId = 42L,
            status = status,
            name = "Drink water",
            iconId = 1,
            colorArgb = 0xFFE0E0E0.toInt(),
            createdAt = Instant.parse("2026-04-01T00:00:00Z"),
            startDate = startDate,
            endDate = endDate,
            frequency = frequency,
            targets = listOf(HabitTarget(dayPart = DayPart.FULL_DAY, targetCount = 1))
        )
    }

    private fun settings(reminderEnabled: Boolean, reminderTime: String?): HabitNotificationSettings {
        return HabitNotificationSettings(
            habitId = 42L,
            reminderEnabled = reminderEnabled,
            reminderTime = reminderTime
        )
    }
}
