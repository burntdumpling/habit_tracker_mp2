package com.example.application.data.network.dto

import com.example.application.habit.DayPart
import com.example.application.habit.HabitStatus
import com.squareup.moshi.Json

data class CalendarHabitSummaryDto(
    @Json(name = "targetTotal") val targetTotal: Int,
    @Json(name = "actualTotal") val actualTotal: Int,
    @Json(name = "isCompleted") val isCompleted: Boolean
)

data class CalendarDaySummaryDto(
    @Json(name = "plannedCount") val plannedCount: Int,
    @Json(name = "completedCount") val completedCount: Int,
    @Json(name = "hasAnyCompleted") val hasAnyCompleted: Boolean
)

data class CalendarTargetDto(
    @Json(name = "dayPart") val dayPart: DayPart,
    @Json(name = "targetCount") val targetCount: Int
)

data class CalendarActualDto(
    @Json(name = "dayPart") val dayPart: DayPart,
    @Json(name = "amount") val amount: Int
)

data class CalendarHabitDto(
    @Json(name = "habitId") val habitId: Long,
    @Json(name = "name") val name: String,
    @Json(name = "iconId") val iconId: Int,
    @Json(name = "status") val status: HabitStatus,
    @Json(name = "targets") val targets: List<CalendarTargetDto>,
    @Json(name = "actual") val actual: List<CalendarActualDto>,
    @Json(name = "summary") val summary: CalendarHabitSummaryDto
)

data class CalendarDayDto(
    @Json(name = "date") val date: String, // ISO YYYY-MM-DD
    @Json(name = "summary") val summary: CalendarDaySummaryDto,
    @Json(name = "habits") val habits: List<CalendarHabitDto>
)

data class CalendarRangeDto(
    @Json(name = "dateFrom") val dateFrom: String,
    @Json(name = "dateTo") val dateTo: String,
    @Json(name = "days") val days: List<CalendarDayDto>
)

data class UpsertHabitLogRequest(
    @Json(name = "date") val date: String, // ISO YYYY-MM-DD
    @Json(name = "dayPart") val dayPart: DayPart,
    @Json(name = "amount") val amount: Int
)
