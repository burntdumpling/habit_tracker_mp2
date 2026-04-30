package com.example.application.plus

import com.example.application.data.habits.HabitsRepository
import com.example.application.data.network.dto.CalendarDayDto
import com.example.application.data.network.dto.CalendarRangeDto
import com.example.application.data.network.dto.HabitLogDto
import com.example.application.data.network.dto.UpsertHabitLogRequest
import com.example.application.data.notifications.AppNotificationSettings
import com.example.application.data.notifications.HabitNotificationSettings
import com.example.application.data.notifications.NotificationSettingsRepository
import com.example.application.habit.CreateHabitRequest
import com.example.application.habit.DayPart
import com.example.application.habit.Habit
import com.example.application.habit.HabitStatus
import com.example.application.notifications.NotificationScheduler
import com.example.application.notifications.NotificationSchedulingCoordinator
import com.example.application.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CreateHabitViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun saveHabit_createMode_persistsReminderSettingsAfterSuccess() = runTest {
        val habitsRepository = FakeHabitsRepository()
        val notificationSettingsRepository = FakeNotificationSettingsRepository()
        val notificationScheduler = FakeNotificationScheduler()
        val notificationSchedulingCoordinator = NotificationSchedulingCoordinator(
            notificationSettingsRepository = notificationSettingsRepository,
            notificationScheduler = notificationScheduler
        )
        val viewModel = CreateHabitViewModel()
        var onSuccessCalled = false

        viewModel.updateName("Drink water")
        viewModel.updateDayPart(DayPart.MORNING)
        viewModel.updateReminderEnabled(true)
        viewModel.updateReminderTime("08:30")

        viewModel.saveHabit(
            repository = habitsRepository,
            notificationSettingsRepository = notificationSettingsRepository,
            notificationSchedulingCoordinator = notificationSchedulingCoordinator
        ) {
            onSuccessCalled = true
        }
        advanceUntilIdle()

        val createdHabitId = habitsRepository.lastCreatedHabitId
        assertEquals(true, onSuccessCalled)
        assertEquals(createdHabitId, notificationSettingsRepository.lastHabitId)
        assertEquals(true, notificationSettingsRepository.lastReminderEnabled)
        assertEquals("08:30", notificationSettingsRepository.lastReminderTime)
        assertEquals(createdHabitId, notificationScheduler.lastRescheduledHabitId)
    }

    @Test
    fun updateReminderEnabled_setsDefaultTimeByDayPartWhenTimeIsNull() {
        val viewModel = CreateHabitViewModel()

        viewModel.updateDayPart(DayPart.EVENING)
        viewModel.updateReminderEnabled(true)

        val state = viewModel.state.value
        assertTrue(state.reminderEnabled)
        assertEquals("20:00", state.reminderTime)
    }

    private class FakeHabitsRepository : HabitsRepository {
        private val habitsFlow = MutableStateFlow<List<Habit>>(emptyList())
        var lastCreatedHabitId: Long = 101L

        override val habits: MutableStateFlow<List<Habit>> = habitsFlow

        override suspend fun fetchHabits(status: String?): Result<Unit> = Result.success(Unit)

        override suspend fun addHabit(request: CreateHabitRequest): Result<Habit> {
            val habit = Habit(
                habitId = lastCreatedHabitId,
                status = HabitStatus.ACTIVE,
                name = request.name,
                iconId = request.iconId,
                colorArgb = request.colorArgb,
                createdAt = Instant.now(),
                startDate = request.startDate,
                endDate = request.endDate,
                frequency = request.frequency,
                targets = request.targets
            )
            habitsFlow.value = habitsFlow.value + habit
            return Result.success(habit)
        }

        override suspend fun deleteHabit(habitId: Long): Result<Unit> = Result.success(Unit)

        override fun getHabit(habitId: Long): Habit? = habitsFlow.value.firstOrNull { it.habitId == habitId }

        override suspend fun updateHabit(habit: Habit): Result<Unit> = Result.success(Unit)

        override suspend fun getHabitLogs(dateFrom: LocalDate, dateTo: LocalDate): Result<List<HabitLogDto>> {
            return Result.success(emptyList())
        }

        override suspend fun upsertHabitLog(habitId: Long, request: UpsertHabitLogRequest): Result<HabitLogDto> {
            return Result.failure(UnsupportedOperationException())
        }

        override suspend fun getCalendarRange(
            dateFrom: LocalDate,
            dateTo: LocalDate,
            status: String?
        ): Result<CalendarRangeDto> {
            return Result.failure(UnsupportedOperationException())
        }

        override suspend fun getCalendarDay(date: LocalDate, status: String?): Result<CalendarDayDto> {
            return Result.failure(UnsupportedOperationException())
        }
    }

    private class FakeNotificationSettingsRepository : NotificationSettingsRepository {
        private val appSettings = MutableStateFlow(AppNotificationSettings())

        var lastHabitId: Long? = null
        var lastReminderEnabled: Boolean? = null
        var lastReminderTime: String? = null

        override val appSettingsFlow: Flow<AppNotificationSettings> = appSettings

        override suspend fun setNotificationsEnabled(enabled: Boolean) = Unit

        override fun habitSettingsFlow(habitId: Long): Flow<HabitNotificationSettings> {
            return MutableStateFlow(HabitNotificationSettings(habitId = habitId))
        }

        override suspend fun updateHabitReminderEnabled(habitId: Long, enabled: Boolean) {
            lastHabitId = habitId
            lastReminderEnabled = enabled
        }

        override suspend fun updateHabitReminderTime(habitId: Long, reminderTime: String?) {
            lastHabitId = habitId
            lastReminderTime = reminderTime
        }

        override suspend fun updateLastReminderScheduledAt(habitId: Long, value: String?) = Unit

        override suspend fun updateLastCompletionNotifiedDate(habitId: Long, value: String?) = Unit

        override suspend fun updateLastStreakNotified(habitId: Long, value: Int?) = Unit

        override suspend fun clearHabitSettings(habitId: Long) = Unit

        override suspend fun getHabitSettingsOnce(habitId: Long): HabitNotificationSettings {
            return HabitNotificationSettings(habitId = habitId)
        }
    }

    private class FakeNotificationScheduler : NotificationScheduler {
        var lastRescheduledHabitId: Long? = null

        override suspend fun scheduleHabitReminder(habitId: Long) = Unit
        override suspend fun cancelHabitReminder(habitId: Long) = Unit
        override suspend fun rescheduleHabitReminder(habitId: Long) {
            lastRescheduledHabitId = habitId
        }
        override suspend fun scheduleAllHabits() = Unit
        override suspend fun cancelAllHabits() = Unit
    }
}
