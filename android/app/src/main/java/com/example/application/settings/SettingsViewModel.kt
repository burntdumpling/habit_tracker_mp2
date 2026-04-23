package com.example.application.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.application.data.session.SessionManager
import com.example.application.data.settings.HabitStatusFilter
import com.example.application.data.settings.SettingsPreferencesRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val sessionManager: SessionManager,
    private val settingsPreferencesRepository: SettingsPreferencesRepository,
    appVersionName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            appVersionLabel = buildVersionLabel(appVersionName)
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            settingsPreferencesRepository.habitStatusFilter.collect { filter ->
                _uiState.update {
                    it.copy(
                        selectedHabitStatusFilter = filter,
                        habitStatusFilterSubtitle = filter.asSubtitle()
                    )
                }
            }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.LogoutClicked -> {
                _uiState.update {
                    it.copy(
                        activeDialog = SettingsDialogType.LOGOUT_CONFIRM,
                        dialogTitle = "Выйти из аккаунта?",
                        dialogMessage = "Вы завершите текущую сессию на этом устройстве."
                    )
                }
            }

            SettingsAction.ConfirmLogout -> handleLogout()

            SettingsAction.NotificationsClicked -> emitEvent(SettingsEvent.OpenNotificationSettings)

            is SettingsAction.SyncNotificationsEnabled -> {
                _uiState.update {
                    it.copy(
                        notificationsEnabledInSystem = action.enabled,
                        notificationsSubtitle = if (action.enabled) {
                            "Разрешены в системе"
                        } else {
                            "Отключены в системе"
                        }
                    )
                }
            }

            SettingsAction.OpenSupport -> {
                _uiState.update {
                    it.copy(
                        activeDialog = SettingsDialogType.SUPPORT,
                        dialogTitle = "Поддержка",
                        dialogMessage = it.supportEmail
                    )
                }
            }

            SettingsAction.SupportSendEmailClicked -> {
                emitEvent(SettingsEvent.OpenEmailClient(_uiState.value.supportEmail))
                dismissDialog()
            }

            SettingsAction.SupportCopyEmailClicked -> {
                emitEvent(SettingsEvent.CopyToClipboard(_uiState.value.supportEmail))
                emitEvent(SettingsEvent.ShowMessage("Email скопирован"))
                dismissDialog()
            }

            SettingsAction.OpenStatusFilter -> {
                _uiState.update {
                    it.copy(
                        activeDialog = SettingsDialogType.STATUS_FILTER,
                        dialogTitle = "Отображение привычек",
                        dialogMessage = "Выберите какие статусы показывать на главном экране"
                    )
                }
            }

            is SettingsAction.SelectHabitStatusFilter -> {
                persistStatusFilter(action.filter)
                dismissDialog()
            }

            SettingsAction.OpenSecurityPlaceholder -> openPlaceholder(
                title = "Безопасность",
                message = "Функция не реализована в текущей версии."
            )

            SettingsAction.OpenArchivePlaceholder -> openPlaceholder(
                title = "Архив",
                message = "Архив привычек появится в следующих обновлениях."
            )

            SettingsAction.OpenPrivacyPlaceholder -> openPlaceholder(
                title = "Политика конфиденциальности",
                message = "Раздел готовится и будет добавлен в ближайших релизах."
            )

            SettingsAction.OpenSharePlaceholder -> openPlaceholder(
                title = "Поделиться",
                message = "Функция скоро будет доступна."
            )

            SettingsAction.OpenFeedbackPlaceholder -> openPlaceholder(
                title = "Предложения",
                message = "Канал предложений готовится. Пока можно написать в поддержку."
            )

            SettingsAction.DismissDialog -> dismissDialog()
        }
    }

    private fun persistStatusFilter(filter: HabitStatusFilter) {
        viewModelScope.launch {
            settingsPreferencesRepository.setHabitStatusFilter(filter)
        }
    }

    private fun handleLogout() {
        sessionManager.logout()
        dismissDialog()
        emitEvent(SettingsEvent.Logout)
    }

    private fun openPlaceholder(title: String, message: String) {
        _uiState.update {
            it.copy(
                activeDialog = SettingsDialogType.PLACEHOLDER,
                dialogTitle = title,
                dialogMessage = message
            )
        }
    }

    private fun dismissDialog() {
        _uiState.update {
            it.copy(
                activeDialog = SettingsDialogType.NONE,
                dialogTitle = "",
                dialogMessage = ""
            )
        }
    }

    private fun emitEvent(event: SettingsEvent) {
        _events.tryEmit(event)
    }

    private fun buildVersionLabel(versionName: String): String {
        val safeVersion = versionName.takeIf { it.isNotBlank() } ?: "unknown"
        return "Версия: $safeVersion"
    }
}

private fun HabitStatusFilter.asSubtitle(): String {
    return when (this) {
        HabitStatusFilter.ALL -> "Все привычки"
        HabitStatusFilter.ACTIVE -> "Только активные"
        HabitStatusFilter.PAUSED -> "Только приостановленные"
        HabitStatusFilter.FINISHED -> "Только завершённые"
    }
}
