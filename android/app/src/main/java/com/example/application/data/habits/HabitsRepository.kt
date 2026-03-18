package com.example.application.data.habits

import com.example.application.habit.CreateHabitRequest
import com.example.application.habit.Habit
import com.example.application.data.network.dto.*
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

interface HabitsRepository {
    val habits: StateFlow<List<Habit>>
    suspend fun fetchHabits(): Result<Unit>
    suspend fun addHabit(request: CreateHabitRequest): Result<Habit>
    suspend fun deleteHabit(habitId: Long): Result<Unit>
    fun getHabit(habitId: Long): Habit?
    suspend fun updateHabit(habit: Habit): Result<Unit>

    suspend fun getHabitLogs(dateFrom: LocalDate, dateTo: LocalDate): Result<List<HabitLogDto>>
    suspend fun upsertHabitLog(habitId: Long, request: UpsertHabitLogRequest): Result<HabitLogDto>

    suspend fun getCalendarRange(dateFrom: LocalDate, dateTo: LocalDate, status: String? = null): Result<CalendarRangeDto>
    suspend fun getCalendarDay(date: LocalDate, status: String? = null): Result<CalendarDayDto>
}
