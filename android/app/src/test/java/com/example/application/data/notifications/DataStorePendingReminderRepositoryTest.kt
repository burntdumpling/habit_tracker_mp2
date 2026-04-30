package com.example.application.data.notifications

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.application.notifications.PendingReminder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStorePendingReminderRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun duplicateHabitIdInBucket_isDeduplicated() = runTest {
        val repository = createRepository()
        val bucketKey = 1_710_000L

        repository.addPendingReminder(bucketKey, pendingReminder(habitId = 1L, name = "First"))
        repository.addPendingReminder(bucketKey, pendingReminder(habitId = 1L, name = "Updated"))

        val reminders = repository.getPendingReminders(bucketKey)

        assertEquals(1, reminders.size)
        assertEquals("Updated", reminders.single().habitName)
    }

    @Test
    fun clearBucket_removesAllPendingReminders() = runTest {
        val repository = createRepository()
        val bucketKey = 1_710_000L

        repository.addPendingReminder(bucketKey, pendingReminder(habitId = 1L, name = "First"))
        repository.addPendingReminder(bucketKey, pendingReminder(habitId = 2L, name = "Second"))
        repository.clearBucket(bucketKey)

        assertTrue(repository.getPendingReminders(bucketKey).isEmpty())
    }

    private fun createRepository(): DataStorePendingReminderRepository {
        val dataStore = PreferenceDataStoreFactory.create {
            temporaryFolder.newFile("pending_${System.nanoTime()}.preferences_pb")
        }
        return DataStorePendingReminderRepository(dataStore)
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
