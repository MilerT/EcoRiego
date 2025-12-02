package com.example.riegosostenible.network

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // URL base de tu API. ¡Asegúrate de que la IP sea la correcta para tu red!
    // Si usas un emulador de Android, usa 10.0.2.2 para referirte al 'localhost' de tu PC.
    private const val BASE_URL = "http://172.22.134.221:8080/"

    // --- SOLUCIÓN AL ERROR ---
    // Hacemos 'gson' una propiedad PÚBLICA usando 'val'.
    // Ahora será accesible desde cualquier parte de la app (ej: LoginActivity).
    // 'by lazy' asegura que se cree una sola vez, cuando se usa por primera vez.
    val gson: Gson by lazy {
        Gson()
    }

    // (Recomendado) Interceptor para ver las peticiones a la API en el Logcat.
    // Esto es muy útil para depurar y ver qué envía y recibe tu app.
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Cliente OkHttp que usa el interceptor
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    // Instancia de Retrofit configurada para usar nuestra instancia de Gson y el logger
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // Usamos el cliente con el logger
            .addConverterFactory(GsonConverterFactory.create(gson)) // Usamos nuestra instancia de gson
            .build()
    }

    // El servicio de la API que usarán tus Activities, también con inicialización 'lazy'
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
