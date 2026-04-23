package com.example.application.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.application.data.statistics.StatisticsAggregator
import com.example.application.data.statistics.StatisticsComputedData
import com.example.application.data.statistics.StatisticsPeriod
import com.example.application.data.statistics.StatisticsRepository
import com.example.application.data.statistics.StatisticsRequest
import com.example.application.data.statistics.StatisticsSourceBundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

data class StatisticsUiState(
    val selectedPeriod: StatisticsPeriod = StatisticsPeriod.WEEK,
    val anchorDate: LocalDate = LocalDate.now(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val data: StatisticsComputedData? = null
)

class StatisticsViewModel(
    private val repository: StatisticsRepository,
    private val aggregate: (StatisticsSourceBundle) -> StatisticsComputedData = StatisticsAggregator::compute
) : ViewModel() {

    private val loadRequestId = AtomicLong(0L)

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        val requestId = loadRequestId.incrementAndGet()
        val stateSnapshot = _uiState.value

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val request = StatisticsRequest(
                period = stateSnapshot.selectedPeriod,
                anchorDate = stateSnapshot.anchorDate
            )

            repository.getStatisticsSources(request)
                .onSuccess { sources ->
                    if (requestId != loadRequestId.get()) return@onSuccess
                    val computedData = aggregate(sources)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            data = computedData
                        )
                    }
                }
                .onFailure { throwable ->
                    if (requestId != loadRequestId.get()) return@onFailure
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Failed to load statistics"
                        )
                    }
                }
        }
    }

    fun changePeriod(period: StatisticsPeriod) {
        if (_uiState.value.selectedPeriod == period) return
        _uiState.update { it.copy(selectedPeriod = period) }
        loadStats()
    }

    fun changeAnchorDate(date: LocalDate) {
        if (_uiState.value.anchorDate == date) return
        _uiState.update { it.copy(anchorDate = date) }
        loadStats()
    }

    fun retry() {
        loadStats()
    }
}
