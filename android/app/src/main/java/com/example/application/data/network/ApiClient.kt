package com.example.application.data.network

import com.example.application.BuildConfig
import com.example.application.data.auth.AuthApi
import com.example.application.data.network.api.HabitApiService
import com.example.application.data.session.SessionManager
import com.example.application.data.storage.TokenStorage
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class ApiClient(
    tokenStorage: TokenStorage,
    sessionManager: SessionManager
) {
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthHeaderInterceptor(tokenStorage))
        .addInterceptor(UnauthorizedInterceptor(sessionManager))
        .apply {
            if (BuildConfig.DEBUG) {
                val logging = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
                addInterceptor(logging)
            }
        }
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(NetworkConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    fun createAuthApi(): AuthApi = retrofit.create(AuthApi::class.java)

    fun createHabitApi(): HabitApiService = retrofit.create(HabitApiService::class.java)
}
