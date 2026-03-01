package com.example.application.plus

import android.graphics.Color
import androidx.lifecycle.ViewModel
import java.time.DayOfWeek
import com.example.application.habit.DayPart
import com.example.application.habit.FrequencyType
import com.example.application.habit.Frequency
import com.example.application.habit.Habit
import com.example.application.data.habits.HabitsRepository
import com.example.application.habit.CreateHabitRequest
import com.example.application.habit.HabitTarget
import java.time.LocalDate
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * State representing the draft of a new habit being created through the wizard.
 */
data class CreateHabitDraftState(
    val currentStep: Int = 1,
    val name: String = "",
    val colorArgb: Int = Color.parseColor("#E0E0E0"),
    val iconId: Int = 1,
    val frequencyType: FrequencyType = FrequencyType.DAILY,
    val weekDays: List<DayOfWeek> = emptyList(),
    val targetCount: Int = 1,
    val dayPart: DayPart = DayPart.FULL_DAY,
    val editingHabitId: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for managing the state of the "Create Habit" wizard.
 */
class CreateHabitViewModel : ViewModel() {

    private val _state = MutableStateFlow(CreateHabitDraftState())
    val state = _state.asStateFlow()

    fun loadHabitForEdit(habit: Habit) {
        val freqType = when (habit.frequency) {
            is Frequency.Daily -> FrequencyType.DAILY
            is Frequency.Weekly -> FrequencyType.WEEKLY
            is Frequency.EveryNDays -> FrequencyType.EVERY_N_DAYS
        }
        
        val weekDays = if (habit.frequency is Frequency.Weekly) {
            habit.frequency.weekDays.toList()
        } else {
            emptyList()
        }

        _state.update { 
            it.copy(
                name = habit.name,
                colorArgb = habit.colorArgb,
                iconId = habit.iconId,
                frequencyType = freqType,
                weekDays = weekDays,
                targetCount = habit.targets.firstOrNull()?.targetCount ?: 1,
                dayPart = habit.targets.firstOrNull()?.dayPart ?: DayPart.FULL_DAY,
                editingHabitId = habit.habitId
            )
        }
    }

    fun updateName(name: String) {
        _state.update { it.copy(name = name) }
    }

    fun updateColor(color: Int) {
        _state.update { it.copy(colorArgb = color) }
    }

    fun updateIcon(iconId: Int) {
        _state.update { it.copy(iconId = iconId) }
    }

    fun updateFrequencyType(type: FrequencyType) {
        _state.update { it.copy(frequencyType = type) }
    }

    fun updateWeekDays(days: List<DayOfWeek>) {
        _state.update { it.copy(weekDays = days) }
    }

    fun updateTargetCount(count: Int) {
        _state.update { it.copy(targetCount = count) }
    }

    fun updateDayPart(part: DayPart) {
        _state.update { it.copy(dayPart = part) }
    }

    fun setLoading(isLoading: Boolean) {
        _state.update { it.copy(isLoading = isLoading) }
    }

    fun setError(error: String?) {
        _state.update { it.copy(error = error) }
    }

    fun saveHabit(repository: HabitsRepository, onSuccess: () -> Unit) {
        viewModelScope.launch {
            setLoading(true)
            setError(null)
            
            val stateVal = _state.value
            val frequency = when (stateVal.frequencyType) {
                FrequencyType.DAILY -> Frequency.Daily
                FrequencyType.WEEKLY -> Frequency.Weekly(stateVal.weekDays.toSet())
                FrequencyType.EVERY_N_DAYS -> Frequency.EveryNDays(1)
            }
            
            val targets = listOf(HabitTarget(stateVal.dayPart, stateVal.targetCount))
            val editId = stateVal.editingHabitId

            val result = if (editId != null) {
                val currentHabit = repository.getHabit(editId)
                if (currentHabit != null) {
                    val updatedHabit = currentHabit.copy(
                        name = stateVal.name,
                        iconId = stateVal.iconId,
                        colorArgb = stateVal.colorArgb,
                        frequency = frequency,
                        targets = targets
                    )
                    repository.updateHabit(updatedHabit)
                } else {
                    Result.failure(Exception("Habit not found"))
                }
            } else {
                val request = CreateHabitRequest(
                    name = stateVal.name,
                    iconId = stateVal.iconId,
                    colorArgb = stateVal.colorArgb,
                    startDate = LocalDate.now(),
                    endDate = null,
                    frequency = frequency,
                    targets = targets
                )
                repository.addHabit(request)
            }

            result.onSuccess {
                setLoading(false)
                onSuccess()
            }.onFailure {
                setLoading(false)
                setError(it.message ?: "Ошибка сохранения")
            }
        }
    }

    fun nextStep() {
        if (_state.value.currentStep < 7) {
            _state.update { it.copy(currentStep = it.currentStep + 1) }
        }
    }

    fun prevStep() {
        if (_state.value.currentStep > 1) {
            _state.update { it.copy(currentStep = it.currentStep - 1) }
        }
    }
}
