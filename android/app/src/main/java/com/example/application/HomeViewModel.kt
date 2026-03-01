package com.example.application

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.application.data.habits.HabitsRepository
import com.example.application.data.network.dto.UpdateLogDto
import com.example.application.habit.Habit
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(
    private val repository: HabitsRepository
) : ViewModel() {

    val habits: StateFlow<List<Habit>> = repository.habits
    val progressMap = mutableStateMapOf<Long, Int>()

    fun loadData() {
        viewModelScope.launch {
            repository.fetchHabits()
            val today = LocalDate.now()
            repository.getHabitLogs(today, today).onSuccess { logs ->
                progressMap.clear()
                logs.forEach { log ->
                    progressMap[log.habitId] = log.amount
                }
            }
        }
    }

    fun incrementProgress(habit: Habit) {
        val currentProgress = progressMap[habit.habitId] ?: 0
        val newProgress = currentProgress + 1
        
        // Оптимистичное обновление UI
        progressMap[habit.habitId] = newProgress
        
        viewModelScope.launch {
            val today = LocalDate.now()
            // Определяем dayPart для лога. 
            // В идеале мы должны знать, в какой части дня нажал пользователь, 
            // но пока берем первую из целей привычки (или FULL_DAY)
            val dayPart = habit.targets.firstOrNull()?.dayPart ?: com.example.application.habit.DayPart.FULL_DAY
            
            val request = UpdateLogDto(
                date = today.toString(),
                dayPart = dayPart,
                amount = newProgress
            )
            repository.updateHabitLog(habit.habitId, request).onFailure {
                // В случае ошибки откатываем локальный прогресс (опционально)
                progressMap[habit.habitId] = currentProgress
            }
        }
    }
}
