package com.example.riegosostenible.network

// Esta data class es el "espejo" de nuestro JSON de respuesta
// {"message": "..."}
data class AuthResponse(
    val message: String
)