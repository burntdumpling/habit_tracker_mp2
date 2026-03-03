package com.example.application.habit

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime


// Статус привычки
enum class HabitStatus {
    ACTIVE,
    PAUSED,
    FINISHED
}

// Тип частоты
enum class FrequencyType {
    DAILY,
    WEEKLY,
    EVERY_N_DAYS
}

// Часть дня
enum class DayPart {
    MORNING,
    DAY,
    EVENING,
    FULL_DAY
}


// Вспомогательное: как показать время в МСК (если хранишь Instant).
object TimeMsk {
    private val MSK: ZoneId = ZoneId.of("Europe/Moscow")
    fun toMsk(instant: Instant): ZonedDateTime = instant.atZone(MSK)
}