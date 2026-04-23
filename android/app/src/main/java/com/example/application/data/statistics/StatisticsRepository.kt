package com.example.application.data.statistics

interface StatisticsRepository {
    suspend fun getStatisticsSources(request: StatisticsRequest): Result<StatisticsSourceBundle>
}

