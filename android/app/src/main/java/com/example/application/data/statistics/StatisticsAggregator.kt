package com.example.application.data.statistics

import com.example.application.data.network.dto.CalendarDayDto
import com.example.application.habit.HabitStatus
import java.time.LocalDate
import kotlin.math.roundToInt

object StatisticsAggregator {

    fun compute(source: StatisticsSourceBundle): StatisticsComputedData {
        val timeline = source.calendarDays
            .map { day ->
                val date = LocalDate.parse(day.date)
                val planned = day.summary.plannedCount
                val completed = day.summary.completedCount
                StatisticsDayPoint(
                    date = date,
                    plannedCount = planned,
                    completedCount = completed,
                    hasAnyCompleted = day.summary.hasAnyCompleted,
                    isPerfectDay = planned > 0 && completed == planned
                )
            }
            .sortedBy { it.date }

        val overview = computeOverview(source, timeline)
        val habits = computeHabitMetrics(source)
        val streaks = computeStreaks(timeline)
        val insights = computeInsights(source, habits)

        return StatisticsComputedData(
            period = source.period,
            dateFrom = source.dateFrom,
            dateTo = source.dateTo,
            overview = overview,
            timeline = timeline,
            habits = habits,
            streaks = streaks,
            insights = insights
        )
    }

    private fun computeOverview(
        source: StatisticsSourceBundle,
        timeline: List<StatisticsDayPoint>
    ): StatisticsOverview {
        val totalPlanned = timeline.sumOf { it.plannedCount }
        val totalCompleted = timeline.sumOf { it.completedCount }
        val missedCount = maxOf(0, totalPlanned - totalCompleted)
        val completionRatePercent = percent(totalCompleted, totalPlanned)
        val activeHabitsCount = source.calendarDays
            .asSequence()
            .flatMap { day -> day.habits.asSequence().map { it.habitId } }
            .toSet()
            .size

        return StatisticsOverview(
            totalPlanned = totalPlanned,
            totalCompleted = totalCompleted,
            missedCount = missedCount,
            completionRatePercent = completionRatePercent,
            activeHabitsCount = activeHabitsCount
        )
    }

    private fun computeHabitMetrics(source: StatisticsSourceBundle): List<StatisticsHabitMetrics> {
        val metaByHabitId = source.habits.associateBy { it.habitId }
        val accum = linkedMapOf<Long, HabitAccumulator>()

        source.calendarDays.forEach { day ->
            day.habits.forEach { habit ->
                val item = accum.getOrPut(habit.habitId) {
                    HabitAccumulator(
                        habitId = habit.habitId,
                        fallbackName = habit.name,
                        fallbackStatus = habit.status
                    )
                }
                item.plannedDays += 1
                if (habit.summary.isCompleted) {
                    item.completedDays += 1
                }
                item.doneTotal += habit.summary.actualTotal
                item.targetTotal += habit.summary.targetTotal
            }
        }

        return accum.values
            .filter { it.plannedDays > 0 }
            .map { item ->
                val meta = metaByHabitId[item.habitId]
                val completionRatePercent = percent(item.completedDays, item.plannedDays)
                StatisticsHabitMetrics(
                    habitId = item.habitId,
                    name = meta?.name ?: item.fallbackName,
                    colorArgb = meta?.colorArgb ?: 0,
                    status = meta?.status ?: item.fallbackStatus,
                    plannedDays = item.plannedDays,
                    completedDays = item.completedDays,
                    doneTotal = item.doneTotal,
                    targetTotal = item.targetTotal,
                    completionRatePercent = completionRatePercent
                )
            }
            .sortedWith(
                compareByDescending<StatisticsHabitMetrics> { it.completionRatePercent }
                    .thenByDescending { it.plannedDays }
                    .thenBy { it.habitId }
            )
    }

    private fun computeStreaks(timeline: List<StatisticsDayPoint>): StatisticsStreaks {
        val scheduledDays = timeline.filter { it.plannedCount > 0 }
        if (scheduledDays.isEmpty()) {
            return StatisticsStreaks(
                currentStreakDays = 0,
                bestStreakDays = 0,
                consistencyPercent = 0
            )
        }

        val successfulScheduledDays = scheduledDays.count { it.isPerfectDay }
        val consistencyPercent = percent(successfulScheduledDays, scheduledDays.size)
        val currentStreak = computeCurrentStreak(scheduledDays)
        val bestStreak = computeBestStreak(scheduledDays)

        return StatisticsStreaks(
            currentStreakDays = currentStreak,
            bestStreakDays = bestStreak,
            consistencyPercent = consistencyPercent
        )
    }

    private fun computeCurrentStreak(scheduledDays: List<StatisticsDayPoint>): Int {
        var count = 0
        for (idx in scheduledDays.lastIndex downTo 0) {
            if (scheduledDays[idx].isPerfectDay) {
                count += 1
            } else {
                break
            }
        }
        return count
    }

    private fun computeBestStreak(scheduledDays: List<StatisticsDayPoint>): Int {
        var best = 0
        var current = 0
        scheduledDays.forEach { day ->
            if (day.isPerfectDay) {
                current += 1
                if (current > best) best = current
            } else {
                current = 0
            }
        }
        return best
    }

    private fun computeInsights(
        source: StatisticsSourceBundle,
        habits: List<StatisticsHabitMetrics>
    ): StatisticsInsights {
        val bestHabitId = habits
            .maxWithOrNull(
                compareBy<StatisticsHabitMetrics> { it.completionRatePercent }
                    .thenBy { it.plannedDays }
                    .thenByDescending { it.habitId }
            )
            ?.habitId

        val mostStableHabitId = habits
            .maxWithOrNull(
                compareBy<StatisticsHabitMetrics> { it.completedDays }
                    .thenBy { it.completionRatePercent }
                    .thenByDescending { it.habitId }
            )
            ?.habitId

        val growthHabitId = computeGrowthHabitId(source)

        return StatisticsInsights(
            bestHabitId = bestHabitId,
            mostStableHabitId = mostStableHabitId,
            growthHabitId = growthHabitId
        )
    }

    private fun computeGrowthHabitId(source: StatisticsSourceBundle): Long? {
        val sortedDays = source.calendarDays.sortedBy { it.date }
        if (sortedDays.size < 2) return null

        val splitIndex = sortedDays.size / 2
        val firstHalf = sortedDays.subList(0, splitIndex)
        val secondHalf = sortedDays.subList(splitIndex, sortedDays.size)
        if (firstHalf.isEmpty() || secondHalf.isEmpty()) return null

        val firstStats = accumulateByHalf(firstHalf)
        val secondStats = accumulateByHalf(secondHalf)

        val candidates = (firstStats.keys + secondStats.keys)
            .mapNotNull { habitId ->
                val left = firstStats[habitId] ?: HalfStat()
                val right = secondStats[habitId] ?: HalfStat()
                val totalScheduled = left.plannedDays + right.plannedDays
                if (totalScheduled < 2) return@mapNotNull null

                val leftRate = ratio(left.completedDays, left.plannedDays)
                val rightRate = ratio(right.completedDays, right.plannedDays)
                val growth = rightRate - leftRate
                if (growth <= 0.0) return@mapNotNull null

                GrowthCandidate(
                    habitId = habitId,
                    growth = growth,
                    secondHalfPlannedDays = right.plannedDays
                )
            }

        return candidates.maxWithOrNull(
            compareBy<GrowthCandidate> { it.growth }
                .thenBy { it.secondHalfPlannedDays }
                .thenByDescending { it.habitId }
        )?.habitId
    }

    private fun accumulateByHalf(days: List<CalendarDayDto>): Map<Long, HalfStat> {
        val result = mutableMapOf<Long, HalfStat>()
        days.forEach { day ->
            day.habits.forEach { habit ->
                val item = result.getOrPut(habit.habitId) { HalfStat() }
                item.plannedDays += 1
                if (habit.summary.isCompleted) {
                    item.completedDays += 1
                }
            }
        }
        return result
    }

    private fun percent(numerator: Int, denominator: Int): Int {
        if (denominator == 0) return 0
        return (numerator * 100.0 / denominator.toDouble()).roundToInt()
    }

    private fun ratio(numerator: Int, denominator: Int): Double {
        if (denominator == 0) return 0.0
        return numerator.toDouble() / denominator.toDouble()
    }

    private data class HabitAccumulator(
        val habitId: Long,
        val fallbackName: String,
        val fallbackStatus: HabitStatus,
        var plannedDays: Int = 0,
        var completedDays: Int = 0,
        var doneTotal: Int = 0,
        var targetTotal: Int = 0
    )

    private data class HalfStat(
        var plannedDays: Int = 0,
        var completedDays: Int = 0
    )

    private data class GrowthCandidate(
        val habitId: Long,
        val growth: Double,
        val secondHalfPlannedDays: Int
    )
}
