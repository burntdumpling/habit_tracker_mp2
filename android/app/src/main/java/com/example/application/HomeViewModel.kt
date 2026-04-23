package com.example.application

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.application.data.habits.HabitsRepository
import com.example.application.data.network.dto.UpsertHabitLogRequest
import com.example.application.data.settings.HabitStatusFilter
import com.example.application.data.settings.SettingsPreferencesRepository
import com.example.application.habit.DayPart
import com.example.application.habit.Habit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicLong

class HomeViewModel(
    private val repository: HabitsRepository,
    private val settingsPreferencesRepository: SettingsPreferencesRepository
) : ViewModel() {

    val habits: StateFlow<List<Habit>> = repository.habits
    val progressMap = mutableStateMapOf<Long, Int>()
    private val mskZoneId = ZoneId.of("Europe/Moscow")
    private val loadRequestId = AtomicLong(0L)
    private val _statusFilter = MutableStateFlow(HabitStatusFilter.ALL)
    private val _selectedDate = MutableStateFlow(LocalDate.now(mskZoneId))
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    init {
        viewModelScope.launch {
            settingsPreferencesRepository.habitStatusFilter.collect { filter ->
                _statusFilter.value = filter
                loadData()
            }
        }
    }

    fun setSelectedDate(date: LocalDate) {
        if (_selectedDate.value == date) return
        _selectedDate.value = date
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val requestId = loadRequestId.incrementAndGet()
            val date = selectedDate.value
            val status = _statusFilter.value.toApiStatusOrNull()
            repository.fetchHabits(status = status)
            repository.getHabitLogs(date, date).onSuccess { logs ->
                if (requestId != loadRequestId.get()) return@onSuccess
                progressMap.clear()
                logs.forEach { log ->
                    progressMap[log.habitId] = log.amount
                }
            }.onFailure {
                if (requestId != loadRequestId.get()) return@onFailure
                progressMap.clear()
            }
        }
    }

    fun incrementProgress(habit: Habit, dayPart: DayPart, date: LocalDate) {
        val currentProgress = progressMap[habit.habitId] ?: 0
        val newProgress = currentProgress + 1
        
        // Оптимистичное обновление UI
        progressMap[habit.habitId] = newProgress
        
        viewModelScope.launch {
            val request = UpsertHabitLogRequest(
                date = date.toString(),
                dayPart = dayPart,
                amount = newProgress
            )
            repository.upsertHabitLog(habit.habitId, request).onFailure {
                // В случае ошибки откатываем локальный прогресс (опционально)
                progressMap[habit.habitId] = currentProgress
            }
        }
    }
}
