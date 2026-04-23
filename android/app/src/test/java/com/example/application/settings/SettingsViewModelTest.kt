package com.example.application.settings

import com.example.application.data.auth.UserOut
import com.example.application.data.session.AuthState
import com.example.application.data.session.SessionManager
import com.example.application.data.settings.HabitStatusFilter
import com.example.application.data.settings.SettingsPreferencesRepository
import com.example.application.data.storage.TokenStorage
import com.example.application.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tokenStorage = FakeTokenStorage()
    private val sessionManager = SessionManager(tokenStorage)
    private val settingsRepository = FakeSettingsPreferencesRepository()

    @Test
    fun logout_confirm_opens_and_closes_dialog() = runTest {
        val viewModel = createViewModel()

        viewModel.onAction(SettingsAction.LogoutClicked)

        assertEquals(SettingsDialogType.LOGOUT_CONFIRM, viewModel.uiState.value.activeDialog)

        viewModel.onAction(SettingsAction.DismissDialog)

        assertEquals(SettingsDialogType.NONE, viewModel.uiState.value.activeDialog)
    }

    @Test
    fun confirm_logout_clears_token_and_emits_logout_event() = runTest {
        tokenStorage.saveToken("token")
        sessionManager.setAuthenticated(UserOut(id = "user", email = "user@example.com"))
        val viewModel = createViewModel()
        val events = mutableListOf<SettingsEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.take(1).toList(events)
        }

        viewModel.onAction(SettingsAction.ConfirmLogout)
        advanceUntilIdle()

        assertEquals(listOf(SettingsEvent.Logout), events)
        assertNull(tokenStorage.getToken())
        assertEquals(AuthState.Unauthenticated, sessionManager.state.value)
    }

    @Test
    fun support_dialog_opens_and_send_email_emits_event() = runTest {
        val viewModel = createViewModel()

        viewModel.onAction(SettingsAction.OpenSupport)

        assertEquals(SettingsDialogType.SUPPORT, viewModel.uiState.value.activeDialog)

        val events = mutableListOf<SettingsEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.take(1).toList(events)
        }
        viewModel.onAction(SettingsAction.SupportSendEmailClicked)
        advanceUntilIdle()

        assertEquals(listOf(SettingsEvent.OpenEmailClient("khusainov@sfedu.ru")), events)
        assertEquals(SettingsDialogType.NONE, viewModel.uiState.value.activeDialog)
    }

    @Test
    fun copy_support_email_emits_copy_and_message_events() = runTest {
        val viewModel = createViewModel()
        val events = mutableListOf<SettingsEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.take(2).toList(events)
        }

        viewModel.onAction(SettingsAction.SupportCopyEmailClicked)
        advanceUntilIdle()

        assertEquals(
            listOf(
                SettingsEvent.CopyToClipboard("khusainov@sfedu.ru"),
                SettingsEvent.ShowMessage("Email скопирован")
            ),
            events
        )
    }

    @Test
    fun notifications_click_emits_open_settings_event() = runTest {
        val viewModel = createViewModel()
        val events = mutableListOf<SettingsEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.take(1).toList(events)
        }

        viewModel.onAction(SettingsAction.NotificationsClicked)
        advanceUntilIdle()

        assertEquals(listOf(SettingsEvent.OpenNotificationSettings), events)
    }

    @Test
    fun status_filter_dialog_opens_and_closes() = runTest {
        val viewModel = createViewModel()

        viewModel.onAction(SettingsAction.OpenStatusFilter)

        assertEquals(SettingsDialogType.STATUS_FILTER, viewModel.uiState.value.activeDialog)

        viewModel.onAction(SettingsAction.DismissDialog)

        assertEquals(SettingsDialogType.NONE, viewModel.uiState.value.activeDialog)
    }

    @Test
    fun selecting_status_filter_persists_and_closes_dialog() = runTest {
        val viewModel = createViewModel()
        viewModel.onAction(SettingsAction.OpenStatusFilter)

        viewModel.onAction(SettingsAction.SelectHabitStatusFilter(HabitStatusFilter.PAUSED))

        assertEquals(HabitStatusFilter.PAUSED, settingsRepository.savedFilter)
        assertEquals(SettingsDialogType.NONE, viewModel.uiState.value.activeDialog)
    }

    @Test
    fun ui_state_updates_from_repository_flow() = runTest {
        val viewModel = createViewModel()

        settingsRepository.emit(HabitStatusFilter.FINISHED)

        assertEquals(HabitStatusFilter.FINISHED, viewModel.uiState.value.selectedHabitStatusFilter)
    }

    private fun createViewModel(): SettingsViewModel {
        return SettingsViewModel(
            sessionManager = sessionManager,
            settingsPreferencesRepository = settingsRepository,
            appVersionName = "1.2.3"
        )
    }

    private class FakeSettingsPreferencesRepository : SettingsPreferencesRepository {
        private val filter = MutableStateFlow(HabitStatusFilter.ALL)
        var savedFilter: HabitStatusFilter? = null

        override val habitStatusFilter = filter

        override suspend fun setHabitStatusFilter(filter: HabitStatusFilter) {
            savedFilter = filter
            this.filter.value = filter
        }

        fun emit(filter: HabitStatusFilter) {
            this.filter.value = filter
        }
    }

    private class FakeTokenStorage : TokenStorage {
        private var token: String? = null

        override fun getToken(): String? = token

        override fun saveToken(token: String) {
            this.token = token
        }

        override fun clear() {
            token = null
        }
    }
}
