package com.example.application.habit



data class HabitTarget(
    val dayPart: DayPart, // часть дня
    val targetCount: Int  // кол-во выполнения привычки
) {
    init {
        require(targetCount >= 1) { "targetCount must be >= 1" }
        // Выброс ошибки! Если кол-во меньше 1
    }
}