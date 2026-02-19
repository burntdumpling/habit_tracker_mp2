package com.example.application.data.storage

import android.content.Context

class SharedPrefsTokenStorage(
    context: Context
) : TokenStorage {
    private val prefs = context.applicationContext.getSharedPreferences(
        "auth_prefs",
        Context.MODE_PRIVATE
    )

    override fun getToken(): String? = prefs.getString("access_token", null)

    override fun saveToken(token: String) {
        prefs.edit().putString("access_token", token).apply()
    }

    override fun clear() {
        prefs.edit().remove("access_token").apply()
    }
}
