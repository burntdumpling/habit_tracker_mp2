package com.example.application.data

import android.content.Context
import com.example.application.data.auth.AuthApi
import com.example.application.data.auth.AuthRepository
import com.example.application.data.network.ApiClient
import com.example.application.data.session.SessionManager
import com.example.application.data.storage.SharedPrefsTokenStorage
import com.example.application.data.storage.TokenStorage

class AppContainer(
    context: Context
) {
    private val appContext = context.applicationContext

    val tokenStorage: TokenStorage = SharedPrefsTokenStorage(appContext)
    val sessionManager: SessionManager = SessionManager(tokenStorage)
    private val apiClient: ApiClient = ApiClient(tokenStorage, sessionManager)
    val authApi: AuthApi = apiClient.createAuthApi()
    val authRepository: AuthRepository = AuthRepository(authApi, tokenStorage, sessionManager)
    
    val habitsRepository: com.example.application.data.habits.HabitsRepository = com.example.application.data.habits.InMemoryHabitsRepository()
}
