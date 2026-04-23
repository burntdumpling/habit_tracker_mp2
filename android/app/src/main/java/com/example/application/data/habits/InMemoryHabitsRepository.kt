package com.example.application.data.habits

import com.example.application.habit.CreateHabitRequest
import com.example.application.habit.Habit
import com.example.application.habit.HabitStatus
import com.example.application.data.network.dto.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

class InMemoryHabitsRepository : HabitsRepository {
    
    private val nextId = AtomicLong(1L)
    private val _habits = MutableStateFlow<List<Habit>>(emptyList())
    
    override val habits: StateFlow<List<Habit>> = _habits.asStateFlow()

    override suspend fun fetchHabits(status: String?): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun addHabit(request: CreateHabitRequest): Result<Habit> = runCatching {
        val habit = Habit(
            habitId = nextId.getAndIncrement(),
            status = HabitStatus.ACTIVE,
            name = request.name,
            iconId = request.iconId,
            colorArgb = request.colorArgb,
            createdAt = Instant.now(),
            startDate = LocalDate.now(),
            endDate = request.endDate,
            frequency = request.frequency,
            targets = request.targets
        )
        
        _habits.update { current ->
            // Добавляем в начало списка для удобства UI
            listOf(habit) + current
        }
        
        habit
    }

    override suspend fun deleteHabit(habitId: Long): Result<Unit> = runCatching {
        val current = _habits.value
        val updated = current.filter { it.habitId != habitId }
        
        if (updated.size == current.size) {
            throw IllegalArgumentException("Habit with id $habitId not found")
        }
        
        _habits.value = updated
    }

    override fun getHabit(habitId: Long): Habit? {
        return _habits.value.find { it.habitId == habitId }
    }

    override suspend fun updateHabit(habit: Habit): Result<Unit> = runCatching {
        _habits.update { current ->
            current.map { if (it.habitId == habit.habitId) habit else it }
        }
    }

    override suspend fun getHabitLogs(
        dateFrom: LocalDate,
        dateTo: LocalDate
    ): Result<List<HabitLogDto>> {
        return Result.success(emptyList())
    }

    override suspend fun upsertHabitLog(
        habitId: Long,
        request: UpsertHabitLogRequest
    ): Result<HabitLogDto> {
        return Result.failure(Exception("Not implemented"))
    }

    override suspend fun getCalendarRange(
        dateFrom: LocalDate,
        dateTo: LocalDate,
        status: String?
    ): Result<CalendarRangeDto> {
        return Result.failure(Exception("Not implemented"))
    }

    override suspend fun getCalendarDay(
        date: LocalDate,
        status: String?
    ): Result<CalendarDayDto> {
        return Result.failure(Exception("Not implemented"))
    }
}
