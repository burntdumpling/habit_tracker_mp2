package com.example.application.data.storage

interface TokenStorage {
    fun getToken(): String?
    fun saveToken(token: String)
    fun clear()
}
