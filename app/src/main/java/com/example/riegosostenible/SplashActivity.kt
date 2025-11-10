package com.example.riegosostenible

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager

class SplashActivity : AppCompatActivity() {

    private val SPLASH_TIME_OUT: Long = 3000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({

            // =======================================================
            // ¡ESTE ES EL CÓDIGO CORREGIDO!
            // Ahora inicia LoginActivity en lugar de MainActivity
            // =======================================================
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)

            finish()
        }, SPLASH_TIME_OUT)
    }
}