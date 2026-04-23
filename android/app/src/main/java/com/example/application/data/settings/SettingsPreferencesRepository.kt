package com.example.application.data.settings

import kotlinx.coroutines.flow.Flow

interface SettingsPreferencesRepository {
    val habitStatusFilter: Flow<HabitStatusFilter>
    suspend fun setHabitStatusFilter(filter: HabitStatusFilter)
}

enum class HabitStatusFilter {
    ALL,
    ACTIVE,
    PAUSED,
    FINISHED;

    fun toApiStatusOrNull(): String? = if (this == ALL) null else name
}
