package com.example.application.data.auth

import com.example.application.data.session.SessionManager
import com.example.application.data.storage.TokenStorage
import retrofit2.HttpException

class AuthRepository(
    private val authApi: AuthApi,
    private val tokenStorage: TokenStorage,
    private val sessionManager: SessionManager
) {
    suspend fun register(email: String, password: String): UserOut {
        return authApi.register(RegisterRequest(email, password))
    }

    suspend fun login(email: String, password: String): UserOut {
        val tokenOut = authApi.login(username = email, password = password)
        tokenStorage.saveToken(tokenOut.access_token)
        val me = authApi.me("Bearer ${tokenOut.access_token}")
        sessionManager.setAuthenticated(me)
        return me
    }

    suspend fun bootstrap() {
        val token = tokenStorage.getToken().orEmpty()
        if (token.isBlank()) {
            sessionManager.setUnauthenticated()
            return
        }

        try {
            val me = authApi.me("Bearer $token")
            sessionManager.setAuthenticated(me)
        } catch (e: HttpException) {
            if (e.code() == 401) {
                sessionManager.logout()
            } else {
                throw e
            }
        }
    }

    fun logout() {
        sessionManager.logout()
    }
}
