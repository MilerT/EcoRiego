package com.example.riegosostenible.network

// OJO: Los nombres de las variables (timestamp, humedad)
// deben ser IDÉNTICOS a los del DTO de Spring Boot
data class HistorialData(
    val timestamp: String, // Recibimos la fecha como String de la API
    val humedad: Double
)