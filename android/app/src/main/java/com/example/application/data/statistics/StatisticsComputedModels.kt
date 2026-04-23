package com.example.application.data.statistics

import com.example.application.habit.HabitStatus
import java.time.LocalDate

data class StatisticsComputedData(
    val period: StatisticsPeriod,
    val dateFrom: LocalDate,
    val dateTo: LocalDate,
    val overview: StatisticsOverview,
    val timeline: List<StatisticsDayPoint>,
    val habits: List<StatisticsHabitMetrics>,
    val streaks: StatisticsStreaks,
    val insights: StatisticsInsights
)

data class StatisticsOverview(
    val totalPlanned: Int,
    val totalCompleted: Int,
    val missedCount: Int,
    val completionRatePercent: Int,
    val activeHabitsCount: Int
)

data class StatisticsDayPoint(
    val date: LocalDate,
    val plannedCount: Int,
    val completedCount: Int,
    val hasAnyCompleted: Boolean,
    val isPerfectDay: Boolean
)

data class StatisticsHabitMetrics(
    val habitId: Long,
    val name: String,
    val colorArgb: Int,
    val status: HabitStatus,
    val plannedDays: Int,
    val completedDays: Int,
    val doneTotal: Int,
    val targetTotal: Int,
    val completionRatePercent: Int
)

data class StatisticsStreaks(
    val currentStreakDays: Int,
    val bestStreakDays: Int,
    val consistencyPercent: Int
)

data class StatisticsInsights(
    val bestHabitId: Long?,
    val mostStableHabitId: Long?,
    val growthHabitId: Long?
)

