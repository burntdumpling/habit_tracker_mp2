package com.example.application.habit

import java.time.DayOfWeek

sealed interface Frequency {

    // Каждый день
    object Daily : Frequency

    // По выбранным дням недели
    data class Weekly(
        val weekDays: Set<DayOfWeek>
    ) : Frequency {
        init {
            require(weekDays.isNotEmpty()) { "Weekly.weekDays must not be empty" }
            // Выброс ошибки! Если польщователь не выбрал дни
        }
    }
}