package com.example.application.data.session

import com.example.application.data.auth.UserOut
import com.example.application.data.storage.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(
    private val tokenStorage: TokenStorage
) {
    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun setAuthenticated(user: UserOut) {
        _state.value = AuthState.Authenticated(user)
    }

    fun setUnauthenticated() {
        _state.value = AuthState.Unauthenticated
    }

    fun logout() {
        tokenStorage.clear()
        _state.value = AuthState.Unauthenticated
    }
}
