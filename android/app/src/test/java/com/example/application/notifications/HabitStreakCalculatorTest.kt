package com.example.application.notifications

import com.example.application.data.habits.HabitsRepository
import com.example.application.data.network.dto.CalendarActualDto
import com.example.application.data.network.dto.CalendarDayDto
import com.example.application.data.network.dto.CalendarDaySummaryDto
import com.example.application.data.network.dto.CalendarHabitDto
import com.example.application.data.network.dto.CalendarHabitSummaryDto
import com.example.application.data.network.dto.CalendarRangeDto
import com.example.application.data.network.dto.CalendarTargetDto
import com.example.application.data.network.dto.HabitLogDto
import com.example.application.data.network.dto.UpsertHabitLogRequest
import com.example.application.habit.CreateHabitRequest
import com.example.application.habit.DayPart
import com.example.application.habit.Frequency
import com.example.application.habit.Habit
import com.example.application.habit.HabitStatus
import com.example.application.habit.HabitTarget
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

class HabitStreakCalculatorTest {

    @Test
    fun consecutiveScheduledCompleted_daysAreCounted() = kotlinx.coroutines.test.runTest {
        val date = LocalDate.of(2026, 4, 23)
        val completion = mapOf(
            date to true,
            date.minusDays(1) to true,
            date.minusDays(2) to false
        )
        val repository = FakeHabitsRepository(
            habit = dailyHabit(start = date.minusDays(30)),
            completionByDate = completion
        )
        val calculator = DefaultHabitStreakCalculator(repository)

        val streak = calculator.getCurrentStreak(1L, date)

        assertEquals(2, streak)
    }

    @Test
    fun unscheduledGaps_doNotBreakStreak() = kotlinx.coroutines.test.runTest {
        val date = LocalDate.of(2026, 4, 22) // Wednesday
        val completion = mapOf(
            date to true,                     // Wed
            date.minusDays(2) to true,        // Mon
            date.minusDays(7) to true,        // Previous Wed
            date.minusDays(9) to false        // Previous Mon (break point)
        )
        val repository = FakeHabitsRepository(
            habit = weeklyHabit(
                start = date.minusDays(60),
                days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
            ),
            completionByDate = completion
        )
        val calculator = DefaultHabitStreakCalculator(repository)

        val streak = calculator.getCurrentStreak(1L, date)

        assertEquals(3, streak)
    }

    @Test
    fun firstMissedScheduledDay_breaksStreak() = kotlinx.coroutines.test.runTest {
        val date = LocalDate.of(2026, 4, 23)
        val completion = mapOf(
            date to true,
            date.minusDays(1) to false,
            date.minusDays(2) to true
        )
        val repository = FakeHabitsRepository(
            habit = dailyHabit(start = date.minusDays(30)),
            completionByDate = completion
        )
        val calculator = DefaultHabitStreakCalculator(repository)

        val streak = calculator.getCurrentStreak(1L, date)

        assertEquals(1, streak)
    }

    private fun dailyHabit(start: LocalDate): Habit {
        return Habit(
            habitId = 1L,
            status = HabitStatus.ACTIVE,
            name = "Habit",
            iconId = 1,
            colorArgb = 0xFFE0E0E0.toInt(),
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            startDate = start,
            endDate = null,
            frequency = Frequency.Daily,
            targets = listOf(HabitTarget(dayPart = DayPart.FULL_DAY, targetCount = 1))
        )
    }

    private fun weeklyHabit(start: LocalDate, days: Set<DayOfWeek>): Habit {
        return Habit(
            habitId = 1L,
            status = HabitStatus.ACTIVE,
            name = "Habit",
            iconId = 1,
            colorArgb = 0xFFE0E0E0.toInt(),
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            startDate = start,
            endDate = null,
            frequency = Frequency.Weekly(days),
            targets = listOf(HabitTarget(dayPart = DayPart.FULL_DAY, targetCount = 1))
        )
    }

    private class FakeHabitsRepository(
        private val habit: Habit,
        private val completionByDate: Map<LocalDate, Boolean>
    ) : HabitsRepository {
        override val habits = MutableStateFlow(listOf(habit))

        override suspend fun fetchHabits(status: String?): Result<Unit> = Result.success(Unit)
        override suspend fun addHabit(request: CreateHabitRequest): Result<Habit> = Result.failure(NotImplementedError())
        override suspend fun deleteHabit(habitId: Long): Result<Unit> = Result.failure(NotImplementedError())
        override fun getHabit(habitId: Long): Habit? = if (habitId == habit.habitId) habit else null
        override suspend fun updateHabit(habit: Habit): Result<Unit> = Result.failure(NotImplementedError())
        override suspend fun getHabitLogs(dateFrom: LocalDate, dateTo: LocalDate): Result<List<HabitLogDto>> {
            return Result.failure(NotImplementedError())
        }
        override suspend fun upsertHabitLog(habitId: Long, request: UpsertHabitLogRequest): Result<HabitLogDto> {
            return Result.failure(NotImplementedError())
        }

        override suspend fun getCalendarRange(
            dateFrom: LocalDate,
            dateTo: LocalDate,
            status: String?
        ): Result<CalendarRangeDto> {
            val days = generateSequence(dateFrom) { current ->
                if (current.isBefore(dateTo)) current.plusDays(1) else null
            }.toList()
            return Result.success(
                CalendarRangeDto(
                    dateFrom = dateFrom.toString(),
                    dateTo = dateTo.toString(),
                    days = days.map { day ->
                        val completed = completionByDate[day] == true
                        CalendarDayDto(
                            date = day.toString(),
                            summary = CalendarDaySummaryDto(
                                plannedCount = 1,
                                completedCount = if (completed) 1 else 0,
                                hasAnyCompleted = completed
                            ),
                            habits = listOf(
                                CalendarHabitDto(
                                    habitId = habit.habitId,
                                    name = habit.name,
                                    iconId = habit.iconId,
                                    status = habit.status,
                                    targets = listOf(CalendarTargetDto(DayPart.FULL_DAY, 1)),
                                    actual = listOf(CalendarActualDto(DayPart.FULL_DAY, if (completed) 1 else 0)),
                                    summary = CalendarHabitSummaryDto(
                                        targetTotal = 1,
                                        actualTotal = if (completed) 1 else 0,
                                        isCompleted = completed
                                    )
                                )
                            )
                        )
                    }
                )
            )
        }

        override suspend fun getCalendarDay(date: LocalDate, status: String?): Result<CalendarDayDto> {
            return Result.failure(NotImplementedError())
        }
    }
}
