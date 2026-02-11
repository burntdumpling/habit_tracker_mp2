package com.example.application.data.network

import retrofit2.HttpException
import java.io.IOException

fun toUserMessage(e: Throwable): String {
    return when (e) {
        is HttpException -> {
            val body = e.response()?.errorBody()?.string()
            val parsed = parseApiError(body)
            if (parsed.isNotBlank()) parsed else "HTTP ${e.code()}"
        }
        is IOException -> "Проблема с сетью"
        else -> "Неизвестная ошибка"
    }
}
