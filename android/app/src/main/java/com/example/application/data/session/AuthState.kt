package com.example.application.data.session

import com.example.application.data.auth.UserOut

sealed interface AuthState {
    data object Loading : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val user: UserOut) : AuthState
}
