package com.example.application.data.settings

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsPreferencesRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun habit_status_filter_maps_to_api_status() {
        assertNull(HabitStatusFilter.ALL.toApiStatusOrNull())
        assertEquals("ACTIVE", HabitStatusFilter.ACTIVE.toApiStatusOrNull())
        assertEquals("PAUSED", HabitStatusFilter.PAUSED.toApiStatusOrNull())
        assertEquals("FINISHED", HabitStatusFilter.FINISHED.toApiStatusOrNull())
    }

    @Test
    fun default_filter_is_all() = runTest {
        val repository = createRepository()

        assertEquals(HabitStatusFilter.ALL, repository.habitStatusFilter.first())
    }

    @Test
    fun reads_and_writes_filter() = runTest {
        val repository = createRepository()

        repository.setHabitStatusFilter(HabitStatusFilter.PAUSED)

        assertEquals(HabitStatusFilter.PAUSED, repository.habitStatusFilter.first())
    }

    @Test
    fun corrupted_filter_value_falls_back_to_all() = runTest {
        val dataStore = createDataStore()
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("habit_status_filter")] = "BROKEN"
        }
        val repository = DataStoreSettingsPreferencesRepository(dataStore)

        assertEquals(HabitStatusFilter.ALL, repository.habitStatusFilter.first())
    }

    private fun createRepository(): DataStoreSettingsPreferencesRepository {
        return DataStoreSettingsPreferencesRepository(createDataStore())
    }

    private fun createDataStore() = PreferenceDataStoreFactory.create {
        temporaryFolder.newFile("settings_${System.nanoTime()}.preferences_pb")
    }
}
