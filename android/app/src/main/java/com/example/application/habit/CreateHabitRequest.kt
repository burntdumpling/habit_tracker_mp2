package com.example.application.habit

import java.time.LocalDate

// Запрос создание привычки
data class CreateHabitRequest(
    val name: String,               // Название
    val iconId: Int,                // Иконка
    val colorArgb: Int,             // Цвет иконки
    val startDate: LocalDate,       // Дата начала
    val endDate: LocalDate?,        // Дата конца
    val frequency: Frequency,       // Частота
    val targets: List<HabitTarget>  // Точная частота
) {
    init {
        require(name.isNotBlank()) { "name must not be blank" }
        // Проверка: имя не должно быть пустым или состоять только из пробелов.
        // Если условие false — выбрасывается IllegalArgumentException с этим текстом.

        require(iconId >= 0) { "iconId must be >= 0" }
        // Проверка: iconId не должен быть отрицательным.
        // Отрицательное значение почти всегда означает ошибку выбора/данных.

        require(targets.isNotEmpty()) { "targets must not be empty" }
        // Проверка: список целей (targets) не должен быть пустым.
        // Иначе непонятно, "сколько и когда" выполнять привычку.

        if (endDate != null) {
            // Если дата окончания задана (то есть привычка имеет конец) — делаем доп. проверку.

            require(!endDate.isBefore(startDate)) { "endDate must be >= startDate" }
            // Проверка: endDate не может быть раньше startDate.
            // Иначе получится "привычка заканчивается раньше, чем начинается".
        }

        val parts = targets.map { it.dayPart }
        // Берём из каждого HabitTarget только dayPart (MORNING/DAY/EVENING/FULL_DAY)
        // и получаем список частей дня, которые выбрал пользователь.

        require(parts.distinct().size == parts.size) { "targets must not contain duplicate dayPart" }
        // distinct() убирает повторы.
        // Если после distinct размер меньше — значит dayPart повторялся (например два MORNING).
        // Это запрещаем, потому что цель для одной части дня должна быть ровно одна.

        val hasFullDay = targets.any { it.dayPart == DayPart.FULL_DAY }
        // Проверяем: есть ли среди целей часть дня FULL_DAY (целый день).

        if (hasFullDay) {
            // Если выбран FULL_DAY, то мы запрещаем выбирать одновременно другие части дня,
            // чтобы не было двойного смысла ("целый день" + "утро").

            require(targets.size == 1) { "If FULL_DAY is used, it must be the only target" }
            // Проверка: если FULL_DAY присутствует, то targets должен содержать только одну запись (FULL_DAY).
        }
    }
}