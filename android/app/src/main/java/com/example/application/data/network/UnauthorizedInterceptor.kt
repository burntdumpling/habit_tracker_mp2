package com.example.application.data.network

import com.example.application.data.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class UnauthorizedInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            sessionManager.logout()
        }
        return response
    }
}
