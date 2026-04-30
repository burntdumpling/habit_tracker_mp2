package com.example.application.data.notifications

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.application.notifications.PendingReminder
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first

private val Context.pendingReminderDataStore by preferencesDataStore(name = "pending_reminders")

class DataStorePendingReminderRepository(
    private val dataStore: DataStore<Preferences>,
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
) : PendingReminderRepository {

    constructor(context: Context) : this(context.pendingReminderDataStore)

    private val adapter = moshi.adapter<List<PendingReminder>>(
        Types.newParameterizedType(List::class.java, PendingReminder::class.java)
    )

    override suspend fun addPendingReminder(bucketKey: Long, reminder: PendingReminder) {
        dataStore.edit { preferences ->
            val key = bucketKey(bucketKey)
            val updated = decode(preferences[key])
                .filterNot { it.habitId == reminder.habitId }
                .plus(reminder)
                .sortedWith(compareBy<PendingReminder> { it.scheduledAtMillis }.thenBy { it.habitId })

            preferences[key] = encode(updated)
        }
    }

    override suspend fun getPendingReminders(bucketKey: Long): List<PendingReminder> {
        val preferences = dataStore.data.first()
        return decode(preferences[bucketKey(bucketKey)])
    }

    override suspend fun removePendingReminders(bucketKey: Long, habitIds: Set<Long>) {
        if (habitIds.isEmpty()) return

        dataStore.edit { preferences ->
            val key = bucketKey(bucketKey)
            val remaining = decode(preferences[key]).filterNot { it.habitId in habitIds }
            if (remaining.isEmpty()) {
                preferences.remove(key)
            } else {
                preferences[key] = encode(remaining)
            }
        }
    }

    override suspend fun clearBucket(bucketKey: Long) {
        dataStore.edit { preferences ->
            preferences.remove(bucketKey(bucketKey))
        }
    }

    private fun encode(reminders: List<PendingReminder>): String {
        return adapter.toJson(reminders)
    }

    private fun decode(raw: String?): List<PendingReminder> {
        return raw?.let(adapter::fromJson).orEmpty()
    }

    private companion object {
        fun bucketKey(bucketKey: Long): Preferences.Key<String> {
            return stringPreferencesKey("pending_reminder_bucket_$bucketKey")
        }
    }
}
