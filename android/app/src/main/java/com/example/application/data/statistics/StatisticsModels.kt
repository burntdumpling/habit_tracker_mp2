package com.example.application.data.statistics

import com.example.application.data.network.dto.CalendarDayDto
import com.example.application.data.network.dto.HabitDto
import java.time.LocalDate

enum class StatisticsPeriod {
    WEEK,
    MONTH,
    ALL
}

data class StatisticsRequest(
    val period: StatisticsPeriod,
    val anchorDate: LocalDate
)

data class StatisticsSourceBundle(
    val period: StatisticsPeriod,
    val dateFrom: LocalDate,
    val dateTo: LocalDate,
    val habits: List<HabitDto>,
    val calendarDays: List<CalendarDayDto>
)

