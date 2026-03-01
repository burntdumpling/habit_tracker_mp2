package com.example.application.data.network.dto

import com.example.application.habit.DayPart
import com.squareup.moshi.Json

data class HabitLogDto(
    @Json(name = "habitId") val habitId: Long,
    @Json(name = "date") val date: String, // YYYY-MM-DD
    @Json(name = "dayPart") val dayPart: DayPart,
    @Json(name = "amount") val amount: Int,
    @Json(name = "createdAt") val createdAt: String?,
    @Json(name = "updatedAt") val updatedAt: String?
)

data class UpdateLogDto(
    @Json(name = "date") val date: String, // YYYY-MM-DD
    @Json(name = "dayPart") val dayPart: DayPart,
    @Json(name = "amount") val amount: Int
)
