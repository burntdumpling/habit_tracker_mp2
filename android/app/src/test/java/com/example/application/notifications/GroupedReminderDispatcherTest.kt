package com.example.application.notifications

import com.example.application.data.notifications.PendingReminderRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupedReminderDispatcherTest {

    @Test
    fun emptyBucket_isNoOp() = runTest {
        val repository = FakePendingReminderRepository()
        val notifier = FakeReminderNotifier()
        val dispatcher = GroupedReminderDispatcher(repository, ReminderDispatchDecider(), notifier)

        val result = dispatcher.dispatch(bucketKey = 1_710_000L)

        assertTrue(!result.dispatched)
        assertNull(notifier.singlePayload)
        assertNull(notifier.groupedPayload)
    }

    @Test
    fun dispatchClearsBucketAfterSingleReminder() = runTest {
        val repository = FakePendingReminderRepository()
        val bucketKey = 1_710_000L
        repository.addPendingReminder(bucketKey, pendingReminder(habitId = 1L, name = "Read"))
        val notifier = FakeReminderNotifier()
        val dispatcher = GroupedReminderDispatcher(repository, ReminderDispatchDecider(), notifier)

        val result = dispatcher.dispatch(bucketKey)

        assertTrue(result.dispatched)
        assertEquals("Read", notifier.singlePayload?.habitName)
        assertTrue(repository.getPendingReminders(bucketKey).isEmpty())
    }

    @Test
    fun dispatchUsesGroupedReminderForMultipleItems() = runTest {
        val repository = FakePendingReminderRepository()
        val bucketKey = 1_710_000L
        repository.addPendingReminder(bucketKey, pendingReminder(habitId = 1L, name = "Read"))
        repository.addPendingReminder(bucketKey, pendingReminder(habitId = 2L, name = "Run"))
        val notifier = FakeReminderNotifier()
        val dispatcher = GroupedReminderDispatcher(repository, ReminderDispatchDecider(), notifier)

        val result = dispatcher.dispatch(bucketKey)

        assertTrue(result.dispatched)
        assertEquals(2, notifier.groupedPayload?.count)
        assertEquals(listOf("Read", "Run"), notifier.groupedPayload?.habitNames)
        assertTrue(repository.getPendingReminders(bucketKey).isEmpty())
    }

    private class FakeReminderNotifier : ReminderNotifier {
        var singlePayload: ReminderNotificationPayload? = null
        var groupedPayload: GroupedReminderPayload? = null

        override fun showReminder(payload: ReminderNotificationPayload) {
            singlePayload = payload
        }

        override fun showGroupedReminder(payload: GroupedReminderPayload) {
            groupedPayload = payload
        }
    }

    private class FakePendingReminderRepository : PendingReminderRepository {
        private val storage = linkedMapOf<Long, MutableList<PendingReminder>>()

        override suspend fun addPendingReminder(bucketKey: Long, reminder: PendingReminder) {
            val bucket = storage.getOrPut(bucketKey) { mutableListOf() }
            bucket.removeAll { it.habitId == reminder.habitId }
            bucket.add(reminder)
            bucket.sortBy { it.scheduledAtMillis }
        }

        override suspend fun getPendingReminders(bucketKey: Long): List<PendingReminder> {
            return storage[bucketKey]?.toList().orEmpty()
        }

        override suspend fun removePendingReminders(bucketKey: Long, habitIds: Set<Long>) {
            val bucket = storage[bucketKey] ?: return
            bucket.removeAll { it.habitId in habitIds }
            if (bucket.isEmpty()) {
                storage.remove(bucketKey)
            }
        }

        override suspend fun clearBucket(bucketKey: Long) {
            storage.remove(bucketKey)
        }
    }

    private fun pendingReminder(habitId: Long, name: String): PendingReminder {
        return PendingReminder(
            habitId = habitId,
            habitName = name,
            bodyText = name,
            date = "2026-04-30",
            scheduledAtMillis = 1_000L + habitId
        )
    }
}
