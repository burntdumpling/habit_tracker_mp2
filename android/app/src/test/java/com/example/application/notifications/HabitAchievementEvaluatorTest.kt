package com.example.application.notifications

import com.example.application.data.habits.HabitsRepository
import com.example.application.data.network.dto.CalendarDayDto
import com.example.application.data.network.dto.CalendarDaySummaryDto
import com.example.application.data.network.dto.CalendarHabitDto
import com.example.application.data.network.dto.CalendarHabitSummaryDto
import com.example.application.data.network.dto.CalendarRangeDto
import com.example.application.data.network.dto.HabitLogDto
import com.example.application.data.network.dto.UpsertHabitLogRequest
import com.example.application.data.notifications.AppNotificationSettings
import com.example.application.data.notifications.HabitNotificationSettings
import com.example.application.data.notifications.NotificationSettingsRepository
import com.example.application.habit.CreateHabitRequest
import com.example.application.habit.DayPart
import com.example.application.habit.Frequency
import com.example.application.habit.Habit
import com.example.application.habit.HabitStatus
import com.example.application.habit.HabitTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HabitAchievementEvaluatorTest {

    private val zoneId = ZoneId.of("Europe/Moscow")
    private val today = LocalDate.now(zoneId)

    @Test
    fun completedToday_dailyNotSent_sendsDailyAndStoresDate() = runTest {
        val habitsRepository = FakeHabitsRepository(today)
        val notificationRepository = FakeNotificationSettingsRepository()
        val completionChecker = FakeCompletionChecker(true)
        val streakCalculator = FakeStreakCalculator(0)
        val notifier = FakeNotifier()
        val evaluator = DefaultHabitAchievementEvaluator(
            habitsRepository = habitsRepository,
            notificationSettingsRepository = notificationRepository,
            habitCompletionChecker = completionChecker,
            habitStreakCalculator = streakCalculator,
            achievementNotifier = notifier,
            notificationAvailabilityChecker = FakeAvailabilityChecker(true),
            zoneId = zoneId
        )

        evaluator.evaluateForHabitDate(1L, today)

        assertEquals(1, notifier.dailyCalls)
        assertEquals(today.toString(), notificationRepository.lastCompletionNotifiedDate)
    }

    @Test
    fun completedToday_sameCompletionDate_doesNotSendDuplicateDaily() = runTest {
        val habitsRepository = FakeHabitsRepository(today)
        val notificationRepository = FakeNotificationSettingsRepository(
            initialSettings = HabitNotificationSettings(
                habitId = 1L,
                lastCompletionNotifiedDate = today.toString()
            )
        )
        val notifier = FakeNotifier()
        val evaluator = DefaultHabitAchievementEvaluator(
            habitsRepository = habitsRepository,
            notificationSettingsRepository = notificationRepository,
            habitCompletionChecker = FakeCompletionChecker(true),
            habitStreakCalculator = FakeStreakCalculator(0),
            achievementNotifier = notifier,
            notificationAvailabilityChecker = FakeAvailabilityChecker(true),
            zoneId = zoneId
        )

        evaluator.evaluateForHabitDate(1L, today)

        assertEquals(0, notifier.dailyCalls)
    }

    @Test
    fun streakReaches3_withNullLast_sendsStreak3() = runTest {
        val notifier = FakeNotifier()
        val notificationRepository = FakeNotificationSettingsRepository()
        val evaluator = DefaultHabitAchievementEvaluator(
            habitsRepository = FakeHabitsRepository(today),
            notificationSettingsRepository = notificationRepository,
            habitCompletionChecker = FakeCompletionChecker(true),
            habitStreakCalculator = FakeStreakCalculator(3),
            achievementNotifier = notifier,
            notificationAvailabilityChecker = FakeAvailabilityChecker(true),
            zoneId = zoneId
        )

        evaluator.evaluateForHabitDate(1L, today)

        assertEquals(1, notifier.streakCalls)
        assertEquals(3, notificationRepository.lastStreakNotified)
    }

    @Test
    fun streakReaches7_withLast3_sendsStreak7() = runTest {
        val notifier = FakeNotifier()
        val notificationRepository = FakeNotificationSettingsRepository(
            initialSettings = HabitNotificationSettings(habitId = 1L, lastStreakNotified = 3)
        )
        val evaluator = DefaultHabitAchievementEvaluator(
            habitsRepository = FakeHabitsRepository(today),
            notificationSettingsRepository = notificationRepository,
            habitCompletionChecker = FakeCompletionChecker(true),
            habitStreakCalculator = FakeStreakCalculator(7),
            achievementNotifier = notifier,
            notificationAvailabilityChecker = FakeAvailabilityChecker(true),
            zoneId = zoneId
        )

        evaluator.evaluateForHabitDate(1L, today)

        assertEquals(1, notifier.streakCalls)
        assertEquals(7, notificationRepository.lastStreakNotified)
    }

    @Test
    fun streakReaches7_withLast7_doesNotSendDuplicate() = runTest {
        val notifier = FakeNotifier()
        val notificationRepository = FakeNotificationSettingsRepository(
            initialSettings = HabitNotificationSettings(habitId = 1L, lastStreakNotified = 7)
        )
        val evaluator = DefaultHabitAchievementEvaluator(
            habitsRepository = FakeHabitsRepository(today),
            notificationSettingsRepository = notificationRepository,
            habitCompletionChecker = FakeCompletionChecker(true),
            habitStreakCalculator = FakeStreakCalculator(7),
            achievementNotifier = notifier,
            notificationAvailabilityChecker = FakeAvailabilityChecker(true),
            zoneId = zoneId
        )

        evaluator.evaluateForHabitDate(1L, today)

        assertEquals(0, notifier.streakCalls)
    }

    @Test
    fun pastDate_noOp() = runTest {
        val notifier = FakeNotifier()
        val notificationRepository = FakeNotificationSettingsRepository()
        val evaluator = DefaultHabitAchievementEvaluator(
            habitsRepository = FakeHabitsRepository(today),
            notificationSettingsRepository = notificationRepository,
            habitCompletionChecker = FakeCompletionChecker(true),
            habitStreakCalculator = FakeStreakCalculator(30),
            achievementNotifier = notifier,
            notificationAvailabilityChecker = FakeAvailabilityChecker(true),
            zoneId = zoneId
        )

        evaluator.evaluateForHabitDate(1L, today.minusDays(1))

        assertEquals(0, notifier.dailyCalls)
        assertEquals(0, notifier.streakCalls)
        assertEquals(null, notificationRepository.lastCompletionNotifiedDate)
        assertEquals(null, notificationRepository.lastStreakNotified)
    }

    @Test
    fun notificationsDisabled_orPermissionMissing_noSendNoDedupeUpdate() = runTest {
        val notifier = FakeNotifier()
        val notificationRepositoryDisabled = FakeNotificationSettingsRepository(
            appSettings = AppNotificationSettings(notificationsEnabled = false)
        )
        val evaluatorDisabled = DefaultHabitAchievementEvaluator(
            habitsRepository = FakeHabitsRepository(today),
            notificationSettingsRepository = notificationRepositoryDisabled,
            habitCompletionChecker = FakeCompletionChecker(true),
            habitStreakCalculator = FakeStreakCalculator(7),
            achievementNotifier = notifier,
            notificationAvailabilityChecker = FakeAvailabilityChecker(true),
            zoneId = zoneId
        )

        evaluatorDisabled.evaluateForHabitDate(1L, today)

        assertEquals(0, notifier.dailyCalls)
        assertEquals(0, notifier.streakCalls)
        assertEquals(null, notificationRepositoryDisabled.lastCompletionNotifiedDate)
        assertEquals(null, notificationRepositoryDisabled.lastStreakNotified)

        val notificationRepositoryNoPermission = FakeNotificationSettingsRepository()
        val evaluatorNoPermission = DefaultHabitAchievementEvaluator(
            habitsRepository = FakeHabitsRepository(today),
            notificationSettingsRepository = notificationRepositoryNoPermission,
            habitCompletionChecker = FakeCompletionChecker(true),
            habitStreakCalculator = FakeStreakCalculator(7),
            achievementNotifier = notifier,
            notificationAvailabilityChecker = FakeAvailabilityChecker(false),
            zoneId = zoneId
        )

        evaluatorNoPermission.evaluateForHabitDate(1L, today)

        assertEquals(0, notifier.dailyCalls)
        assertEquals(0, notifier.streakCalls)
        assertEquals(null, notificationRepositoryNoPermission.lastCompletionNotifiedDate)
        assertEquals(null, notificationRepositoryNoPermission.lastStreakNotified)
    }

    private class FakeCompletionChecker(
        private val completed: Boolean
    ) : HabitCompletionChecker {
        override suspend fun isHabitCompletedOnDate(habitId: Long, date: LocalDate): Boolean = completed
    }

    private class FakeStreakCalculator(
        private val streak: Int
    ) : HabitStreakCalculator {
        override suspend fun getCurrentStreak(habitId: Long, date: LocalDate): Int = streak
    }

    private class FakeAvailabilityChecker(
        private val available: Boolean
    ) : SystemNotificationAvailabilityChecker {
        override fun areNotificationsAvailable(): Boolean = available
    }

    private class FakeNotifier : HabitAchievementNotifier {
        var dailyCalls = 0
        var streakCalls = 0

        override fun showDailyCompletion(payload: AchievementNotificationPayload) {
            dailyCalls += 1
        }

        override fun showStreakAchievement(payload: AchievementNotificationPayload) {
            streakCalls += 1
        }
    }

    private class FakeNotificationSettingsRepository(
        appSettings: AppNotificationSettings = AppNotificationSettings(notificationsEnabled = true),
        initialSettings: HabitNotificationSettings = HabitNotificationSettings(habitId = 1L)
    ) : NotificationSettingsRepository {
        private val appState = MutableStateFlow(appSettings)
        private var habitState: HabitNotificationSettings = initialSettings
        var lastCompletionNotifiedDate: String? = null
        var lastStreakNotified: Int? = null

        override val appSettingsFlow: Flow<AppNotificationSettings> = appState
        override suspend fun setNotificationsEnabled(enabled: Boolean) {
            appState.value = AppNotificationSettings(notificationsEnabled = enabled)
        }
        override fun habitSettingsFlow(habitId: Long): Flow<HabitNotificationSettings> {
            return MutableStateFlow(habitState.copy(habitId = habitId))
        }
        override suspend fun updateHabitReminderEnabled(habitId: Long, enabled: Boolean) = Unit
        override suspend fun updateHabitReminderTime(habitId: Long, reminderTime: String?) = Unit
        override suspend fun updateLastReminderScheduledAt(habitId: Long, value: String?) = Unit
        override suspend fun updateLastCompletionNotifiedDate(habitId: Long, value: String?) {
            lastCompletionNotifiedDate = value
            habitState = habitState.copy(lastCompletionNotifiedDate = value)
        }
        override suspend fun updateLastStreakNotified(habitId: Long, value: Int?) {
            lastStreakNotified = value
            habitState = habitState.copy(lastStreakNotified = value)
        }
        override suspend fun clearHabitSettings(habitId: Long) = Unit
        override suspend fun getHabitSettingsOnce(habitId: Long): HabitNotificationSettings {
            return habitState.copy(habitId = habitId)
        }
    }

    private class FakeHabitsRepository(
        private val date: LocalDate
    ) : HabitsRepository {
        private val habit = Habit(
            habitId = 1L,
            status = HabitStatus.ACTIVE,
            name = "Пить воду",
            iconId = 1,
            colorArgb = 0xFFE0E0E0.toInt(),
            createdAt = Instant.parse("2026-04-01T00:00:00Z"),
            startDate = date.minusDays(90),
            endDate = null,
            frequency = Frequency.Daily,
            targets = listOf(HabitTarget(dayPart = DayPart.FULL_DAY, targetCount = 8))
        )

        override val habits: MutableStateFlow<List<Habit>> = MutableStateFlow(listOf(habit))

        override suspend fun fetchHabits(status: String?): Result<Unit> = Result.success(Unit)
        override suspend fun addHabit(request: CreateHabitRequest): Result<Habit> = Result.failure(NotImplementedError())
        override suspend fun deleteHabit(habitId: Long): Result<Unit> = Result.failure(NotImplementedError())
        override fun getHabit(habitId: Long): Habit? = if (habitId == habit.habitId) habit else null
        override suspend fun updateHabit(habit: Habit): Result<Unit> = Result.failure(NotImplementedError())
        override suspend fun getHabitLogs(dateFrom: LocalDate, dateTo: LocalDate): Result<List<HabitLogDto>> {
            return Result.failure(NotImplementedError())
        }
        override suspend fun upsertHabitLog(habitId: Long, request: UpsertHabitLogRequest): Result<HabitLogDto> {
            return Result.failure(NotImplementedError())
        }
        override suspend fun getCalendarRange(
            dateFrom: LocalDate,
            dateTo: LocalDate,
            status: String?
        ): Result<CalendarRangeDto> = Result.failure(NotImplementedError())

        override suspend fun getCalendarDay(date: LocalDate, status: String?): Result<CalendarDayDto> {
            val dayDto = CalendarDayDto(
                date = date.toString(),
                summary = CalendarDaySummaryDto(plannedCount = 1, completedCount = 1, hasAnyCompleted = true),
                habits = listOf(
                    CalendarHabitDto(
                        habitId = 1L,
                        name = "Пить воду",
                        iconId = 1,
                        status = HabitStatus.ACTIVE,
                        targets = emptyList(),
                        actual = emptyList(),
                        summary = CalendarHabitSummaryDto(targetTotal = 8, actualTotal = 8, isCompleted = true)
                    )
                )
            )
            return Result.success(dayDto)
        }
    }
}
