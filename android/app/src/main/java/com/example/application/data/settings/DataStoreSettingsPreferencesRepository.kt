package com.example.application.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings_prefs")

class DataStoreSettingsPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) : SettingsPreferencesRepository {

    constructor(context: Context) : this(context.settingsDataStore)

    override val habitStatusFilter: Flow<HabitStatusFilter> = dataStore.data
        .map { preferences ->
            parseFilter(preferences[HABIT_STATUS_FILTER_KEY])
        }

    override suspend fun setHabitStatusFilter(filter: HabitStatusFilter) {
        dataStore.edit { preferences ->
            preferences[HABIT_STATUS_FILTER_KEY] = filter.name
        }
    }

    private fun parseFilter(value: String?): HabitStatusFilter {
        return value
            ?.let {
                runCatching { HabitStatusFilter.valueOf(it) }.getOrNull()
            }
            ?: HabitStatusFilter.ALL
    }

    private companion object {
        val HABIT_STATUS_FILTER_KEY: Preferences.Key<String> = stringPreferencesKey("habit_status_filter")
    }
}
