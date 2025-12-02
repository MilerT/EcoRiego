package com.example.riegosostenible

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.riegosostenible.databinding.ActivityLoginBinding
import com.example.riegosostenible.network.ApiClient
import com.example.riegosostenible.network.AuthResponse
import com.example.riegosostenible.network.LoginRequest
import com.example.riegosostenible.network.RegisterRequest
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val apiService = ApiClient.apiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Asignar funciones a los botones
        binding.loginButton.setOnClickListener {
            handleLogin()
        }

        binding.registerButton.setOnClickListener {
            handleRegister()
        }
    }

    // --- Lógica de INICIO DE SESIÓN ---
    private fun handleLogin() {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            showToast("Por favor, ingresa email y contraseña")
            return
        }

        val loginRequest = LoginRequest(email, password)

        lifecycleScope.launch {
            try {
                val response = apiService.login(loginRequest)

                if (response.isSuccessful) {
                    // Éxito: Leemos el mensaje del JSON
                    val message = response.body()?.message ?: "Inicio de sesión exitoso"
                    showToast(message)
                    // Ir a la pantalla principal
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Error (ej: 401 Credenciales inválidas)
                    val errorBody = response.errorBody()?.string()
                    // Intentamos leer el JSON de error
                    val errorMessage = try {
                        ApiClient.gson.fromJson(errorBody, AuthResponse::class.java).message
                    } catch (e: Exception) {
                        "Error al iniciar sesión"
                    }
                    showToast(errorMessage)
                }
            } catch (e: IOException) {
                showToast("Error de red. ¿Tu API está encendida y estás en la misma red WiFi?")
                Log.e("LoginActivity", "Error de red", e)
            } catch (e: HttpException) {
                showToast("Error del servidor (HTTP).")
                Log.e("LoginActivity", "Error HTTP", e)
            }
        }
    }

    // --- Lógica de REGISTRO ---
    private fun handleRegister() {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            showToast("Por favor, ingresa email y contraseña")
            return
        }

        val registerRequest = RegisterRequest(email, password)

        lifecycleScope.launch {
            try {
                val response = apiService.register(registerRequest)

                if (response.isSuccessful) {
                    // Éxito: Leemos el mensaje del JSON
                    val message = response.body()?.message ?: "Registro exitoso"
                    showToast(message)
                    // Ir a la pantalla principal
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Error (ej: 400 Usuario ya existe)
                    val errorBody = response.errorBody()?.string()
                    // Intentamos leer el JSON de error
                    val errorMessage = try {
                        ApiClient.gson.fromJson(errorBody, AuthResponse::class.java).message
                    } catch (e: Exception) {
                        "Error en el registro"
                    }
                    showToast(errorMessage)
                }
            } catch (e: IOException) {
                showToast("Error de red. ¿Tu API está encendida y estás en la misma red WiFi?")
                Log.e("LoginActivity", "Error de red", e)
            } catch (e: HttpException) {
                showToast("Error del servidor (HTTP).")
                Log.e("LoginActivity", "Error HTTP", e)
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}