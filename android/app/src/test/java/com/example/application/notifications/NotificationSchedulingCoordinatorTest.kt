package com.example.application.notifications

import com.example.application.data.notifications.AppNotificationSettings
import com.example.application.data.notifications.HabitNotificationSettings
import com.example.application.data.notifications.NotificationSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class NotificationSchedulingCoordinatorTest {

    @Test
    fun onGlobalNotificationsChanged_false_callsCancelAll() = runTest {
        val scheduler = FakeNotificationScheduler()
        val repository = FakeNotificationSettingsRepository()
        val coordinator = NotificationSchedulingCoordinator(repository, scheduler)

        coordinator.onGlobalNotificationsChanged(false)

        assertEquals(1, scheduler.cancelAllCalls)
        assertEquals(0, scheduler.scheduleAllCalls)
    }

    @Test
    fun onGlobalNotificationsChanged_true_callsScheduleAll() = runTest {
        val scheduler = FakeNotificationScheduler()
        val repository = FakeNotificationSettingsRepository()
        val coordinator = NotificationSchedulingCoordinator(repository, scheduler)

        coordinator.onGlobalNotificationsChanged(true)

        assertEquals(1, scheduler.scheduleAllCalls)
        assertEquals(0, scheduler.cancelAllCalls)
    }

    @Test
    fun onHabitCreatedOrUpdated_callsReschedule() = runTest {
        val scheduler = FakeNotificationScheduler()
        val repository = FakeNotificationSettingsRepository()
        val coordinator = NotificationSchedulingCoordinator(repository, scheduler)

        coordinator.onHabitCreatedOrUpdated(42L)

        assertEquals(42L, scheduler.lastRescheduledHabitId)
    }

    @Test
    fun onHabitDeleted_callsCancelHabit() = runTest {
        val scheduler = FakeNotificationScheduler()
        val repository = FakeNotificationSettingsRepository()
        val coordinator = NotificationSchedulingCoordinator(repository, scheduler)

        coordinator.onHabitDeleted(55L)

        assertEquals(55L, scheduler.lastCanceledHabitId)
    }

    @Test
    fun onHabitCompletedForDate_todayAndCompleted_callsReschedule() = runTest {
        val zoneId = ZoneId.of("Europe/Moscow")
        val today = LocalDate.now(zoneId)
        val scheduler = FakeNotificationScheduler()
        val repository = FakeNotificationSettingsRepository()
        val completionChecker = FakeHabitCompletionChecker(completed = true)
        val coordinator = NotificationSchedulingCoordinator(
            notificationSettingsRepository = repository,
            notificationScheduler = scheduler,
            habitCompletionChecker = completionChecker,
            zoneId = zoneId
        )

        coordinator.onHabitCompletedForDate(10L, today)

        assertEquals(10L, scheduler.lastRescheduledHabitId)
    }

    @Test
    fun onHabitCompletedForDate_pastDate_noop() = runTest {
        val zoneId = ZoneId.of("Europe/Moscow")
        val today = LocalDate.now(zoneId)
        val scheduler = FakeNotificationScheduler()
        val repository = FakeNotificationSettingsRepository()
        val completionChecker = FakeHabitCompletionChecker(completed = true)
        val coordinator = NotificationSchedulingCoordinator(
            notificationSettingsRepository = repository,
            notificationScheduler = scheduler,
            habitCompletionChecker = completionChecker,
            zoneId = zoneId
        )

        coordinator.onHabitCompletedForDate(10L, today.minusDays(1))

        assertEquals(null, scheduler.lastRescheduledHabitId)
    }

    private class FakeNotificationScheduler : NotificationScheduler {
        var scheduleAllCalls: Int = 0
        var cancelAllCalls: Int = 0
        var lastRescheduledHabitId: Long? = null
        var lastCanceledHabitId: Long? = null

        override suspend fun scheduleHabitReminder(habitId: Long) = Unit

        override suspend fun cancelHabitReminder(habitId: Long) {
            lastCanceledHabitId = habitId
        }

        override suspend fun rescheduleHabitReminder(habitId: Long) {
            lastRescheduledHabitId = habitId
        }

        override suspend fun scheduleAllHabits() {
            scheduleAllCalls += 1
        }

        override suspend fun cancelAllHabits() {
            cancelAllCalls += 1
        }
    }

    private class FakeNotificationSettingsRepository : NotificationSettingsRepository {
        private val appSettings = MutableStateFlow(AppNotificationSettings())

        override val appSettingsFlow: Flow<AppNotificationSettings> = appSettings

        override suspend fun setNotificationsEnabled(enabled: Boolean) {
            appSettings.value = AppNotificationSettings(notificationsEnabled = enabled)
        }

        override fun habitSettingsFlow(habitId: Long): Flow<HabitNotificationSettings> {
            return MutableStateFlow(HabitNotificationSettings(habitId = habitId))
        }

        override suspend fun updateHabitReminderEnabled(habitId: Long, enabled: Boolean) = Unit
        override suspend fun updateHabitReminderTime(habitId: Long, reminderTime: String?) = Unit
        override suspend fun updateLastReminderScheduledAt(habitId: Long, value: String?) = Unit
        override suspend fun updateLastCompletionNotifiedDate(habitId: Long, value: String?) = Unit
        override suspend fun updateLastStreakNotified(habitId: Long, value: Int?) = Unit
        override suspend fun clearHabitSettings(habitId: Long) = Unit
        override suspend fun getHabitSettingsOnce(habitId: Long): HabitNotificationSettings {
            return HabitNotificationSettings(habitId = habitId)
        }
    }

    private class FakeHabitCompletionChecker(
        private val completed: Boolean
    ) : HabitCompletionChecker {
        override suspend fun isHabitCompletedOnDate(habitId: Long, date: LocalDate): Boolean {
            return completed
        }
    }
}
