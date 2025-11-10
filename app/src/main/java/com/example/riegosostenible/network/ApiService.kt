package com.example.riegosostenible.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    // --- Login / Registro ---
    @POST("/api/auth/registrar")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    // ===================================
    // ¡AÑADIR ESTA FUNCIÓN PARA EL GRÁFICO!
    @GET("/api/humedad/historial-demo")
    suspend fun getHistorialDemo(): Response<List<HistorialData>>
    // ===================================
}