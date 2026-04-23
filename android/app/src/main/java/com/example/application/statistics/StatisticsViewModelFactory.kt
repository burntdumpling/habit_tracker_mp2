package com.example.application.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.application.data.statistics.StatisticsAggregator
import com.example.application.data.statistics.StatisticsComputedData
import com.example.application.data.statistics.StatisticsRepository
import com.example.application.data.statistics.StatisticsSourceBundle

class StatisticsViewModelFactory(
    private val repository: StatisticsRepository,
    private val aggregate: (StatisticsSourceBundle) -> StatisticsComputedData = StatisticsAggregator::compute
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatisticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatisticsViewModel(
                repository = repository,
                aggregate = aggregate
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
