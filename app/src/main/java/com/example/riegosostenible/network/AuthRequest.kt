package com.example.riegosostenible.network

data class RegisterRequest(
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

