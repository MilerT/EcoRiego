package com.example.riegosostenible.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    // =================================================================
    // ¡¡¡CAMBIA ESTA IP!!! (Usa tu IP local, no localhost)
    // Ejemplo: "http://192.168.1.10:8080/"
    // ¡¡¡ASEGÚRATE DE QUE LA BARRA / ESTÉ AL FINAL!!!
    // =================================================================
    private const val BASE_URL = "http://192.168.20.119:8080/"

    // =================================================================
    // ¡¡AQUÍ ESTÁ LA LÍNEA QUE FALTABA!!
    // Hacemos pública la herramienta "gson" para que LoginActivity pueda usarla
    // =================================================================
    val gson: Gson = GsonBuilder().create()

    // El cliente Retrofit
    private val retrofit: Retrofit = Retrofit.Builder() 
        .baseUrl(BASE_URL)
        // Le decimos a Retrofit que use nuestra variable gson
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    // La implementación de nuestra interfaz ApiService
    val apiService: ApiService = retrofit.create(ApiService::class.java)
}