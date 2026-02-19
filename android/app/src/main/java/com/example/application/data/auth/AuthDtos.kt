package com.example.application.data.auth

data class RegisterRequest(
    val email: String,
    val password: String
)

data class UserOut(
    val id: String,
    val email: String
)

data class TokenOut(
    val access_token: String,
    val token_type: String
)
