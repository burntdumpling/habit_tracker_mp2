package com.example.application.data.habits

import com.example.application.data.network.api.HabitApiService
import com.example.application.data.network.dto.HabitLogDto
import com.example.application.data.network.dto.UpdateLogDto
import com.example.application.data.network.mapper.toCreateDto
import com.example.application.data.network.mapper.toDomain
import com.example.application.data.network.mapper.toDto
import com.example.application.habit.CreateHabitRequest
import com.example.application.habit.Habit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.lang.Exception
import java.time.LocalDate

class NetworkHabitsRepository(
    private val api: HabitApiService
) : HabitsRepository {

    private val _habits = MutableStateFlow<List<Habit>>(emptyList())
    override val habits: StateFlow<List<Habit>> = _habits.asStateFlow()

    override suspend fun fetchHabits(): Result<Unit> = runCatching {
        val habitDtos = api.getHabits()
        val domainHabits = habitDtos.map { it.toDomain() }
        _habits.value = domainHabits
    }

    override suspend fun addHabit(request: CreateHabitRequest): Result<Habit> = runCatching {
        val dto = request.toDto()
        val response = api.createHabit(dto)
        val newHabit = response.toDomain()
        _habits.update { it + newHabit }
        newHabit
    }

    override suspend fun deleteHabit(habitId: Long): Result<Unit> = runCatching {
        api.deleteHabit(habitId)
        _habits.update { current ->
            current.filter { it.habitId != habitId }
        }
    }

    override fun getHabit(habitId: Long): Habit? {
        return _habits.value.find { it.habitId == habitId }
    }

    override suspend fun updateHabit(habit: Habit): Result<Unit> = runCatching {
        val updateDto = habit.toCreateDto()
        val response = api.updateHabit(habit.habitId, updateDto)
        val updatedHabit = response.toDomain()
        _habits.update { current ->
            current.map { if (it.habitId == habit.habitId) updatedHabit else it }
        }
    }

    override suspend fun getHabitLogs(dateFrom: LocalDate, dateTo: LocalDate): Result<List<HabitLogDto>> = runCatching {
        api.getHabitLogs(dateFrom.toString(), dateTo.toString())
    }

    override suspend fun updateHabitLog(habitId: Long, request: UpdateLogDto): Result<HabitLogDto> = runCatching {
        api.updateHabitLog(habitId, request)
    }
}
