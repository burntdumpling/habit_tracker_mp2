package com.example.application.data.statistics

import com.example.application.data.network.api.HabitApiService
import com.example.application.data.network.dto.CalendarDayDto
import com.example.application.data.network.dto.HabitDto
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class NetworkStatisticsRepository(
    private val api: HabitApiService
) : StatisticsRepository {

    override suspend fun getStatisticsSources(request: StatisticsRequest): Result<StatisticsSourceBundle> = runCatching {
        val habits = api.getHabits()

        val (dateFrom, dateTo) = when (request.period) {
            StatisticsPeriod.WEEK -> resolveWeekRange(request.anchorDate)
            StatisticsPeriod.MONTH -> resolveMonthRange(request.anchorDate)
            StatisticsPeriod.ALL -> resolveAllRange(request.anchorDate, habits)
        }

        val calendarDays = when (request.period) {
            StatisticsPeriod.WEEK,
            StatisticsPeriod.MONTH -> loadCalendarDaysForRange(dateFrom, dateTo)

            StatisticsPeriod.ALL -> {
                if (habits.isEmpty() || request.anchorDate < dateFrom) {
                    emptyList()
                } else {
                    loadCalendarDaysBatched(dateFrom, dateTo)
                }
            }
        }

        StatisticsSourceBundle(
            period = request.period,
            dateFrom = dateFrom,
            dateTo = dateTo,
            habits = habits,
            calendarDays = calendarDays
        )
    }

    private fun resolveWeekRange(anchorDate: LocalDate): Pair<LocalDate, LocalDate> {
        val start = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val end = anchorDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        return start to end
    }

    private fun resolveMonthRange(anchorDate: LocalDate): Pair<LocalDate, LocalDate> {
        val start = anchorDate.withDayOfMonth(1)
        val end = anchorDate.withDayOfMonth(anchorDate.lengthOfMonth())
        return start to end
    }

    private fun resolveAllRange(
        anchorDate: LocalDate,
        habits: List<HabitDto>
    ): Pair<LocalDate, LocalDate> {
        if (habits.isEmpty()) {
            return anchorDate to anchorDate
        }

        val minStartDate = habits.minOf { LocalDate.parse(it.startDate) }
        if (anchorDate < minStartDate) {
            return minStartDate to minStartDate
        }

        return minStartDate to anchorDate
    }

    private suspend fun loadCalendarDaysForRange(
        dateFrom: LocalDate,
        dateTo: LocalDate
    ): List<CalendarDayDto> {
        val range = api.getCalendarRange(dateFrom = dateFrom.toString(), dateTo = dateTo.toString())
        return range.days
            .distinctBy { it.date }
            .sortedBy { it.date }
    }

    private suspend fun loadCalendarDaysBatched(
        dateFrom: LocalDate,
        dateTo: LocalDate
    ): List<CalendarDayDto> {
        if (dateTo < dateFrom) {
            return emptyList()
        }

        val byDate = linkedMapOf<String, CalendarDayDto>()
        var currentFrom = dateFrom

        while (!currentFrom.isAfter(dateTo)) {
            val currentTo = minOf(currentFrom.plusDays(MAX_RANGE_DAYS - 1L), dateTo)
            val response = api.getCalendarRange(
                dateFrom = currentFrom.toString(),
                dateTo = currentTo.toString()
            )
            response.days.forEach { day ->
                byDate[day.date] = day
            }
            currentFrom = currentTo.plusDays(1)
        }

        return byDate
            .values
            .sortedBy { it.date }
    }

    private companion object {
        const val MAX_RANGE_DAYS = 31L
    }
}
