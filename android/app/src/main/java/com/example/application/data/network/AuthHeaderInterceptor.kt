package com.example.application.data.network

import com.example.application.data.storage.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response

class AuthHeaderInterceptor(
    private val tokenStorage: TokenStorage
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = tokenStorage.getToken().orEmpty()

        if (token.isBlank()) {
            return chain.proceed(original)
        }

        val newRequest = original.newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()

        return chain.proceed(newRequest)
    }
}
