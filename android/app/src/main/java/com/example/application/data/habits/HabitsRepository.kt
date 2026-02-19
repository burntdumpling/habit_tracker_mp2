package com.example.application.data.habits

import com.example.application.habit.CreateHabitRequest
import com.example.application.habit.Habit
import kotlinx.coroutines.flow.StateFlow

interface HabitsRepository {
    val habits: StateFlow<List<Habit>>
    fun addHabit(request: CreateHabitRequest): Result<Habit>
    fun deleteHabit(habitId: Long): Result<Unit>
}
