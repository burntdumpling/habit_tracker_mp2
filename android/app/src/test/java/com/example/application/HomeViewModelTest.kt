package com.example.application

import com.example.application.data.habits.HabitsRepository
import com.example.application.data.network.dto.CalendarDayDto
import com.example.application.data.network.dto.CalendarRangeDto
import com.example.application.data.network.dto.HabitLogDto
import com.example.application.data.network.dto.UpsertHabitLogRequest
import com.example.application.data.settings.HabitStatusFilter
import com.example.application.data.settings.SettingsPreferencesRepository
import com.example.application.habit.CreateHabitRequest
import com.example.application.habit.Habit
import com.example.application.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun default_filter_loads_habits_without_status() = runTest {
        val habitsRepository = FakeHabitsRepository()
        HomeViewModel(
            repository = habitsRepository,
            settingsPreferencesRepository = FakeSettingsPreferencesRepository()
        )
        advanceUntilIdle()

        assertEquals(listOf(null), habitsRepository.fetchStatuses)
    }

    @Test
    fun filter_change_reloads_habits_with_api_status() = runTest {
        val habitsRepository = FakeHabitsRepository()
        val settingsRepository = FakeSettingsPreferencesRepository()
        HomeViewModel(
            repository = habitsRepository,
            settingsPreferencesRepository = settingsRepository
        )
        advanceUntilIdle()

        settingsRepository.emit(HabitStatusFilter.ACTIVE)
        advanceUntilIdle()

        assertEquals(listOf(null, "ACTIVE"), habitsRepository.fetchStatuses)
    }

    private class FakeSettingsPreferencesRepository : SettingsPreferencesRepository {
        private val filter = MutableStateFlow(HabitStatusFilter.ALL)

        override val habitStatusFilter = filter

        override suspend fun setHabitStatusFilter(filter: HabitStatusFilter) {
            this.filter.value = filter
        }

        fun emit(filter: HabitStatusFilter) {
            this.filter.value = filter
        }
    }

    private class FakeHabitsRepository : HabitsRepository {
        private val _habits = MutableStateFlow<List<Habit>>(emptyList())
        val fetchStatuses = mutableListOf<String?>()

        override val habits: StateFlow<List<Habit>> = _habits

        override suspend fun fetchHabits(status: String?): Result<Unit> {
            fetchStatuses += status
            return Result.success(Unit)
        }

        override suspend fun addHabit(request: CreateHabitRequest): Result<Habit> {
            return Result.failure(NotImplementedError())
        }

        override suspend fun deleteHabit(habitId: Long): Result<Unit> {
            return Result.failure(NotImplementedError())
        }

        override fun getHabit(habitId: Long): Habit? = null

        override suspend fun updateHabit(habit: Habit): Result<Unit> {
            return Result.failure(NotImplementedError())
        }

        override suspend fun getHabitLogs(
            dateFrom: LocalDate,
            dateTo: LocalDate
        ): Result<List<HabitLogDto>> {
            return Result.success(emptyList())
        }

        override suspend fun upsertHabitLog(
            habitId: Long,
            request: UpsertHabitLogRequest
        ): Result<HabitLogDto> {
            return Result.failure(NotImplementedError())
        }

        override suspend fun getCalendarRange(
            dateFrom: LocalDate,
            dateTo: LocalDate,
            status: String?
        ): Result<CalendarRangeDto> {
            return Result.failure(NotImplementedError())
        }

        override suspend fun getCalendarDay(
            date: LocalDate,
            status: String?
        ): Result<CalendarDayDto> {
            return Result.failure(NotImplementedError())
        }
    }
}
