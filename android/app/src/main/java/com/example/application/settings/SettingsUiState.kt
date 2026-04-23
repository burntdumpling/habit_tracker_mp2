package com.example.application.settings

import com.example.application.data.settings.HabitStatusFilter

data class SettingsUiState(
    val notificationsEnabledInSystem: Boolean = false,
    val notificationsSubtitle: String = "Статус неизвестен",
    val selectedHabitStatusFilter: HabitStatusFilter = HabitStatusFilter.ALL,
    val habitStatusFilterSubtitle: String = "Все привычки",
    val activeDialog: SettingsDialogType = SettingsDialogType.NONE,
    val dialogTitle: String = "",
    val dialogMessage: String = "",
    val supportEmail: String = "khusainov@sfedu.ru",
    val appVersionLabel: String = "Версия: -"
)

enum class SettingsDialogType {
    NONE,
    PLACEHOLDER,
    SUPPORT,
    LOGOUT_CONFIRM,
    STATUS_FILTER
}

sealed interface SettingsAction {
    data object LogoutClicked : SettingsAction
    data object ConfirmLogout : SettingsAction
    data object NotificationsClicked : SettingsAction
    data class SyncNotificationsEnabled(val enabled: Boolean) : SettingsAction
    data object OpenSupport : SettingsAction
    data object SupportSendEmailClicked : SettingsAction
    data object SupportCopyEmailClicked : SettingsAction
    data object OpenStatusFilter : SettingsAction
    data class SelectHabitStatusFilter(val filter: HabitStatusFilter) : SettingsAction
    data object OpenSecurityPlaceholder : SettingsAction
    data object OpenArchivePlaceholder : SettingsAction
    data object OpenPrivacyPlaceholder : SettingsAction
    data object OpenSharePlaceholder : SettingsAction
    data object OpenFeedbackPlaceholder : SettingsAction
    data object DismissDialog : SettingsAction
}

sealed interface SettingsEvent {
    data object Logout : SettingsEvent
    data object OpenNotificationSettings : SettingsEvent
    data class OpenEmailClient(val email: String) : SettingsEvent
    data class CopyToClipboard(val value: String) : SettingsEvent
    data class ShowMessage(val message: String) : SettingsEvent
}
