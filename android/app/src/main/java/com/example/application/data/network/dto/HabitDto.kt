package com.example.application.data.network.dto

import java.time.DayOfWeek
import com.example.application.habit.DayPart
import com.example.application.habit.FrequencyType
import com.example.application.habit.HabitStatus
import com.squareup.moshi.Json

data class HabitDto(
    @Json(name = "habitId") val habitId: Long,
    @Json(name = "status") val status: HabitStatus,
    @Json(name = "name") val name: String,
    @Json(name = "iconId") val iconId: Int,
    @Json(name = "createdAt") val createdAt: String, // ISO 8601
    @Json(name = "startDate") val startDate: String, // YYYY-MM-DD
    @Json(name = "endDate") val endDate: String?,    // YYYY-MM-DD
    @Json(name = "frequency") val frequency: FrequencyDto,
    @Json(name = "targets") val targets: List<HabitTargetDto>
)

data class FrequencyDto(
    @Json(name = "type") val type: FrequencyType,
    @Json(name = "weekDays") val weekDays: List<DayOfWeek>?,
    @Json(name = "everyNDays") val everyNDays: Int?
)

data class HabitTargetDto(
    @Json(name = "dayPart") val dayPart: DayPart,
    @Json(name = "targetCount") val targetCount: Int
)

data class CreateHabitDto(
    @Json(name = "status") val status: HabitStatus,
    @Json(name = "name") val name: String,
    @Json(name = "iconId") val iconId: Int,
    @Json(name = "startDate") val startDate: String, // YYYY-MM-DD
    @Json(name = "endDate") val endDate: String?,    // YYYY-MM-DD
    @Json(name = "frequency") val frequency: FrequencyDto,
    @Json(name = "targets") val targets: List<HabitTargetDto>
)
