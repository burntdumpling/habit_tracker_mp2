package com.example.application.data.network

import org.json.JSONArray
import org.json.JSONObject

private const val DEFAULT_ERROR_MESSAGE = "Ошибка запроса"

fun parseApiError(errorBody: String?): String {
    if (errorBody.isNullOrBlank()) return DEFAULT_ERROR_MESSAGE

    return try {
        val root = JSONObject(errorBody)
        if (!root.has("detail")) return DEFAULT_ERROR_MESSAGE

        val detail = root.get("detail")
        when (detail) {
            is String -> detail
            is JSONArray -> {
                val messages = mutableListOf<String>()
                for (i in 0 until detail.length()) {
                    val item = detail.optJSONObject(i) ?: continue
                    val msg = item.optString("msg", "Validation error")
                    val locArray = item.optJSONArray("loc")
                    val path = if (locArray != null) {
                        buildString {
                            for (j in 0 until locArray.length()) {
                                val part = locArray.optString(j)
                                if (part.isNotBlank()) {
                                    if (isNotEmpty()) append('.')
                                    append(part)
                                }
                            }
                        }
                    } else {
                        ""
                    }
                    messages.add(if (path.isNotBlank()) "$path: $msg" else msg)
                }
                if (messages.isNotEmpty()) messages.joinToString("\n") else DEFAULT_ERROR_MESSAGE
            }
            else -> DEFAULT_ERROR_MESSAGE
        }
    } catch (_: Exception) {
        DEFAULT_ERROR_MESSAGE
    }
}
