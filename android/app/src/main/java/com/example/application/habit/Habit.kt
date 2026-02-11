package com.example.application.habit

import java.time.Instant
import java.time.LocalDate

// Habit — привычка (то, что обычно возвращает сервер)
data class Habit(
    val habitId: Long,                   // выдаёт сервер
    val status: HabitStatus,             // ACTIVE / PAUSED / FINISHED
    val name: String,                    // Название
    val iconId: Int,                     // иконка
    val colorArgb: Int,                  // цвет иконки
    val createdAt: Instant,              // выставляет сервер (момент времени)
    val startDate: LocalDate,            // дата начала действия
    val endDate: LocalDate?,             // может быть null (без окончания)
    val frequency: Frequency,            // Частота
    val targets: List<HabitTarget>       // Точная частота
) {
    init {
        // Проверка: habitId должен быть больше 0.
        // Это актуально для Habit, который пришёл с сервера: сервер выдаёт id (1,2,3,...).
        // Если habitId = 0, значит объект "не настоящий" или ошибка данных.
        require(habitId > 0L) { "habitId must be > 0" }

        // Проверка: имя не должно быть пустым или состоять только из пробелов.
        // Иначе в UI будет пустое название, статистика/поиск/списки будут ломаться.
        require(name.isNotBlank()) { "name must not be blank" }

        // Проверка: iconId не должен быть отрицательным.
        // Обычно iconId — это id ресурса или индекс иконки в списке.
        // Отрицательное значение — почти всегда ошибка.
        require(iconId >= 0) { "iconId must be >= 0" }

        // Проверка: список целей (targets) не должен быть пустым.
        // Если targets пустой, непонятно "сколько и когда" выполнять привычку.
        require(targets.isNotEmpty()) { "targets must not be empty" }

        // Если endDate задана (не null) — тогда проверяем корректность диапазона дат.
        if (endDate != null) {
            // Проверка: endDate не должна быть раньше startDate.
            // Иначе получится "привычка заканчивается раньше, чем начинается".
            require(!endDate.isBefore(startDate)) { "endDate must be >= startDate" }
        }

        // Комментарий: дальше проверим, что части дня в целях не повторяются.
        // Например нельзя два раза указать MORNING -> 2 и MORNING -> 3.
        // Цель для каждой части дня должна быть одна.
        val parts = targets.map { it.dayPart } // Берём из targets только dayPart (MORNING/DAY/EVENING/FULL_DAY)

        // distinct() убирает повторы.
        // Если после distinct элементов стало меньше — значит были дубликаты dayPart.
        require(parts.distinct().size == parts.size) { "targets must not contain duplicate dayPart" }

        // Проверка правила FULL_DAY.
        // Если выбран FULL_DAY, то другие части дня обычно запрещены,
        // чтобы не было двойного смысла: "и целый день, и утро".
        val hasFullDay = targets.any { it.dayPart == DayPart.FULL_DAY } // Есть ли цель с FULL_DAY?

        // Если FULL_DAY есть, то targets должен содержать только одну запись (только FULL_DAY).
        if (hasFullDay) {
            require(targets.size == 1) { "If FULL_DAY is used, it must be the only target" }
        }
    }
}