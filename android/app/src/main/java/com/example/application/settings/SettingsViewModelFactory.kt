package com.example.application.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.application.data.notifications.NotificationSettingsRepository
import com.example.application.data.session.SessionManager
import com.example.application.data.settings.SettingsPreferencesRepository
import com.example.application.notifications.NotificationSchedulingCoordinator

class SettingsViewModelFactory(
    private val sessionManager: SessionManager,
    private val settingsPreferencesRepository: SettingsPreferencesRepository,
    private val notificationSettingsRepository: NotificationSettingsRepository,
    private val notificationSchedulingCoordinator: NotificationSchedulingCoordinator,
    private val appVersionName: String
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(
                sessionManager = sessionManager,
                settingsPreferencesRepository = settingsPreferencesRepository,
                notificationSettingsRepository = notificationSettingsRepository,
                notificationSchedulingCoordinator = notificationSchedulingCoordinator,
                appVersionName = appVersionName
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
