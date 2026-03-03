package com.example.application.data.network.mapper

import com.example.application.data.network.dto.CreateHabitDto
import com.example.application.data.network.dto.FrequencyDto
import com.example.application.data.network.dto.HabitDto
import com.example.application.data.network.dto.HabitTargetDto
import com.example.application.habit.CreateHabitRequest
import com.example.application.habit.Frequency
import com.example.application.habit.Habit
import com.example.application.habit.HabitTarget
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

fun HabitDto.toDomain(): Habit {
    return Habit(
        habitId = habitId,
        status = status,
        name = name,
        iconId = iconId,
        colorArgb = 0xFFEAA065.toInt(), // Default color as requested
        createdAt = Instant.parse(createdAt),
        startDate = LocalDate.parse(startDate, dateFormatter),
        endDate = endDate?.let { LocalDate.parse(it, dateFormatter) },
        frequency = frequency.toDomain(),
        targets = targets.map { it.toDomain() }
    )
}

fun FrequencyDto.toDomain(): Frequency {
    return when (type) {
        com.example.application.habit.FrequencyType.DAILY -> Frequency.Daily
        com.example.application.habit.FrequencyType.WEEKLY -> Frequency.Weekly(
            weekDays = weekDays?.toSet() ?: emptySet()
        )
        com.example.application.habit.FrequencyType.EVERY_N_DAYS -> Frequency.EveryNDays(
            n = everyNDays ?: 1
        )
    }
}

fun HabitTargetDto.toDomain(): HabitTarget {
    return HabitTarget(
        dayPart = dayPart,
        targetCount = targetCount
    )
}

fun CreateHabitRequest.toDto(): CreateHabitDto {
    return CreateHabitDto(
        status = com.example.application.habit.HabitStatus.ACTIVE,
        name = name,
        iconId = iconId,
        startDate = startDate.format(dateFormatter),
        endDate = endDate?.format(dateFormatter),
        frequency = frequency.toDto(),
        targets = targets.map { it.toDto() }
    )
}

fun Frequency.toDto(): FrequencyDto {
    return when (this) {
        is Frequency.Daily -> FrequencyDto(
            type = com.example.application.habit.FrequencyType.DAILY,
            weekDays = null,
            everyNDays = null
        )
        is Frequency.Weekly -> FrequencyDto(
            type = com.example.application.habit.FrequencyType.WEEKLY,
            weekDays = weekDays.toList(),
            everyNDays = null
        )
        is Frequency.EveryNDays -> FrequencyDto(
            type = com.example.application.habit.FrequencyType.EVERY_N_DAYS,
            weekDays = null,
            everyNDays = n
        )
    }
}

fun HabitTarget.toDto(): HabitTargetDto {
    return HabitTargetDto(
        dayPart = dayPart,
        targetCount = targetCount
    )
}

fun Habit.toCreateDto(): CreateHabitDto {
    return CreateHabitDto(
        status = status,
        name = name,
        iconId = iconId,
        startDate = startDate.format(dateFormatter),
        endDate = endDate?.format(dateFormatter),
        frequency = frequency.toDto(),
        targets = targets.map { it.toDto() }
    )
}
